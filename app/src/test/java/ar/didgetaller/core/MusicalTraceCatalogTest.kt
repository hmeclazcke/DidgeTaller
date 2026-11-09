package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class MusicalTraceCatalogTest {
    private class Pending(val request: ComponentRequest, val complete: (Result<DroneComponentCurve>) -> Unit) : AnalysisTask {
        var cancelled = false
        override fun cancel() { cancelled = true }
    }
    private val tasks = mutableListOf<Pending>()
    private val snapshots = mutableListOf<TraceSnapshot>()
    private val loader = AnalysisLoader<ComponentRequest, DroneComponentCurve> { r, _, complete ->
        Result.success(Pending(r, complete).also { tasks += it })
    }
    private val catalog = MusicalTraceCatalog(loader, snapshots::add, 2)
    private fun request(id: String, duration: Long = 1000, profile: InstrumentProfile = InstrumentProfile.DIDGERIDOO) =
        MusicalTraceRequest(id, duration, emptyList(), profile)
    private val curve = DroneComponentCurve(emptyList(), 10.0)
    private fun complete(id: String) { catalog.request(request(id)); tasks.last().complete(Result.success(curve)) }

    @Test fun canonicalRequestsAreKeyedCachedAndRetainTheirOwnMetadata() {
        val a = request("A"); val b = request("B", 2000)
        catalog.request(a); catalog.request(b); catalog.request(a)
        assertEquals(2, tasks.size)
        tasks[1].complete(Result.success(curve)); tasks[0].complete(Result.success(curve))
        assertEquals(1000L, catalog.trace(a)?.durationMs); assertEquals(2000L, catalog.trace(b)?.durationMs)
        assertEquals(MusicalTrace.Meaning.SPECTRAL_COMPONENT, catalog.trace(a)?.meaning)
        catalog.request(a); assertEquals(2, tasks.size)
        assertNull(catalog.trace(a.copy(durationMs = 3000)))
        catalog.request(a.copy(durationMs = 3000)); tasks.last().complete(Result.success(curve))
        assertEquals(3000L, catalog.trace(a.copy(durationMs = 3000))?.durationMs)
    }
    @Test fun removingAndReopeningCannotPublishStaleOrDuplicateOutcomes() {
        val a = request("A"); catalog.request(a); val old = tasks.single()
        catalog.remove("A"); assertTrue(old.cancelled)
        catalog.request(a); val current = tasks.last(); val count = snapshots.size
        old.complete(Result.success(curve)); old.complete(Result.failure(Exception("old")))
        assertEquals(count, snapshots.size); assertNull(catalog.trace(a))
        current.complete(Result.success(curve)); current.complete(Result.failure(Exception("duplicate")))
        assertNotNull(catalog.trace(a)); assertFalse("A" in catalog.snapshot.failures)
    }
    @Test fun pinnedAndCurrentProductsRemainWithinTheBoundedDecodedCache() {
        assertThrows(IllegalArgumentException::class.java) { MusicalTraceCatalog(loader, {}, 1) }
        catalog.pin("A"); complete("A"); complete("B"); complete("C")
        assertEquals(setOf("A", "C"), catalog.snapshot.products.keys)
        assertNull(catalog.trace(request("B")))
        catalog.pin(null); complete("D"); assertEquals(setOf("C", "D"), catalog.snapshot.products.keys)
    }
    @Test fun releaseKeepsPinnedWorkButRevokesUnselectedFailuresAndAllowsRetry() {
        catalog.pin("A"); catalog.request(request("A")); val current = tasks.single()
        catalog.release("A"); assertFalse(current.cancelled)
        current.complete(Result.failure(Exception("invalid"))); assertTrue("A" in catalog.snapshot.failures)
        catalog.request(request("A")); assertEquals(1, tasks.size)
        catalog.pin(null); catalog.release("A"); assertFalse("A" in catalog.snapshot.failures)
        catalog.request(request("A")); assertEquals(2, tasks.size)
        catalog.release("A"); assertTrue(tasks.last().cancelled)
        tasks.last().complete(Result.success(curve)); assertNull(catalog.trace(request("A")))
    }
    @Test fun closeRetainsValidEvidenceButRejectsAllFurtherPublicationAndAdmission() {
        complete("A"); catalog.request(request("B")); val count = snapshots.size
        catalog.close(); assertTrue(tasks.last().cancelled)
        tasks.last().complete(Result.success(curve)); catalog.request(request("C"))
        catalog.remove("A"); catalog.release("A"); catalog.pin("B"); catalog.close()
        assertEquals(count, snapshots.size); assertEquals(2, tasks.size)
        assertNotNull(catalog.trace(request("A"))); assertNull(catalog.trace(request("B")))
    }
    @Test fun fluteIsSynchronousHistoricalEvidenceWithBoundedMemoizationAndNoWavRequest() {
        val flute = request("F", profile = InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        catalog.request(flute); assertTrue(tasks.isEmpty())
        val result = catalog.trace(flute)
        assertEquals(MusicalTrace.Meaning.ESTIMATED_PITCH, result?.meaning)
        assertSame(result, catalog.trace(flute))
        catalog.trace(flute.copy(sessionId = "G")); catalog.trace(flute.copy(sessionId = "H"))
        assertNotSame(result, catalog.trace(flute)); catalog.remove("F")
        assertTrue(tasks.isEmpty())
    }
    @Test fun removalPublishesTheMissingProductToObserversAndInvalidatesFluteMemoization() {
        complete("A");assertNotNull(snapshots.last().products["A"])
        catalog.remove("A");assertFalse(snapshots.last().products.containsKey("A"))
        val flute=request("F",profile=InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        val before=catalog.trace(flute);val other=catalog.trace(flute.copy(sessionId="G"))
        catalog.remove("F")
        assertNotSame(before,catalog.trace(flute));assertSame(other,catalog.trace(flute.copy(sessionId="G")))
    }

}
