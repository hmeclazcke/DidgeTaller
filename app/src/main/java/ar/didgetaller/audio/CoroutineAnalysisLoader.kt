package ar.didgetaller.audio

import ar.didgetaller.core.AnalysisLoader
import ar.didgetaller.core.AnalysisProgress
import ar.didgetaller.core.AnalysisTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** One caller-owned scheduler policy serves saved diagnostics, comparison and model association. */
internal class CoroutineAnalysisLoader<R : Any, V : Any>(private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val compute: (R, () -> Boolean, (AnalysisProgress<V>) -> Unit) -> V) : AnalysisLoader<R, V> {
    override fun submit(request: R, progress: (AnalysisProgress<V>) -> Unit, complete: (Result<V>) -> Unit): Result<AnalysisTask> =
        submitProgressWork(scope, dispatcher, progress, complete) { cancelled, update ->
            compute(request, cancelled, update)
        }.map { job -> AnalysisTask { job.cancel() } }
}
