package ar.didgetaller.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ar.didgetaller.core.FocusedMetronomeClick
import ar.didgetaller.core.MetronomeEngine
import ar.didgetaller.core.MetronomeSettings
import ar.didgetaller.core.MetronomeState
import ar.didgetaller.data.MetronomePreferences

/** Android presentation adapter; timing, focus ownership and transport decisions live in the domain. */
class PracticeMetronome(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val preferences = MetronomePreferences(context)
    private val clock = AndroidMetronomeClock()
    private val output = FocusedMetronomeClick(AndroidMetronomeClick(), AndroidMetronomeFocus(context), ::pause)
    private var closed = false
    var state by mutableStateOf(MetronomeState(settings = preferences.load())); private set
    private val engine: MetronomeEngine = MetronomeEngine(clock, output, ::publish)
    init { engine.update(state.settings) }
    private fun publish(next: MetronomeState) {
        main.post { if (engine.state === next) state = next }
    }
    fun start() = engine.start()
    fun pause() = engine.pause()
    fun stop() = engine.stop()
    fun update(settings: MetronomeSettings) {
        if (closed) return
        preferences.save(settings)
        engine.update(settings)
    }
    /** Synchronous admission before AudioRecord, not a deferred Compose effect. */
    fun captureActive(active: Boolean) = engine.captureActive(active)
    fun requestSound(enabled: Boolean) = update(engine.state.settings.copy(audible = enabled))
    fun close() {
        if (closed) return
        closed = true
        engine.close(); state = engine.state; clock.close(); main.removeCallbacksAndMessages(null)
    }
}
