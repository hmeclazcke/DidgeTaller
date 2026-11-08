package ar.didgetaller.core

import ar.didgetaller.lab.CoroutineSimulationLoader
import ar.didgetaller.testing.QueuedDispatcher
import java.io.Closeable
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SimulationCoroutineLoaderTest {
    private class Harness : Closeable {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val errors = mutableListOf<Throwable>(); val shapes = mutableListOf<AcousticModel.Shape>()
        val scope = CoroutineScope(SupervisorJob() + ui + CoroutineExceptionHandler { _, error -> errors += error })
        var evaluate: (AcousticModel.Shape) -> AcousticModel.Simulation = {
            AcousticModel.Simulation(listOf(1f), listOf(it.lengthMm / 20.0), it.lengthMm / 20.0)
        }
        val loader = CoroutineSimulationLoader(scope, worker) { shape -> shapes += shape; evaluate(shape) }
        val snapshots = mutableListOf<SimulationState>()
        val owner = SimulationController(loader, snapshots::add)
        override fun close() { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun computationAndPublicationBelongToDifferentControlledTurns() = Harness().use { h ->
        val shape = AcousticModel.Shape(lengthMm = 1600)
        h.owner.request(shape); h.ui.drain()
        assertTrue(h.shapes.isEmpty()); assertTrue(h.owner.state is SimulationState.Working)
        h.worker.drain(); assertEquals(listOf(shape), h.shapes)
        assertTrue(h.owner.state is SimulationState.Working)
        h.ui.drain(); assertTrue(h.owner.state is SimulationState.Ready)
        assertEquals(80.0, h.owner.state.result!!.baseHz, 0.0); assertTrue(h.errors.isEmpty())
    }

    @Test fun replacingACompletedWorkerRejectsItsStillQueuedOwnerDelivery() = Harness().use { h ->
        h.owner.request(AcousticModel.Shape()); h.ui.drain(); h.worker.drain()
        h.owner.request(AcousticModel.Shape(lengthMm = 1600)); h.ui.drain(); h.worker.drain(); h.ui.drain()
        assertEquals(2, h.shapes.size)
        assertEquals(80.0, h.owner.state.result!!.baseHz, 0.0)
        assertEquals(1, h.snapshots.count { it is SimulationState.Ready }); assertTrue(h.errors.isEmpty())
    }

    @Test fun disposalBeforeTheWorkerTurnPreventsEvaluationAndLatePublication() = Harness().use { h ->
        h.owner.request(AcousticModel.Shape()); h.ui.drain(); h.owner.close()
        h.worker.drain(); h.ui.drain()
        assertTrue(h.shapes.isEmpty()); assertSame(SimulationState.Disposed, h.owner.state)
        assertTrue(h.errors.isEmpty())
    }

    @Test fun cancelledScopeRejectsSchedulingAndModelErrorsRemainVisible() = Harness().use { h ->
        h.evaluate = { throw IllegalArgumentException("synthetic model error") }
        h.owner.request(AcousticModel.Shape()); h.ui.drain(); h.worker.drain(); h.ui.drain()
        assertEquals("synthetic model error", (h.owner.state as SimulationState.Failed).cause.message)
        assertTrue(h.errors.isEmpty())
        h.scope.cancel(); h.owner.request(AcousticModel.Shape())
        assertTrue((h.owner.state as SimulationState.Failed).cause is CancellationException)
        assertEquals(1, h.shapes.size)
    }

    @Test fun observerErrorsDoNotBecomeASecondModelOutcome() = Harness().use { h ->
        var calls = 0
        assertTrue(h.loader.submit(AcousticModel.Shape()) { calls++; error("observer") }.isSuccess)
        h.ui.drain(); h.worker.drain(); h.ui.drain()
        assertEquals(1, calls); assertEquals("observer", h.errors.single().message)
    }

    @Test fun rapidShapeChangesDoNotEvaluateCancelledQueuedRequests() = Harness().use { h ->
        for (length in 1500..1530) h.owner.request(AcousticModel.Shape(lengthMm = length))
        h.ui.drain(); h.worker.drain(); h.ui.drain()
        assertEquals(listOf(AcousticModel.Shape(lengthMm = 1530)), h.shapes)
        assertEquals(76.5, h.owner.state.result!!.baseHz, 0.0)
        assertEquals(1, h.snapshots.count { it is SimulationState.Ready }); assertTrue(h.errors.isEmpty())
    }

    @Test fun defaultEvaluatorRunsTheRequestedCylindricalModelRatherThanReturningAnEmptyPlaceholder() {
        val ui = QueuedDispatcher(); val worker = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val owner = SimulationController(CoroutineSimulationLoader(scope, worker)) {}
        try {
            owner.request(AcousticModel.Shape(1500, 40, 40, 40))
            ui.drain(); worker.drain(); ui.drain()
            assertTrue(owner.state is SimulationState.Ready)
            val model = owner.state.result!!
            // Approximate cylindrical quarter-wave resonance, not physical instrument calibration.
            assertEquals(343.0 / (4 * 1.5), model.baseHz, 3.0)
            assertEquals(621, model.impedance.size)
            assertTrue(model.impedance.all { it.isFinite() && it in 0f..1f })
            assertTrue(model.frequenciesHz.zipWithNext().all { (a, b) -> b > a })
        } finally { owner.close(); scope.cancel(); ui.drain(); worker.drain(); ui.drain() }
    }
}
