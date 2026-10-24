package ar.didgetaller.core

import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SavedYinReplayTest {
    @Test fun replayUsesWavWindowEndsAndReanalyzesChangedSource() {
        var audible = true
        val source = object : MonoPcmSource {
            override val sampleRate = 44100
            override val frameCount = 44100L * 3
            override fun read(startFrame: Long, destination: ShortArray): Int {
                val count = minOf(destination.size, (frameCount - startFrame).toInt())
                repeat(count) { i ->
                    val t = (startFrame + i).toDouble() / sampleRate
                    destination[i] = if (audible)
                        ((.12 * sin(2 * PI * 70 * t) + .07 * sin(2 * PI * 140 * t)) * 32767).toInt().toShort()
                    else 0
                }
                return count
            }
        }
        val voiced = SavedYinReplay.analyze(source)
        assertTrue(voiced.frames.any { it.acceptedHz != null })
        assertTrue(voiced.frames.all { it.endFrame - it.startFrame == 16384L })
        assertEquals(voiced.frames.map { it.endFrame * 1000 / 44100 }, voiced.points.map { it.timeMs })
        audible = false
        val silent = SavedYinReplay.analyze(source)
        assertEquals(voiced.version, silent.version)
        assertTrue(silent.frames.all { it.acceptedHz == null && it.reason == "low_amplitude" })
    }

    @Test fun privateHistoricalLowPointIsRejectedByCurrentSourceReplayWhenAvailable() {
        val wav = System.getenv("DIDGE_COMPONENT_WAV")?.let(::File)
        assumeTrue("Protected WAV is opt-in", wav?.isFile == true)
        val replay = Pcm16WavReader(wav!!).use { SavedYinReplay.analyze(it) }
        val tail = replay.frames.minBy { kotlin.math.abs(it.centerMs - 38174.0) }
        assertEquals("low_comparison_amplitude", tail.reason)
        assertNull(tail.acceptedHz)
        assertTrue((tail.comparisonRms ?: 1.0) < .005)
        val energetic = replay.frames.minBy { kotlin.math.abs(it.centerMs - 33158.0) }
        assertTrue(energetic.rms > .09)
        assertTrue((energetic.candidateHz ?: 0.0) in 34.0..38.0)
        assertEquals(214, replay.frames.size)
    }

    @Test fun replayPreservesCapturedExpandedRangeToots() {
        val captured = listOf(SessionAnalysis.Point(400, 180.0, .95))
        assertTrue(SavedYinReplay.usesExpandedRange(captured))
        assertFalse(SavedYinReplay.usesExpandedRange(listOf(SessionAnalysis.Point(400, 140.0, .95))))
        val source = object : MonoPcmSource {
            override val sampleRate = 44100
            override val frameCount = 44100L * 2
            override fun read(startFrame: Long, destination: ShortArray): Int {
                val count = minOf(destination.size, (frameCount - startFrame).toInt())
                repeat(count) { i ->
                    val t = (startFrame + i).toDouble() / sampleRate
                    destination[i] = (.2 * sin(2 * PI * 180 * t) * 32767).toInt().toShort()
                }
                return count
            }
        }
        val replay = SavedYinReplay.analyze(source, expandedRange = true)
        assertTrue(replay.frames.any { (it.acceptedHz ?: 0.0) in 175.0..185.0 })
    }
}
