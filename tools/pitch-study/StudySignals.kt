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

// Independent confirmation after the small initial holdout. No candidate changes.
internal fun replication() = sequence {
    val seeds = Random(1290827)
    for (profile in listOf(didge, flute)) {
        val low = profile.minHz.toDouble()
        val high = profile.initialMaxHz.toDouble()
        val frequencies = listOf(low, high) + List(30) { low * (high / low).pow(seeds.nextDouble()) }
        for (hz in frequencies) for (kind in categories) {
            yield(synthesize(kind, hz, 44100, profile, seeds.nextInt()))
        }
    }
}

/** Probe sensitivity to the strength of the residual fundamental, not just phase and F0. */
internal fun harmonicBalance() = sequence {
    for (profile in listOf(didge, flute)) {
        val frequencies = if (profile == didge) listOf(36.5, 47.0, 66.7, 71.2, 103.4, 139.0)
            else listOf(201.5, 295.0, 403.0, 557.2, 790.0, 1256.0)
        for (hz in frequencies) for (h in listOf(2, 3, 5)) for (percent in listOf(4, 8, 12, 16, 20, 28, 40)) {
            val frame = ShortArray(profile.windowSamples) { i ->
                val angle = 2 * PI * i * hz / 44100
                (14000 * (sin(h * angle) + percent / 100.0 * sin(angle + .7) + .06 * sin((h + 1) * angle))).roundToInt().toShort()
            }
            yield(Trial("dominant$h", profile, 44100, percent, hz, frame))
        }
    }
}

/** A passive filter excited only by noise has a spectral peak but no oscillator F0. */
internal fun resonantNoise() = sequence {
    val rate = 44100
    for (profile in listOf(didge, flute)) {
        val centers = if (profile == didge) listOf(42, 73, 118) else listOf(260, 610, 1360)
        for (center in centers) for (q in listOf(2, 5, 10)) for (seed in 71..86) {
            val random = Random((center * 1000L + q * 100 + seed))
            val pole = exp(-PI * center / q / rate)
            val coefficient = 2 * pole * cos(2 * PI * center / rate)
            var a = 0.0
            var b = 0.0
            fun sample(): Double {
                val next = coefficient * a - pole * pole * b + random.nextGaussian()
                b = a
                a = next
                return next
            }
            repeat(rate / 2) { sample() }
            val signal = DoubleArray(profile.windowSamples) { sample() }
            val gain = 20000 / signal.maxOf { abs(it) }
            yield(Trial("resonator${center}Hz-Q$q", profile, rate, seed, 0.0,
                ShortArray(signal.size) { (signal[it] * gain).roundToInt().toShort() }))
        }
    }
}

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
