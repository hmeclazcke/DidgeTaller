package ar.didgetaller.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

/** The JSON codec runs on the JVM; real preference/disk integration is covered on Android. */
class JsonPreferenceDocumentTest {
    private class Preferences {
        val values = mutableMapOf<String, String>()
        val prefs = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> values[args!![0]] ?: args[1]
                "edit" -> editor()
                else -> error("Unexpected preference operation: ${method.name}")
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, String?>()
            return Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)) { editor, method, args ->
                when (method.name) {
                    "putString" -> { pending[args!![0] as String] = args[1] as String?; editor }
                    "commit" -> {
                        pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                        true
                    }
                    else -> error("Unexpected editor operation: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
    }

    @Test fun missingDocumentsUseTheirOwnRootTypesAndPublishOneCompleteJsonValue() {
        val storage = Preferences()
        val array = JsonPreferenceDocument.array(storage.prefs, "recordings")
        val objectValue = JsonPreferenceDocument.objectValue(storage.prefs, "extras")
        assertEquals(0, array.read().length())
        assertEquals(0, objectValue.read().length())
        assertFalse(array.damaged)
        array.requireWritable()
        assertEquals(7, array.update { it.put(JSONObject().put("id", "historical")); 7 })
        objectValue.update { it.put("unknown", JSONArray().put("preserved")) }
        assertEquals("historical", JsonPreferenceDocument.array(storage.prefs, "recordings").read().getJSONObject(0).getString("id"))
        assertEquals("preserved", objectValue.read().getJSONArray("unknown").getString(0))
        assertEquals(storage.values["recordings"], storage.values["recordings.lastGood"])
    }

    @Test fun invalidArrayRootsAndTrailingContentCannotReplaceHistoricalMetadata() {
        val storage = Preferences()
        val document = JsonPreferenceDocument.array(storage.prefs, "recordings")
        document.update { it.put("historical") }
        for (bad in listOf("", " ", "broken", "[", "{\"version\":999}", "123", "[] trailing")) {
            storage.values["recordings"] = bad
            assertTrue(bad, document.damaged)
            assertEquals("historical", document.read().getString(0))
            assertEquals(bad, storage.values["recordings"])
            assertThrows(StoredDocument.Unavailable::class.java) { document.requireWritable() }
            assertThrows(StoredDocument.Unavailable::class.java) { document.update { it.put("replacement") } }
            assertTrue(document.recover())
            assertFalse(document.damaged)
            assertEquals("historical", document.read().getString(0))
        }
    }

    @Test fun invalidObjectRootDoesNotBecomeWritableEmptyExtras() {
        val storage = Preferences()
        storage.values["extras"] = "[]"
        val document = JsonPreferenceDocument.objectValue(storage.prefs, "extras")
        assertTrue(document.damaged)
        assertEquals(0, document.read().length())
        assertThrows(StoredDocument.Unavailable::class.java) { document.update { it.put("new", true) } }
        assertThrows(StoredDocument.Unavailable::class.java) { document.recover() }
        assertEquals("[]", storage.values["extras"])
    }
}
