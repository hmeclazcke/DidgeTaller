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
                        complete: (Result<WavAnalysis.Result>) -> Unit): Result<RecordingAnalysisTask> =
        submitProgressWork(scope, dispatcher, progress, complete) { cancelled, update ->
            compute(request, cancelled, update)
        }.map { job -> RecordingAnalysisTask { job.cancel() } }
}
