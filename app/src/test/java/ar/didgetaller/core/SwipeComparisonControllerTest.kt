package ar.didgetaller.core

import ar.didgetaller.SwipeComparisonState
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class SwipeComparisonControllerTest {
    private class Pending(val request: SwipeComparisonRequest, val progress: (Int) -> Unit,
        val complete: (Result<SwipeCurve>) -> Unit) : SwipeComparisonTask {
        var cancellations = 0
        var onCancel: () -> Unit = {}
        override fun cancel() { cancellations++; onCancel() }
    }
    private class Loader : SwipeComparisonLoader {
        val tasks = mutableListOf<Pending>()
        override fun submit(request: SwipeComparisonRequest, progress: (Int) -> Unit,
            complete: (Result<SwipeCurve>) -> Unit): Result<SwipeComparisonTask> =
            Result.success(Pending(request, progress, complete).also { tasks += it })
    }
    private val a = SwipeComparisonRequest("a")
    private val b = SwipeComparisonRequest("b")
    private val curve = SwipeCurve(listOf(SwipeCurve.Sample(20.0, 70.0)), 10.0, 35, 140)

    @Test fun disabledSelectionDoesNotScheduleWorkAndReadyToggleReusesOnlyItsOwnCurve() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, false)
        assertTrue(owner.state is SwipeComparisonStatus.Disabled); assertTrue(loader.tasks.isEmpty())
        owner.select(a, true); loader.tasks.single().complete(Result.success(curve))
        owner.select(a, false); assertFalse(owner.state.enabled); assertSame(curve, owner.state.curve)
        owner.select(a, true); assertSame(curve, owner.state.curve); assertEquals(1, loader.tasks.size)
        assertEquals(100, owner.state.progress); assertFalse(owner.state.running)
        owner.select(b, true); assertNull(owner.state.curve); assertEquals(b, loader.tasks.last().request)
    }

    @Test fun progressIsBoundedMonotonicAndCannotChangeATerminalOutcome() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, true); val task = loader.tasks.single()
        task.progress(-20); assertEquals(0, owner.state.progress)
        task.progress(45); task.progress(10); assertEquals(45, owner.state.progress)
        task.progress(120); assertEquals(100, owner.state.progress); assertTrue(owner.state.running)
        task.complete(Result.success(curve)); val ready = owner.state
        task.progress(20); task.complete(Result.failure(IllegalArgumentException("late")))
        assertSame(ready, owner.state); assertSame(curve, owner.state.curve)
    }

    @Test fun replacementRevokesProgressCompletionFailureAndCancellationFromThePreviousRequest() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, true); val old = loader.tasks.single()
        owner.select(b, true); val current = loader.tasks.last(); current.progress(30)
        val selected = owner.state
        assertEquals(1, old.cancellations)
        old.progress(90); old.complete(Result.success(curve))
        old.complete(Result.failure(IllegalStateException("old")))
        old.complete(Result.failure(CancellationException("old cancellation")))
        assertSame(selected, owner.state)
        current.complete(Result.success(curve)); assertEquals(b, owner.state.request)
    }

    @Test fun repeatedLoadingRequestsCancelTheirPredecessorWithoutSharingItsPublicationGrant() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, true); val old = loader.tasks.single()
        owner.select(a, true); val current = loader.tasks.last()
        assertNotSame(old, current); assertEquals(1, old.cancellations)
        old.complete(Result.success(curve)); assertTrue(owner.state.running); assertNull(owner.state.curve)
        current.complete(Result.success(curve)); assertFalse(owner.state.running); assertSame(curve, owner.state.curve)
    }

    @Test fun disablingInvalidatesBeforeAProducerSynchronouslyReportsCancellation() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, true); val task = loader.tasks.single()
        task.onCancel = { task.complete(Result.failure(IllegalStateException("cancel callback"))) }
        owner.select(a, false)
        assertTrue(owner.state is SwipeComparisonStatus.Disabled)
        assertFalse(owner.state.enabled); assertFalse(owner.state.running); assertNull(owner.state.failure)
        task.progress(90); assertEquals(0, owner.state.progress)
    }

    @Test fun failedWorkRetainsItsRequestAndProgressAndCanBeRetried() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, true); val task = loader.tasks.single(); task.progress(35)
        val error = IllegalArgumentException("synthetic invalid source")
        task.complete(Result.failure(error))
        assertTrue(owner.state is SwipeComparisonStatus.Failed); assertSame(error, owner.state.failure)
        assertTrue(owner.state.enabled); assertFalse(owner.state.running); assertEquals(35, owner.state.progress)
        owner.select(a, true); assertEquals(2, loader.tasks.size); assertEquals(0, owner.state.progress)
        loader.tasks.last().complete(Result.success(curve)); assertNull(owner.state.failure)
    }

    @Test fun schedulingFailureAndCooperativeCancellationHaveDifferentOutcomes() {
        val error = IllegalStateException("rejected")
        val owner = SwipeComparisonController(SwipeComparisonLoader { _, _, _ -> Result.failure(error) }) {}
        owner.select(a, true); assertSame(error, owner.state.failure); assertFalse(owner.state.running)
        val loader = Loader(); val cancelled = SwipeComparisonController(loader) {}
        cancelled.select(a, true); loader.tasks.single().complete(Result.failure(CancellationException()))
        assertTrue(cancelled.state is SwipeComparisonStatus.Disabled); assertNull(cancelled.state.failure)
    }

    @Test fun inlineCompletionOwnsTheResultEvenWhenTheReturnedHandleOrSubmissionFailsLater() {
        var cancellations = 0
        val owner = SwipeComparisonController(SwipeComparisonLoader { _, _, complete ->
            complete(Result.success(curve))
            Result.success(SwipeComparisonTask { cancellations++ })
        }) {}
        owner.select(a, true); assertSame(curve, owner.state.curve); assertEquals(1, cancellations)
        val failedSubmission = SwipeComparisonController(SwipeComparisonLoader { _, _, complete ->
            complete(Result.success(curve)); Result.failure(IllegalStateException("late scheduling error"))
        }) {}
        failedSubmission.select(a, true); assertSame(curve, failedSubmission.state.curve)
    }

    @Test fun reentrantObserverReplacementCannotScheduleOrClearTheWrongSelection() {
        val loader = Loader(); lateinit var owner: SwipeComparisonController
        owner = SwipeComparisonController(loader) { status ->
            if (status.running && status.request == a) owner.select(b, false)
        }
        owner.select(a, true)
        assertEquals(b, owner.state.request); assertFalse(owner.state.enabled); assertTrue(loader.tasks.isEmpty())
    }

    @Test fun navigationClearPermitsAnotherRequestButDisposalIsPermanentAndIdempotent() {
        val loader = Loader(); var notices = 0
        val owner = SwipeComparisonController(loader) { notices++ }
        owner.select(a, true); val old = loader.tasks.single()
        owner.clear(); assertSame(SwipeComparisonStatus.Idle, owner.state); assertEquals(1, old.cancellations)
        owner.select(b, true); val current = loader.tasks.last()
        owner.close(); assertEquals(1, current.cancellations); val terminalNotices = notices
        owner.clear(); owner.close(); owner.select(a, true)
        current.complete(Result.success(curve)); current.progress(90)
        assertSame(SwipeComparisonStatus.Disposed, owner.state)
        assertEquals(terminalNotices, notices); assertEquals(2, loader.tasks.size)
    }

    @Test fun observerFailureCannotPreventOwnedResourceCancellation() {
        val loader = Loader(); var reject = false
        val owner = SwipeComparisonController(loader) { if (reject) error("observer") }
        owner.select(a, true); reject = true
        assertThrows(IllegalStateException::class.java) { owner.close() }
        assertEquals(1, loader.tasks.single().cancellations)
        assertSame(SwipeComparisonStatus.Disposed, owner.state)
    }

    @Test fun cancellationCanRequestAnotherSelectionWithoutTheInterruptedCommandOverwritingIt() {
        val loader = Loader(); val owner = SwipeComparisonController(loader) {}
        owner.select(a, true); val old = loader.tasks.single()
        val replacement = SwipeComparisonRequest("replacement from cancellation")
        old.onCancel = { owner.select(replacement, true) }
        owner.select(b, true)
        assertEquals(listOf(a, replacement), loader.tasks.map { it.request })
        assertEquals(replacement, owner.state.request); assertTrue(owner.state.running)
    }

    @Test fun presentationAndSharedRangeReflectOnlyTheCurrentTypedSnapshot() {
        val ready = SwipeComparisonState(SwipeComparisonStatus.Ready(a, curve))
        assertEquals("a", ready.sessionId); assertTrue(ready.enabled); assertFalse(ready.failed)
        assertSame(curve, ready.curve); assertEquals(100, ready.progress)
        assertEquals(AxisWindow(33.25, 147.0), ready.range)
        val disabled = SwipeComparisonState(SwipeComparisonStatus.Disabled(a, curve))
        assertNull(disabled.range); assertSame(curve, disabled.curve); assertFalse(disabled.running)
        assertNull(swipeComparisonWindow(false))
        val failed = SwipeComparisonState(SwipeComparisonStatus.Failed(a, 15, IllegalArgumentException()))
        assertTrue(failed.failed); assertTrue(failed.enabled); assertEquals(15, failed.progress)
        val closed = SwipeComparisonState(SwipeComparisonStatus.Disposed)
        assertNull(closed.sessionId); assertFalse(closed.enabled); assertNull(closed.curve)
    }
}
