package ar.didgetaller.data

import org.json.JSONArray
import org.json.JSONObject

/** Invalid individual markers are omitted from display but retained in the source document. */
internal object SessionToolsMetadata {
    fun marker(value: JSONObject): SessionToolsStore.Marker? {
        val time = value.optLong("timeMs", -1)
        val id = value.optString("id")
        val type = value.optString("type")
        if (time < 0 || id.isBlank() || type.isBlank()) return null
        return SessionToolsStore.Marker(id, time, type, value.optString("label"))
    }

    fun entry(root: JSONObject, id: String): JSONObject {
        check(!root.has(id) || root.optJSONObject(id) != null) { "Invalid stored session tools" }
        return root.optJSONObject(id) ?: JSONObject().also { root.put(id, it) }
    }

    fun markersForUpdate(entry: JSONObject): JSONArray {
        check(!entry.has("markers") || entry.optJSONArray("markers") != null) { "Invalid stored marker list" }
        return entry.optJSONArray("markers") ?: JSONArray().also { entry.put("markers", it) }
    }
}
