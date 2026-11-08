package ar.didgetaller.core

sealed class SimulationState(val result: AcousticModel.Simulation?) {
    data object Empty : SimulationState(null)
    class Working(previous: AcousticModel.Simulation?) : SimulationState(previous)
    class Ready(value: AcousticModel.Simulation) : SimulationState(value)
    class Failed(val cause: Throwable) : SimulationState(null)
    data object Disposed : SimulationState(null)
}

/** One owner thread controls shape requests; a cancellation request is not a publication grant. */
class SimulationController(private val loader: SimulationLoader, private val changed: (SimulationState) -> Unit) {
    var state: SimulationState = SimulationState.Empty; private set
    private class Operation(var task: SimulationTask? = null)
    private var active: Operation? = null

    fun request(shape: AcousticModel.Shape) {
        if (state === SimulationState.Disposed) return
        val previous = active
        val operation = Operation()
        active = operation
        previous?.task?.cancel()
        if (active !== operation) return
        publish(SimulationState.Working(state.result))
        // A UI observer can replace the shape or dispose while receiving Working.
        if (active !== operation) return
        loader.submit(shape) { finish(operation, it) }.fold(
            { task -> if (active === operation) operation.task = task else task.cancel() },
            { error -> finish(operation, Result.failure(error)) },
        )
    }

    private fun finish(operation: Operation, result: Result<AcousticModel.Simulation>) {
        if (active !== operation) return
        active = null
        operation.task = null
        publish(result.fold({ SimulationState.Ready(it) }, { SimulationState.Failed(it) }))
    }

    fun close() {
        if (state === SimulationState.Disposed) return
        val previous = active
        active = null
        try { publish(SimulationState.Disposed) }
        finally { previous?.task?.cancel() }
    }

    private fun publish(next: SimulationState) { state = next; changed(next) }
}
