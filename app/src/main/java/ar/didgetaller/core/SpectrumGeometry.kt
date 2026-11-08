package ar.didgetaller.core

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/** Display preparation only; spectral measurements, peak assignments and axes are unchanged. */
data class SpectrumGeometry(val curve: List<PlotPoint>, val guides: List<Double>, val peaks: List<Peak>) {
    data class Peak(val position: PlotPoint, val order: Int?)

    companion object {
        fun prepare(analysis: WavAnalysis.Result?, projection: PlotProjection, columns: Int): SpectrumGeometry? {
            val spectrum = analysis?.spectrum ?: return null
            val reference = spectrum.density.maxOrNull() ?: 0.0
            if (reference <= 1e-16) return null
            val curve = PlotReduction.spectrum(spectrum, projection.viewport.x, columns).map { (hz, density) ->
                projection.pixels(hz, 10 * log10(max(1e-30, density) / reference))
            }
            val peaks = analysis.harmonics.peaks.filter { it.hz in projection.viewport.x.low..projection.viewport.x.high }
                .map { Peak(projection.pixels(it.hz, it.relativeDb), it.order) }
            return SpectrumGeometry(curve, guides(analysis.harmonics.fundamentalHz, projection), peaks)
        }

        private fun guides(frequency: Double?, projection: PlotProjection): List<Double> {
            if (frequency == null) return emptyList()
            val window = projection.viewport.x
            return (1..min(128, (window.high / frequency).toInt())).map { it * frequency }
                .filter { it >= window.low }.map { projection.pixels(it, 0.0).x }
        }
    }
}
