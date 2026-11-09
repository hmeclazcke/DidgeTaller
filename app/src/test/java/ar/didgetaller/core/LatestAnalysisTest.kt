package ar.didgetaller.core

import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class LatestAnalysisTest {
    private class Pending(val request: String, val progress: (AnalysisProgress<String>) -> Unit,
        val complete: (Result<String>) -> Unit) : AnalysisTask {
        var cancellations = 0
        var onCancel: () -> Unit = {}
        override fun cancel() { cancellations++; onCancel() }
    }
    private class Loader : AnalysisLoader<String, String> {
        val tasks = mutableListOf<Pending>()
        override fun submit(request: String, progress: (AnalysisProgress<String>) -> Unit,
            complete: (Result<String>) -> Unit): Result<AnalysisTask> =
            Result.success(Pending(request, progress, complete).also { tasks += it })
    }
    private val loader = Loader()
    private val observed = mutableListOf<AnalysisState<String, String>>()
    private val owner = LatestAnalysis(loader, observed::add)

    @Test fun replacementRevokesEveryOldOutcomeAndTerminalIsExactlyOnce() {
        owner.replace("A"); val old = loader.tasks.single(); old.progress(AnalysisProgress(40))
        owner.replace("B"); val current = loader.tasks.last(); current.progress(AnalysisProgress(25))
        assertEquals(1, old.cancellations); val selected = owner.state
        old.progress(AnalysisProgress(90)); old.complete(Result.success("wrong")); old.complete(Result.failure(Exception("late")))
        assertSame(selected, owner.state); assertEquals("B", selected.request)
        current.complete(Result.success("right")); val terminal = owner.state
        current.progress(AnalysisProgress(95)); current.complete(Result.failure(Exception("duplicate")))
        assertSame(terminal, owner.state); assertEquals("right", terminal.value); assertEquals(100, terminal.progress)
    }
    @Test fun progressIsMonotonicBoundedAndCannotReviveCancelledWork() {
        owner.replace("A"); val task = loader.tasks.single()
        task.progress(AnalysisProgress(-5)); assertEquals(0, owner.state.progress)
        task.progress(AnalysisProgress(40)); task.progress(AnalysisProgress(20)); assertEquals(40, owner.state.progress)
        task.progress(AnalysisProgress(150)); assertEquals(100, owner.state.progress)
        owner.cancel(); val cancelled = owner.state
        assertTrue(cancelled is AnalysisState.Cancelled); assertEquals(100, cancelled.progress)
        task.progress(AnalysisProgress(60)); task.complete(Result.success("late")); owner.cancel()
        assertSame(cancelled, owner.state); assertEquals(1, task.cancellations)
    }
    @Test fun disposalIsPermanentAndClearCannotReopenIt() {
        owner.replace("A"); val task = loader.tasks.single()
        owner.close(); val disposed = owner.state
        assertSame(AnalysisState.Disposed, disposed); assertEquals(1, task.cancellations)
        task.complete(Result.success("late")); task.progress(AnalysisProgress(90))
        owner.close(); owner.clear(); owner.cancel(); owner.replace("B"); owner.select("C")
        assertSame(disposed, owner.state); assertEquals(1, loader.tasks.size)
    }
    @Test fun clearRevokesWorkButAllowsAFreshRequest() {
        owner.replace("A"); val old = loader.tasks.single()
        owner.clear(); assertSame(AnalysisState.Idle, owner.state)
        owner.select("A"); old.complete(Result.success("obsolete"))
        assertTrue(owner.state is AnalysisState.Loading); assertEquals(2, loader.tasks.size)
        loader.tasks.last().complete(Result.success("fresh")); assertEquals("fresh", owner.state.value)
    }
    @Test fun selectReusesPendingAndReadyButExplicitReplacementRefreshes() {
        owner.select("A"); owner.select("A"); assertEquals(1, loader.tasks.size)
        loader.tasks.single().complete(Result.success("cached"))
        owner.select("A"); assertEquals(1, loader.tasks.size); assertEquals("cached", owner.state.value)
        owner.replace("A"); assertEquals(2, loader.tasks.size); assertNull(owner.state.value)
    }
    @Test fun failuresAndCancellationAreDistinctAndRetryable() {
        owner.select("A"); val error = IllegalArgumentException("invalid")
        loader.tasks.last().complete(Result.failure(error))
        assertSame(error, owner.state.failure); assertTrue(owner.state is AnalysisState.Failed)
        owner.select("A"); assertEquals(2, loader.tasks.size)
        loader.tasks.last().progress(AnalysisProgress(30))
        loader.tasks.last().complete(Result.failure(CancellationException()))
        assertTrue(owner.state is AnalysisState.Cancelled); assertNull(owner.state.failure)
        assertEquals(30, owner.state.progress)
        owner.select("A"); assertEquals(3, loader.tasks.size)
    }
    @Test fun cancellationReentrancyCannotOverwriteANewerRequest() {
        owner.replace("A"); loader.tasks.single().onCancel = { owner.replace("C") }
        owner.replace("B")
        assertEquals(listOf("A", "C"), loader.tasks.map { it.request }); assertEquals("C", owner.state.request)
    }
    @Test fun observerCanCloseBeforeSchedulingAndCleanupStillOccursIfItThrows() {
        lateinit var closing: LatestAnalysis<String, String>
        closing = LatestAnalysis(loader) { if (it is AnalysisState.Loading) closing.close() }
        closing.replace("A"); assertSame(AnalysisState.Disposed, closing.state); assertTrue(loader.tasks.isEmpty())
        var throwing = false
        val failing = LatestAnalysis(loader) { if (throwing) throw IllegalStateException("observer") }
        failing.replace("B"); val task = loader.tasks.single(); throwing = true
        assertThrows(IllegalStateException::class.java) { failing.close() }
        assertEquals(1, task.cancellations); assertSame(AnalysisState.Disposed, failing.state)
    }
    @Test fun synchronousCompletionAndSchedulingFailureAreTerminal() {
        var cancellations = 0
        val immediate = LatestAnalysis(AnalysisLoader<String, String> { _, _, complete ->
            complete(Result.success("done")); Result.success(AnalysisTask { cancellations++ })
        }) { }
        immediate.replace("A"); assertEquals("done", immediate.state.value); assertEquals(1, cancellations)
        val error = IllegalStateException("schedule")
        val rejected = LatestAnalysis(AnalysisLoader<String, String> { _, _, _ -> Result.failure(error) }) { }
        rejected.replace("A"); assertSame(error, rejected.state.failure)
        assertTrue(rejected.state is AnalysisState.Failed)
    }
    @Test fun cancellingACompletedOperationPreservesItsResult() {
        owner.replace("A"); loader.tasks.single().complete(Result.success("done")); val ready = owner.state
        owner.cancel(); assertSame(ready, owner.state); assertEquals(0, loader.tasks.single().cancellations)
        assertEquals(2, observed.size)
    }
    @Test fun typedPreviewsCannotRegressAndCancellationKeepsTheLastPartialResult() {
        owner.replace("A"); val current = loader.tasks.single()
        current.progress(AnalysisProgress(20, "best"))
        current.progress(AnalysisProgress(10, "obsolete"))
        assertEquals("best", owner.state.value); assertEquals(20, owner.state.progress)
        current.progress(AnalysisProgress(30)); assertEquals("best", owner.state.value)
        owner.cancel(); assertEquals("best", owner.state.value)
        current.progress(AnalysisProgress(40, "late")); assertEquals("best", owner.state.value)
        owner.replace("B"); assertNull(owner.state.value)
        loader.tasks.last().progress(AnalysisProgress(50, "partial"))
        loader.tasks.last().complete(Result.failure(IllegalArgumentException("error")))
        assertTrue(owner.state is AnalysisState.Failed); assertEquals("partial", owner.state.value)
        owner.close(); assertNull(owner.state.value)
    }
}
