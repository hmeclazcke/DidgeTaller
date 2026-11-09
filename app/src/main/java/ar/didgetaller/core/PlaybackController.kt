package ar.didgetaller.core

import java.io.File

/** Platform effects are ports; one controller owns preparation, pause and seek state. */
interface PlaybackBackend {
    val positionMs: Int
    val durationMs: Int
    val playing: Boolean
    fun prepare(file: File, events: PlaybackEvents)
    fun start()
    fun pause()
    fun seek(positionMs: Long)
    fun release()
}

interface PlaybackEvents {
    fun prepared()
    fun seekCompleted()
    fun completed()
    fun failed(error: Throwable)
}

interface PlaybackScheduler {
    fun schedule(task: () -> Unit)
    fun cancel()
}

/** All calls/events belong to the UI owner thread; a released backend has no authority. */
class PlaybackController(
    private val createBackend: () -> PlaybackBackend,
    private val scheduler: PlaybackScheduler,
    private val changed: (String?, Boolean) -> Unit,
    private val error: (Throwable) -> Unit,
) {
    private sealed interface State {
        data object Stopped : State
        data object Disposed : State
        data class Preparing(val id: String, val target: Long, val play: Boolean) : State
        // Intent and platform legality are separate: an initial seek can precede the first start.
        data class Ready(val id: String, val paused: Boolean, val hasStarted: Boolean = false) : State
    }
    private data class Seeking(val sent: Long, val latest: Long = sent)
    private var state: State = State.Stopped
    private var backend: PlaybackBackend? = null
    private var seeking: Seeking? = null
    var loop: LongRange? = null; private set
    val id get() = (state as? State.Ready)?.id
    val ready get() = state is State.Ready
    val paused get() = (state as? State.Ready)?.paused ?: true

    fun open(sessionId: String, file: File, play: Boolean, position: Long = 0) {
        if (state == State.Disposed) return
        stop()
        if (state == State.Disposed) return
        perform {
            val candidate = createBackend()
            backend = candidate
            state = State.Preparing(sessionId, bounded(position), play)
            candidate.prepare(file, events(candidate))
        }
    }

    private fun events(candidate: PlaybackBackend) = object : PlaybackEvents {
        override fun prepared() = current(candidate) { prepared(candidate) }
        override fun seekCompleted() = current(candidate) { seekCompleted(candidate) }
        override fun completed() = current(candidate) { completed(candidate) }
        override fun failed(error: Throwable) { if (backend === candidate) fail(error) }
    }

    private fun current(candidate: PlaybackBackend, action: () -> Unit) {
        if (backend === candidate) perform(action)
    }

    private fun prepared(candidate: PlaybackBackend) {
        val pending = state as? State.Preparing ?: return
        state = State.Ready(pending.id, !pending.play)
        if (pending.target > 0) requestSeek(bounded(pending.target, candidate.durationMs))
        else if (pending.play) start(candidate)
        changed(id, paused)
        if (backend === candidate) scheduler.schedule(::poll)
    }

    private fun requestSeek(target: Long) {
        val current = backend ?: return
        val destination = bounded(target, current.durationMs)
        val pending = seeking
        if (pending != null) { seeking = pending.copy(latest = destination); return }
        seeking = Seeking(destination)
        current.seek(destination)
    }

    private fun seekCompleted(candidate: PlaybackBackend) {
        val pending = seeking ?: return
        seeking = null
        if (pending.latest != pending.sent) requestSeek(bounded(pending.latest, candidate.durationMs))
        else resumeAfterSeek(candidate)
    }

    private fun resumeAfterSeek(candidate: PlaybackBackend) {
        if (paused || candidate.playing) return
        if (candidate.positionMs >= candidate.durationMs) completed(candidate) else start(candidate)
    }

    private fun start(candidate: PlaybackBackend) {
        val current = state as? State.Ready ?: return
        state = current.copy(hasStarted = true)
        candidate.start()
    }

    private fun completed(candidate: PlaybackBackend) {
        val current = state as? State.Ready ?: return
        // A completion from before a pause/seek has no authority over the newer transport intent.
        if (current.paused || seeking != null || candidate.playing) return
        val region = loop
        if (region != null) requestSeek(region.first)
        else { state = current.copy(paused = true); changed(id, true) }
    }

    private fun poll() {
        val candidate = backend ?: return
        if (!ready) return
        current(candidate) {
            val region = loop
            if (seeking == null && region != null && !paused && candidate.playing && candidate.positionMs >= region.last)
                requestSeek(region.first)
            if (backend === candidate) scheduler.schedule(::poll)
        }
    }

    fun toggle(sessionId: String, file: File) {
        val pending = state as? State.Preparing
        if (pending?.id == sessionId) { state = pending.copy(play = !pending.play); return }
        val candidate = backend
        if (id != sessionId || !ready || candidate == null) { open(sessionId, file, true); return }
        current(candidate) {
            val previous = state as State.Ready
            state = previous.copy(paused = !previous.paused)
            if (previous.paused) resume(candidate) else if (previous.hasStarted) candidate.pause()
            changed(id, paused)
        }
    }

    private fun resume(candidate: PlaybackBackend) {
        if (seeking != null) return
        if (candidate.positionMs >= candidate.durationMs - 20) requestSeek(loop?.first ?: 0L)
        else start(candidate)
    }

    fun seek(sessionId: String, file: File, position: Long) {
        val pending = state as? State.Preparing
        val region = loop.takeIf { id == sessionId || pending?.id == sessionId }
        val target = bounded(region?.let { position.coerceIn(it.first, it.last) } ?: position)
        if (pending?.id == sessionId) { state = pending.copy(target = target); return }
        if (id != sessionId || !ready) { open(sessionId, file, false, target); return }
        perform { requestSeek(target) }
    }

    fun loop(region: LongRange?) {
        if (state == State.Disposed) return
        require(region == null || (region.first >= 0 && region.last > region.first))
        loop = region
        if (region != null && ready && position().toLong() !in region) perform { requestSeek(region.first) }
    }

    fun position(): Int {
        seeking?.let { return it.latest.toInt() }
        (state as? State.Preparing)?.let { return it.target.toInt() }
        return valueOrStop { if (ready) backend!!.positionMs else 0 }
    }

    fun duration(): Int = valueOrStop { if (ready) backend!!.durationMs else 0 }

    fun restart(sessionId: String, file: File) {
        if (id != sessionId || !ready) open(sessionId, file, true)
        else { seek(sessionId, file, 0); if (paused) toggle(sessionId, file) }
    }

    fun stop() {
        if (state != State.Disposed) retire(State.Stopped)
    }

    fun close() {
        if (state != State.Disposed) retire(State.Disposed)
    }

    private fun retire(next: State) {
        scheduler.cancel()
        val old = backend
        backend = null
        state = next
        seeking = null
        loop = null
        // Release failures are observable; clear ownership before cleanup to avoid recursion.
        try { old?.release() } catch (failure: Exception) { error(failure) }
        changed(null, false)
    }

    private fun fail(failure: Throwable) { stop(); error(failure) }
    private fun perform(action: () -> Unit) { try { action() } catch (failure: Exception) { fail(failure) } }
    private fun valueOrStop(value: () -> Int): Int = try { value() } catch (failure: Exception) { fail(failure); 0 }
    private fun bounded(position: Long, duration: Int = Int.MAX_VALUE) = position.coerceIn(0, duration.coerceAtLeast(0).toLong())
}
