package ar.didgetaller.study

import ar.didgetaller.core.InstrumentProfile
import com.sun.management.ThreadMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.*

private fun challenge(held: Boolean) = sequence {
    val rng = Random(if (held) 290153 else 82021)
    for (profile in InstrumentProfile.entries) repeat(if (held) 28 else 10) { group ->
        val didge = profile == InstrumentProfile.DIDGERIDOO
        val f = if (didge) 65 + 10 * rng.nextDouble() else 205 * 6.5.pow(rng.nextDouble())
        for (h in listOf(2, 3, 5)) for (family in listOf("weak", "missing", "formant", "isolated", "evenOnly", "detuned")) {
            val residual = .025 + .22 * rng.nextDouble()
            val extra = .03 + .22 * rng.nextDouble()
            val phase = rng.nextDouble() * 2 * PI
            val pcm = ShortArray(profile.windowSamples) { i ->
                val t = i / 44100.0
                val a = 2 * PI * f * t
                val dominant = sin(h * a + phase)
                val neighbor = extra * sin((h + 1) * a)
                val value = when (family) {
                    "weak" -> dominant + residual * sin(a) + neighbor
                    "missing" -> dominant + neighbor
                    "formant" -> (.55 + .45 * cos(2 * PI * 3 * t)) * dominant + neighbor + residual * sin(a)
                    "isolated" -> dominant
                    "evenOnly" -> sin(2 * a) + .35 * sin(4 * a + phase)
                    else -> dominant + extra * sin((h + 1.107) * a)
                }
                (14000 * value).roundToInt().toShort()
            }
            // A lone line can come from a source at h*f, or a filtered source at f.
            // Two even lines have the same ambiguity. Detuned sources have no labelled source F0.
            val target = if (family in listOf("isolated", "evenOnly", "detuned")) Double.NaN else f
            yield(Trial("$family$h", profile, 44100, group, target, pcm))
        }
        // Abrupt exact-octave/third-register changes with phase continuity, at varying callback offsets.
        val boundary = 40960 + rng.nextInt(8192)
        val ratio = if (group % 2 == 0 || !didge) 2.0 else 2.8
        val old = if (didge) f else f / 2 + 120
        val new = old * ratio
        val hop = 4096 * profile.analysisChunks
        val first = profile.windowSamples + hop - 4096
        for (end in first..boundary + 42000 step hop) if (end >= boundary) {
            val pcm = ShortArray(profile.windowSamples) { i ->
                val n = end - profile.windowSamples + i
                val angle = 2 * PI * (old * min(n, boundary) + new * max(0, n - boundary)) / 44100
                (12000 * (sin(angle) + .25 * sin(2 * angle))).roundToInt().toShort()
            }
            yield(Trial("jump$ratio", profile, 44100, group, new, pcm, (end - boundary) * 1000.0 / 44100,
                if (didge) 220 else profile.initialMaxHz))
        }
    }
}

internal fun phaseTwo(parent: File, split: String) {
    // Same digital observation under two latent-source labels. An estimator cannot distinguish them.
    fun sine(hz: Double) = ShortArray(16384) { (16000 * sin(2 * PI * hz * it / 44100)).roundToInt().toShort() }
    check(sine(340.0).contentEquals(sine(5 * 68.0)))
    val dir = File(parent, "phase2").apply { mkdirs() }
    val trials = when (split) {
        "dev" -> challenge(false)
        "test" -> challenge(true)
        "old" -> synthetic(true).filter { it.rate == 44100 } + harmonicBalance() + resonantNoise() + transitions(true)
        "flute" -> publicFlute(File(parent, "Flute.nonvib.mf.C5B5.aiff")).asSequence()
        else -> error("Unknown split $split")
    }
    val beds = InstrumentProfile.entries.associateWith { HarmonicStudy(it.windowSamples) }
    val bean = ManagementFactory.getPlatformMXBean(ThreadMXBean::class.java)
    bean.isThreadAllocatedMemoryEnabled = true
    bean.isThreadCpuTimeEnabled = true
    val tid = Thread.currentThread().threadId()
    val methods: List<Pair<String, (Trial) -> Decision>> = listOf(
        "YIN" to { t -> reading(t, t.pcm) },
        "Adaptive" to ::adaptive,
        "Harmonic" to { t -> beds.getValue(t.profile).estimate(t) })
    // Warm the execution paths before measuring. This PCM is never scored.
    val warm = sequenceOf(InstrumentProfile.DIDGERIDOO, InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        .flatMap { p -> challenge(false).filter { it.profile == p }.take(18) }.toList()
    repeat(6) { for (t in warm) for ((_, estimate) in methods) estimate(t) }
    File(dir, "$split.csv").printWriter().use { out ->
        out.println("id,profile,category,seed,target_hz,elapsed_ms,max_hz,offset,samples,estimator,estimated_hz,strength,wall_ns,cpu_ns,bytes")
        File(dir, "$split.pcm").outputStream().buffered().use { stream ->
            var offset = 0L
            for ((id, t) in trials.withIndex()) {
                val bytes = ByteBuffer.allocate(t.pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                bytes.asShortBuffer().put(t.pcm)
                stream.write(bytes.array())
                for ((name, estimate) in methods) {
                    val allocated = bean.getThreadAllocatedBytes(tid)
                    val cpu = bean.currentThreadCpuTime
                    val start = System.nanoTime()
                    val d = estimate(t)
                    val wall = System.nanoTime() - start
                    val cpuUsed = bean.currentThreadCpuTime - cpu
                    val used = bean.getThreadAllocatedBytes(tid) - allocated
                    out.println(listOf(id,t.profile,t.category,t.seed,t.target,t.elapsedMs,t.upperHz,offset,t.pcm.size,
                        name,d.hz,d.strength,wall,cpuUsed,used).joinToString(","))
                }
                offset += t.pcm.size * 2L
            }
        }
    }
}
