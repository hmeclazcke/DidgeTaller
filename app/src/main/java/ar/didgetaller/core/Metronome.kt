package ar.didgetaller.core

enum class MetronomeSubdivision(val pulses: Int) { QUARTER(1), EIGHTH(2), TRIPLET(3), SIXTEENTH(4) }
enum class MetronomeMode { STOPPED, RUNNING, PAUSED }

data class MetronomeSettings(val bpm: Int = 90, val beatsPerBar: Int = 4,
    val subdivision: MetronomeSubdivision = MetronomeSubdivision.QUARTER,
    val accentFirst: Boolean = true, val audible: Boolean = false, val volume: Float = 0.5f) {
    init {
        require(bpm in 30..240)
        require(beatsPerBar in 2..4)
        require(volume in 0f..1f)
    }
    fun pulse(ordinal: Long): MetronomePulse {
        val beat = (ordinal / subdivision.pulses % beatsPerBar).toInt() + 1
        val part = (ordinal % subdivision.pulses).toInt() + 1
        return MetronomePulse(beat, part, accentFirst && beat == 1 && part == 1)
    }
    companion object {
        fun restore(bpm: Int, beats: Int, subdivision: String?, accent: Boolean, audible: Boolean, volume: Float): MetronomeSettings =
            MetronomeSettings(bpm.coerceIn(30, 240), beats.coerceIn(2, 4),
                MetronomeSubdivision.entries.firstOrNull { it.name == subdivision } ?: MetronomeSubdivision.QUARTER,
                accent, audible, volume.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f)
    }
}
data class MetronomePulse(val beat: Int, val subdivision: Int, val accent: Boolean)
data class MetronomeState(val settings: MetronomeSettings = MetronomeSettings(),
    val mode: MetronomeMode = MetronomeMode.STOPPED, val pulse: MetronomePulse? = null,
    val captureActive: Boolean = false, val audioFailed: Boolean = false)

/** Integer rational grid. Deadlines never depend on the delivery time of the previous callback. */
class MetronomeGrid(settings: MetronomeSettings) {
    private val denominator = settings.bpm.toLong() * settings.subdivision.pulses
    fun offset(ordinal: Long): Long = ordinal / denominator * MINUTE + ordinal % denominator * MINUTE / denominator
    fun due(elapsedNanos: Long): Long = elapsedNanos / MINUTE * denominator + ((elapsedNanos % MINUTE + 1) * denominator - 1) / MINUTE
    private companion object { const val MINUTE = 60_000_000_000L }
}
fun interface MetronomeTask { fun cancel() }
interface MetronomeClock {
    fun nowNanos(): Long
    /** Never invoke inline. A cancelled callback may still arrive and must lose ownership. */
    fun at(deadlineNanos: Long, action: () -> Unit): MetronomeTask
}
interface MetronomeClick {
    fun play(accent: Boolean, volume: Float): Boolean
    /** Stop queued sound and release the output before returning, including on microphone admission. */
    fun silence()
    fun close()
}

/** One monitor serializes effects and admission: capture cannot open until click output is silent. */
class MetronomeEngine(private val clock: MetronomeClock, private val click: MetronomeClick,
    private val changed: (MetronomeState) -> Unit) {
    @Volatile var state = MetronomeState(); private set
    private var grid = MetronomeGrid(state.settings)
    private var generation = 0L
    private var ordinal = 0L
    private var anchor = 0L
    private var pending: MetronomeTask? = null
    private var closed = false

    @Synchronized fun start() {
        if (closed || state.mode == MetronomeMode.RUNNING) return
        val waiting = if (state.mode == MetronomeMode.PAUSED) grid.offset(1) else 0L
        anchor = clock.nowNanos() + waiting - grid.offset(ordinal)
        publish(state.copy(mode = MetronomeMode.RUNNING, audioFailed = false))
        schedule()
    }
    @Synchronized fun pause() {
        if (closed || state.mode != MetronomeMode.RUNNING) return
        invalidate()
        click.silence()
        publish(state.copy(mode = MetronomeMode.PAUSED))
    }
    @Synchronized fun stop() {
        if (closed) return
        invalidate(); click.silence(); ordinal = 0
        publish(state.copy(mode = MetronomeMode.STOPPED, pulse = null))
    }
    @Synchronized fun update(settings: MetronomeSettings) {
        if (closed || settings == state.settings) return
        val previous = state.settings
        val timingChanged = settings.bpm != previous.bpm || settings.subdivision != previous.subdivision || settings.beatsPerBar != previous.beatsPerBar
        grid = MetronomeGrid(settings)
        publish(state.copy(settings = settings, audioFailed = false))
        if (!settings.audible || settings.volume == 0f) click.silence()
        if (timingChanged) rephase(previous)
    }
    private fun rephase(previous: MetronomeSettings) {
        invalidate()
        if (previous.subdivision != state.settings.subdivision || previous.beatsPerBar != state.settings.beatsPerBar) {
            ordinal = 0
            publish(state.copy(pulse = null))
        }
        anchor = clock.nowNanos() + grid.offset(1) - grid.offset(ordinal)
        if (state.mode == MetronomeMode.RUNNING) schedule()
    }
    @Synchronized fun captureActive(active: Boolean) {
        if (closed || state.captureActive == active) return
        if (active) click.silence()
        publish(state.copy(captureActive = active))
    }
    @Synchronized fun close() {
        if (closed) return
        stop(); closed = true; click.close()
    }
    private fun invalidate() {
        generation++; pending?.cancel(); pending = null
    }
    private fun schedule() {
        val owner = generation
        val expected = ordinal
        pending = clock.at(anchor + grid.offset(ordinal)) { tick(owner, expected) }
    }
    @Synchronized private fun tick(owner: Long, expected: Long) {
        if (closed || generation != owner || ordinal != expected || state.mode != MetronomeMode.RUNNING) return
        val elapsed = clock.nowNanos() - anchor
        if (elapsed < grid.offset(ordinal)) { schedule(); return }
        val due = maxOf(ordinal, grid.due(elapsed))
        val pulse = state.settings.pulse(due)
        ordinal = due + 1
        val failed = sound(pulse)
        publish(state.copy(pulse = pulse, audioFailed = failed))
        schedule()
    }
    private fun sound(pulse: MetronomePulse): Boolean {
        if (!state.settings.audible || state.captureActive || state.settings.volume == 0f) return false
        if (state.audioFailed) return true
        val failed = !runCatching { click.play(pulse.accent, state.settings.volume) }.getOrDefault(false)
        if (failed) click.silence()
        return failed
    }
    private fun publish(next: MetronomeState) { state = next; changed(next) }
}
