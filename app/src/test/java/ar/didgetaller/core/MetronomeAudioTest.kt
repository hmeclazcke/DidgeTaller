package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MetronomeAudioTest {
    @Test fun mediaMuteAndZeroVolumeBothPreventAudibility() {
        assertTrue(mediaIsMuted(true, 5))
        assertTrue(mediaIsMuted(false, 0))
        assertTrue(mediaIsMuted(true, 0))
        assertFalse(mediaIsMuted(false, 5))
    }
    @Test fun bothClickBuffersHaveAudibleEnergyDistinctAccentAndNoClipping() {
        val normal = MetronomePcm.generate(false)
        val accent = MetronomePcm.generate(true)
        assertEquals(576, normal.size); assertEquals(normal.size, accent.size)
        for (pcm in listOf(normal, accent)) {
            val rms = sqrt(pcm.sumOf { it.toDouble() * it } / pcm.size) / 32768
            assertTrue("PCM must not be silent", rms > .15)
            assertTrue(pcm.maxOf { abs(it.toInt()) } in 20000..30000)
            assertEquals(0, pcm.first().toInt())
            assertTrue(abs(pcm.last().toInt()) < 10)
        }
        assertFalse(normal.contentEquals(accent))
    }
    @Test fun requestedSoundAndEffectiveSilenceAreDifferentStates() {
        val settings = MetronomeSettings(audible = true)
        assertEquals(MetronomeSoundStatus.OFF, metronomeSoundStatus(settings.copy(audible = false), false, false, false))
        assertEquals(MetronomeSoundStatus.BLOCKED, metronomeSoundStatus(settings, true, false, false))
        assertEquals(MetronomeSoundStatus.SYSTEM_MUTED, metronomeSoundStatus(settings, false, false, true))
        assertEquals(MetronomeSoundStatus.GAIN_ZERO, metronomeSoundStatus(settings.copy(volume = 0f), false, false, false))
        assertEquals(MetronomeSoundStatus.FAILED, metronomeSoundStatus(settings, false, true, false))
        assertEquals(MetronomeSoundStatus.ON, metronomeSoundStatus(settings, false, false, false))
    }
}
