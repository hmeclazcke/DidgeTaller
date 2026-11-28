package ar.didgetaller.study

import ar.didgetaller.core.*
import com.sun.management.ThreadMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.sound.sampled.AudioSystem
import kotlin.math.*

private val fluteFile = "Flute.nonvib.mf.C5B5.aiff"
private const val fluteHash = "43d8d6c620fe9c508c30b568b023e54fc226960adcb8455467e511a22866066c"

/** Nominal chromatic note labels from the publisher. No estimate is used to align notes. */
internal fun publicFlute(file: File): List<Trial> {
    check(MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) } == fluteHash)
    val pcm = AudioSystem.getAudioInputStream(file).use { input ->
        require(input.format.run { isBigEndian && sampleRate == 44100f && channels == 1 && sampleSizeInBits == 16 })
        val samples = ByteBuffer.wrap(input.readAllBytes()).asShortBuffer()
        ShortArray(samples.remaining()).also { samples.get(it) }
    }
    // Central seconds of the twelve amplitude-separated notes in the pinned file.
    val centers = listOf(1.44, 4.16, 6.86, 9.65, 12.35, 15.04, 17.53, 20.07, 22.80, 25.64, 28.51, 31.43)
    val profile = InstrumentProfile.NATIVE_AMERICAN_FLUTE
    return centers.flatMapIndexed { index, center ->
        listOf(-.4, 0.0, .4).map { offset ->
            val start = ((center + offset) * 44100).roundToInt() - profile.windowSamples / 2
            Trial("nominalFlute", profile, 44100, start, 440 * 2.0.pow((index + 3) / 12.0),
                pcm.copyOfRange(start, start + profile.windowSamples))
        }
    }
}

private fun costs(directory: File, split: String, fixtures: List<Trial>) {
    val bean = ManagementFactory.getPlatformMXBean(ThreadMXBean::class.java)
    check(bean.isThreadAllocatedMemorySupported && bean.isCurrentThreadCpuTimeSupported)
    bean.isThreadAllocatedMemoryEnabled = true
    bean.isThreadCpuTimeEnabled = true
    val tid = Thread.currentThread().threadId()
    var checksum = 0.0
    File(directory, "$split-cost.csv").printWriter().use { out ->
        out.println("profile,mode,frames,wall_ns,cpu_ns,allocated_bytes,p95_call_ns")
        fun measure(profile: InstrumentProfile, label: String, calls: Int, frames: Int = calls, run: () -> Double) {
            repeat(4) { repeat(calls) { checksum += run() } }
            repeat(9) {
                val durations = LongArray(calls)
                val bytes = bean.getThreadAllocatedBytes(tid)
                val cpu = bean.currentThreadCpuTime
                val start = System.nanoTime()
                repeat(calls) { index ->
                    val before = System.nanoTime()
                    checksum += run()
                    durations[index] = System.nanoTime() - before
                }
                val wall = System.nanoTime() - start
                val usedCpu = bean.currentThreadCpuTime - cpu
                val usedBytes = bean.getThreadAllocatedBytes(tid) - bytes
                durations.sort()
                val p95 = durations[ceil(calls * .95).toInt() - 1]
                out.println("${profile.name},$label,$frames,$wall,$usedCpu,$usedBytes,$p95")
            }
        }
        for (profile in InstrumentProfile.entries) {
            val fixture = fixtures.first { it.profile == profile && it.rate == 44100 && it.category == "rich" }
            for (method in Estimator.entries) measure(profile, method.name, 32) {
                method.estimate(fixture.pcm, 44100, profile).first
            }
            val stream = PitchStream(profile)
            // Use a continuous oscillator across callback boundaries.
            val cyclesPerChunk = if (profile == InstrumentProfile.DIDGERIDOO) 7 else 43
            val chunk = ShortArray(4096) { i -> (9000 * sin(2 * PI * i * cyclesPerChunk / 4096)).toInt().toShort() }
            measure(profile, "LiveWithVisuals", 32) {
                var hz = 0.0
                repeat(profile.analysisChunks) { hz += stream.append(chunk)?.hz ?: 0.0 }
                hz
            }
        }
        val source = object : MonoPcmSource {
            override val sampleRate = 44100
            override val frameCount = 88200L
            private val pcm = ShortArray(frameCount.toInt()) { i ->
                val angle = 2 * PI * 68 * i / sampleRate
                (8000 * sin(angle) + 5000 * sin(2 * angle) + 3000 * sin(3 * angle)).toInt().toShort()
            }
            override fun read(startFrame: Long, destination: ShortArray): Int {
                val count = min(destination.size, pcm.size - startFrame.toInt())
                pcm.copyInto(destination, 0, startFrame.toInt(), startFrame.toInt() + count)
                return count
            }
        }
        val profile = InstrumentProfile.DIDGERIDOO
        val replay = SavedYinReplay.analyze(source)
        val component = DroneComponentAnalysis().analyze(source)
        check(replay.frames.all { abs(checkNotNull(it.acceptedHz) - 68) < 1 })
        check(component.samples.any { abs((it.hz ?: 0.0) - 68) < 1 })
        measure(profile, "OfflineReplay", 1, replay.frames.size) { SavedYinReplay.analyze(source).frames.size.toDouble() }
        measure(profile, "OfflineComponent", 1, component.samples.size) { DroneComponentAnalysis().analyze(source).samples.size.toDouble() }
    }
    check(checksum.isFinite() && checksum > 0)
}

fun main(args: Array<String>) {
    val directory = File(args[0]).apply { mkdirs() }
    val split = args[1]
    if (split == "android-harmonic-parity") {
        checkAndroidHarmonicParity()
        return
    }
    if (split == "phase3-check") {
        checkSeparatedLines()
        return
    }
    if (split.startsWith("phase3-")) {
        phaseThree(directory, split.removePrefix("phase3-"))
        return
    }
    if (split.startsWith("phase2-")) {
        phaseTwo(directory, split.removePrefix("phase2-"))
        return
    }
    val trials = when (split) {
        "public" -> publicFlute(File(directory, fluteFile))
        "replication" -> replication().toList()
        "balance" -> harmonicBalance().toList()
        "resonators" -> resonantNoise().toList()
        "development", "held" -> (synthetic(split == "held") + transitions(split == "held")).toList()
        else -> error("Unknown evaluation split: $split")
    }
    File(directory, "$split.csv").printWriter().use { out ->
        out.println("profile,category,rate,seed,target_hz,elapsed_ms,estimator,estimated_hz,strength")
        for (trial in trials) for (method in Estimator.entries) {
            val (hz, strength) = method.estimate(trial.pcm, trial.rate, trial.profile)
            out.println(listOf(trial.profile.name, trial.category, trial.rate, trial.seed, trial.target, trial.elapsedMs,
                method.name, hz, strength).joinToString(","))
        }
    }
    if (split == "held") costs(directory, split, trials)
}
