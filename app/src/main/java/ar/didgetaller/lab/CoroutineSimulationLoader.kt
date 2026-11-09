package ar.didgetaller.lab

import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.SimulationLoader
import ar.didgetaller.core.SimulationTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The caller's scope owns completion; arithmetic stays on a supplied worker dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CoroutineSimulationLoader(private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
    private val evaluate: (AcousticModel.Shape) -> AcousticModel.Simulation = AcousticModel::evaluate,
) : SimulationLoader {
    override fun submit(shape: AcousticModel.Shape,
                        complete: (Result<AcousticModel.Simulation>) -> Unit): Result<SimulationTask> = try {
        scope.ensureActive()
        val job = scope.launch {
            val result = try { Result.success(withContext(dispatcher) { evaluate(shape) }) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Result.failure(error) }
            // Observer errors belong to the owner; never convert them to model failures.
            complete(result)
        }
        Result.success(SimulationTask { job.cancel() })
    } catch (error: Exception) { Result.failure(error) }
}
