package ar.didgetaller.core

import kotlin.math.*

enum class PitchAlgorithm {
    STANDARD, HARMONIC;

    fun forCapture(measuring: Boolean) = if (measuring) this else STANDARD
}
data class HarmonicEstimate(val hz: Double, val rank: Double)

/** Phase 3 PERIOD. One FFT workspace per stream; the ranking is not a probability. */
class HarmonicYin(windowSize: Int) {
    private val fft = SpectralAnalysis(windowSize)
    private data class Peak(val hz: Double, val amplitude: Double)
    private data class Match(val hz: Double, val rank: Double)

    fun estimate(pcm: ShortArray, rate: Int, profile: InstrumentProfile, upperHz: Int,
                 yin: PitchDetector.Reading): HarmonicEstimate {
        val accepted = LivePitch.next(LivePitch.State(), yin, profile).storedHz ?: 0.0
        val original = HarmonicEstimate(accepted, yin.confidence)
        if (accepted == 0.0) return original
        val periods = yin.diagnostic?.minima.orEmpty().filter { it.confidence >= .65 }
        val match = select(fft.frame(pcm, rate), profile.validMinHz..upperHz.toDouble(), periods)
        return match?.let { HarmonicEstimate(it.hz, it.rank) } ?: original
    }

    private fun select(spectrum: PowerSpectrum, range: ClosedFloatingPointRange<Double>,
                       periods: List<PitchDetector.Minimum>): Match? {
        val peaks = peaks(spectrum, range.endInclusive * 12)
        if (peaks.any { !it.hz.isFinite() }) return null
        val total = peaks.sumOf { it.amplitude }
        val fits = peaks.flatMap { peak ->
            (1..12).mapNotNull { order ->
                val seed = peak.hz / order
                if (seed in range) fit(seed, peaks, spectrum.binHz, total) else null
            }
        }.filter { fit -> periods.any { abs(it.hz / fit.hz - 1) < .03 } }.sortedByDescending { it.rank }
        return winner(fits)
    }

    private fun winner(fits: List<Match>): Match? {
        val best = fits.firstOrNull() ?: return null
        val rival = fits.firstOrNull { abs(it.hz / best.hz - 1) > .03 }
        return best.takeIf { best.rank - (rival?.rank ?: 0.0) >= .035 }
    }

    private fun peaks(spectrum: PowerSpectrum, ceiling: Double): List<Peak> {
        val power = spectrum.density
        val bins = 2..min(power.lastIndex - 1, (ceiling / spectrum.binHz).toInt())
        val floor = bins.maxOf { power[it] } * .0004
        return bins.mapNotNull { k ->
            val center = power[k]
            if (center <= floor || center < power[k-1] || center <= power[k+1]) null
            else interpolated(power, k, spectrum.binHz)
        }.sortedByDescending { it.amplitude }.take(16)
    }

    private fun interpolated(power: DoubleArray, k: Int, bin: Double): Peak {
        val left = ln(max(power[k-1], 1e-30))
        val center = ln(power[k])
        val right = ln(max(power[k+1], 1e-30))
        val shift = (.5 * (left-right) / (left-2*center+right)).coerceIn(-.5,.5)
        return Peak((k+shift)*bin, sqrt(power[k]))
    }

    private fun fit(seed: Double, peaks: List<Peak>, bin: Double, total: Double): Match? {
        val orders = mutableSetOf<Int>()
        var matched = 0.0
        var weighted = 0.0
        var residual = 0.0
        for (peak in peaks) {
            val order = (peak.hz / seed).roundToInt()
            if (order !in 1..12) continue
            val error = abs(peak.hz - order*seed) / max(bin/2, peak.hz*.002)
            if (error > 1) continue
            if (!orders.add(order)) return null
            matched += peak.amplitude
            weighted += peak.amplitude * peak.hz / order
            residual += peak.amplitude * error
        }
        if (!supported(orders) || matched < .90*total) return null
        return Match(weighted/matched, (matched-residual)/total)
    }

    private fun supported(orders: Set<Int>): Boolean =
        orders.size >= 2 && orders.reduce(::gcd) == 1 &&
            (orders.size > 2 || 1 in orders || orders.max()-orders.min() == 1)

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
