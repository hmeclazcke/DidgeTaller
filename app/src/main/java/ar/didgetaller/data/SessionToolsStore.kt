package ar.didgetaller.data

import android.content.Context
import ar.didgetaller.core.InstrumentProfile
import org.json.JSONObject
import java.util.UUID

/** Additive metadata only; every read/modify/write shares the document owner's lock. */
class SessionToolsStore(context: Context) {
    private val prefs = context.getSharedPreferences("session_tools", 0)
    private val extras = JsonPreferenceDocument.objectValue(prefs, "extras")
    data class Marker(val id: String, val timeMs: Long, val type: String, val label: String)

    fun designId(id: String): String? = extras.read().optJSONObject(id)?.optString("designId")?.takeIf { it.isNotBlank() }

    fun associate(id: String, designId: String?) {
        extras.update { root ->
            val value = SessionToolsMetadata.entry(root, id)
            if (designId == null) value.remove("designId") else value.put("designId", designId)
        }
    }

    fun markers(id: String): List<Marker> {
        val values = extras.read().optJSONObject(id)?.optJSONArray("markers") ?: return emptyList()
        return (0 until values.length()).mapNotNull { values.optJSONObject(it)?.let(SessionToolsMetadata::marker) }.sortedBy { it.timeMs }
    }

    fun addMarker(id: String, timeMs: Long, type: String, label: String) {
        require(timeMs >= 0 && type.isNotBlank())
        extras.update { root ->
            val value = SessionToolsMetadata.entry(root, id)
            val values = SessionToolsMetadata.markersForUpdate(value)
            values.put(JSONObject().put("id", UUID.randomUUID().toString()).put("timeMs", timeMs)
                .put("type", type).put("label", label.take(120)))
            value.put("markers", values)
        }
    }

    fun deleteMarker(id: String, markerId: String) {
        extras.update { root ->
            val values = root.optJSONObject(id)?.optJSONArray("markers")
            if (values != null) for (i in values.length() - 1 downTo 0) {
                if (values.optJSONObject(i)?.optString("id") == markerId) values.remove(i)
            }
        }
    }

    fun forget(id: String) { extras.update { it.remove(id) } }
    fun forgetDesign(id: String) {
        extras.update { root ->
            root.keys().forEach { key ->
                root.optJSONObject(key)?.takeIf { it.optString("designId") == id }?.remove("designId")
            }
        }
    }

    fun instrument() = InstrumentProfile.fromStored(prefs.getString("instrument", null))
    fun setInstrument(profile: InstrumentProfile) { check(prefs.edit().putString("instrument", profile.name).commit()) }
    fun limitSeconds() = prefs.getInt("limitSeconds", 900).coerceIn(60, 3600)
    fun setLimitSeconds(seconds: Int) {
        require(seconds in 60..3600)
        check(prefs.edit().putInt("limitSeconds", seconds).commit())
    }
}
