package ar.didgetaller.core

import java.util.concurrent.CancellationException

sealed class RecordingAnalysisStatus(val request: RecordingAnalysisRequest? = null,
    val progress: Int = 0, val result: WavAnalysis.Result? = null, val failure: Throwable? = null,
    val running: Boolean = false, val cancelled: Boolean = false) {
    data object Idle : RecordingAnalysisStatus()
    class Running(request: RecordingAnalysisRequest, progress: Int) :
        RecordingAnalysisStatus(request, progress, running = true)
    class Ready(request: RecordingAnalysisRequest, value: WavAnalysis.Result) :
        RecordingAnalysisStatus(request, 100, result = value)
    class Failed(request: RecordingAnalysisRequest, error: Throwable) :
        RecordingAnalysisStatus(request, failure = error)
    class Cancelled(request: RecordingAnalysisRequest, progress: Int) :
        RecordingAnalysisStatus(request, progress, cancelled = true)
    data object Disposed : RecordingAnalysisStatus()
}

/** One owner thread grants progress/terminal authority; cancelling work revokes that grant first. */
class RecordingAnalysisController(private val loader: RecordingAnalysisLoader,
                                  private val changed: (RecordingAnalysisStatus) -> Unit) {
    private class Operation(val request: RecordingAnalysisRequest, var task: RecordingAnalysisTask? = null)
    private var active: Operation? = null
    var state: RecordingAnalysisStatus = RecordingAnalysisStatus.Idle; private set

    fun request(request: RecordingAnalysisRequest) {
        if (state === RecordingAnalysisStatus.Disposed) return
        val previous = active
        val operation = Operation(request)
        active = operation
        previous?.task?.cancel()
        if (active !== operation) return
        publish(RecordingAnalysisStatus.Running(request, 0))
        if (active !== operation) return
        loader.submit(request, { progress(operation, it) }, { finish(operation, it) }).fold(
            { task -> if (active === operation) operation.task = task else task.cancel() },
            { error -> finish(operation, Result.failure(error)) },
        )
    }

    private fun progress(operation: Operation, percent: Int) {
        if (active !== operation) return
        publish(RecordingAnalysisStatus.Running(operation.request, percent.coerceIn(state.progress, 100)))
    }

    private fun finish(operation: Operation, result: Result<WavAnalysis.Result>) {
        if (active !== operation) return
        active = null
        operation.task = null
        publish(result.fold({ RecordingAnalysisStatus.Ready(operation.request, it) }, { error ->
            if (error is CancellationException) RecordingAnalysisStatus.Cancelled(operation.request, state.progress)
            else RecordingAnalysisStatus.Failed(operation.request, error)
        }))
    }

    fun cancel(clear: Boolean = false) {
        if (state === RecordingAnalysisStatus.Disposed) return
        val previous = active
        if (previous == null && !clear) return
        active = null
        try {
            publish(if (clear) RecordingAnalysisStatus.Idle else
                RecordingAnalysisStatus.Cancelled(checkNotNull(previous).request, state.progress))
        } finally { previous?.task?.cancel() }
    }

    fun close() {
        if (state === RecordingAnalysisStatus.Disposed) return
        val previous = active
        active = null
        try { publish(RecordingAnalysisStatus.Disposed) }
        finally { previous?.task?.cancel() }
    }

    private fun publish(next: RecordingAnalysisStatus) { state = next; changed(next) }
}
