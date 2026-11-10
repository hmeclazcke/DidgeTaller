package ar.didgetaller.core

import kotlin.math.PI
import kotlin.math.sin

/** Reusable, deterministic PCM16 output, independent of platform acceptance of a track. */
object MetronomePcm {
    const val RATE = 48000
    fun generate(accent: Boolean): ShortArray {
        val frequency = if (accent) 1800.0 else 1100.0
        return ShortArray(576) { sample ->
            val envelope = (1.0 - sample / 576.0) * (1.0 - sample / 576.0)
            (sin(2 * PI * frequency * sample / RATE) * envelope * 22000).toInt().toShort()
        }
    }
}

enum class MetronomeSoundStatus { OFF, BLOCKED, SYSTEM_MUTED, GAIN_ZERO, FAILED, ON }

fun mediaIsMuted(muted: Boolean, volume: Int): Boolean = muted || volume == 0

fun metronomeSoundStatus(settings: MetronomeSettings, capture: Boolean, failed: Boolean,
    systemMuted: Boolean): MetronomeSoundStatus = when {
    capture -> MetronomeSoundStatus.BLOCKED
    !settings.audible -> MetronomeSoundStatus.OFF
    failed -> MetronomeSoundStatus.FAILED
    systemMuted -> MetronomeSoundStatus.SYSTEM_MUTED
    settings.volume == 0f -> MetronomeSoundStatus.GAIN_ZERO
    else -> MetronomeSoundStatus.ON
}
