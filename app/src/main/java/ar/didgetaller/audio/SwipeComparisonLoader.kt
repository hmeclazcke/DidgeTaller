package ar.didgetaller.audio

import ar.didgetaller.core.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** The caller supplies the existing shared serial DSP dispatcher. */
internal class CoroutineSwipeComparisonLoader(private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val compute: (SwipeComparisonRequest, () -> Boolean, (Int) -> Unit) -> SwipeCurve,
) : SwipeComparisonLoader {
    override fun submit(request: SwipeComparisonRequest, progress: (Int) -> Unit,
        complete: (Result<SwipeCurve>) -> Unit): Result<SwipeComparisonTask> =
        submitProgressWork(scope, dispatcher, progress, complete) { cancelled, update ->
            compute(request, cancelled, update)
        }.map { job -> SwipeComparisonTask { job.cancel() } }
}
