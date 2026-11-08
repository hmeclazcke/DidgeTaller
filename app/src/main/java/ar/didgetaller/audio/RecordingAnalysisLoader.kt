package ar.didgetaller.audio

import ar.didgetaller.core.RecordingAnalysisLoader
import ar.didgetaller.core.RecordingAnalysisRequest
import ar.didgetaller.core.RecordingAnalysisTask
import ar.didgetaller.core.WavAnalysis
import kotlinx.coroutines.*

/** Caller-scoped scheduling; the cache retains ownership of each reader and atomic publication. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CoroutineRecordingAnalysisLoader(private val scope: CoroutineScope,
    private val compute: (RecordingAnalysisRequest, () -> Boolean, (Int) -> Unit) -> WavAnalysis.Result,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : RecordingAnalysisLoader {
    override fun submit(request: RecordingAnalysisRequest, progress: (Int) -> Unit,
                        complete: (Result<WavAnalysis.Result>) -> Unit): Result<RecordingAnalysisTask> = try {
        scope.ensureActive()
        val job = scope.launch {
            val owner = this
            val result = try {
                Result.success(withContext(dispatcher) {
                    val worker = currentCoroutineContext()
                    compute(request, { !worker.isActive }, { percent -> owner.launch { progress(percent) } })
                })
            } catch (error: Exception) { Result.failure(error) }
            // Observer failures must not be reclassified as WAV or cache failures.
            complete(result)
            (result.exceptionOrNull() as? CancellationException)?.let { throw it }
        }
        Result.success(RecordingAnalysisTask { job.cancel() })
    } catch (error: Exception) { Result.failure(error) }
}
