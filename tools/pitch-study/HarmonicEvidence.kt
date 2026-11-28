package ar.didgetaller.study

import ar.didgetaller.core.PitchDetector
import ar.didgetaller.core.SpectralAnalysis
import kotlin.math.*

internal data class Pattern(val hz: Double, val rank: Double, val orders: Set<Int>) {
    val anchored get() = 1 in orders || orders.size >= 3
}

/** Match resolvable peaks. A sparse high-order coincidence needs a consecutive pair. */
private fun pattern(seed: Double, peaks: List<Line>, bin: Double, total: Double): Pattern? {
    val orders = mutableSetOf<Int>()
    var weightedHz = 0.0
    var matched = 0.0
    var residual = 0.0
    for (peak in peaks) {
        val order = (peak.hz / seed).roundToInt()
        if (order !in 1..12) continue
        val tolerance = max(bin / 2, peak.hz * .002)
        val error = abs(peak.hz - order * seed) / tolerance
        if (error > 1) continue
        if (!orders.add(order)) return null
        matched += peak.amplitude
        weightedHz += peak.amplitude * peak.hz / order
        residual += peak.amplitude * error
    }
    if (orders.size < 2 || orders.reduce(::gcd) != 1) return null
    if (orders.size == 2 && 1 !in orders && orders.max() - orders.min() != 1) return null
    if (matched < .90 * total) return null
    return Pattern(weightedHz / matched, (matched - residual) / total, orders)
}

internal class PeakPatterns(size: Int) {
    private val fft = SpectralAnalysis(size)

    private fun peaks(t: Trial): List<Line> {
        if (t.pcm.sumOf { it.toDouble() * it } < t.pcm.size * (.005 * 32768).pow(2)) return emptyList()
        val spectrum = fft.frame(t.pcm, t.rate)
        val peaks = spectralLines(spectrum, t.upperHz * 12.0)
        // A spectrally flat impulse has no defined parabolic peak location.
        return if (peaks.any { !it.hz.isFinite() }) emptyList() else peaks
    }

    fun candidates(t: Trial): List<Pattern> {
        val peaks = peaks(t)
        val total = peaks.sumOf { it.amplitude }
        val range = t.profile.validMinHz..t.upperHz.toDouble()
        return peaks.flatMap { peak ->
            (1..12).mapNotNull { order ->
                val seed = peak.hz / order
                if (seed in range) pattern(seed, peaks, t.rate.toDouble()/t.pcm.size, total) else null
            }
        }.sortedByDescending { it.rank }
    }

    /** Start from integer-lag YIN minima and let resolved peaks refine their period estimates. */
    fun fromPeriods(t: Trial): Decision {
        var periods = emptyList<PitchDetector.Minimum>()
        val yin = reading(t, t.pcm) { periods = it.minima }
        if (yin.hz == 0.0) return yin
        val lines = peaks(t)
        val total = lines.sumOf { it.amplitude }
        val fits = periods.filter { it.confidence >= .65 }.flatMap { period ->
            lines.mapNotNull { line ->
                val order = (line.hz / period.hz).roundToInt()
                if (order !in 1..12) return@mapNotNull null
                val seed = line.hz / order
                if (seed !in t.profile.validMinHz..t.upperHz.toDouble() || abs(seed/period.hz-1) >= .03) null
                else pattern(seed, lines, t.rate.toDouble()/t.pcm.size, total)
            }
        }.sortedByDescending { it.rank }
        return choose(fits)?.let { Decision(it.hz, it.rank) } ?: yin
    }
}

private fun choose(candidates: List<Pattern>): Pattern? {
    val best = candidates.firstOrNull() ?: return null
    val rival = candidates.firstOrNull { abs(it.hz / best.hz - 1) > .03 }
    return best.takeIf { best.rank - (rival?.rank ?: 0.0) >= .035 }
}

internal enum class Route { YIN, PHASE2, SPECTRAL, PERIOD, CONDITIONAL, CONTEXT, SEPARATE }
internal data class Output(val pitch: Decision, val hypothesis: Pattern? = null)
private val absent = Decision(0.0, 0.0)

/** One instance per contiguous stream. Only directly supported observations renew its anchor. */
internal class EvidenceReader(size: Int, private val route: Route) {
    private val peaks = PeakPatterns(size)
    private val previousSelector = HarmonicStudy(size)
    private var anchor = 0.0
    private var age = 0

    fun reset() { anchor = 0.0; age = 0 }

    fun read(t: Trial): Output {
        if (route == Route.YIN) return Output(reading(t, t.pcm))
        if (route == Route.PHASE2) return Output(previousSelector.estimate(t))
        if (route == Route.SPECTRAL) {
            val best = choose(peaks.candidates(t))
            return Output(best?.let { Decision(it.hz, it.rank) } ?: absent)
        }
        var minima = emptyList<PitchDetector.Minimum>()
        val yin = reading(t, t.pcm) { minima = it.minima }
        if (yin.hz == 0.0) { reset(); return Output(yin) }
        val periods = minima.filter { it.confidence >= .65 }
        val conflict = periods.any { abs(it.hz / yin.hz - 1) > .03 }
        if (route == Route.CONDITIONAL && !conflict) return Output(yin)
        val best = choose(peaks.candidates(t).filter { p -> periods.any { abs(it.hz / p.hz - 1) < .03 } })
        if (best == null) { reset(); return Output(yin) }
        if (route == Route.SEPARATE) return Output(yin, best)
        val supported = best.anchored || (route == Route.CONTEXT && age in 1..2 && abs(anchor / best.hz - 1) < .03)
        if (best.anchored) { anchor = best.hz; age = 1 } else if (age > 0) age++
        return if (route == Route.PERIOD || supported) Output(Decision(best.hz, best.rank), best) else Output(yin)
    }
}
