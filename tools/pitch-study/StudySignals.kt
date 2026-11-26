package ar.didgetaller.study

import ar.didgetaller.core.InstrumentProfile
import java.util.Random
import kotlin.math.*

internal data class Trial(val category: String, val profile: InstrumentProfile, val rate: Int,
    val seed: Int, val target: Double, val pcm: ShortArray, val elapsedMs: Double = 0.0)

private val didge = InstrumentProfile.DIDGERIDOO
private val flute = InstrumentProfile.NATIVE_AMERICAN_FLUTE
private val categories = listOf("pure", "rich", "missing", "second", "third", "fifth", "envelope",
    "am", "fm", "breathDip", "snr20", "snr6", "snr0", "quiet", "silence", "noise", "impulse", "twoSources")

internal fun synthetic(held: Boolean) = sequence {
    val randomSeed = if (held) 9301 else 742
    for (profile in listOf(didge, flute)) {
        val frequencies = if (profile == didge) {
            if (held) listOf(35.8, 64.3, 92.7, 137.1) else listOf(39.0, 72.1, 116.0)
        } else if (held) listOf(202.3, 487.6, 918.7, 1550.0) else listOf(218.0, 442.0, 1280.0)
        for (rate in listOf(32000, 44100, 48000)) for (f in frequencies) {
            for ((index, kind) in categories.withIndex()) {
                val seed = randomSeed + index * 11 + f.toInt() + rate
                yield(synthesize(kind, f, rate, profile, seed))
            }
        }
    }
}

private fun synthesize(kind: String, hz: Double, rate: Int, profile: InstrumentProfile, seed: Int): Trial {
    val rng = Random(seed.toLong())
    val phase = rng.nextDouble() * 2 * PI
    val phases = DoubleArray(10) { rng.nextDouble() * 2 * PI }
    val weights = when (kind) {
        "pure", "fm", "quiet" -> listOf(1.0)
        "missing" -> listOf(0.0, .8, .6)
        "second" -> listOf(.07, 1.0, .05)
        "third" -> listOf(.06, .09, 1.0)
        "fifth" -> listOf(.06, .10, .04, .07, 1.0)
        else -> listOf(1.0, .6, .32, .21, .18, .12, .1, .09, .08, .07)
    }
    val signal = DoubleArray(profile.windowSamples) { i ->
        val t = i.toDouble() / rate
        val modulation = if (kind == "fm") .012 * hz / 4 * (cos(phase) - cos(8 * PI * t + phase)) else 0.0
        val angle = 2 * PI * hz * t + modulation
        var sum = 0.0
        for ((h, weight) in weights.withIndex()) {
            if ((h + 1) * hz >= rate / 2.0) break
            // Moving spectral emphasis on one harmonic family, not labelled human vowels.
            val emphasis = if (kind == "envelope") exp(-.3 * (h - 4 - 3 * sin(14 * t + phase)).pow(2)) + .04 else 1.0
            sum += weight * emphasis * sin((h + 1) * angle + phases[h])
        }
        when (kind) {
            "silence", "noise" -> 0.0
            "impulse" -> if (i == profile.windowSamples / 3) 2.8 else 0.0
            "twoSources" -> .15 * sum + .28 * sin(2 * PI * hz * sqrt(2.0) * t)
            "am" -> .15 * sum * (.55 + .45 * sin(2 * PI * 6 * t + phase))
            "breathDip" -> .24 * sum * (1 - .98 * exp(-((i.toDouble() / profile.windowSamples - .65) / .17).pow(2)))
            "quiet" -> .003 * sum
            else -> .24 * sum
        }
    }
    val snr = when (kind) { "snr20" -> 20.0; "snr6" -> 6.0; "snr0" -> 0.0; else -> null }
    val noise = DoubleArray(signal.size) { if (snr != null || kind == "noise") rng.nextGaussian() else 0.0 }
    val gain = when {
        snr != null -> sqrt(signal.sumOf { it * it } / noise.sumOf { it * it }) * 10.0.pow(-snr / 20)
        kind == "noise" -> .18
        else -> 0.0
    }
    for (i in signal.indices) signal[i] += gain * noise[i]
    val scale = 30000 / max(1.0, signal.maxOf { abs(it) })
    val pcm = ShortArray(signal.size) { (signal[it] * scale).roundToInt().toShort() }
    val endTime = (signal.size - 1).toDouble() / rate
    val target = when (kind) {
        "silence", "noise", "impulse" -> 0.0
        "twoSources" -> Double.NaN
        "fm" -> hz * (1 + .012 * sin(8 * PI * endTime + phase))
        else -> hz
    }
    return Trial(kind, profile, rate, seed, target, pcm)
}

/** Vary where the note changes relative to the real 4096-sample callback grid. */
internal fun transitions(held: Boolean) = sequence {
    for (profile in listOf(didge, flute)) for (offset in listOf(341, 1777, 3861, 6123)) {
        val from = if (profile == didge) 61.0 else 419.0
        val to = from * if (held) 2.13 else 1.84
        val boundary = 40960 + offset
        val rate = 44100
        val pcm = ShortArray(boundary + 35000) { i ->
            val cycles = (from * min(i, boundary) + to * max(0, i - boundary)) / rate
            (11000 * sin(2 * PI * cycles)).toInt().toShort()
        }
        val hop = 4096 * profile.analysisChunks
        val first = profile.windowSamples + hop - 4096
        for (end in first..pcm.size step hop) if (end >= boundary) {
            yield(Trial("register", profile, rate, offset, to, pcm.copyOfRange(end - profile.windowSamples, end),
                (end - boundary) * 1000.0 / rate))
        }
    }
}
