package ar.didgetaller.core

/** Android supplies the lease; the domain owns when it may be requested and revoked. */
fun interface MetronomeFocus {
    fun request(changed: (Int) -> Unit): MetronomeTask?
}

class FocusedMetronomeClick(private val output: MetronomeClick, private val focus: MetronomeFocus,
    private val focusLost: () -> Unit) : MetronomeClick {
    private var lease: MetronomeTask? = null
    @Volatile private var generation = 0L
    @Volatile private var closed = false
    override fun play(accent: Boolean, volume: Float): Boolean {
        if (closed) return false
        if (lease == null) lease = acquire()
        if (lease == null) return false
        return output.play(accent, volume)
    }
    private fun acquire(): MetronomeTask? {
        val owner = generation
        return focus.request { change -> changed(owner, change) }
    }
    private fun changed(owner: Long, change: Int) {
        if (owner != generation || closed || change >= 0) return
        focusLost()
    }
    override fun silence() {
        generation++
        val previous = lease
        lease = null
        try { output.silence() } finally { previous?.cancel() }
    }
    override fun close() {
        if (closed) return
        closed = true; silence(); output.close()
    }
}

/** Foreground display ownership is a pure practice policy, independent of Window effects. */
fun practiceKeepsScreenAwake(recording: Boolean, playing: Boolean, paused: Boolean, rhythm: MetronomeMode): Boolean =
    recording || (playing && !paused) || rhythm == MetronomeMode.RUNNING
