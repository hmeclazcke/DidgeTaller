package ar.didgetaller.core

/** WAV facts, validated once at the loader boundary; not persisted session metadata. */
data class WorkspaceSource(val sessionId: String, val frames: Long, val rate: Int,
                           val signature: String, val level: AudioLevel?) {
    init { require(sessionId.isNotBlank() && frames >= 0 && rate > 0 && signature.isNotBlank()) }
    val durationMs: Long = Math.addExact(Math.multiplyExact(frames / rate, 1000L), frames % rate * 1000 / rate)
}

data class VisualRequest(val startMs: Long, val endMs: Long, val lowHz: Double = 0.0,
                         val highHz: Double = 500.0, val logarithmic: Boolean = false,
                         val rangeDb: Double = 70.0, val reference: Double = 0.0) {
    init {
        require(endMs > startMs)
        require(lowHz.isFinite() && highHz.isFinite() && highHz > lowHz && lowHz >= 0)
        require(rangeDb.isFinite() && rangeDb > 0 && reference.isFinite() && reference >= 0)
    }
    fun dataKey() = VisualDataKey(startMs, endMs, lowHz, highHz, logarithmic)
    companion object {
        fun forView(kind: VisualKind, viewport: PlotViewport, logarithmic: Boolean = false,
                    rangeDb: Double = 70.0, reference: Double = 0.0): VisualRequest {
            val start = viewport.x.low.toLong()
            val end = viewport.x.high.toLong().coerceAtLeast(start + 1)
            if (kind == VisualKind.WAVE) return VisualRequest(start, end)
            return VisualRequest(start, end, viewport.y.low.coerceAtLeast(0.0), viewport.y.high,
                logarithmic, rangeDb, reference)
        }
    }
}

/** Palette/reference changes reuse spectral power, never stale colors or another viewport. */
data class VisualDataKey(val startMs: Long, val endMs: Long, val lowHz: Double,
                         val highHz: Double, val logarithmic: Boolean) {
    fun cacheKey() = "$startMs-$endMs-$lowHz-$highHz-$logarithmic"
}

enum class VisualKind { WAVE, SPECTROGRAM }
sealed interface WorkspaceStatus {
    data object Empty : WorkspaceStatus
    data object Opening : WorkspaceStatus
    data object Rendering : WorkspaceStatus
    data object Ready : WorkspaceStatus
    data object Cancelled : WorkspaceStatus
    data class Failed(val message: String) : WorkspaceStatus
}

/** One immutable published snapshot. Image is opaque to the domain, Bitmap only in Android. */
data class WorkspaceState<Image>(val sessionId: String? = null, val source: WorkspaceSource? = null,
    val wave: AudioVisuals.Wave? = null, val spectrogram: AudioVisuals.Spectrogram? = null,
    val bitmap: Image? = null, val status: WorkspaceStatus = WorkspaceStatus.Empty, val progress: Int = 0) {
    val durationMs get() = source?.durationMs ?: 0L
    val rate get() = source?.rate ?: 44100
    val frames get() = source?.frames ?: 0L
    val signature get() = source?.signature.orEmpty()
    val level get() = source?.level
    val loading get() = status == WorkspaceStatus.Opening || status == WorkspaceStatus.Rendering
    val error get() = when (val current = status) {
        is WorkspaceStatus.Failed -> current.message
        WorkspaceStatus.Cancelled -> "Visualization canceled."
        else -> null
    }
}

sealed interface PreparedVisual<out Image> {
    data class Wave(val data: AudioVisuals.Wave) : PreparedVisual<Nothing>
    data class Spectral<Image>(val data: AudioVisuals.Spectrogram, val bitmap: Image) : PreparedVisual<Image>
}

data class VisualWork(val source: WorkspaceSource, val request: VisualRequest, val kind: VisualKind,
                      val wave: AudioVisuals.Wave?, val tile: AudioVisuals.Spectrogram?)

fun interface WorkspaceTask { fun cancel() }

/** Callbacks and requests share the UI owner thread; workers do not write UI state. */
interface WorkspaceLoader<Image> {
    fun open(sessionId: String, complete: (Result<WorkspaceSource>) -> Unit): WorkspaceTask
    fun render(work: VisualWork, progress: (Int) -> Unit,
               complete: (Result<PreparedVisual<Image>>) -> Unit): WorkspaceTask
}
