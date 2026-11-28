package ar.didgetaller.study

import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.LivePitch
import ar.didgetaller.core.PitchDetector
import kotlin.math.*

/** All three decisions use the production profile's final acceptance policy. */
internal enum class Estimator {
    Production, RecentHalf, NormalizedCorrelation;

    fun estimate(frame: ShortArray, rate: Int, profile: InstrumentProfile): Pair<Double, Double> {
        val reading = when (this) {
            Production -> PitchDetector.analyze(frame, rate, profile.minHz, profile.initialMaxHz, false)
            RecentHalf -> PitchDetector.analyze(frame.copyOfRange(frame.size / 2, frame.size),
                rate, profile.minHz, profile.initialMaxHz, false)
            NormalizedCorrelation -> nsdf(frame, rate, profile)
        }
        val accepted = LivePitch.next(LivePitch.State(), reading, profile).storedHz ?: 0.0
        return accepted to reading.confidence
    }
}

// MPM-inspired independent time-domain comparator. No Tartini code or temporal tracking.
private fun nsdf(pcm: ShortArray, rate: Int, profile: InstrumentProfile): PitchDetector.Reading {
    val stride = if (profile == InstrumentProfile.DIDGERIDOO) 4 else 1
    val fs = rate.toDouble() / stride
    val last = ceil(fs / profile.minHz).toInt() + 2
    val size = 2200 + last
    val offset = if (stride == 4) 0 else (pcm.size - size) / 2
    val mean = pcm.average()
    val x = DoubleArray(size) { i ->
        var sum = 0.0
        repeat(stride) { j -> sum += pcm[(offset + i) * stride + j] - mean }
        sum / stride / 32768
    }
    if (x.sumOf { it * it } < size * .005 * .005) return PitchDetector.Reading()
    val curve = DoubleArray(last + 1)
    for (lag in 1..last) {
        var product = 0.0
        var energy = 0.0
        for (i in lag until size) {
            val a = x[i]
            val b = x[i - lag]
            product += a * b
            energy += a * a + b * b
        }
        curve[lag] = if (energy > 0) 2 * product / energy else 0.0
    }
    // Only the largest peak of each positive lobe after the first negative crossing.
    val peaks = mutableListOf<Int>()
    var crossed = false
    var peak = 0
    for (lag in 1 until last) {
        if (curve[lag] <= 0) {
            crossed = true
            if (peak > 0) peaks += peak
            peak = 0
        } else if (crossed && curve[lag] >= curve[lag - 1] && curve[lag] > curve[lag + 1]
            && (peak == 0 || curve[lag] > curve[peak])) peak = lag
    }
    if (peak > 0) peaks += peak
    val candidates = peaks.filter { it >= floor(fs / profile.initialMaxHz).toInt() }
    val highest = candidates.maxOfOrNull { curve[it] } ?: return PitchDetector.Reading()
    val best = candidates.first { curve[it] >= .93 * highest }
    val a = curve[best - 1]
    val b = curve[best]
    val c = curve[best + 1]
    val delta = (0.5 * (a - c) / (a - 2 * b + c)).coerceIn(-.5, .5)
    val hz = fs / (best + delta)
    return PitchDetector.Reading(if (b >= .65 && hz in profile.minHz * .95..profile.initialMaxHz * 1.05) hz else 0.0, b)
}
