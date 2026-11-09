package ar.didgetaller.core

import java.io.File
import java.util.UUID

/** One microphone worker's file ownership. A retired worker never shares this state with its successor. */
class RecordingCapture(
    private val openWriter: (File, Int) -> ProgressiveWavWriter = { file, rate -> ProgressiveWavWriter(file, rate) }
) {
    val id = CaptureId()
    sealed interface Outcome {
        data class Ready(val file: File) : Outcome
        data class Failed(val error: Throwable) : Outcome
        data class Discarded(val success: Boolean) : Outcome
    }

    private sealed interface State {
        data object Monitoring : State
        data class Recording(val writer: ProgressiveWavWriter, val limitSamples: Long, var discard: Discard? = null) : State
        data object Finished : State
    }

    // A claimed discard remains a discard even if its rename fails; it must never authorize saving.
    private data class Discard(val file: File?)
    private var state: State = State.Monitoring

    @Synchronized
    fun start(file: File, sampleRate: Int, limitSeconds: Int): Boolean {
        if (state != State.Monitoring) return false
        require(sampleRate > 0 && limitSeconds > 0)
        state = State.Recording(openWriter(file, sampleRate), sampleRate.toLong() * limitSeconds)
        return true
    }

    /** Returns true when this recording's duration limit has been reached. */
    @Synchronized
    fun append(samples: ShortArray, count: Int): Boolean {
        require(count in 0..samples.size)
        val current = state as? State.Recording ?: return false
        val remaining = current.limitSamples - current.writer.samplesWritten
        val accepted = minOf(count.toLong(), remaining).toInt()
        if (accepted > 0) current.writer.append(samples, accepted)
        return current.writer.samplesWritten >= current.limitSamples
    }

    @Synchronized
    fun discard(): Boolean {
        val current = state as? State.Recording ?: return false
        if (current.discard != null) return false
        val partial = current.writer.file
        val claimed = runCatching {
            require(partial.extension == "part" && UUID.fromString(partial.nameWithoutExtension).toString() == partial.nameWithoutExtension)
            val target = File(partial.parentFile, "${partial.nameWithoutExtension}.discard")
            check(!target.exists() && partial.renameTo(target))
            target
        }.getOrNull()
        current.discard = Discard(claimed)
        return true
    }

    fun finish(released: Boolean): Outcome? = complete(released)?.outcome

    /** Claim the terminal envelope once, including monitor-only workers. A duplicate emits no event. */
    fun complete(released: Boolean): CaptureTerminal? {
        val current = synchronized(this) {
            val owned = state
            state = State.Finished
            owned
        }
        return when (current) {
            State.Finished -> null
            State.Monitoring -> CaptureTerminal(id, null)
            is State.Recording -> CaptureTerminal(id, finalizeRecording(current, released))
        }
    }

    /** File I/O happens outside the state lock; only the claiming worker owns this writer. */
    private fun finalizeRecording(current: State.Recording, released: Boolean): Outcome {
        val finalized = runCatching { current.writer.close() }
        val discard = current.discard
        if (discard != null) {
            val removed = runCatching { check(discard.file != null && discard.file.delete()) }
            return Outcome.Discarded(released && finalized.isSuccess && removed.isSuccess)
        }
        return finalized.fold({ Outcome.Ready(current.writer.file) }, { Outcome.Failed(it) })
    }
}
