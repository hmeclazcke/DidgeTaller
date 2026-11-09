package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class SimulationControllerTest {
    private class Pending(val complete: (Result<AcousticModel.Simulation>) -> Unit) : SimulationTask {
        var cancellations = 0
        var onCancel: () -> Unit = {}
        override fun cancel() { cancellations++; onCancel() }
    }
    private class Loader : SimulationLoader {
        val tasks = mutableListOf<Pending>()
        var rejected: Throwable? = null
        var inline: AcousticModel.Simulation? = null
        override fun submit(shape: AcousticModel.Shape, complete: (Result<AcousticModel.Simulation>) -> Unit): Result<SimulationTask> {
            rejected?.let { return Result.failure(it) }
            val pending = Pending(complete).also { tasks += it }
            inline?.let { complete(Result.success(it)) }
            return Result.success(pending)
        }
    }
    private val shape = AcousticModel.Shape()
    private val first = AcousticModel.Simulation(listOf(1f), listOf(70.0), 70.0)
    private val second = AcousticModel.Simulation(listOf(2f), listOf(80.0), 80.0)

    @Test fun newestShapeOwnsSuccessFailureAndDuplicateTerminalEvents() {
        val loader = Loader(); val snapshots = mutableListOf<SimulationState>()
        val owner = SimulationController(loader, snapshots::add)
        owner.request(shape); val a = loader.tasks.single()
        owner.request(shape.copy(lengthMm = 1600)); val b = loader.tasks.last()
        assertEquals(1, a.cancellations)
        val waiting = owner.state
        a.complete(Result.success(first)); a.complete(Result.failure(IllegalStateException("old")))
        assertSame(waiting, owner.state)
        b.complete(Result.success(second)); val ready = owner.state
        assertTrue(ready is SimulationState.Ready); assertSame(second, ready.result)
        b.complete(Result.failure(IllegalStateException("duplicate"))); b.complete(Result.success(first))
        assertSame(ready, owner.state); assertSame(ready, snapshots.last())
    }

    @Test fun workingRetainsTheLastCompletedModelAndFailureRemainsRetryable() {
        val loader = Loader(); val owner = SimulationController(loader) {}
        owner.request(shape); loader.tasks.last().complete(Result.success(first))
        owner.request(shape.copy(lengthMm = 1600))
        assertTrue(owner.state is SimulationState.Working); assertSame(first, owner.state.result)
        val error = IllegalArgumentException("bad synthetic geometry")
        loader.tasks.last().complete(Result.failure(error))
        assertSame(error, (owner.state as SimulationState.Failed).cause); assertNull(owner.state.result)
        owner.request(shape); loader.tasks.last().complete(Result.success(second))
        assertSame(second, owner.state.result)
    }

    @Test fun closeInvalidatesBeforeCancellationAndCannotBeReopened() {
        val loader = Loader(); val snapshots = mutableListOf<SimulationState>()
        val owner = SimulationController(loader, snapshots::add)
        owner.request(shape); val pending = loader.tasks.single()
        pending.onCancel = { pending.complete(Result.success(first)) }
        owner.close(); val count = snapshots.size
        pending.complete(Result.failure(IllegalStateException("late")))
        owner.close(); owner.request(shape)
        assertSame(SimulationState.Disposed, owner.state); assertNull(owner.state.result)
        assertEquals(1, pending.cancellations); assertEquals(1, loader.tasks.size)
        assertEquals(count, snapshots.size)
    }

    @Test fun disposalWhilePublishingWorkingDoesNotStartAWorker() {
        val loader = Loader(); lateinit var owner: SimulationController
        owner = SimulationController(loader) { if (it is SimulationState.Working) owner.close() }
        owner.request(shape)
        assertTrue(loader.tasks.isEmpty()); assertSame(SimulationState.Disposed, owner.state)
    }

    @Test fun inlineCompletionReleasesItsHandleAndSubmissionFailureDoesNotStayBusy() {
        val loader = Loader(); loader.inline = first
        val owner = SimulationController(loader) {}
        owner.request(shape)
        assertSame(first, owner.state.result); assertEquals(1, loader.tasks.single().cancellations)
        val rejected = IllegalStateException("synthetic scheduling rejection")
        loader.rejected = rejected
        owner.request(shape)
        assertSame(rejected, (owner.state as SimulationState.Failed).cause)
        assertNull(owner.state.result)
    }

    @Test fun replacementDuringWorkingPublicationCannotLaunchTheSupersededRequest() {
        val loader = Loader(); lateinit var owner: SimulationController; var replaced = false
        owner = SimulationController(loader) { if (it is SimulationState.Working && !replaced) {
            replaced = true; owner.request(shape.copy(lengthMm = 1600))
        } }
        owner.request(shape)
        assertEquals(1, loader.tasks.size)
        loader.tasks.single().complete(Result.success(second))
        assertSame(second, owner.state.result)
    }

    @Test fun failingDisposalObserverStillCancelsTheOwnedTask() {
        val loader = Loader(); val owner = SimulationController(loader) {
            if (it === SimulationState.Disposed) throw IllegalStateException("observer")
        }
        owner.request(shape)
        assertEquals("observer", assertThrows(IllegalStateException::class.java) { owner.close() }.message)
        assertEquals(1, loader.tasks.single().cancellations)
        assertSame(SimulationState.Disposed, owner.state)
    }

    @Test fun inlineObserverFailureIsNotTurnedIntoAnAnalysisErrorOrSecondPublication() {
        val loader = Loader(); loader.inline = first
        var count = 0
        val owner = SimulationController(loader) { if (it is SimulationState.Ready) { count++; error("observer") } }
        assertThrows(IllegalStateException::class.java) { owner.request(shape) }
        assertEquals(1, count); assertTrue(owner.state is SimulationState.Ready)
    }

    @Test fun cancellationThatDisposesTheOwnerCannotBeOverwrittenByTheReplacement() {
        val loader = Loader(); val owner = SimulationController(loader) {}
        owner.request(shape)
        loader.tasks.single().onCancel = { owner.close() }
        owner.request(shape.copy(lengthMm = 1600))
        assertSame(SimulationState.Disposed, owner.state)
        assertEquals(1, loader.tasks.size)
        owner.request(shape); assertEquals(1, loader.tasks.size)
    }
}
