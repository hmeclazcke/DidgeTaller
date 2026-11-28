package ar.didgetaller.core

import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class DetectorComparisonTest {
    @Test fun liveSelectionLeavesStandardEvidenceAndOtherAudioToolsAlone() {
        val conventional = LivePitch.State(hz = 102.0, storedHz = 102.0)
        val raw = PitchDetector.Reading(hz = 102.0, confidence = .96)
        val profile = InstrumentProfile.DIDGERIDOO
        assertSame(conventional, LivePitch.display(LivePitch.State(), raw, profile, conventional))
        val assisted = raw.copy(harmonic = HarmonicEstimate(68.0, .33))
        assertEquals(68.0, LivePitch.display(LivePitch.State(), assisted, profile, conventional).hz, 0.0)
        assertEquals(PitchAlgorithm.STANDARD, PitchAlgorithm.HARMONIC.forCapture(false))
        assertEquals(PitchAlgorithm.HARMONIC, PitchAlgorithm.HARMONIC.forCapture(true))
    }
    private class Signal(val pcm: ShortArray, override val sampleRate: Int = 44100) : MonoPcmSource {
        override val frameCount = pcm.size.toLong()
        override fun read(startFrame: Long, destination: ShortArray): Int {
            val at = startFrame.toInt()
            val count = minOf(destination.size, pcm.size - at)
            pcm.copyInto(destination, 0, at, at + count)
            return count
        }
    }

    @Test fun bothChannelsShareWindowsAndPreserveStandardReadingsAndSilence() {
        for (profile in InstrumentProfile.entries) {
            val f = if (profile == InstrumentProfile.DIDGERIDOO) 68.0 else 245.0
            val rate = 48000
            val source = Signal(ShortArray(rate * 3) { i ->
                val t = i * 2 * PI * f / rate
                if (i in rate..2*rate) 0 else (10000*sin(3*t) + 800*sin(4*t) + 300*sin(t)).toInt().toShort()
            }, rate)
            val baseline = DetectorComparison.analyze(source, profile, profile.initialMaxHz, PitchAlgorithm.STANDARD)
            val result = DetectorComparison.analyze(source, profile, profile.initialMaxHz, PitchAlgorithm.HARMONIC)
            assertEquals(baseline.frames.map { it.centerMs to it.standard }, result.frames.map { it.centerMs to it.standard })
            assertTrue(baseline.frames.all { it.harmonic == null })
            assertTrue(result.frames.any { abs((it.harmonic ?: 0.0) - f) < .3 })
            assertTrue(result.frames.any { abs((it.standard ?: 0.0) - (it.harmonic ?: 0.0)) > 1 })
            val quiet = result.at(1500)!!
            assertNull(quiet.harmonic)
            assertNull(quiet.standard)
            assertNull(result.at(0))
            assertNull(result.at(3100))
            assertFalse(result.marks { it.harmonic }.filterIsInstance<TraceDrawing.Segment>().any {
                it.from.timeMs < quiet.centerMs && it.to.timeMs > quiet.centerMs
            })
        }
    }

    @Test fun sparseHarmonicsCanSupportAnInferenceButCannotIdentifyTheSource() {
        val profile = InstrumentProfile.DIDGERIDOO
        val samePressure = Signal(ShortArray(44100) { i ->
            (9500*sin(2*PI*340*i/44100) + 2300*sin(2*PI*408*i/44100)).toInt().toShort()
        })
        val result = DetectorComparison.analyze(samePressure, profile, 140, PitchAlgorithm.HARMONIC)
        assertTrue(result.frames.all { abs((it.harmonic ?: 0.0) - 68) < .2 })
        assertTrue(result.at(result.frames.first().centerMs.roundToLong()) == result.frames.first())
    }

    @Test fun cancelledReplayStopsReadingAtNextChunk() {
        var checks = 0
        assertThrows(CancellationException::class.java) {
            DetectorComparison.analyze(Signal(ShortArray(44100)), InstrumentProfile.DIDGERIDOO, 140, PitchAlgorithm.HARMONIC)
                { ++checks == 3 }
        }
        assertEquals(3, checks)
    }
}
