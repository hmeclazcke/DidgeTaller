package ar.didgetaller.core

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/** Resumen robusto de las lecturas obtenidas mientras se grabó el WAV. */
object SessionAnalysis {
    data class Point(val timeMs: Long, val hz: Double?, val confidence: Double) {
        val usable: Boolean get() = isUsable(35.0, 220.0)
        fun isUsable(minHz: Double, maxHz: Double): Boolean =
            hz != null && hz.isFinite() && hz in minHz..maxHz && confidence in 0.70..1.0
    }

    data class Summary(
        val typicalHz: Double,
        val lowHz: Double,
        val highHz: Double,
        val stabilityPercent: Int,
        val validPoints: Int,
        val totalPoints: Int
    )

    fun summarize(points: List<Point>, minHz: Double = 35.0, maxHz: Double = 220.0): Summary? {
        require(minHz > 0 && maxHz > minHz)
        val valid = points.filter { it.isUsable(minHz, maxHz) }
        if (valid.size < 3) return null
        // Agrupar por semitono evita que un toot u octave breve desplace al drone predominante.
        val dominant = valid.groupBy { Music.midi(it.hz!!).roundToInt() }
            .maxWithOrNull(compareBy<Map.Entry<Int, List<Point>>> { it.value.size }.thenBy { it.value.sumOf(Point::confidence) })
            ?.value.orEmpty()
        if (dominant.size < 3 || dominant.size * 2 <= valid.size) return null
        val values = dominant.map { it.hz!! }.sorted()
        val typical = median(values)
        val cents = dominant.map { 1200.0 * log2(it.hz!! / typical) }
        val stable = cents.count { abs(it) <= 25.0 }
        // Percentiles descartan extremos espurios sin ocultar el rango habitual.
        val low = values[((values.size - 1) * 0.10).toInt()]
        val high = values[((values.size - 1) * 0.90).toInt()]
        return Summary(typical, low, high, (100.0 * stable / dominant.size).roundToInt(), dominant.size, points.size)
    }

    private fun median(sorted: List<Double>): Double {
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2 else sorted[middle]
    }
}
