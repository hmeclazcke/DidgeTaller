package ar.didgetaller.core

import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class RecordingAnalysisControllerTest {
    private class Pending(val request: RecordingAnalysisRequest, val progress: (Int) -> Unit,
                          val complete: (Result<WavAnalysis.Result>) -> Unit) : RecordingAnalysisTask {
        var cancellations = 0
        var onCancel: () -> Unit = {}
        override fun cancel() { cancellations++; onCancel() }
    }
    private class Loader : RecordingAnalysisLoader {
        val tasks = mutableListOf<Pending>()
        var inline: Result<WavAnalysis.Result>? = null
        var rejected: Throwable? = null
        override fun submit(request: RecordingAnalysisRequest, progress: (Int) -> Unit,
                            complete: (Result<WavAnalysis.Result>) -> Unit): Result<RecordingAnalysisTask> {
            rejected?.let { return Result.failure(it) }
            val task = Pending(request, progress, complete).also { tasks += it }
            inline?.let(complete)
            return Result.success(task)
        }
    }
    private val a = RecordingAnalysisRequest("A", InstrumentProfile.DIDGERIDOO, 100, 500, true)
    private val b = a.copy(sessionId = "B", startMs = 200, endMs = 900)
    private val result = WavAnalysis.Result(0, 4096, 44100, 1, PowerSpectrum(44100, 4096, DoubleArray(2049)),
        HarmonicAnalysis.Result(null, emptyList(), null), emptyList(), null, null, null, 0.0, null)

    @Test fun replacementOwnsEveryProgressResultFailureAndCancellation() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {}
        owner.request(a); val old = loader.tasks.single(); old.progress(40)
        owner.request(b); val current = loader.tasks.last(); current.progress(20)
        val expected = owner.state
        old.progress(99); old.complete(Result.success(result)); old.complete(Result.failure(CancellationException()))
        old.complete(Result.failure(IllegalStateException("old")))
        assertSame(expected, owner.state); assertEquals(b, owner.state.request)
        assertEquals(1, old.cancellations); assertEquals(0, current.cancellations)
        current.complete(Result.success(result))
        assertSame(result, owner.state.result); assertEquals(100, owner.state.progress)
        assertFalse(owner.state.running)
    }

    @Test fun progressIsBoundedMonotonicAndCannotChangeEitherTerminalOutcome() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {}
        owner.request(a); val task = loader.tasks.single()
        task.progress(-1); assertEquals(0, owner.state.progress)
        task.progress(50); task.progress(20); assertEquals(50, owner.state.progress)
        task.progress(101); assertEquals(100, owner.state.progress)
        task.complete(Result.success(result)); val done = owner.state
        task.progress(5); task.complete(Result.failure(IllegalStateException("duplicate")))
        owner.cancel(); assertSame(done, owner.state)
        owner.request(b); val failed = loader.tasks.last(); val error = IllegalStateException("invalid")
        failed.complete(Result.failure(error)); val terminal = owner.state
        failed.progress(90); failed.complete(Result.success(result))
        assertSame(terminal, owner.state); assertSame(error, owner.state.failure); assertEquals(0, owner.state.progress)
    }

    @Test fun cancellationInvalidatesBeforeCleanupAndClearDoesNotDeleteAnyRecording() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {}
        owner.request(a); val task = loader.tasks.single(); task.progress(30)
        task.onCancel = { task.progress(99); task.complete(Result.success(result)) }
        owner.cancel(); val stopped = owner.state
        assertTrue(stopped.cancelled); assertFalse(stopped.running); assertEquals(30, stopped.progress)
        task.progress(40); task.complete(Result.failure(CancellationException()))
        assertSame(stopped, owner.state); assertEquals(1, task.cancellations)
        owner.cancel(clear = true); assertSame(RecordingAnalysisStatus.Idle, owner.state)
        owner.request(b); assertEquals(2, loader.tasks.size)
    }

    @Test fun disposalIsPermanentAndDuplicateTerminalEventsAreHarmless() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {}
        owner.request(a); val task = loader.tasks.single()
        owner.close(); task.progress(90); task.complete(Result.success(result))
        owner.request(b); owner.cancel(true); owner.close()
        assertSame(RecordingAnalysisStatus.Disposed, owner.state)
        assertEquals(1, task.cancellations); assertEquals(1, loader.tasks.size)
    }

    @Test fun inlineSuccessAndSchedulingFailureNeverLeaveTheOwnerWorking() {
        val loader = Loader(); loader.inline = Result.success(result)
        val owner = RecordingAnalysisController(loader) {}
        owner.request(a); assertSame(result, owner.state.result)
        assertEquals(1, loader.tasks.single().cancellations)
        val error = IllegalStateException("rejected")
        loader.rejected = error; owner.request(b)
        assertSame(error, owner.state.failure); assertFalse(owner.state.running)
    }

    @Test fun cancelledWorkerOutcomeIsExplicitAndRetryable() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {}
        owner.request(a); loader.tasks.single().complete(Result.failure(CancellationException("interrupted")))
        assertTrue(owner.state.cancelled); assertNull(owner.state.failure)
        owner.request(b); loader.tasks.last().complete(Result.success(result))
        assertSame(result, owner.state.result)
    }

    @Test fun reentrantObserverCanReplaceOrDisposeBeforeTheWorkerStarts() {
        val loader = Loader(); lateinit var owner: RecordingAnalysisController
        owner = RecordingAnalysisController(loader) { if (it.running && it.request == a) owner.request(b) }
        owner.request(a); assertEquals(listOf(b), loader.tasks.map { it.request })
        owner.close()
        val empty = Loader(); lateinit var closed: RecordingAnalysisController
        closed = RecordingAnalysisController(empty) { if (it.running) closed.close() }
        closed.request(a); assertTrue(empty.tasks.isEmpty()); assertSame(RecordingAnalysisStatus.Disposed, closed.state)
    }

    @Test fun disposalObserverFailureStillReleasesTheOwnedWorker() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {
            if (it === RecordingAnalysisStatus.Disposed) error("observer")
        }
        owner.request(a)
        assertEquals("observer", assertThrows(IllegalStateException::class.java) { owner.close() }.message)
        assertEquals(1, loader.tasks.single().cancellations)
    }

    @Test fun cancellingWithAFailingObserverStillCancelsOnlyTheOwnedTask() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) { if (it.cancelled) error("observer") }
        owner.request(a)
        assertThrows(IllegalStateException::class.java) { owner.cancel() }
        assertEquals(1, loader.tasks.single().cancellations); assertTrue(owner.state.cancelled)
    }

    @Test fun cancellingTheReplacedTaskCanDisposeTheOwnerBeforeAnotherWorkerStarts() {
        val loader = Loader(); val owner = RecordingAnalysisController(loader) {}
        owner.request(a); loader.tasks.single().onCancel = { owner.close() }
        owner.request(b)
        assertSame(RecordingAnalysisStatus.Disposed, owner.state); assertEquals(1, loader.tasks.size)
    }

    @Test fun disposalPublishesItsTerminalStateExactlyOnce() {
        val loader = Loader(); val seen = mutableListOf<RecordingAnalysisStatus>()
        val owner = RecordingAnalysisController(loader, seen::add)
        owner.request(a); owner.close(); val count = seen.size
        owner.close(); owner.cancel(true); owner.request(b)
        assertEquals(count, seen.size); assertEquals(1, seen.count { it === RecordingAnalysisStatus.Disposed })
    }
}
