package ar.didgetaller.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import ar.didgetaller.core.MetronomeClick
import ar.didgetaller.core.MetronomePcm

/** Two reusable 12 ms PCM clicks. No PCM generation/allocation on the pulse path. */
internal class AndroidMetronomeClick : MetronomeClick {
    private val samples = mapOf(false to MetronomePcm.generate(false), true to MetronomePcm.generate(true))
    private val tracks = mutableMapOf<Boolean, AudioTrack>()
    override fun play(accent: Boolean, volume: Float): Boolean {
        val track = tracks.getOrPut(accent) { create(samples.getValue(accent)) }
        track.stop()
        check(track.reloadStaticData() == AudioTrack.SUCCESS)
        check(track.setVolume(volume) == AudioTrack.SUCCESS)
        track.play()
        return track.playState == AudioTrack.PLAYSTATE_PLAYING
    }
    private fun create(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(MetronomePcm.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
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
    internal fun renderedFrames(): Int = tracks.values.sumOf { it.playbackHeadPosition }
    internal fun routedDeviceTypes(): List<Int> = tracks.values.mapNotNull { it.routedDevice?.type }.distinct()
}
