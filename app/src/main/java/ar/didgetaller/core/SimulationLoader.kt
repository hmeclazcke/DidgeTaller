package ar.didgetaller.core

fun interface SimulationTask {
    fun cancel()
}

/** Results return to the state owner's thread; cancellation alone does not authorize publication. */
fun interface SimulationLoader {
    fun submit(shape: AcousticModel.Shape, complete: (Result<AcousticModel.Simulation>) -> Unit): Result<SimulationTask>
}
