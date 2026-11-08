package ar.didgetaller.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Display-only reductions; source-time RMS and spectral power remain unchanged. */
object PlotReduction {
    fun levelPixels(audio: AudioLevel?, window: AxisWindow, width: Float): FloatArray {
        if (audio == null || width <= 0) return floatArrayOf()
        val pixels = FloatArray(ceil(width).toInt().coerceAtLeast(1))
        val first = floor(window.low * audio.rate / 1000 / audio.hop).toInt().coerceIn(0, audio.rms.size)
        val end = ceil(window.high * audio.rate / 1000 / audio.hop).toInt().coerceIn(first, audio.rms.size)
        for (index in first until end) {
            val a = (window.fraction(index.toDouble() * audio.hop * 1000 / audio.rate) * width).toInt().coerceIn(0, pixels.lastIndex)
            val b = ceil(window.fraction(min(audio.frames, (index + 1L) * audio.hop) * 1000.0 / audio.rate) * width)
                .toInt().coerceIn(a, pixels.lastIndex)
            for (column in a..b) pixels[column] = max(pixels[column], audio.rms[index])
        }
        return pixels
    }

    fun spectrum(spectrum: PowerSpectrum?, window: AxisWindow, columns: Int): List<Pair<Double, Double>> {
        if (spectrum == null || columns <= 0 || spectrum.density.isEmpty()) return emptyList()
        val bin = spectrum.sampleRate.toDouble() / spectrum.fftSize
        val first = ceil(window.low / bin).toInt().coerceIn(0, spectrum.density.lastIndex)
        val last = floor(window.high / bin).toInt().coerceIn(first, spectrum.density.lastIndex)
        val count = columns.coerceAtLeast(2)
        if (last - first < count) return (first..last).map { it * bin to spectrum.density[it] }
        val peaks = DoubleArray(count)
        for (index in first..last) {
            val cell = ((index * bin - window.low) / window.span * (count - 1)).toInt().coerceIn(0, count - 1)
            peaks[cell] = max(peaks[cell], spectrum.density[index])
        }
        return peaks.mapIndexed { index, value -> window.value(index.toDouble() / (count - 1)) to value }
    }
}
