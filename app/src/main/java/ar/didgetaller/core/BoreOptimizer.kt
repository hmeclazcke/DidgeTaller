package ar.didgetaller.core

import java.util.Random
import kotlin.math.*

/**
 * Independent evolutionary inverse-design experiment. Concepts inspired by
 * DidgeLab's documented *purpose*, NOT its Python implementation.
 * Optimize simulated impedance peaks; simulated peaks are NOT guaranteed to be
 * playable toots or to match a real instrument.
 */
object BoreOptimizer {
    data class Target(
        val droneHz: Double = 73.42,
        val secondHz: Double = 174.61,
        val thirdHz: Double = 293.66
    ) {
        init { require(droneHz in 40.0..135.0 && secondHz in 110.0..380.0 && thirdHz in 170.0..540.0) }
    }
    data class Settings(val generations: Int = 16, val population: Int = 14, val seed: Long = 20261019L) {
        init { require(generations in 1..100 && population in 4..64) }
    }
    data class Candidate(val shape: AcousticModel.Shape, val fitness: Double, val peaks: List<Double>)

    fun search(
        initial: AcousticModel.Shape,
        target: Target,
        settings: Settings = Settings(),
        cancelled: () -> Boolean = { false },
        progress: (generation: Int, best: Candidate) -> Unit = { _, _ -> }
    ): Candidate? {
        val random = Random(settings.seed)
        fun evaluate(shape: AcousticModel.Shape): Candidate {
            val simulation = AcousticModel.evaluate(shape, stepHz = 3, maxHz = 570, segments = 42)
            val frequencies = simulation.frequenciesHz
            fun cents(actual: Double?, expected: Double): Double =
                if (actual == null || actual <= 0.0) 1800.0
                else abs(1200.0 * ln(actual / expected) / ln(2.0))
            var error = 2.2*cents(frequencies.getOrNull(0), target.droneHz) +
                        1.0*cents(frequencies.getOrNull(1), target.secondHz) +
                        0.8*cents(frequencies.getOrNull(2), target.thirdHz)
            val pts = shape.controlPoints()
            // Manufacture-friendly soft constraint: discourage severe abrupt narrowing,
            // but allow non-monotonic bores when musically helpful.
            for (i in 1 until pts.size) {
                val slope = abs(pts[i].diameterMm - pts[i-1].diameterMm) /
                    (pts[i].positionMm - pts[i-1].positionMm).toDouble()
                if (slope > 0.13) error += (slope - 0.13) * 650.0
                if (pts[i].diameterMm < pts[i-1].diameterMm - 8) error += 15.0
            }
            return Candidate(shape, error, frequencies)
        }
        val fractions = doubleArrayOf(0.0,0.22,0.44,0.66,0.85,1.0)
        fun toGenome(shape: AcousticModel.Shape): DoubleArray =
            doubleArrayOf(shape.lengthMm.toDouble()) + fractions.map {
                shape.diameter((shape.lengthMm * it).roundToInt())
            }.toDoubleArray()
        fun fromGenome(genes: DoubleArray): AcousticModel.Shape {
            val length = genes[0].roundToInt().coerceIn(950, 2200)
            val points = fractions.mapIndexed { i, f ->
                AcousticModel.BorePoint((f * length).roundToInt(), genes[i+1].coerceIn(23.0,170.0))
            }
            val mouth = points.first().diameterMm.roundToInt().coerceIn(23,60)
            val bell = points.last().diameterMm.roundToInt().coerceIn(30,170)
            return AcousticModel.Shape(length,mouth,points[3].diameterMm.roundToInt().coerceIn(15,180),bell,points)
        }
        fun mutate(genome: DoubleArray, amplitude: Double): DoubleArray {
            val child = genome.copyOf()
            child[0] = (child[0] + random.nextGaussian() * 105.0 * amplitude).coerceIn(950.0,2200.0)
            val n = 1 + random.nextInt(3)
            repeat(n) {
                val index = 1 + random.nextInt(fractions.size)
                child[index] = (child[index] + random.nextGaussian() * 11.0 * amplitude)
                    .coerceIn(if (index==1) 23.0 else 26.0, if(index==1)60.0 else 170.0)
            }
            return child
        }
        val initialGenome = toGenome(initial)
        var generation = mutableListOf(initialGenome)
        while (generation.size < settings.population) {
            generation.add(mutate(initialGenome, 1.5 + random.nextDouble()))
        }
        var best: Candidate? = null
        for (iteration in 0 until settings.generations) {
            if (cancelled()) return null
            val rated = mutableListOf<Pair<DoubleArray,Candidate>>()
            for (genome in generation) {
                if (cancelled()) return null
                val result = evaluate(fromGenome(genome))
                rated.add(genome to result)
            }
            rated.sortBy { it.second.fitness }
            if (best == null || rated.first().second.fitness < best.fitness) best = rated.first().second
            progress(iteration+1, best)
            if (iteration == settings.generations-1) break
            val survivors = rated.take(max(2, settings.population/4)).map { it.first }
            val newGeneration = mutableListOf<DoubleArray>()
            newGeneration.add(toGenome(best.shape))
            newGeneration.addAll(survivors.map { it.copyOf() })
            val amplitude = (1.3 - iteration.toDouble() / settings.generations).coerceAtLeast(0.25)
            while (newGeneration.size < settings.population) {
                val selected = survivors[random.nextInt(survivors.size)]
                newGeneration.add(mutate(selected, amplitude))
            }
            generation = newGeneration
        }
        return best
    }
}
