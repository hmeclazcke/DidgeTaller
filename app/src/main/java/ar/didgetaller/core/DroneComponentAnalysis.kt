package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.*

/**
 * A measured low spectral family, not an F0 estimate or a musical note.
 * The original pitch readings and WAV are never changed.
 */
data class DroneComponentCurve(val samples: List<Sample>, val hopMs: Double) {
    data class Sample(
        val timeMs: Double,
        val hz: Double?,
        val candidateHz: Double?,
        val rms: Double,
        val reason: Reason,
    )
    enum class Reason { COMPONENT, COMPETING_PERIODICITY, QUIET, NO_PEAK, NO_FAMILY, TEMPORALLY_UNSUPPORTED }
}

/** The component is independent of accepted YIN; only instrument scope changes visibility. */
object DroneComponentOverlay {
    fun visible(curve: DroneComponentCurve, instrument: InstrumentProfile): List<DroneComponentCurve.Sample> {
        if (instrument != InstrumentProfile.DIDGERIDOO) return emptyList()
        return curve.samples
    }
}

/**
 * Conservative, bounded saved-WAV analysis. Two upper harmonics must corroborate
 * a low spectral line. Competing periodicities are flagged, not interpreted as F0.
 * This cannot establish which oscillator generated a component.
 */
class DroneComponentAnalysis {
    companion object {
        const val WINDOW = 14112 // 320 ms at 44.1 kHz
        const val HOP = 8192 // 186 ms; deliberately cheaper than the 40 ms research hop
        private const val FFT = 65536
        private const val MIN_HZ = 30.0
        private const val MAX_HZ = 220.0
        private const val CONTEXT_HZ = 500.0
    }

    fun analyze(source: MonoPcmSource, cancelled: () -> Boolean = { false },
                progress: (Int) -> Unit = {}): DroneComponentCurve {
        require(source.sampleRate == 44100) { "Drone component analysis requires 44.1 kHz PCM" }
        val count = ((source.frameCount - WINDOW).coerceAtLeast(0) / HOP + 1)
            .takeIf { source.frameCount >= WINDOW } ?: 0
        require(count <= 12000) { "Recording exceeds the component analysis budget" }
        val spectrum = SpectralAnalysis(FFT)
        val pcm = ShortArray(WINDOW)
        val raw = ArrayList<DroneComponentCurve.Sample>(count.toInt())
        var lastProgress = -1
        for (index in 0 until count.toInt()) {
            if (cancelled()) throw CancellationException()
            val start = index.toLong() * HOP
            check(source.read(start, pcm) == WINDOW) { "Incomplete WAV read" }
            val timeMs = (start + WINDOW / 2) * 1000.0 / source.sampleRate
            val mean = pcm.sumOf { it.toDouble() } / WINDOW
            val rms = sqrt(pcm.sumOf { (it - mean).let { delta -> delta * delta } } / WINDOW) / 32768.0
            val sample = if (rms < .005) {
                DroneComponentCurve.Sample(timeMs, null, null, rms, DroneComponentCurve.Reason.QUIET)
            } else {
                classify(timeMs, rms, spectrum.frame(pcm, source.sampleRate))
            }
            raw += sample
            val percent = ((index + 1) * 100 / count).toInt()
            if (percent != lastProgress) { progress(percent); lastProgress = percent }
        }
        val accepted = raw.mapIndexed { index, sample ->
            val hz = sample.hz
            if (hz == null) sample else {
                val supported = listOf(index - 1, index + 1).any { neighbor ->
                    raw.getOrNull(neighbor)?.hz?.let { abs(1200 * log2(hz / it)) <= 75 } == true
                }
                if (supported) sample else sample.copy(hz = null,
                    reason = DroneComponentCurve.Reason.TEMPORALLY_UNSUPPORTED)
            }
        }
        progress(100)
        return DroneComponentCurve(accepted, HOP * 1000.0 / source.sampleRate)
    }

    private fun classify(timeMs: Double, rms: Double, spectrum: PowerSpectrum): DroneComponentCurve.Sample {
        val binHz = spectrum.binHz
        val first = ceil(MIN_HZ / binHz).toInt()
        val last = floor(CONTEXT_HZ / binHz).toInt()
        val db = DoubleArray(last + 1) { i -> 10 * log10(max(spectrum.density[i], 1e-300)) }
        val noise = db.copyOfRange(first, last + 1).also { it.sort() }[(last - first) / 2]
        val lowLast = floor(MAX_HZ / binHz).toInt()
        val maximum = db.copyOfRange(first, lowLast + 1).max()
        val allPeaks = peaks(db, first, last, binHz)
        val candidates = allPeaks.filter { it.bin <= lowLast && it.db - noise >= 12 &&
            it.db - maximum >= -20 }
        val chosen = candidates.minByOrNull { it.hz }
            ?: return DroneComponentCurve.Sample(timeMs, null, null, rms, DroneComponentCurve.Reason.NO_PEAK)
        val hz = chosen.hz
        fun maximumNear(target: Double, radius: Double): Double {
            val lo = ceil((target - radius) / binHz).toInt().coerceIn(first, last)
            val hi = floor((target + radius) / binHz).toInt().coerceIn(lo, last)
            return (lo..hi).maxOf { db[it] }
        }
        fun harmonic(order: Int): Boolean {
            val target = hz * order
            if (target > CONTEXT_HZ - 13) return false
            val center = maximumNear(target, 2.0)
            val sides = ArrayList<Double>()
            for (bin in ceil((target - 13) / binHz).toInt()..floor((target - 6) / binHz).toInt())
                if (bin in first..last) sides += db[bin]
            for (bin in ceil((target + 6) / binHz).toInt()..floor((target + 13) / binHz).toInt())
                if (bin in first..last) sides += db[bin]
            if (sides.isEmpty()) return false
            sides.sort()
            return center - noise >= 12 && center - sides[sides.size / 2] >= 8
        }
        val upperAmbiguity = allPeaks.any { peak ->
            peak.db - noise >= 12 && peak.db - maximum >= -12 &&
                listOf(1.5, 2.5, 3.5).any { ratio ->
                    abs(1200 * log2(peak.hz / (hz * ratio))) <= 35
                }
        }
        val half = hz / 2
        val lowerLine = half >= MIN_HZ && maximumNear(half, 2.0) - maximum >= -35 &&
            maximumNear(half, 2.0) - noise >= 12
        val reason = when {
            !harmonic(2) || !harmonic(3) -> DroneComponentCurve.Reason.NO_FAMILY
            upperAmbiguity || lowerLine -> DroneComponentCurve.Reason.COMPETING_PERIODICITY
            else -> DroneComponentCurve.Reason.COMPONENT
        }
        return DroneComponentCurve.Sample(timeMs, hz.takeIf { reason != DroneComponentCurve.Reason.NO_FAMILY },
            hz, rms, reason)
    }

    private data class Peak(val bin: Int, val hz: Double, val db: Double)

    private fun peaks(db: DoubleArray, first: Int, last: Int, binHz: Double): List<Peak> {
        val found = ArrayList<Peak>()
        for (bin in first + 1 until last) {
            val value = db[bin]
            if (value <= db[bin - 1] || value < db[bin + 1]) continue
            var left = value
            var at = bin - 1
            while (at >= first && db[at] <= value) { left = min(left, db[at]); at-- }
            var right = value
            at = bin + 1
            while (at <= last && db[at] <= value) { right = min(right, db[at]); at++ }
            if (value - max(left, right) < 3) continue
            val curvature = db[bin - 1] - 2 * value + db[bin + 1]
            val delta = if (abs(curvature) > 1e-9)
                (.5 * (db[bin - 1] - db[bin + 1]) / curvature).coerceIn(-.5, .5) else 0.0
            found += Peak(bin, (bin + delta) * binHz, value)
        }
        val minBins = round(6 / binHz).toInt().coerceAtLeast(1)
        val chosen = ArrayList<Peak>()
        for (peak in found.sortedByDescending { it.db }) {
            if (chosen.none { abs(it.bin - peak.bin) < minBins }) chosen += peak
        }
        return chosen
    }
}
