package ar.didgetaller.core

import kotlin.math.*

/** Digital PSD in full-scale squared/Hz. This is not sound pressure or dB SPL. */
data class PowerSpectrum(val sampleRate: Int, val fftSize: Int, val density: DoubleArray) {
    val binHz: Double get() = sampleRate.toDouble() / fftSize
    val totalPower: Double get() = density.sum() * binHz
    val centroidHz: Double? get() {
        val sum = density.sum()
        return if (sum > 1e-16) density.indices.sumOf { it * binHz * density[it] } / sum else null
    }
    /** null for silence; reference is the maximum PSD bin of this spectrum, never a fake 0 dB tone. */
    fun relativeDb(bin: Int, floorDb: Double = -100.0): Double? {
        val reference = density.maxOrNull() ?: 0.0
        return if (reference <= 1e-16) null else (10 * log10(max(density[bin], 1e-300) / reference)).coerceAtLeast(floorDb)
    }
    data class Band(val fromHz: Double, val toHz: Double, val fraction: Double)
    fun bands(edges: List<Double> = listOf(100.0, 250.0, 500.0, 1000.0, 2000.0, 5000.0)): List<Band> {
        require(edges.all { it.isFinite() && it > 0 } && edges.zipWithNext().all { it.first < it.second })
        val nyquist = sampleRate / 2.0
        val limits = listOf(0.0) + edges.filter { it < nyquist } + nyquist
        val sum = density.sum()
        return limits.zipWithNext().mapIndexed { index, (low, high) ->
            val power = density.indices.filter { it * binHz >= low && (it * binHz < high || index == limits.size - 2) }
                .sumOf { density[it] }
            Band(low, high, if (sum > 1e-16) power / sum else 0.0)
        }
    }
}

/** Hann-periodic periodogram. Workspace is reused on one worker, never shared concurrently. */
class SpectralAnalysis(val fftSize: Int = 32768) {
    init { require(fftSize in 4096..65536 && fftSize and (fftSize - 1) == 0) }
    private val re = DoubleArray(fftSize)
    private val im = DoubleArray(fftSize)
    fun frame(pcm: ShortArray, sampleRate: Int, count: Int = pcm.size): PowerSpectrum {
        require(sampleRate > 0 && count in 2..min(pcm.size, fftSize))
        re.fill(0.0); im.fill(0.0)
        val mean = (0 until count).sumOf { pcm[it].toDouble() } / count
        var windowEnergy = 0.0
        for (i in 0 until count) {
            val window = 0.5 - 0.5 * cos(2 * PI * i / count)
            re[i] = (pcm[i] - mean) / 32768.0 * window
            windowEnergy += window * window
        }
        Radix2Fft.forward(re, im)
        val density = DoubleArray(fftSize / 2 + 1) { bin ->
            val side = if (bin == 0 || bin == fftSize / 2) 1.0 else 2.0
            side * (re[bin] * re[bin] + im[bin] * im[bin]) / (sampleRate * windowEnergy)
        }
        return PowerSpectrum(sampleRate, fftSize, density)
    }
}
