package ar.didgetaller.study

import ar.didgetaller.core.InstrumentProfile
import java.util.Random
import kotlin.math.*

internal data class Observation(val trial: Trial, val frame: Int = 0, val lowestLine: Double = Double.NaN)

private fun staticSignals(p: InstrumentProfile, rate: Int, group: Int, f: Double, rng: Random) = sequence {
    val h = listOf(2, 3, 5, 7)[group % 4]
    val weak = .035 + .22 * rng.nextDouble()
    val neighbor = .06 + .27 * rng.nextDouble()
    val hiss = .001 + .045 * rng.nextDouble()
    fun signal(name: String, target: Double, lines: List<Pair<Double, Double>>): Observation {
        val phases = DoubleArray(lines.size) { 2 * PI * rng.nextDouble() }
        val pcm = ShortArray(p.windowSamples) { sample ->
            val angle = 2 * PI * sample / rate
            val v = lines.withIndex().sumOf { (i, line) -> line.second * sin(angle * line.first + phases[i]) }
            ((v + hiss * rng.nextGaussian()) * 11000).roundToInt().toShort()
        }
        return Observation(Trial(name, p, rate, group, target, pcm), lowestLine = lines.minOf { it.first })
    }
    val pair = listOf(h * f to 1.0, (h + 1) * f to neighbor)
    yield(signal("weak$h", f, pair + (f to weak)))
    val missing = signal("missing$h", f, pair)
    yield(missing)
    // Exactly the same digital observation under two independently driven oscillators.
    yield(missing.copy(trial = missing.trial.copy(category = "independentExact", target = Double.NaN)))
    yield(signal("sparse", f, listOf(3*f to .25, 5*f to 1.0)))
    yield(signal("stable", f, listOf(f to 1.0, 2*f to neighbor, 3*f to weak)))
    yield(signal("twoSources", Double.NaN, listOf(f to .6, 1.37*f to .8, 2*f to weak, 2.74*f to .3)))
    yield(signal("nearHarmonic", Double.NaN, listOf(h*f to 1.0, (h+1.06+.08*rng.nextDouble())*f to neighbor)))
    yield(signal("independentThree", Double.NaN, listOf(3*f to .4, 5.003*f to 1.0, 7.009*f to neighbor)))
    yield(signal("stretched", Double.NaN, listOf(2*f to .5, 3.013*f to 1.0, 5.055*f to neighbor)))
    yield(signal("broadband", 0.0, listOf(f to 0.0)))
    yield(Observation(Trial("silence", p, rate, group, 0.0, ShortArray(p.windowSamples))))
    val pole = exp(-PI*f/(rate*(3 + 10*rng.nextDouble())))
    val coefficient = 2*pole*cos(2*PI*f/rate)
    var a = 0.0
    var b = 0.0
    fun resonator(): Double {
        val value = coefficient*a - pole*pole*b + rng.nextGaussian()
        b = a
        a = value
        return value
    }
    repeat(rate/2) { resonator() }
    val filtered = DoubleArray(p.windowSamples) { resonator() }
    val gain = 16000 / filtered.maxOf { abs(it) }
    yield(Observation(Trial("resonator", p, rate, group, 0.0,
        ShortArray(filtered.size) { (filtered[it]*gain).roundToInt().toShort() })))
}

/** Consecutive overlapping windows on one PCM stream with independent reset between scenarios. */
private fun stream(p: InstrumentProfile, rate: Int, group: Int, f: Double, rng: Random, name: String) = sequence {
    val hop = 4096 * p.analysisChunks
    val size = p.windowSamples
    val boundary = size + 3*hop + rng.nextInt(hop)
    val ratio = if (group % 2 == 0) 2.0 else 2.7
    val phase = 2 * PI * rng.nextDouble()
    val noise = .003 + .014 * rng.nextDouble()
    val h = if (group % 2 == 0) 5 else 7
    val total = size + 12*hop
    var angle = phase
    fun frequency(i: Int): Double = when {
        i < boundary -> f
        name == "register" -> f*ratio
        name == "glide" -> f*(1 + .055*sin(2*PI*(i-boundary)/rate*1.7))
        else -> f
    }
    val samples = ShortArray(total) { i ->
        val tone = when {
            i < boundary || name == "register" -> sin(angle) + .4*sin(2*angle)
            name == "gap" -> 0.0
            name == "glide" -> sin(angle) + .4*sin(2*angle)
            else -> (.6 + .3*cos(2*PI*(i-boundary)/rate*3))*sin(h*angle) + .2*sin((h+1)*angle)
        }
        angle += 2*PI*frequency(i)/rate
        (11000*(tone + if (name == "gap" && i >= boundary) 0.0 else noise*rng.nextGaussian())).roundToInt().toShort()
    }
    for ((frame, end) in (size..total step hop).withIndex()) {
        val category = when {
            end <= boundary -> "${name}_before"
            name == "gap" && end-size < boundary -> "gap_mixed"
            else -> name
        }
        val target = if (category == "gap") 0.0 else if (category == "gap_mixed") Double.NaN else frequency(end-1)
        yield(Observation(Trial(category, p, rate, group, target, samples.copyOfRange(end-size, end),
            (end-boundary)*1000.0/rate, if (p == InstrumentProfile.DIDGERIDOO) 220 else p.initialMaxHz), frame))
    }
}

internal fun independentSignals(held: Boolean) = sequence {
    val rng = Random(if (held) 7369021L else 957131L)
    for (p in InstrumentProfile.entries) repeat(if (held) 36 else 12) { group ->
        val rate = listOf(32000,44100,48000)[group % 3]
        val f = if (p == InstrumentProfile.DIDGERIDOO) 48 + 31*rng.nextDouble() else 215 * 2.65.pow(rng.nextDouble())
        yieldAll(staticSignals(p, rate, group, f, rng))
        for (name in listOf("articulation","register","gap","glide")) yieldAll(stream(p, rate, group, f, rng, name))
    }
}
