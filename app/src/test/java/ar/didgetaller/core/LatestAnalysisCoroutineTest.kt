package ar.didgetaller.core

import ar.didgetaller.audio.CoroutineAnalysisLoader
import ar.didgetaller.testing.QueuedDispatcher
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LatestAnalysisCoroutineTest {
    @Test fun queuedWorkAndProgressAreScopedAndCannotOverwriteReplacement() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val executed = mutableListOf<String>(); val seen = mutableListOf<AnalysisState<String, String>>()
        val owner = LatestAnalysis(CoroutineAnalysisLoader(scope, worker) { request: String, _, progress ->
            executed += request; progress(AnalysisProgress(70)); "result-$request"
        }, seen::add)
        try {
            owner.replace("A"); ui.drain(); assertTrue(executed.isEmpty())
            worker.drain(); owner.replace("B"); ui.drain()
            assertEquals("B", owner.state.request); assertEquals(0, owner.state.progress)
            worker.drain(); ui.drain(); assertEquals("result-B", owner.state.value)
            assertEquals(listOf("A", "B"), executed)
            assertEquals(1, seen.count { it is AnalysisState.Ready })
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
    @Test fun cancellationDuringComputeClosesItsResourceAndNeverPublishesAResult() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(SupervisorJob() + ui + CoroutineExceptionHandler { _, e -> errors += e })
        var closed = false; var cancelled = false; lateinit var owner: LatestAnalysis<String, String>
        owner = LatestAnalysis(CoroutineAnalysisLoader(scope, worker) { _: String, stop, _ ->
            try { owner.clear(); cancelled = stop(); throw CancellationException("cancelled") }
            finally { closed = true }
        }) { }
        try {
            owner.replace("A"); ui.drain(); worker.drain(); ui.drain()
            assertTrue(closed); assertTrue(cancelled); assertTrue(errors.isEmpty())
            assertSame(AnalysisState.Idle, owner.state)
            scope.cancel(); owner.replace("B")
            assertTrue(owner.state is AnalysisState.Cancelled)
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
    @Test fun failureReturnsToTheOwnerAndDisposalBeforeDispatchRunsNoIo() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui); var executions = 0
        val owner = LatestAnalysis(CoroutineAnalysisLoader<String, String>(scope, worker) { _, _, _ ->
            executions++; throw IllegalStateException("invalid WAV")
        }) { }
        try {
            owner.replace("A"); ui.drain(); worker.drain(); ui.drain()
            assertEquals("invalid WAV", owner.state.failure?.message)
            owner.replace("B"); ui.drain(); owner.close(); worker.drain(); ui.drain()
            assertEquals(1, executions); assertSame(AnalysisState.Disposed, owner.state)
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
}
