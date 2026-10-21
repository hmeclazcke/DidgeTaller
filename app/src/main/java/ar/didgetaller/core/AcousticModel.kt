package ar.didgetaller.core

import java.util.Locale
import kotlin.math.*

/**
 * Original Kotlin implementation of a one-dimensional lossy transmission-line bore model.
 * Inspired by didgeridoo acoustics literature and the capabilities of DidgeLab/CADSD.
 * DOES NOT incorporate, port or reproduce DidgeLab's source code.
 * Not a player/lips model and not a measured predictor of perceived timbre or playability.
 */
object AcousticModel {
    data class BorePoint(val positionMm: Int, val diameterMm: Double)

    data class Shape(
        val lengthMm: Int = 1500,
        val mouthMm: Int = 32,
        val middleMm: Int = 43,
        val bellMm: Int = 85,
        /** Non-empty list overrides the three-diameter legacy model. Includes both endpoints. */
        val points: List<BorePoint> = emptyList()
    ) {
        init {
            require(lengthMm in 500..3000) { "Longitud fuera de rango" }
            require(mouthMm in 15..180 && middleMm in 15..180 && bellMm in 15..200)
            if (points.isNotEmpty()) {
                require(points.size in 2..32)
                require(points.first().positionMm == 0 && points.last().positionMm == lengthMm)
                require(points.zipWithNext().all { (a, b) -> b.positionMm > a.positionMm })
                require(points.all { it.diameterMm in 15.0..220.0 && it.diameterMm.isFinite() })
            }
        }
        fun controlPoints(): List<BorePoint> = if (points.isNotEmpty()) points else listOf(
            BorePoint(0, mouthMm.toDouble()),
            BorePoint((lengthMm * 0.68).roundToInt(), middleMm.toDouble()),
            BorePoint(lengthMm, bellMm.toDouble())
        )
        fun diameter(positionMm: Int): Double {
            val p = positionMm.coerceIn(0, lengthMm)
            val nodes = controlPoints()
            if (p == 0) return nodes.first().diameterMm
            if (p == lengthMm) return nodes.last().diameterMm
            val j = nodes.indexOfFirst { it.positionMm >= p }.coerceAtLeast(1)
            val left = nodes[j - 1]; val right = nodes[j]
            val fraction = (p - left.positionMm).toDouble() / (right.positionMm - left.positionMm)
            return left.diameterMm + fraction * (right.diameterMm - left.diameterMm)
        }
        fun withControlPoints(newPoints: List<BorePoint>): Shape {
            val ordered = newPoints.sortedBy { it.positionMm }
            return copy(
                mouthMm = ordered.first().diameterMm.roundToInt().coerceIn(15,180),
                middleMm = ordered[ordered.size / 2].diameterMm.roundToInt().coerceIn(15,180),
                bellMm = ordered.last().diameterMm.roundToInt().coerceIn(15,200),
                points = ordered
            )
        }
        fun csv(): String = buildString {
            append("distancia_mm,diametro_interior_mm\n")
            for (node in controlPoints()) append("${node.positionMm},${String.format(Locale.US, "%.3f", node.diameterMm)}\n")
        }
    }

    data class Peak(val hz: Double, val strength: Double)
    data class Simulation(
        val impedance: List<Float>,
        val frequenciesHz: List<Double>,
        val baseHz: Double,
        val minHz: Int = 30,
        val maxHz: Int = 650,
        val stepHz: Int = 1,
        val peaks: List<Peak> = emptyList()
    )

    private data class Complex(val real: Double, val imag: Double) {
        operator fun plus(o: Complex) = Complex(real + o.real, imag + o.imag)
        operator fun times(o: Complex) = Complex(real * o.real - imag * o.imag, real * o.imag + imag * o.real)
        operator fun times(n: Double) = Complex(real * n, imag * n)
        operator fun div(o: Complex): Complex {
            val denom = o.real * o.real + o.imag * o.imag
            if (denom < 1e-24) return Complex(1e12, 0.0)
            return Complex((real * o.real + imag * o.imag) / denom,
                           (imag * o.real - real * o.imag) / denom)
        }
        fun magnitude() = hypot(real, imag)
    }

    /**
     * Plane-wave transfer matrices, with approximate thermoviscous damping and radiation load.
     * The bore is discretized into short cylindrical segments of locally varying area.
     * rho 1.20 kg/m3, c 343 m/s, dynamic viscosity 1.81e-5 Pa*s, gamma 1.4, Pr 0.71.
     * The impedance is normalized against the mouth characteristic impedance.
     */
    fun evaluate(shape: Shape, stepHz: Int = 1, maxHz: Int = 650, segments: Int = 72): Simulation {
        require(stepHz in 1..12 && maxHz in 150..1200 && segments in 12..256)
        val low = 30
        val rho = 1.20; val speed = 343.0; val viscosity = 1.81e-5
        val spacing = shape.lengthMm / 1000.0 / segments
        val radii = DoubleArray(segments) { n ->
            shape.diameter(((n + 0.5) * shape.lengthMm / segments).roundToInt()) / 2000.0
        }
        val zc = DoubleArray(segments) { n -> rho * speed / (PI * radii[n] * radii[n]) }
        val mouthZ = zc[0]
        val bellR = shape.diameter(shape.lengthMm) / 2000.0
        val bellZ = rho * speed / (PI * bellR * bellR)
        val frequencies = IntArray((maxHz - low) / stepHz + 1) { low + it * stepHz }
        val raw = DoubleArray(frequencies.size)

        for (idx in frequencies.indices) {
            val frequency = frequencies[idx].toDouble()
            val omega = 2.0 * PI * frequency
            val wave = omega / speed
            val ka = wave * bellR
            // Unflanged-open-pipe, small-ka radiation approximation, not valid at high ka.
            var load = Complex(bellZ * (0.5 * ka * ka).coerceAtMost(0.85), bellZ * 0.6133 * ka)
            for (segment in segments - 1 downTo 0) {
                val delta = sqrt(viscosity / (2.0 * rho * omega))
                val alpha = wave * delta * (1.0 + (1.4 - 1.0) / sqrt(0.71)) / radii[segment]
                val a = (alpha * spacing).coerceAtMost(3.0)
                val b = wave * spacing
                // cosh(alpha*L + i*k*L), sinh(alpha*L + i*k*L)
                val ch = Complex(cosh(a) * cos(b), sinh(a) * sin(b))
                val sh = Complex(sinh(a) * cos(b), cosh(a) * sin(b))
                val input = (load * ch + sh * zc[segment]) /
                    (ch + (load / Complex(zc[segment], 0.0)) * sh)
                load = input
            }
            raw[idx] = ln(1.0 + load.magnitude() / mouthZ).coerceAtMost(20.0)
        }
        val peakList = detectPeaks(raw, low, stepHz)
        val highest = raw.maxOrNull()?.coerceAtLeast(1e-9) ?: 1.0
        return Simulation(raw.map { (it / highest).toFloat() }, peakList.map { it.hz },
            peakList.firstOrNull()?.hz ?: 0.0, low, maxHz, stepHz, peakList)
    }

    private fun detectPeaks(signal: DoubleArray, low: Int, step: Int): List<Peak> {
        val result = mutableListOf<Peak>()
        val maximum = signal.maxOrNull() ?: return result
        for (i in 2 until signal.size - 2) {
            if (signal[i] <= signal[i - 1] || signal[i] < signal[i + 1]) continue
            if (signal[i] < maximum * 0.16) continue
            // Quadratic interpolation between three frequency samples.
            val y1 = signal[i-1]; val y2 = signal[i]; val y3 = signal[i+1]
            val denom = y1 - 2.0*y2 + y3
            val fraction = if (abs(denom) < 1e-12) 0.0
                else (0.5 * (y1 - y3) / denom).coerceIn(-0.5,0.5)
            val hz = low + (i + fraction) * step
            if (result.lastOrNull()?.let { hz - it.hz < 14.0 } == true) {
                if (result.last().strength < signal[i]) result[result.lastIndex] = Peak(hz, signal[i])
            } else result.add(Peak(hz, signal[i]))
            if (result.size >= 12) break
        }
        return result
    }

    fun description(result: Simulation): String = buildString {
        if (result.frequenciesHz.isEmpty()) { append("No se encontraron resonancias en el rango."); return@buildString }
        append("Resonancia grave estimada: ${Music.note(result.baseHz)} · ${Music.hzText(result.baseHz)}")
        result.frequenciesHz.drop(1).take(5).forEachIndexed { index, hz ->
            append("\nResonancia ${index + 2}: ${Music.note(hz)} · ${Music.hzText(hz)}")
        }
    }
}
