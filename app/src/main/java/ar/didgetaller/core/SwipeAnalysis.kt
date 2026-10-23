package ar.didgetaller.core

import kotlin.math.*
import java.util.concurrent.CancellationException

/** Experimental hypotheses only; never a SessionAnalysis or persisted measurement. */
data class SwipeCurve(val samples: List<Sample>, val hopMs: Double, val minHz: Int, val maxHz: Int) {
    data class Sample(val timeMs: Double, val hz: Double?)
    fun linked(index: Int): Boolean {
        if (index <= 0 || index >= samples.size) return false
        val a = samples[index - 1]; val b = samples[index]
        val first = a.hz ?: return false; val second = b.hz ?: return false
        return b.timeMs - a.timeMs <= hopMs * 1.01 && abs(1200 * log2(second / first)) < 600
    }
    fun nearest(timeMs: Long) = samples.minByOrNull { abs(it.timeMs - timeMs) }
        ?.takeIf { abs(it.timeMs - timeMs) <= hopMs / 2 }
}

/** Bounded, overlapping calls to the original SPTK SWIPE' kernel. No temporal correction. */
class SwipeAnalysis(private val kernel: (ShortArray, Int, Int, Int, Int, Int) -> DoubleArray = SwipeNative::estimate) {
    fun analyze(source: MonoPcmSource, instrument: InstrumentProfile,
                cancelled: () -> Boolean = { false }, progress: (Int) -> Unit = {}): SwipeCurve {
        require(instrument == InstrumentProfile.DIDGERIDOO && source.sampleRate == 44100) {
            "SWIPE comparison supports 44100 Hz didgeridoo sessions only"
        }
        val n = instrument.windowSamples
        val hop = n / 2
        val offset = if (instrument == InstrumentProfile.DIDGERIDOO) n / 4 else 0
        val first = offset.toLong() + n / 2
        val count = if (source.frameCount < first + n / 2) 0 else (source.frameCount - n / 2 - first) / hop + 1
        require(count <= 20000) { "Recording exceeds the comparison budget" }
        val fft = 2.0.pow(round(log2(8.0 * source.sampleRate / instrument.minHz))).toInt()
        val context = ((max(n, 2 * fft) + hop - 1) / hop) * hop
        val blockSize = 8
        require(context * 2 + blockSize * hop <= 262144) { "Sample rate exceeds the comparison budget" }
        val samples = ArrayList<SwipeCurve.Sample>(count.toInt())
        var lastProgress = -1
        fun check() { if (cancelled()) throw CancellationException() }
        check()
        var index = 0
        while (index < count) {
            check()
            val endIndex = min(index + blockSize, count.toInt())
            val center = first + index.toLong() * hop
            val lastCenter = first + (endIndex - 1L) * hop
            val start = max(offset.toLong(), center - context)
            val end = min(source.frameCount, lastCenter + context)
            val pcm = ShortArray((end - start).toInt())
            check(source.read(start, pcm) == pcm.size) { "Incomplete WAV read" }
            val result = kernel(pcm, pcm.size, source.sampleRate, hop, instrument.minHz, instrument.initialMaxHz)
            check()
            for (i in index until endIndex) {
                val frame = first + i.toLong() * hop
                val local = (frame - start).toInt()
                var energy = 0.0
                for (at in local - n / 2 until local + n / 2) {
                    val value = pcm[at] / 32768.0
                    energy += value * value
                }
                val estimate = result.getOrNull(local / hop)
                val hz = estimate?.takeIf { it.isFinite() && it > 0 && sqrt(energy / n) >= .005 }
                samples += SwipeCurve.Sample(frame * 1000.0 / source.sampleRate, hz)
            }
            index = endIndex
            val percent = (index * 100 / count).toInt()
            if (percent != lastProgress) { progress(percent); lastProgress = percent }
        }
        progress(100)
        return SwipeCurve(samples, hop * 1000.0 / source.sampleRate, instrument.minHz, instrument.initialMaxHz)
    }
}
