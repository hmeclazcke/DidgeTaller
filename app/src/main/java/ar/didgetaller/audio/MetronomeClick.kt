package ar.didgetaller.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import ar.didgetaller.core.MetronomeClick
import kotlin.math.PI
import kotlin.math.sin

/** Two reusable 12 ms PCM clicks. No PCM generation/allocation on the pulse path. */
internal class AndroidMetronomeClick : MetronomeClick {
    private val samples = mapOf(false to pcm(1100.0), true to pcm(1800.0))
    private val tracks = mutableMapOf<Boolean, AudioTrack>()
    override fun play(accent: Boolean, volume: Float): Boolean {
        val track = tracks.getOrPut(accent) { create(samples.getValue(accent)) }
        track.stop()
        check(track.reloadStaticData() == AudioTrack.SUCCESS)
        track.setVolume(volume)
        track.play()
        return true
    }
    private fun create(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(samples.size * 2).build()
        try {
            check(track.state != AudioTrack.STATE_UNINITIALIZED)
            check(track.write(samples, 0, samples.size) == samples.size)
            return track
        } catch (failure: RuntimeException) { track.release(); throw failure }
    }
    override fun silence() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }
    override fun close() = silence()
    private fun pcm(frequency: Double): ShortArray = ShortArray(576) { sample ->
        val envelope = (1.0 - sample / 576.0) * (1.0 - sample / 576.0)
        (sin(2 * PI * frequency * sample / RATE) * envelope * 22000).toInt().toShort()
    }
    private companion object { const val RATE = 48000 }
}
