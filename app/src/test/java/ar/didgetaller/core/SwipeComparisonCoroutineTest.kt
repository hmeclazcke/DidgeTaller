package ar.didgetaller.core

import ar.didgetaller.audio.CoroutineSwipeComparisonLoader
import ar.didgetaller.testing.QueuedDispatcher
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SwipeComparisonCoroutineTest {
    private val a = SwipeComparisonRequest("synthetic-a")
    private val b = SwipeComparisonRequest("synthetic-b")
    private val curve = SwipeCurve(listOf(SwipeCurve.Sample(20.0, 70.0)), 10.0, 35, 140)

    @Test fun replacementOwnsQueuedProgressAndResultOnTheSuppliedOwnerAndWorkerDispatchers() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val executed = mutableListOf<SwipeComparisonRequest>(); val snapshots = mutableListOf<SwipeComparisonStatus>()
        val owner = SwipeComparisonController(CoroutineSwipeComparisonLoader(scope, worker) { r, cancelled, progress ->
            assertFalse(cancelled()); executed += r; progress(25); progress(80); curve
        }, snapshots::add)
        try {
            owner.select(a, true); ui.drain(); assertTrue(executed.isEmpty())
            worker.drain(); assertEquals(listOf(a), executed); assertEquals(0, owner.state.progress)
            owner.select(b, true); ui.drain()
            assertEquals(b, owner.state.request); assertEquals(0, owner.state.progress)
            worker.drain(); ui.drain()
            assertEquals(listOf(a, b), executed); assertSame(curve, owner.state.curve)
            assertEquals(listOf(b), snapshots.filterIsInstance<SwipeComparisonStatus.Ready>().map { it.request })
            assertTrue(snapshots.any { it.request == b && it.running && it.progress == 25 })
            assertTrue(snapshots.any { it.request == b && it.running && it.progress == 80 })
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun queuedCancellationPreventsWorkAndInWorkerCancellationClosesItsResource() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(SupervisorJob() + ui + CoroutineExceptionHandler { _, e -> errors += e })
        var executions = 0; var closed = false; var cancellationObserved = false
        lateinit var owner: SwipeComparisonController
        owner = SwipeComparisonController(CoroutineSwipeComparisonLoader(scope, worker) { _, cancelled, _ ->
            executions++
            try { owner.clear(); cancellationObserved = cancelled(); throw CancellationException("synthetic") }
            finally { closed = true }
        }) {}
        try {
            owner.select(a, true); ui.drain(); owner.clear(); worker.drain(); ui.drain()
            assertEquals(0, executions); assertFalse(closed)
            owner.select(b, true); ui.drain(); worker.drain(); ui.drain()
            assertEquals(1, executions); assertTrue(closed); assertTrue(cancellationObserved)
            assertTrue(errors.isEmpty()); assertSame(SwipeComparisonStatus.Idle, owner.state)
            scope.cancel()
            owner.select(a, true)
            assertTrue(owner.state is SwipeComparisonStatus.Disabled); assertNull(owner.state.failure)
            assertFalse(owner.state.running)
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
}
