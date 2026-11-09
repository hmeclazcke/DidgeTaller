package ar.didgetaller.core

import java.util.concurrent.CancellationException

sealed class SwipeComparisonStatus(val request: SwipeComparisonRequest? = null,
    val enabled: Boolean = false, val running: Boolean = false, val progress: Int = 0,
    val curve: SwipeCurve? = null, val failure: Throwable? = null) {
    data object Idle : SwipeComparisonStatus()
    class Disabled(request: SwipeComparisonRequest, cached: SwipeCurve? = null) :
        SwipeComparisonStatus(request, curve = cached)
    class Loading(request: SwipeComparisonRequest, progress: Int = 0) :
        SwipeComparisonStatus(request, enabled = true, running = true, progress = progress)
    class Ready(request: SwipeComparisonRequest, value: SwipeCurve) :
        SwipeComparisonStatus(request, enabled = true, progress = 100, curve = value)
    class Failed(request: SwipeComparisonRequest, progress: Int, error: Throwable) :
        SwipeComparisonStatus(request, enabled = true, progress = progress, failure = error)
    data object Disposed : SwipeComparisonStatus()
}

/** One owner grants publication to a selected operation, and revokes it before cancellation. */
class SwipeComparisonController(private val loader: SwipeComparisonLoader,
    private val changed: (SwipeComparisonStatus) -> Unit) {
    private class Operation(val request: SwipeComparisonRequest, var task: SwipeComparisonTask? = null)
    private var active: Operation? = null
    var state: SwipeComparisonStatus = SwipeComparisonStatus.Idle; private set

    fun select(request: SwipeComparisonRequest, enabled: Boolean) {
        if (state === SwipeComparisonStatus.Disposed) return
        val cached = state.curve.takeIf { state.request?.sessionId == request.sessionId }
        val previous = active
        val operation = Operation(request)
        active = operation
        previous?.task?.cancel()
        if (active !== operation) return
        val next = selection(request, enabled, cached)
        publish(next)
        if (active !== operation) return
        if (next is SwipeComparisonStatus.Loading) start(operation) else active = null
    }

    private fun selection(request: SwipeComparisonRequest, enabled: Boolean, cached: SwipeCurve?) = when {
        !enabled -> SwipeComparisonStatus.Disabled(request, cached)
        cached != null -> SwipeComparisonStatus.Ready(request, cached)
        else -> SwipeComparisonStatus.Loading(request)
    }

    private fun start(operation: Operation) {
        loader.submit(operation.request, { progress(operation, it) }, { finish(operation, it) }).fold(
            { task -> if (active === operation) operation.task = task else task.cancel() },
            { error -> finish(operation, Result.failure(error)) },
        )
    }

    private fun progress(operation: Operation, percent: Int) {
        if (active !== operation) return
        publish(SwipeComparisonStatus.Loading(operation.request, percent.coerceIn(state.progress, 100)))
    }

    private fun finish(operation: Operation, result: Result<SwipeCurve>) {
        if (active !== operation) return
        active = null
        operation.task = null
        publish(result.fold({ SwipeComparisonStatus.Ready(operation.request, it) }, { error ->
            if (error is CancellationException) SwipeComparisonStatus.Disabled(operation.request)
            else SwipeComparisonStatus.Failed(operation.request, state.progress, error)
        }))
    }

    fun clear() {
        if (state !== SwipeComparisonStatus.Disposed) retire(SwipeComparisonStatus.Idle)
    }

    fun close() {
        if (state !== SwipeComparisonStatus.Disposed) retire(SwipeComparisonStatus.Disposed)
    }

    private fun retire(next: SwipeComparisonStatus) {
        val previous = active
        active = null
        try { publish(next) } finally { previous?.task?.cancel() }
    }

    private fun publish(next: SwipeComparisonStatus) { state = next; changed(next) }
}
