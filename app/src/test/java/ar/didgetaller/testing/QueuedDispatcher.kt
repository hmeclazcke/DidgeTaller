package ar.didgetaller.testing

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/** Explicit owner/worker turns, without timing assumptions or background threads. */
internal class QueuedDispatcher : CoroutineDispatcher() {
    private val pending = ArrayDeque<Runnable>()
    override fun dispatch(context: CoroutineContext, block: Runnable) { pending += block }
    fun drain() { while (pending.isNotEmpty()) pending.removeFirst().run() }
}
