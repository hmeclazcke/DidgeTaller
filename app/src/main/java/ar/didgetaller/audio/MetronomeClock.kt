package ar.didgetaller.audio

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import ar.didgetaller.core.MetronomeClock
import ar.didgetaller.core.MetronomeTask

/** A sleeping audio-priority queue; no polling, spin loop or callback-relative timing. */
internal class AndroidMetronomeClock : MetronomeClock {
    private var thread: HandlerThread? = null
    private var queue: Handler? = null
    override fun nowNanos(): Long = SystemClock.uptimeMillis() * 1_000_000L
    override fun at(deadlineNanos: Long, action: () -> Unit): MetronomeTask {
        val handler = queue ?: newQueue()
        val runnable = Runnable(action)
        // Round up: an early callback must not sound a pulse before its rational deadline.
        handler.postAtTime(runnable, (deadlineNanos + 999_999) / 1_000_000)
        return MetronomeTask { handler.removeCallbacks(runnable) }
    }
    private fun newQueue(): Handler {
        val worker = HandlerThread("practice-metronome", Process.THREAD_PRIORITY_AUDIO)
        worker.start()
        thread = worker
        val handler = Handler(worker.looper)
        queue = handler
        return handler
    }
    fun close() { queue?.removeCallbacksAndMessages(null); thread?.quitSafely() }
}
