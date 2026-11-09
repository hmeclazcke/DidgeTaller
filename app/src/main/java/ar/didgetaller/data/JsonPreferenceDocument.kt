package ar.didgetaller.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

/** Existing JSON formats remain unchanged. Backup and quarantine keys are additive. */
internal class JsonPreferenceDocument<T : Any> private constructor(
    prefs: SharedPreferences,
    key: String,
    private val decode: (String?) -> T,
) {
    private val document = StoredDocument(object : StoredDocument.Storage {
        // Android caches one SharedPreferences instance per file in this process. Every store uses it.
        override val lock: Any = prefs
        override fun read(key: String): String? = prefs.getString(key, null)
        override fun commit(values: Map<String, String?>): Boolean {
            val editor = prefs.edit()
            values.forEach { (key, value) -> editor.putString(key, value) }
            return editor.commit()
        }
    }, key) { decode(it) }

    val damaged: Boolean get() = document.snapshot().health == StoredDocument.Health.DAMAGED
    fun read(): T = decode(document.displayText())
    fun requireWritable() { if (damaged) throw StoredDocument.Unavailable() }
    fun recover(): Boolean = document.recover()
    fun <R> update(change: (T) -> R): R = document.update { text ->
        val value = decode(text)
        val result = change(value)
        value.toString() to result
    }

    companion object {
        fun array(prefs: SharedPreferences, key: String) = JsonPreferenceDocument(prefs, key) {
            parse(it ?: "[]") as? JSONArray ?: throw IllegalArgumentException("Expected a JSON array")
        }
        fun objectValue(prefs: SharedPreferences, key: String) = JsonPreferenceDocument(prefs, key) {
            parse(it ?: "{}") as? JSONObject ?: throw IllegalArgumentException("Expected a JSON object")
        }
        private fun parse(text: String): Any {
            try {
                val tokens = JSONTokener(text)
                val value = tokens.nextValue()
                require(tokens.nextClean() == '\u0000') { "Trailing JSON content" }
                return value
            } catch (failure: JSONException) {
                throw IllegalArgumentException("Unreadable JSON document", failure)
            }
        }
    }
}
