package ar.didgetaller.core

import ar.didgetaller.audio.submitProgressWork
import ar.didgetaller.testing.QueuedDispatcher
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ProgressWorkTest {
    @Test fun cooperativeWorkerCancellationCompletesTheJobAsCancelledRatherThanSuccessful() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val results = mutableListOf<Result<String>>(); val completions = mutableListOf<Throwable?>()
        val cancellation = CancellationException("worker cancelled itself")
        try {
            val job = submitProgressWork(scope, worker, {}, results::add) { _, _ -> throw cancellation }
                .getOrThrow()
            job.invokeOnCompletion { completions += it }
            ui.drain(); worker.drain(); ui.drain()
            assertTrue(job.isCompleted); assertTrue(job.isCancelled)
            // Coroutine stack recovery may copy exceptions; cancellation type and reason remain observable.
            assertTrue(results.single().exceptionOrNull() is CancellationException)
            assertEquals(cancellation.message, results.single().exceptionOrNull()?.message)
            assertTrue(completions.single() is CancellationException)
            assertEquals(cancellation.message, completions.single()?.message)
            assertTrue(scope.isActive)
        } finally { scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
}
