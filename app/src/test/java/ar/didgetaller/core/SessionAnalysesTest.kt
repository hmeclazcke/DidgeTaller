package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class SessionAnalysesTest {
    private data class Request(override val sessionId: String, val revision: Int = 0) : SessionAnalysisRequest
    private class Pending(val request: Request, val progress: (AnalysisProgress<String>) -> Unit, val complete: (Result<String>) -> Unit) : AnalysisTask {
        var cancelled = false
        override fun cancel() { cancelled = true }
    }
    private val tasks = mutableListOf<Pending>()
    private val seen = mutableListOf<AnalysisState<Request, String>>()
    private val pool = SessionAnalyses(AnalysisLoader<Request, String> { r, p, c ->
        Result.success(Pending(r, p, c).also { tasks += it })
    }, seen::add)

    @Test fun independentSessionsCompleteWithoutCancellingEachOther() {
        pool.select(Request("A")); pool.select(Request("B"))
        tasks[1].complete(Result.success("B curve")); tasks[0].complete(Result.success("A curve"))
        assertFalse(tasks.any { it.cancelled })
        assertEquals(listOf("B curve", "A curve"), seen.filterIsInstance<AnalysisState.Ready<Request, String>>().map { it.result })
    }
    @Test fun removingAndReusingAKeyCannotPublishThePreviousGeneration() {
        pool.select(Request("A")); val old = tasks.single(); pool.remove("A")
        assertTrue(old.cancelled)
        pool.select(Request("A", 1)); val current = tasks.last(); val count = seen.size
        old.progress(AnalysisProgress(90)); old.complete(Result.success("obsolete")); old.complete(Result.failure(Exception("old")))
        assertEquals(count, seen.size)
        current.complete(Result.success("new")); assertEquals("new", seen.last().value)
        assertEquals(1, seen.last().request?.revision)
    }
    @Test fun changedInputReplacesItsOwnKeyAndRepeatedReadyInputIsCached() {
        pool.select(Request("A")); pool.select(Request("A")); assertEquals(1, tasks.size)
        pool.select(Request("A", 1)); assertTrue(tasks.first().cancelled)
        tasks.last().complete(Result.success("new")); pool.select(Request("A", 1))
        assertEquals(2, tasks.size); assertEquals("new", seen.last().value)
    }
    @Test fun closeRevokesAllKeysAndCannotBeReopenedByLateCallbacks() {
        pool.select(Request("A")); pool.select(Request("B")); pool.close(); val count = seen.size
        assertTrue(tasks.all { it.cancelled })
        tasks.forEach { it.progress(AnalysisProgress(90)); it.complete(Result.success("late")) }
        pool.close(); pool.remove("A"); pool.select(Request("C"))
        assertEquals(count, seen.size); assertEquals(2, tasks.size)
    }
    @Test fun aFailedKeyCanRetryWithoutAffectingAnotherSession() {
        pool.select(Request("A")); pool.select(Request("B"))
        tasks.first().complete(Result.failure(IllegalArgumentException("invalid")))
        assertEquals("invalid", seen.last().failure?.message)
        pool.select(Request("A")); assertEquals(3, tasks.size); assertFalse(tasks[1].cancelled)
    }
}
