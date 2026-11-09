package ar.didgetaller.core

import java.io.Closeable
import java.util.concurrent.CancellationException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class WorkspaceDataTest {
    @Test fun waveAmplitudeZoomNeverBecomesAFrequencyRequest() {
        val viewport = PlotViewport(AxisWindow(100.0, 123.0), AxisWindow(-.8, -.3))
        val wave = VisualRequest.forView(VisualKind.WAVE, viewport, true, 50.0, .5)
        assertEquals(VisualRequest(100, 123), wave)
        assertEquals(wave, VisualRequest.forView(VisualKind.WAVE, viewport.copy(y = AxisWindow(.2, .7))))
        val spectral = VisualRequest.forView(VisualKind.SPECTROGRAM, viewport.copy(y = AxisWindow(70.0, 300.0)), true, 50.0, .5)
        assertEquals(VisualRequest(100, 123, 70.0, 300.0, true, 50.0, .5), spectral)
    }
    @Test fun publishedFactsHaveOneSourceAndCancellationHasAnExplicitState() {
        val empty = WorkspaceState<String>()
        assertEquals(0L, empty.durationMs); assertEquals(0L, empty.frames); assertEquals(44100, empty.rate)
        assertEquals("", empty.signature); assertNull(empty.level); assertNull(empty.error); assertFalse(empty.loading)
        val level = AudioLevel(48000, 96000, 4800, floatArrayOf(.5f))
        val source = WorkspaceSource("A", 96001, 48000, "actual", level)
        val ready = empty.copy(source = source, sessionId = "A", status = WorkspaceStatus.Ready)
        assertEquals(2000L, ready.durationMs); assertEquals(96001L, ready.frames); assertEquals(48000, ready.rate)
        assertEquals("actual", ready.signature); assertSame(level, ready.level)
        assertTrue(ready.copy(status = WorkspaceStatus.Opening).loading)
        assertTrue(ready.copy(status = WorkspaceStatus.Rendering).loading)
        assertEquals("Visualization canceled.", ready.copy(status = WorkspaceStatus.Cancelled).error)
        assertEquals("failed", ready.copy(status = WorkspaceStatus.Failed("failed")).error)
        assertEquals("0-1000-0.0-500.0-false", VisualRequest(0, 1000).dataKey().cacheKey())
        assertEquals(VisualRequest(0, 1000).dataKey(), VisualRequest(0, 1000, rangeDb = 20.0).dataKey())
    }
    private class Source(override val frameCount: Long = 44100, override val sampleRate: Int = 44100,
                         val afterRead: (Long) -> Unit = {}) : MonoPcmSource, Closeable {
        var closed = false
        override fun close() { check(!closed); closed = true }
        override fun read(startFrame: Long, destination: ShortArray): Int {
            check(!closed)
            val count = minOf(destination.size.toLong(), frameCount - startFrame).toInt()
            repeat(count) { destination[it] = (12000 * sin(2 * PI * 137 * (startFrame + it) / sampleRate)).toInt().toShort() }
            afterRead(startFrame + count)
            return count
        }
    }
    private class Harness {
        val readers = mutableListOf<Source>()
        var fingerprint: (String, () -> Boolean) -> String = { _, _ -> "fingerprint" }
        var cacheCalls = 0
        val data = WorkspaceData({ _: String -> Source().also { readers += it } },
            { id, cancelled -> fingerprint(id, cancelled) }, { _: String, _: String, _: String, compute: () -> AudioVisuals.Spectrogram ->
                cacheCalls++; compute()
            })
        fun work(request: VisualRequest = VisualRequest(0, 1000), kind: VisualKind = VisualKind.WAVE) =
            VisualWork(WorkspaceSource("A", 44100, 44100, "fingerprint", null), request, kind, null, null)
    }

    @Test fun openingReadsFactsAndLevelWithoutLeavingAnyReaderOpen() {
        val h = Harness(); val actual = h.data.open("A") { false }
        assertEquals("A", actual.sessionId); assertEquals("fingerprint", actual.signature)
        assertEquals(44100L, actual.frames); assertEquals(44100, actual.rate); assertEquals(1000L, actual.durationMs)
        assertTrue(actual.level!!.rms.all { it > 0 }); assertEquals(10, actual.level.rms.size)
        assertTrue(h.readers.single().closed)
    }

    @Test fun fingerprintFailureAndCancellationCloseTheOwnedReader() {
        val h = Harness(); h.fingerprint = { _, _ -> throw IllegalStateException("storage unavailable") }
        assertThrows(IllegalStateException::class.java) { h.data.open("A") { false } }
        assertTrue(h.readers.last().closed)
        assertThrows(CancellationException::class.java) { h.data.open("A") { true } }
        assertTrue(h.readers.last().closed)
    }

    @Test fun waveIsExactlyTheExistingAlgorithmAndCachedDataIsNotModified() {
        val h = Harness(); val request = VisualRequest(100, 123)
        val expected = Source().use { AudioVisuals.waveform(it, 4410, 5424) }
        val actual = h.data.wave(h.work(request)) { false }
        assertEquals(expected.start, actual.start); assertEquals(expected.end, actual.end)
        assertEquals(expected.samples, actual.samples)
        assertArrayEquals(expected.min, actual.min, 0f); assertArrayEquals(expected.max, actual.max, 0f)
        assertSame(actual, h.data.wave(h.work(request).copy(wave = actual)) { false })
        assertTrue(h.readers.all { it.closed })
    }

    @Test fun spectralPowerAndRasterMatchTheExistingAlgorithmsExactly() {
        val h = Harness(); val work = h.work(kind = VisualKind.SPECTROGRAM); val progress = mutableListOf<Int>()
        val expected = Source().use { AudioVisuals.spectrogram(it, 0, 44100) }
        val actual = h.data.spectral(work, { false }, progress::add)
        assertArrayEquals(expected.power, actual.tile.power, 0f)
        assertArrayEquals(SpectrogramRaster.pixels(expected, 70.0), actual.pixels)
        assertEquals(listOf(100), progress.takeLast(1)); assertEquals(1, h.cacheCalls)
        val recolored = h.data.spectral(work.copy(request = work.request.copy(rangeDb = 20.0), tile = actual.tile), { false }) {}
        assertSame(actual.tile, recolored.tile); assertEquals(1, h.cacheCalls)
        assertArrayEquals(expected.power, recolored.tile.power, 0f)
        assertArrayEquals(SpectrogramRaster.pixels(expected, 20.0), recolored.pixels)
        assertTrue(h.readers.all { it.closed })
    }

    @Test fun logarithmicZoomAndExplicitReferenceUseTheRequestedPhysicalBand() {
        val h = Harness(); val request = VisualRequest(100, 900, 70.0, 300.0, true, 50.0, .1)
        val expected = Source().use { AudioVisuals.spectrogram(it, 4410, 39690, 70.0, 300.0, true) }
        val actual = h.data.spectral(h.work(request, VisualKind.SPECTROGRAM), { false }) {}
        assertEquals(expected.lowHz, actual.tile.lowHz, 0.0); assertEquals(300.0, actual.tile.highHz, 0.0)
        assertTrue(actual.tile.logarithmic); assertArrayEquals(expected.power, actual.tile.power, 0f)
        assertArrayEquals(SpectrogramRaster.pixels(expected, 50.0, .1), actual.pixels)
        assertEquals(100L, request.dataKey().startMs); assertEquals(900L, request.dataKey().endMs)
        assertEquals(70.0, request.dataKey().lowHz, 0.0); assertEquals(300.0, request.dataKey().highHz, 0.0)
        assertTrue(request.dataKey().logarithmic)
    }

    @Test fun cancellationDuringTheLastReadAndThreadInterruptionPreventReturningData() {
        var cancelled = false
        val source = Source(afterRead = { end -> if (end == 44100L) cancelled = true })
        val data = WorkspaceData({ _: String -> source }, { _: String, _: () -> Boolean -> "signature" },
            { _: String, _: String, _: String, compute: () -> AudioVisuals.Spectrogram -> compute() })
        val work = Harness().work()
        assertThrows(CancellationException::class.java) { data.wave(work) { cancelled } }
        assertTrue(source.closed)
        val h = Harness()
        val cached = AudioVisuals.Wave(0, 44100, 44100, floatArrayOf(0f), floatArrayOf(0f), false)
        // Never interrupt JUnit/PIT's shared runner/communication thread.
        val result = FutureTask {
            Thread.currentThread().interrupt()
            try { runCatching { h.data.wave(h.work().copy(wave = cached)) { false } }.exceptionOrNull() }
            finally { Thread.interrupted() }
        }
        val worker = Thread(result, "workspace-interruption-test").apply { isDaemon = true; start() }
        try { assertTrue(result.get(5, TimeUnit.SECONDS) is CancellationException) }
        finally { worker.interrupt(); worker.join(5000) }
        assertTrue(h.readers.single().closed)
    }

    @Test fun sourceReplacementAndCancellationFailBeforePublishingAndCloseResources() {
        val h = Harness()
        assertThrows(IllegalStateException::class.java) { h.data.wave(h.work().copy(source = h.work().source.copy(frames = 44099))) { false } }
        assertThrows(IllegalStateException::class.java) { h.data.wave(h.work().copy(source = h.work().source.copy(rate = 48000))) { false } }
        assertThrows(CancellationException::class.java) { h.data.spectral(h.work(kind = VisualKind.SPECTROGRAM), { true }) {} }
        assertTrue(h.readers.all { it.closed })
    }

    @Test fun frameMappingClampsBeforeMultiplicationAndRetainsAtLeastTwoFrames() {
        val source = WorkspaceSource("A", 44100, 44100, "signature", null)
        assertEquals(VisualFrames(0, 44100), VisualFrames.of(source, VisualRequest(Long.MIN_VALUE, Long.MAX_VALUE)))
        assertEquals(VisualFrames(44098, 44100), VisualFrames.of(source, VisualRequest(1000, Long.MAX_VALUE)))
        assertEquals(VisualFrames(0, 2), VisualFrames.of(source, VisualRequest(-2, -1)))
        assertEquals(VisualFrames(4410, 5424), VisualFrames.of(source, VisualRequest(100, 123)))
        assertEquals(VisualFrames(0, 2), VisualFrames.of(source.copy(frames = 2), VisualRequest(0, 1)))
        assertEquals(0L, source.copy(frames = 0).durationMs)
        assertEquals(1002L, source.copy(frames = 44223).durationMs)
        assertEquals(Double.MAX_VALUE, VisualRequest(0, 1, highHz = Double.MAX_VALUE).highHz, 0.0)
        assertEquals(Double.MAX_VALUE, VisualRequest(0, 1, rangeDb = Double.MAX_VALUE).rangeDb, 0.0)
        assertEquals(Double.MAX_VALUE, VisualRequest(0, 1, reference = Double.MAX_VALUE).reference, 0.0)
        assertThrows(IllegalArgumentException::class.java) { VisualFrames.of(source.copy(frames = 1), VisualRequest(0, 1)) }
    }

    @Test fun invalidExternalRequestsAndMetadataAreRejectedAtTheirBoundary() {
        assertThrows(IllegalArgumentException::class.java) { VisualRequest(1, 1) }
        assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, lowHz = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, highHz = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, rangeDb = Double.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, reference = -1.0) }
        for (bad in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, lowHz = bad) }
            assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, reference = bad) }
        }
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, highHz = bad) }
            assertThrows(IllegalArgumentException::class.java) { VisualRequest(0, 1, rangeDb = bad) }
        }
        assertThrows(IllegalArgumentException::class.java) { WorkspaceSource("", 1, 1, "signature", null) }
        assertThrows(IllegalArgumentException::class.java) { WorkspaceSource("A", -1, 1, "signature", null) }
        assertThrows(IllegalArgumentException::class.java) { WorkspaceSource("A", 1, 0, "signature", null) }
        assertThrows(IllegalArgumentException::class.java) { WorkspaceSource("A", 1, 1, "", null) }
    }
}
