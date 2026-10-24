package ar.didgetaller.core

import java.util.concurrent.CancellationException

/** Current YIN decisions from the original saved PCM; never writes session history. */
object SavedYinReplay {
    const val VERSION = 1
    private const val RATE = 44100
    private const val CHUNK = 4096
    private const val MAX_SECONDS = 30 * 60

    data class Frame(
        val startFrame: Long,
        val endFrame: Long,
        val acceptedHz: Double?,
        val candidateHz: Double?,
        val periodicity: Double,
        val rms: Double,
        val comparisonRms: Double?,
        val reason: String,
    ) {
        val centerMs: Double get() = (startFrame + endFrame) * 500.0 / RATE
    }

    data class Result(val version: Int, val frames: List<Frame>, val points: List<SessionAnalysis.Point>)

    /** A stored pitch above the normal detector's tolerance proves the expanded capture range. */
    fun usesExpandedRange(captured: List<SessionAnalysis.Point>): Boolean = captured.any { point ->
        point.hz?.let { it > InstrumentProfile.DIDGERIDOO.initialMaxHz * 1.05 &&
            it <= InstrumentProfile.DIDGERIDOO.maxHz * 1.05 } == true
    }

    fun analyze(source: MonoPcmSource, expandedRange: Boolean = false,
                cancelled: () -> Boolean = { false }): Result {
        require(source.sampleRate == RATE && source.frameCount in 1..RATE.toLong() * MAX_SECONDS)
        val stream = PitchStream(InstrumentProfile.DIDGERIDOO,
            maxHz = if (expandedRange) InstrumentProfile.DIDGERIDOO.maxHz
                    else InstrumentProfile.DIDGERIDOO.initialMaxHz,
            includeVisuals = false)
        val chunk = ShortArray(CHUNK)
        val frames = ArrayList<Frame>()
        val points = ArrayList<SessionAnalysis.Point>()
        var at = 0L
        while (at < source.frameCount) {
            if (cancelled()) throw CancellationException()
            val count = source.read(at, chunk)
            check(count in 1..minOf(CHUNK.toLong(), source.frameCount - at).toInt()) {
                "Incomplete saved WAV read"
            }
            at += count
            stream.append(chunk, count)?.let { reading ->
                val diagnostic = checkNotNull(reading.diagnostic)
                val accepted = reading.hz.takeIf { it > 0 && it.isFinite() }
                frames += Frame(at - InstrumentProfile.DIDGERIDOO.windowSamples, at, accepted,
                    diagnostic.candidateHz, reading.confidence, reading.rms,
                    diagnostic.comparisonRms, diagnostic.reason)
                points += SessionAnalysis.Point(at * 1000 / RATE, accepted, reading.confidence)
            }
        }
        return Result(VERSION, frames, points)
    }
}
