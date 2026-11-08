package ar.didgetaller.core

import ar.didgetaller.audio.CoroutineRecordingAnalysisLoader
import ar.didgetaller.testing.QueuedDispatcher
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RecordingAnalysisCoroutineTest {
    private val request = RecordingAnalysisRequest("synthetic", InstrumentProfile.DIDGERIDOO, 100, 900, true)
    private val result = WavAnalysis.Result(0, 4096, 44100, 1, PowerSpectrum(44100, 4096, DoubleArray(2049)),
        HarmonicAnalysis.Result(null, emptyList(), null), emptyList(), null, null, null, 0.0, null)

    @Test fun workerAndProgressReturnToTheOwnerWithoutRunningIoOnTheOwnerTurn() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val errors = mutableListOf<Throwable>(); val executed = mutableListOf<RecordingAnalysisRequest>()
        val scope = CoroutineScope(SupervisorJob() + ui + CoroutineExceptionHandler { _, e -> errors += e })
        val snapshots = mutableListOf<RecordingAnalysisStatus>()
        val owner = RecordingAnalysisController(CoroutineRecordingAnalysisLoader(scope, { r, cancelled, progress ->
            assertFalse(cancelled()); executed += r; progress(20); progress(80); result
        }, worker), snapshots::add)
        try {
            owner.request(request); ui.drain(); assertTrue(executed.isEmpty())
            worker.drain(); assertEquals(listOf(request), executed); assertEquals(0, owner.state.progress)
            ui.drain(); assertSame(result, owner.state.result); assertEquals(100, owner.state.progress)
            assertTrue(snapshots.any { it.running && it.progress == 20 })
            assertTrue(snapshots.any { it.running && it.progress == 80 }); assertTrue(errors.isEmpty())
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun cancellationBeforeWorkerTurnPreventsWorkAndCancelsTheActualJob() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher(); val scope = CoroutineScope(SupervisorJob() + ui)
        var executions = 0
        val owner = RecordingAnalysisController(CoroutineRecordingAnalysisLoader(scope, { _, _, _ -> executions++; result }, worker)) {}
        try {
            owner.request(request); ui.drain(); owner.cancel(); worker.drain(); ui.drain()
            assertEquals(0, executions); assertTrue(owner.state.cancelled)
            owner.request(request); ui.drain(); worker.drain(); ui.drain(); assertSame(result, owner.state.result)
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun cancellationInsideAnExpensiveWorkerIsObservedAndFinallyClosesItsResource() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher(); val scope = CoroutineScope(SupervisorJob() + ui)
        var closed = false; lateinit var owner: RecordingAnalysisController
        owner = RecordingAnalysisController(CoroutineRecordingAnalysisLoader(scope, { _, cancelled, _ ->
            try { owner.cancel(); assertTrue(cancelled()); throw CancellationException("synthetic cancellation") }
            finally { closed = true }
        }, worker)) {}
        try {
            owner.request(request); ui.drain(); worker.drain(); ui.drain()
            assertTrue(closed); assertTrue(owner.state.cancelled); assertNull(owner.state.result)
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun staleQueuedProgressAndCompletionCannotOverwriteAReplacement() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher(); val scope = CoroutineScope(SupervisorJob() + ui)
        val seen = mutableListOf<RecordingAnalysisStatus>()
        val owner = RecordingAnalysisController(CoroutineRecordingAnalysisLoader(scope, { _, _, progress -> progress(80); result }, worker), seen::add)
        try {
            owner.request(request); ui.drain(); worker.drain()
            val replacement = request.copy(sessionId = "replacement")
            owner.request(replacement); ui.drain()
            assertEquals(0, owner.state.progress); assertEquals(replacement, owner.state.request)
            worker.drain(); ui.drain(); assertSame(result, owner.state.result)
            assertEquals(1, seen.count { it is RecordingAnalysisStatus.Ready })
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun workerFailureAndRejectedScopeStayExplicitButObserverFailureIsNotSwallowed() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher(); val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(SupervisorJob() + ui + CoroutineExceptionHandler { _, e -> errors += e })
        val loader = CoroutineRecordingAnalysisLoader(scope, { _, _, _ -> throw IllegalArgumentException("invalid WAV") }, worker)
        try {
            var calls = 0
            loader.submit(request, {}) { assertEquals("invalid WAV", it.exceptionOrNull()?.message); calls++; error("observer") }
            ui.drain(); worker.drain(); ui.drain()
            assertEquals(1, calls); assertEquals("observer", errors.single().message)
            scope.cancel()
            assertTrue(loader.submit(request, {}, {}).exceptionOrNull() is CancellationException)
        } finally { scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
}
