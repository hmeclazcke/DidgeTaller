package ar.didgetaller.data

import android.content.Context
import ar.didgetaller.core.MetronomeSettings

/** A separate preference file never writes a recording, session index or historical setting. */
class MetronomePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("practice_metronome", Context.MODE_PRIVATE)
    fun load(): MetronomeSettings = runCatching {
        MetronomeSettings.restore(prefs.getInt("bpm", 90), prefs.getInt("beats", 4), prefs.getString("subdivision", null),
            prefs.getBoolean("accent", true), prefs.getBoolean("audible", false),
            prefs.getFloat("volume", 0.5f))
    }.getOrDefault(MetronomeSettings())
    fun save(settings: MetronomeSettings) {
        prefs.edit().putInt("bpm", settings.bpm).putInt("beats", settings.beatsPerBar)
            .putString("subdivision", settings.subdivision.name).putBoolean("accent", settings.accentFirst)
            .putBoolean("audible", settings.audible).putFloat("volume", settings.volume).apply()
    }
}
