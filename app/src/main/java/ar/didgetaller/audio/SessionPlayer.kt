package ar.didgetaller.audio

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import ar.didgetaller.core.PlaybackBackend
import ar.didgetaller.core.PlaybackController
import ar.didgetaller.core.PlaybackEvents
import ar.didgetaller.core.PlaybackScheduler
import java.io.File

/** Android owns media effects; the framework-independent controller owns timeline decisions. */
class SessionPlayer(changed: (String?, Boolean) -> Unit, error: (String) -> Unit) {
    private val controller = PlaybackController(::AndroidPlayback, HandlerScheduler(), changed,
        { failure -> error("No se pudo reproducir: ${failure.message}") })
    val id get() = controller.id
    val paused get() = controller.paused
    val ready get() = controller.ready
    val loop get() = controller.loop
    fun open(sessionId: String, file: File, play: Boolean, position: Long = 0) = controller.open(sessionId, file, play, position)
    fun toggle(sessionId: String, file: File) = controller.toggle(sessionId, file)
    fun seek(sessionId: String, file: File, position: Long) = controller.seek(sessionId, file, position)
    fun loop(region: LongRange?) = controller.loop(region)
    fun position() = controller.position()
    fun duration() = controller.duration()
    fun restart(sessionId: String, file: File) = controller.restart(sessionId, file)
    fun stop() = controller.stop()
    fun close() = controller.close()
}

private class HandlerScheduler : PlaybackScheduler {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    override fun schedule(task: () -> Unit) {
        cancel()
        pending = Runnable(task).also { handler.postDelayed(it, 40) }
    }
    override fun cancel() { pending?.let { handler.removeCallbacks(it) }; pending = null }
}

private class AndroidPlayback : PlaybackBackend {
    private val player = MediaPlayer()
    override val positionMs get() = player.currentPosition
    override val durationMs get() = player.duration
    override val playing get() = player.isPlaying
    override fun prepare(file: File, events: PlaybackEvents) {
        player.setDataSource(file.absolutePath)
        player.setOnPreparedListener { events.prepared() }
        player.setOnSeekCompleteListener { events.seekCompleted() }
        player.setOnCompletionListener { events.completed() }
        player.setOnErrorListener { _, what, extra -> events.failed(IllegalStateException("Audio $what/$extra")); true }
        player.prepareAsync()
    }
    override fun start() = player.start()
    override fun pause() = player.pause()
    override fun seek(positionMs: Long) = player.seekTo(positionMs, MediaPlayer.SEEK_CLOSEST)
    override fun release() = player.release()
}
