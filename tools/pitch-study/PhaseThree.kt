package ar.didgetaller.study

import ar.didgetaller.core.InstrumentProfile
import com.sun.management.ThreadMXBean
import java.io.File
import java.lang.management.ManagementFactory

private fun previousChallenge() = sequence {
    var f = Double.NaN
    for (t in challenge(true)) {
        if (t.category == "weak2") f = t.target
        val h = t.category.last().digitToIntOrNull() ?: 0
        val line = when {
            t.category.startsWith("isolated") || t.category.startsWith("detuned") -> f*h
            t.category.startsWith("evenOnly") -> 2*f
            else -> Double.NaN
        }
        yield(Observation(t, lowestLine = line))
    }
}

internal fun phaseThree(parent: File, split: String) {
    val trials = when (split) {
        "dev", "held" -> independentSignals(split == "held")
        "frozen" -> previousChallenge()
        "legacy" -> (synthetic(true) + resonantNoise()).map { Observation(it) }
        "flute" -> publicFlute(File(parent, "Flute.nonvib.mf.C5B5.aiff")).asSequence().map { Observation(it) }
        else -> error("Unknown phase 3 evaluation: $split")
    }
    val readers = InstrumentProfile.entries.associateWith { p ->
        Route.entries.associateWith { route -> EvidenceReader(p.windowSamples, route) }
    }
    val counters = ManagementFactory.getPlatformMXBean(ThreadMXBean::class.java)
    counters.isThreadAllocatedMemoryEnabled = true
    counters.isThreadCpuTimeEnabled = true
    val thread = Thread.currentThread().threadId()
    for (p in InstrumentProfile.entries) {
        val warm = independentSignals(false).filter { it.trial.profile == p }.take(16).toList()
        repeat(3) { for (obs in warm) readers.getValue(p).values.forEach { it.read(obs.trial) } }
    }
    val dir = File(parent, "phase3").apply { mkdirs() }
    File(dir, "$split.csv").bufferedWriter().use { out ->
        out.appendLine("id,profile,rate,seed,category,frame,elapsed_ms,target_hz,lowest_line,estimator,estimated_hz,strength,hypothesis,rank,wall_ns,cpu_ns,bytes")
        for ((id, obs) in trials.withIndex()) {
            val t = obs.trial
            for ((route, reader) in readers.getValue(t.profile)) {
                if (obs.frame == 0) reader.reset()
                val alloc = counters.getThreadAllocatedBytes(thread)
                val cpu = counters.currentThreadCpuTime
                val start = System.nanoTime()
                val result = reader.read(t)
                val wall = System.nanoTime() - start
                val usedCpu = counters.currentThreadCpuTime - cpu
                val bytes = counters.getThreadAllocatedBytes(thread) - alloc
                out.appendLine(listOf(id,t.profile,t.rate,t.seed,t.category,obs.frame,t.elapsedMs,t.target,
                    obs.lowestLine,route,result.pitch.hz,result.pitch.strength,result.hypothesis?.hz ?: 0,
                    result.hypothesis?.rank ?: 0,wall,usedCpu,bytes).joinToString(","))
            }
        }
    }
}
