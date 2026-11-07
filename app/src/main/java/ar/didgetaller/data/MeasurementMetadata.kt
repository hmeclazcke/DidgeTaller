package ar.didgetaller.data

import ar.didgetaller.core.Music
import org.json.JSONObject

/** Legacy numbers may be JSON strings. Nonfinite values never enter the musical display domain. */
internal object MeasurementMetadata {
    fun decode(value: JSONObject, index: Int): LocalStore.Measurement? {
        val hz = value.optString("hz").toDoubleOrNull() ?: value.optDouble("hz", 0.0)
        if (!hz.isFinite() || hz <= 0) return null
        return LocalStore.Measurement(value.optString("id", "legacy-$index"), value.optString("time"),
            hz, value.optString("note", Music.note(hz)), value.optInt("cents", Music.cents(hz)))
    }
}
