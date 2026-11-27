package ar.didgetaller.study

import ar.didgetaller.core.*
import kotlin.math.*

internal data class Decision(val hz: Double, val strength: Double)
internal data class Line(val hz: Double, val amplitude: Double)
private data class Fit(val hz: Double, val score: Double, val harmonics: Set<Int>)

internal fun reading(t: Trial, pcm: ShortArray, observe: ((PitchDetector.Diagnostic) -> Unit)? = null): Decision {
    val p = t.profile
    val raw = PitchDetector.analyzeWithDiagnostics(pcm, t.rate, p.minHz, t.upperHz, false, observe)
    return Decision(LivePitch.next(LivePitch.State(), raw, p).storedHz ?: 0.0, raw.confidence)
}

/** Is the period reported by the long window still supported by the last few cycles? */
internal fun adaptive(t: Trial): Decision {
    val original = reading(t, t.pcm)
    if (original.hz == 0.0) return original
    val lag = (t.rate / original.hz).roundToInt()
    val end = t.pcm.size
    val start = end - min(end / 2 - lag, max(t.rate / 32, 3 * lag))
    var difference = 0.0
    var energy = 0.0
    for (i in start until end) {
        val a = t.pcm[i].toDouble()
        val b = t.pcm[i - lag].toDouble()
        difference += (a - b) * (a - b)
        energy += a * a + b * b
    }
    if (energy == 0.0 || difference < .28 * energy) return original
    val recent = reading(t, t.pcm.copyOfRange(end / 2, end))
    return if (recent.strength >= .85 && recent.hz > 0) recent else original
}

internal fun spectralLines(spectrum: PowerSpectrum, high: Double): List<Line> {
    val power = spectrum.density
    val last = min(power.lastIndex - 1, (high / spectrum.binHz).toInt())
    val largest = (2..last).maxOf { power[it] }
    return (2..last).mapNotNull { k ->
        val center = power[k]
        if (center <= largest * .0004 || center < power[k - 1] || center <= power[k + 1]) return@mapNotNull null
        val a = ln(max(power[k - 1], 1e-30))
        val b = ln(center)
        val c = ln(max(power[k + 1], 1e-30))
        val offset = (.5 * (a - c) / (a - 2 * b + c)).coerceIn(-.5, .5)
        Line((k + offset) * spectrum.binHz, sqrt(center))
    }.sortedByDescending { it.amplitude }.take(16)
}

internal tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

private fun fit(f: Double, lines: List<Line>, bin: Double): Fit {
    var support = 0.0
    var estimate = 0.0
    val orders = mutableSetOf<Int>()
    for (line in lines) {
        val order = (line.hz / f).roundToInt()
        if (order !in 1..12 || abs(line.hz - order * f) > max(1.25 * bin, line.hz * .009)) continue
        orders += order
        support += line.amplitude
        estimate += line.amplitude * line.hz / order
    }
    // Even-only lines support the shorter observable period equally well. No latent-F0 claim.
    if (orders.isEmpty() || orders.reduce(::gcd) != 1) return Fit(f, 0.0, emptySet())
    return Fit(estimate / support, support / lines.sumOf { it.amplitude }, orders)
}

/** Multiple temporal minima corroborated by the spacing of resolvable spectral peaks. */
internal class HarmonicStudy(private val size: Int) {
    private val spectrum = SpectralAnalysis(size)

    fun estimate(t: Trial): Decision {
        var minima = emptyList<PitchDetector.Minimum>()
        val original = reading(t, t.pcm) { minima = it.minima }
        if (original.hz == 0.0) return Decision(0.0, 0.0)
        val lines = spectralLines(spectrum.frame(t.pcm, t.rate), t.upperHz * 12.0)
        val bin = t.rate.toDouble() / size
        val proposals = lines.flatMap { line -> (1..12).map { line.hz / it } }.filter { hz ->
            hz in t.profile.validMinHz..(t.upperHz * 1.005) &&
                minima.any { abs(it.hz / hz - 1) < .035 && it.confidence >= .65 }
        }
        val fits = proposals.map { fit(it, lines, bin) }.filter { it.score > 0 }.sortedByDescending { it.score }
        val best = fits.firstOrNull() ?: return Decision(original.hz, 0.0)
        val runner = fits.firstOrNull { abs(it.hz / best.hz - 1) > .03 }
        val margin = best.score - (runner?.score ?: 0.0)
        val corroborated = best.harmonics.size >= 2 && best.score >= .90 && margin >= .035
        val reported = if (corroborated) best.hz else original.hz
        val agrees = abs(reported / best.hz - 1) <= .03
        // A ranking score for selective evaluation. A single spectral line is weak source evidence.
        val rank = if (!agrees) 0.0 else min(original.strength, best.score) *
            min(1.0, margin / .15) * if (best.harmonics.size == 1) .5 else 1.0
        return Decision(reported, rank)
    }
}
