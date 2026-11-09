package ar.didgetaller.core

import java.util.concurrent.CancellationException

fun interface AnalysisTask { fun cancel() }
data class AnalysisProgress<out V : Any>(val percent: Int, val preview: V? = null)

/** Scheduling failures are values; callbacks and commands belong to one owner thread. */
fun interface AnalysisLoader<R : Any, V : Any> {
    fun submit(request: R, progress: (AnalysisProgress<V>) -> Unit, complete: (Result<V>) -> Unit): Result<AnalysisTask>
}

sealed class AnalysisState<out R : Any, out V : Any>(val request: R? = null,
    val value: V? = null, val progress: Int = 0, val failure: Throwable? = null) {
    data object Idle : AnalysisState<Nothing, Nothing>()
    class Loading<R : Any, V : Any>(request: R, progress: Int = 0, preview: V? = null) : AnalysisState<R, V>(request, preview, progress)
    class Ready<R : Any, V : Any>(request: R, val result: V) : AnalysisState<R, V>(request, result, 100)
    class Failed<R : Any, V : Any>(request: R, progress: Int, error: Throwable, preview: V? = null) : AnalysisState<R, V>(request, preview, progress, error)
    class Cancelled<R : Any, V : Any>(request: R, progress: Int, preview: V? = null) : AnalysisState<R, V>(request, preview, progress)
    data object Disposed : AnalysisState<Nothing, Nothing>()
}

/** Publication is a revocable grant, independent of cooperative worker cancellation. */
class LatestAnalysis<R : Any, V : Any>(private val loader: AnalysisLoader<R, V>,
    private val changed: (AnalysisState<R, V>) -> Unit) {
    private class Operation<R : Any>(val request: R, var task: AnalysisTask? = null)
    private var active: Operation<R>? = null
    var state: AnalysisState<R, V> = AnalysisState.Idle; private set

    /** Repeated observation reuses current work or its successful immutable result. */
    fun select(request: R) {
        if (state.request == request && (state is AnalysisState.Loading || state is AnalysisState.Ready)) return
        replace(request)
    }

    /** Explicit refresh/inspection grants a new operation even for the same input. */
    fun replace(request: R) {
        if (state === AnalysisState.Disposed) return
        val previous = active
        val operation = Operation(request)
        active = operation
        previous?.task?.cancel()
        if (active !== operation) return
        publish(AnalysisState.Loading(request))
        if (active !== operation) return
        loader.submit(request, { progress(operation, it) }, { finish(operation, it) }).fold(
            { task -> if (active === operation) operation.task = task else task.cancel() },
            { error -> finish(operation, Result.failure(error)) })
    }

    private fun progress(operation: Operation<R>, update: AnalysisProgress<V>) {
        if (active === operation && update.percent >= state.progress)
            publish(AnalysisState.Loading(operation.request, update.percent.coerceIn(state.progress, 100), update.preview ?: state.value))
    }

    private fun finish(operation: Operation<R>, result: Result<V>) {
        if (active !== operation) return
        active = null
        operation.task = null
        publish(result.fold({ AnalysisState.Ready(operation.request, it) }, { error ->
            if (error is CancellationException) AnalysisState.Cancelled(operation.request, state.progress, state.value)
            else AnalysisState.Failed(operation.request, state.progress, error, state.value)
        }))
    }

    fun cancel() {
        val previous = active ?: return
        retire(AnalysisState.Cancelled(previous.request, state.progress, state.value))
    }

    fun clear() { if (state !== AnalysisState.Disposed) retire(AnalysisState.Idle) }
    fun close() { if (state !== AnalysisState.Disposed) retire(AnalysisState.Disposed) }

    private fun retire(next: AnalysisState<R, V>) {
        val previous = active
        active = null
        try { publish(next) } finally { previous?.task?.cancel() }
    }

    private fun publish(next: AnalysisState<R, V>) { state = next; changed(next) }
}
