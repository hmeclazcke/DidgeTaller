package ar.didgetaller.core

import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin

/** One executable contract runs against isolated original and rebuilt production classpaths. */
object DifferentialProbe {
    @JvmStatic fun main(arguments: Array<String>) {
        val noise = java.util.Random(20261107)
        for (size in listOf(2048, 4096, 8192, 16384)) {
            for (hz in listOf(35.0, 55.0, 70.0, 140.0, 200.0, 440.0, 880.0, 1600.0)) {
                for (kind in 0..3) {
                    val pcm = ShortArray(size) { i ->
                        val phase = 2 * PI * hz * i / 44100
                        when (kind) {
                            0 -> 0
                            1 -> (8000 * sin(phase)).toInt()
                            2 -> (3000 * sin(phase) + 9000 * sin(2 * phase)).toInt()
                            else -> if (i < size / 2) noise.nextInt(80) - 40 else (9000 * sin(phase)).toInt()
                        }.toShort()
                    }
                    val profile = if (hz >= 200) InstrumentProfile.NATIVE_AMERICAN_FLUTE else InstrumentProfile.DIDGERIDOO
                    var diagnostic: PitchDetector.Diagnostic? = null
                    val reading = PitchDetector.analyzeWithDiagnostics(pcm, 44100, profile.minHz, profile.maxHz, true) {
                        diagnostic = it
                    }
                    println("signal/$size/$hz/$kind: $reading | $diagnostic")
                    val wav = WavCodec.encode(pcm, pcm.size, 44100)
                    println("wav: " + MessageDigest.getInstance("SHA-256").digest(wav).joinToString("") { "%02x".format(it) })
                }
            }
        }
        val random = java.util.Random(42)
        for (profile in InstrumentProfile.entries) {
            repeat(80) { scenario ->
                val points = List(30) { index ->
                    val hz = if (index % 7 == 0) null else profile.minHz + random.nextDouble() * (profile.maxHz - profile.minHz)
                    SessionAnalysis.Point(index * 200L, hz, listOf(.49, .65, .8, .9, .98)[random.nextInt(5)])
                }
                println("trace/$profile/$scenario: " + PitchTrace.build(points, profile))
                println("summary: " + SessionAnalysis.summarize(points, profile.validMinHz, profile.validMaxHz))
                println("range: " + PlotScales.overview(points, profile))
            }
        }
        val viewport = PlotViewport(AxisWindow(30000.0, 90000.0), AxisWindow(35.0, 220.0))
        val projection = PlotProjection(viewport, 320.0, 200.0, 48.0, 12.0)
        for (x in listOf(-10.0, 48.0, 208.0, 368.0, 400.0)) println("projection/$x: " + projection.data(x, 100.0))
        offlineRecordingContracts()
    }

    private fun offlineRecordingContracts() {
        for (profile in InstrumentProfile.entries) for (kind in 0..3) {
            val hz = if (profile == InstrumentProfile.DIDGERIDOO) 70.0 else 440.0
            val pcm = ShortArray(24000) { i ->
                val phase = 2 * PI * hz * i / 44100
                when (kind) {
                    0 -> 0.0
                    1 -> 7000 * sin(phase)
                    2 -> 3000 * sin(phase) + 9000 * sin(2 * phase)
                    else -> if (i in 7000..11000) 0.0 else 7000 * sin(phase * if (i < 12000) 1.0 else 1.5)
                }.toInt().toShort()
            }
            val source = object : MonoPcmSource {
                override val sampleRate = 44100
                override val frameCount = pcm.size.toLong()
                override fun read(startFrame: Long, destination: ShortArray): Int {
                    val count = minOf(destination.size, pcm.size - startFrame.toInt())
                    pcm.copyInto(destination, 0, startFrame.toInt(), startFrame.toInt() + count)
                    return count
                }
            }
            for ((start, end) in listOf(0L to source.frameCount, 211L to source.frameCount - 119)) {
                val progress = mutableListOf<Int>()
                val result = WavAnalysis.analyze(source, start, end,
                    WavAnalysis.Settings(4096, 2048, profile.minHz, profile.maxHz, 10000, profile), progress = progress::add)
                val bytes = java.nio.ByteBuffer.allocate(result.spectrum.density.size * 8)
                result.spectrum.density.forEach(bytes::putDouble)
                val digest = MessageDigest.getInstance("SHA-256").digest(bytes.array()).joinToString("") { "%02x".format(it) }
                println("offline/$profile/$kind/$start: ${result.startFrame}/${result.endFrame}/${result.windows}/$digest/" +
                    "${result.harmonics}/${result.timeline}/${result.summary}/${result.medianValidHz}/" +
                    "${result.meanValidHz}/${result.validWindowFraction}/${result.medianPeriodicityConfidence}/$progress")
            }
        }
    }

}
