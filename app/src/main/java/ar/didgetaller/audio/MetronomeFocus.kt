package ar.didgetaller.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import ar.didgetaller.core.MetronomeFocus
import ar.didgetaller.core.MetronomeTask

/** Only platform acquisition/release; response ordering and loss policy belong to the output owner. */
internal class AndroidMetronomeFocus(context: Context) : MetronomeFocus {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())
    override fun request(changed: (Int) -> Unit): MetronomeTask? {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setOnAudioFocusChangeListener(changed, main).build()
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return null
        return MetronomeTask { audio.abandonAudioFocusRequest(request) }
    }
}
