package ar.didgetaller.core

import java.util.concurrent.CancellationException

/** Sole writer of visualization state; loader callbacks must return to this owner thread. */
class WorkspaceController<Image>(private val loader: WorkspaceLoader<Image>,
                                  private val changed: (WorkspaceState<Image>) -> Unit) {
    var state = WorkspaceState<Image>(); private set
    private data class Target(val request: VisualRequest, val kind: VisualKind)
    private sealed class Operation(val sessionId: String) {
        var task: WorkspaceTask? = null
        class Opening(sessionId: String) : Operation(sessionId)
        class Rendering(sessionId: String, val target: Target) : Operation(sessionId)
    }
    private var active: Operation? = null
    private var completed: Target? = null
    private var waveKey: VisualDataKey? = null
    private var tileKey: VisualDataKey? = null
    private var closed = false

    fun open(sessionId: String) {
        if (closed) return
        resetData()
        val operation = Operation.Opening(sessionId)
        active = operation
        publish(WorkspaceState(sessionId = sessionId, status = WorkspaceStatus.Opening))
        launch(operation) { loader.open(sessionId) { opened(operation, it) } }
    }

    fun render(sessionId: String, request: VisualRequest, kind: VisualKind) {
        val source = state.source ?: return
        if (closed || source.sessionId != sessionId || source.frames < 2) return
        val target = Target(request, kind)
        if ((active as? Operation.Rendering)?.target == target || (!state.loading && state.error == null && completed == target)) return
        invalidate()
        val operation = Operation.Rendering(sessionId, target)
        active = operation
        val key = request.dataKey()
        val work = VisualWork(source, request, kind, state.wave.takeIf { waveKey == key },
            state.spectrogram.takeIf { tileKey == key })
        publish(state.copy(status = WorkspaceStatus.Rendering, progress = 0))
        launch(operation) { loader.render(work, { progress(operation, it) }, { rendered(operation, it) }) }
    }

    private fun launch(operation: Operation, start: () -> WorkspaceTask) {
        // An observer may synchronously clear, dispose or replace this request.
        if (active !== operation) return
        try {
            val task = start()
            if (active === operation) operation.task = task else task.cancel()
        } catch (error: Exception) {
            if (claim(operation)) failed(error)
        }
    }

    private fun opened(operation: Operation.Opening, result: Result<WorkspaceSource>) {
        if (!claim(operation)) return
        result.fold({ source ->
            if (source.sessionId != operation.sessionId) failed(IllegalStateException("Visualization unavailable."))
            else publish(WorkspaceState(sessionId = source.sessionId, source = source, status = WorkspaceStatus.Ready))
        }, ::failed)
    }

    private fun rendered(operation: Operation.Rendering, result: Result<PreparedVisual<Image>>) {
        if (!claim(operation)) return
        result.fold({ visual ->
            accept(operation.target, visual)
        }, ::failed)
    }

    private fun accept(target: Target, visual: PreparedVisual<Image>) {
        val kind = when (visual) {
            is PreparedVisual.Wave -> VisualKind.WAVE
            is PreparedVisual.Spectral -> VisualKind.SPECTROGRAM
        }
        if (kind != target.kind) { failed(IllegalStateException("Visualization unavailable.")); return }
        val next = when (visual) {
            is PreparedVisual.Wave -> {
                waveKey = target.request.dataKey()
                state.copy(wave = visual.data)
            }
            is PreparedVisual.Spectral -> {
                tileKey = target.request.dataKey()
                state.copy(spectrogram = visual.data, bitmap = visual.bitmap)
            }
        }
        completed = target
        publish(next.copy(status = WorkspaceStatus.Ready, progress = 100))
    }

    private fun progress(operation: Operation, percent: Int) {
        if (active === operation) publish(state.copy(progress = percent.coerceIn(0, 100)))
    }

    private fun claim(operation: Operation): Boolean {
        if (active !== operation) return false
        active = null
        operation.task = null
        return true
    }

    private fun invalidate() {
        val previous = active
        active = null
        previous?.task?.cancel()
    }

    private fun failed(error: Throwable) {
        val status = if (error is CancellationException) WorkspaceStatus.Cancelled else
            WorkspaceStatus.Failed(error.message?.takeIf { it.isNotBlank() } ?: "Visualization unavailable.")
        publish(state.copy(status = status))
    }

    fun cancel() {
        if (active == null) return
        invalidate()
        failed(CancellationException())
    }

    fun clear() {
        if (closed) return
        resetData()
        publish(WorkspaceState())
    }

    private fun resetData() {
        invalidate()
        completed = null; waveKey = null; tileKey = null
    }

    fun close() {
        if (closed) return
        closed = true
        resetData()
        publish(WorkspaceState())
    }

    private fun publish(next: WorkspaceState<Image>) { state = next; changed(next) }
}
