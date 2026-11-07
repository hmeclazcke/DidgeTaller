package ar.didgetaller.core

import kotlin.math.*

/** Period evidence only. Input visualization and acceptance remain separate decisions. */
internal class YinPeriodSearch private constructor(
    private val signal: DoubleArray,
    private val sampleRate: Double,
    private val firstLag: Int,
    private val lastLag: Int,
    private val comparisonSize: Int,
    private val offset: Int,
    private val decimation: Int,
) {
    data class Result(val hz: Double, val confidence: Double, val diagnostic: PitchDetector.Diagnostic)

    private fun differenceCurve(): DoubleArray {
        val curve = DoubleArray(lastLag + 1)
        var accumulated = 0.0
        curve[0] = 1.0
        for (lag in 1..lastLag) {
            var squared = 0.0
            for (j in 0 until comparisonSize) {
                val difference = signal[offset + j] - signal[offset + j + lag]
                squared += difference * difference
            }
            accumulated += squared
            curve[lag] = if (accumulated > 1e-15) squared * lag / accumulated else 1.0
        }
        return curve
    }

    private fun firstThresholdMinimum(curve: DoubleArray): Int? {
        var lag = firstLag
        while (lag <= lastLag) {
            if (curve[lag] < 0.14) {
                while (lag < lastLag && curve[lag + 1] < curve[lag]) lag++
                return lag
            }
            lag++
        }
        return null
    }

    private fun lowestMinimum(curve: DoubleArray): Int? {
        var smallest = 1.0
        var best: Int? = null
        for (lag in firstLag..lastLag) {
            if (curve[lag] < smallest) {
                smallest = curve[lag]
                best = lag
            }
        }
        return best
    }

    private fun supportedPeriod(curve: DoubleArray, best: Int): Int {
        if (decimation != 1 || curve[best] <= .025) return best
        val doubled = (max(firstLag, best * 2 - 2)..min(lastLag, best * 2 + 2)).minByOrNull { curve[it] }
        return if (doubled != null && curve[doubled] < curve[best] * .65) doubled else best
    }

    private fun refinedLag(curve: DoubleArray, best: Int): Double {
        if (best <= firstLag || best >= lastLag) return best.toDouble()
        val a = curve[best - 1]
        val b = curve[best]
        val c = curve[best + 1]
        val denominator = a - 2 * b + c
        val delta = if (abs(denominator) > 1e-9) (0.5 * (a - c) / denominator).coerceIn(-0.5, 0.5) else 0.0
        return best + delta
    }

    private fun comparisonRms(pcm: ShortArray, best: Int, wholeRms: Double): Double {
        if (decimation == 1) return wholeRms
        val first = offset * decimation
        val end = (offset + comparisonSize + best) * decimation
        val mean = (first until end).sumOf { pcm[it].toDouble() } / (end - first)
        return sqrt((first until end).sumOf { (pcm[it] - mean).pow(2) } / (end - first)) / 32768.0
    }

    private fun minima(curve: DoubleArray): List<PitchDetector.Minimum> = (firstLag..lastLag).filter { lag ->
        (lag == firstLag || curve[lag] <= curve[lag - 1]) && (lag == lastLag || curve[lag] <= curve[lag + 1])
    }.map { PitchDetector.Minimum(sampleRate / it, (1 - curve[it]).coerceIn(0.0, 1.0)) }

    fun evaluate(pcm: ShortArray, rms: Double, range: IntRange, includeMinima: Boolean): Result {
        val curve = differenceCurve()
        val minimum = firstThresholdMinimum(curve) ?: lowestMinimum(curve)
            ?: return Result(0.0, 0.0, PitchDetector.Diagnostic(null, "no_minimum"))
        val best = supportedPeriod(curve, minimum)
        val hz = sampleRate / refinedLag(curve, best)
        val confidence = (1.0 - curve[best]).coerceIn(0.0, 1.0)
        val supportRms = comparisonRms(pcm, best, rms)
        val reason = acceptanceReason(hz, confidence, supportRms, range)
        return Result(if (reason == "accepted") hz else 0.0, confidence,
            PitchDetector.Diagnostic(hz, reason, offset * decimation, comparisonSize * decimation,
                if (includeMinima) minima(curve) else emptyList(), supportRms))
    }

    private fun acceptanceReason(hz: Double, confidence: Double, rms: Double, range: IntRange): String = when {
        rms < .005 -> "low_comparison_amplitude"
        hz < range.first * .95 || hz > range.last * 1.05 -> "outside_range"
        confidence < .65 -> "low_periodicity"
        else -> "accepted"
    }

    companion object {
        fun prepare(pcm: ShortArray, mean: Double, rate: Int, range: IntRange): YinPeriodSearch? {
            val decimation = if (range.last > 500) 1 else 4
            val sampleRate = rate.toDouble() / decimation
            val signal = DoubleArray(pcm.size / decimation) { i ->
                var sum = 0.0
                for (j in 0 until decimation) sum += pcm[i * decimation + j] - mean
                sum / (decimation * 32768.0)
            }
            val guard = if (decimation == 1) 1 else 0
            val firstLag = max(2, floor(sampleRate / range.last).toInt() - guard)
            val lastLag = min(signal.size / 2, ceil(sampleRate / range.first).toInt() + guard)
            val size = min(2200, signal.size - lastLag - 1)
            if (size < 1000) return null
            val offset = if (decimation == 1) (signal.size - size - lastLag).coerceAtLeast(0) / 2 else 0
            return YinPeriodSearch(signal, sampleRate, firstLag, lastLag, size, offset, decimation)
        }
    }
}
