package ar.didgetaller.core

import java.io.File

enum class RecordingPhase { IDLE, CAPTURING, SAVING, DISCARDING }

data class CapturedSession(val startedAt: Long, val instrument: InstrumentProfile,
                           val points: List<SessionAnalysis.Point> = emptyList())

/** Main-thread recording workflow. Device/file I/O is supplied at the boundary. */
class RecordingCoordinator(
    private val observer: Observer,
    private val publish: (File, CapturedSession, (Result<Unit>) -> Unit) -> Unit
) {
    data class State(val listening: Boolean = false, val phase: RecordingPhase = RecordingPhase.IDLE,
                     val points: List<SessionAnalysis.Point> = emptyList(), val startedAtMs: Long = 0)
    interface Observer {
        fun changed(state: State)
        fun saved()
        fun saveFailed(error: Throwable)
        fun discarded(success: Boolean)
        fun finalizationFailed(error: Throwable)
        fun libraryChanged()
        fun idleAfterDisposal()
    }

    private val handoff = CaptureHandoff<CapturedSession>()
    var state = State(); private set
    private var disposed = false

    /** A delayed permission result must not reopen hardware after its ViewModel was cleared. */
    fun startMonitor(start: () -> CaptureId?): Boolean {
        if (disposed) return false
        handoff.starting()
        val id = start() ?: return false
        monitorStarted(id)
        return true
    }

    fun monitorStarted(id: CaptureId) {
        check(!disposed)
        handoff.start(id)
        change(state.copy(listening = true))
    }

    fun monitorStopped() {
        handoff.stop()
        change(state.copy(listening = false))
    }

    fun begin(id: CaptureId, startedAt: Long, instrument: InstrumentProfile, elapsedMs: Long = 0) {
        check(!disposed && state.phase == RecordingPhase.IDLE)
        handoff.record(id, CapturedSession(startedAt, instrument))
        change(State(listening = true, phase = RecordingPhase.CAPTURING, startedAtMs = elapsedMs))
    }

    fun append(point: SessionAnalysis.Point) {
        if (state.phase != RecordingPhase.CAPTURING) return
        val updated = handoff.updateCurrent { it.copy(points = it.points + point) } ?: return
        change(state.copy(points = updated.points))
    }

    fun saving() {
        if (state.phase != RecordingPhase.CAPTURING) return
        handoff.stop()
        change(state.copy(listening = false, phase = RecordingPhase.SAVING))
    }

    fun discarding() {
        if (state.phase != RecordingPhase.CAPTURING) return
        handoff.stop()
        change(state.copy(listening = false, phase = RecordingPhase.DISCARDING))
    }

    fun terminal(event: CaptureTerminal) {
        val delivery = handoff.terminal(event) ?: return
        if (delivery.endsMicrophone) change(state.copy(listening = false))
        val snapshot = delivery.snapshot ?: return
        complete(delivery, snapshot)
    }

    private fun complete(delivery: CaptureHandoff.Delivery<CapturedSession>, snapshot: CapturedSession) {
        val event = delivery.event
        when (val outcome = event.outcome) {
            is RecordingCapture.Outcome.Ready -> {
                if (delivery.ownsRecording) change(state.copy(phase = RecordingPhase.SAVING))
                publish(outcome.file, snapshot) { published(event.id, it) }
            }
            is RecordingCapture.Outcome.Discarded -> {
                end(delivery)
                if (canNotify(event.id)) observer.discarded(outcome.success)
            }
            is RecordingCapture.Outcome.Failed -> {
                end(delivery)
                if (canNotify(event.id)) observer.finalizationFailed(outcome.error)
            }
            null -> end(delivery)
        }
    }

    private fun end(delivery: CaptureHandoff.Delivery<CapturedSession>) {
        if (delivery.ownsRecording) change(state.copy(phase = RecordingPhase.IDLE, points = emptyList()))
        closeIfIdle()
    }

    private fun published(id: CaptureId, result: Result<Unit>) {
        // Duplicate acknowledgements cannot publish UI state, notices or library changes twice.
        if (!handoff.published(id)) return
        change(state.copy(phase = RecordingPhase.IDLE))
        if (result.isSuccess && !disposed) observer.libraryChanged()
        reportPublication(id, result)
        closeIfIdle()
    }

    private fun reportPublication(id: CaptureId, result: Result<Unit>) {
        if (!canNotify(id)) return
        result.fold({ observer.saved() }, observer::saveFailed)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        closeIfIdle()
    }

    private fun canNotify(id: CaptureId) = !disposed && handoff.canNotify(id)
    private fun closeIfIdle() {
        if (disposed && state.phase == RecordingPhase.IDLE) observer.idleAfterDisposal()
    }
    private fun change(next: State) { state = next; observer.changed(next) }
}
