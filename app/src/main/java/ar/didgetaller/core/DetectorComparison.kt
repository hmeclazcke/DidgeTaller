package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.abs

/** Paired estimates use the same PCM window, sample clock and instrument range. */
class DetectorComparison(val frames: List<Frame>, val durationMs: Long, val hopMs: Double, val sampleRate: Int,
                         private val algorithm: PitchAlgorithm) {
    fun supports(request: PitchAlgorithm) = request == PitchAlgorithm.STANDARD || algorithm == request
    data class Frame(val centerMs: Double, val standard: Double?, val harmonic: Double?)

    fun at(timeMs: Long): Frame? {
        val index = frames.binarySearchBy(timeMs.toDouble()) { it.centerMs }
        if (index >= 0) return frames[index]
        val next = -index - 1
        return listOfNotNull(frames.getOrNull(next - 1), frames.getOrNull(next))
            .minByOrNull { abs(it.centerMs - timeMs) }?.takeIf { abs(it.centerMs - timeMs) <= hopMs / 2 }
    }

    fun marks(frequency: (Frame) -> Double?): List<TraceDrawing.Mark> = buildList {
        var previous: TraceDrawing.Vertex? = null
        for (frame in frames) {
            val current = frequency(frame)?.let { TraceDrawing.Vertex(frame.centerMs, it) }
            if (current != null) add(previous?.let { TraceDrawing.Segment(it, current) } ?: TraceDrawing.Dot(current))
            previous = current
        }
    }

    companion object {
        private const val CHUNK = 4096

        fun analyze(source: MonoPcmSource, profile: InstrumentProfile, upperHz: Int, algorithm: PitchAlgorithm,
                    cancelled: () -> Boolean = { false }): DetectorComparison {
            val rate = source.sampleRate
            require(rate in 8000..192000 && source.frameCount in 1..rate.toLong() * 1800)
            val stream = PitchStream(profile, maxHz = upperHz, includeVisuals = false, algorithm = algorithm, sampleRate = rate)
            val chunk = ShortArray(CHUNK)
            val frames = mutableListOf<Frame>()
            var end = 0L
            while (end < source.frameCount) {
                if (cancelled()) throw CancellationException()
                val count = source.read(end, chunk)
                require(count == minOf(CHUNK.toLong(), source.frameCount - end).toInt())
                end += count
                stream.append(chunk, count)?.let { reading ->
                    val standard = LivePitch.next(LivePitch.State(), reading, profile).storedHz
                    val assisted = reading.harmonic?.hz?.takeIf { it > 0.0 }
                    frames += Frame((end - profile.windowSamples / 2) * 1000.0 / rate, standard, assisted)
                }
            }
            return DetectorComparison(frames, end * 1000 / rate, CHUNK * profile.analysisChunks * 1000.0 / rate, rate, algorithm)
        }
    }
}
