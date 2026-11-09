package ar.didgetaller.core

import java.io.Closeable
import java.util.concurrent.CancellationException

data class VisualFrames(val start: Long, val end: Long) {
    companion object {
        fun of(source: WorkspaceSource, request: VisualRequest): VisualFrames {
            require(source.frames >= 2)
            fun frame(ms: Long): Long {
                val bounded = ms.coerceIn(0, source.durationMs)
                return bounded / 1000 * source.rate + bounded % 1000 * source.rate / 1000
            }
            val start = frame(request.startMs).coerceIn(0, source.frames - 2)
            return VisualFrames(start, frame(request.endMs).coerceIn(start + 2, source.frames))
        }
    }
}

data class WorkspaceRaster(val tile: AudioVisuals.Spectrogram, val pixels: IntArray)

/** Per-operation readers are closed on success, failure and cooperative cancellation. */
class WorkspaceData<Source>(private val openSource: (String) -> Source,
    private val fingerprint: (String, () -> Boolean) -> String,
    private val cachedTile: (String, String, String, () -> AudioVisuals.Spectrogram) -> AudioVisuals.Spectrogram)
    where Source : MonoPcmSource, Source : Closeable {

    fun open(sessionId: String, cancelled: () -> Boolean): WorkspaceSource = openSource(sessionId).use { source ->
        checkCancellation(cancelled)
        val signature = fingerprint(sessionId, cancelled)
        val level = AudioLevel.read(source, cancelled)
        WorkspaceSource(sessionId, source.frameCount, source.sampleRate, signature, level)
    }

    fun wave(work: VisualWork, cancelled: () -> Boolean): AudioVisuals.Wave = read(work, cancelled) { source, frames ->
        work.wave ?: AudioVisuals.waveform(source, frames.start, frames.end, cancelled = cancelled)
    }

    fun spectral(work: VisualWork, cancelled: () -> Boolean, progress: (Int) -> Unit): WorkspaceRaster =
        read(work, cancelled) { source, frames ->
            val request = work.request
            val tile = work.tile ?: cachedTile(work.source.sessionId, work.source.signature, request.dataKey().cacheKey()) {
                AudioVisuals.spectrogram(source, frames.start, frames.end, request.lowHz, request.highHz,
                    request.logarithmic, cancelled = cancelled, progress = progress)
            }
            WorkspaceRaster(tile, SpectrogramRaster.pixels(tile, request.rangeDb, request.reference, cancelled))
        }

    private fun <T> read(work: VisualWork, cancelled: () -> Boolean, compute: (Source, VisualFrames) -> T): T =
        openSource(work.source.sessionId).use { source ->
            checkCancellation(cancelled)
            check(source.sampleRate == work.source.rate && source.frameCount == work.source.frames) {
                "Visualization source changed."
            }
            val result = compute(source, VisualFrames.of(work.source, work.request))
            checkCancellation(cancelled)
            result
        }

    private fun checkCancellation(cancelled: () -> Boolean) {
        if (cancelled() || Thread.currentThread().isInterrupted) throw CancellationException()
    }
}
