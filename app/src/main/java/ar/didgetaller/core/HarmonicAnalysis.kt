package ar.didgetaller.core

import kotlin.math.*

object HarmonicAnalysis {
    data class Peak(val hz: Double, val relativeDb: Double, val order: Int?, val deviationCents: Double?)
    data class Result(val fundamentalHz: Double?, val peaks: List<Peak>, val candidateBandEnergyFraction: Double?)

    /** Candidates, not resonances/toots. A missing H1 is never fabricated. */
    fun analyze(spectrum: PowerSpectrum, fundamentalHz: Double?): Result {
        val f0 = fundamentalHz?.takeIf { it.isFinite() && it > 0 && it < spectrum.sampleRate / 2.0 }
        val power = spectrum.density
        val maximum = power.maxOrNull() ?: 0.0
        if (maximum <= 1e-16) return Result(null, emptyList(), null)
        val candidates = mutableListOf<Peak>()
        for (bin in 2 until power.lastIndex - 1) {
            if (power[bin] <= power[bin - 1] || power[bin] < power[bin + 1] || power[bin] < maximum * 1e-5) continue
            val leftValley = (max(1, bin - 6) until bin).minOf { power[it] }
            val rightValley = (bin + 1..min(power.lastIndex, bin + 6)).minOf { power[it] }
            if (power[bin] < max(leftValley, rightValley) * 4) continue // >= 6 dB local prominence
            val a = ln(max(power[bin - 1], 1e-300)); val b = ln(max(power[bin], 1e-300))
            val c = ln(max(power[bin + 1], 1e-300)); val denominator = a - 2 * b + c
            val delta = if (abs(denominator) > 1e-12) (0.5 * (a - c) / denominator).coerceIn(-0.5, 0.5) else 0.0
            val hz = (bin + delta) * spectrum.binHz
            val nearest = f0?.let { (hz / it).roundToInt().coerceAtLeast(1) }
            val expected = if (nearest != null) nearest * f0!! else null
            val tolerance = if (expected != null) min(f0!! * 0.20, max(spectrum.binHz * 0.75, expected * (2.0.pow(35.0 / 1200) - 1))) else 0.0
            val order = nearest?.takeIf { abs(hz - expected!!) <= tolerance }
            candidates += Peak(hz, 10 * log10(power[bin] / maximum), order,
                order?.let { 1200 * log2(hz / (it * f0!!)) })
        }
        val peaks = candidates.sortedByDescending { it.relativeDb }.take(64).sortedBy { it.hz }
        val occupied = BooleanArray(power.size)
        peaks.filter { it.order != null }.forEach { peak ->
            val bin = (peak.hz / spectrum.binHz).roundToInt()
            for (at in max(1, bin - 2)..min(power.lastIndex, bin + 2)) occupied[at] = true
        }
        val fraction = if (f0 != null) power.indices.filter { occupied[it] }.sumOf { power[it] } / power.sum() else null
        return Result(f0, peaks, fraction)
    }
}
