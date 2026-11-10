package ar.didgetaller.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class ConstructionStoreTest {
    @Test fun versionedMetadataRoundTripsWithoutRewritingLegacyKeys() {
        val memory=Preferences()
        memory.values["designs"]="historical"
        val store=ConstructionStore(memory.prefs)
        assertTrue(store.project("a").isEmpty());assertTrue(store.catalog().isEmpty())
        store.saveProject("a",mapOf("method" to "PVC","geometry" to "0,32\n1500,70"))
        store.saveProject("b",mapOf("method" to "SANDWICH"))
        assertEquals("PVC",ConstructionStore(memory.prefs).project("a")["method"])
        assertEquals("SANDWICH",store.project("b")["method"])
        store.saveCatalog("a,32,40,1500","")
        assertEquals("a,32,40,1500",ConstructionStore(memory.prefs).catalog()["pipes"])
        assertEquals("historical",memory.values["designs"])
        val before=memory.values.toMap()
        assertThrows(IllegalArgumentException::class.java){store.saveCatalog("a,32,40,1500\na,32,40,1500","")}
        assertEquals(before,memory.values)
        memory.values["projects"]="{broken"
        assertThrows(StoredDocument.Unavailable::class.java){store.project("a")}
        assertThrows(StoredDocument.Unavailable::class.java){store.saveProject("a",emptyMap())}
        assertEquals("{broken",memory.values["projects"])
        memory.values["projects"]="{\"schema\":99,\"designs\":{}}"
        assertThrows(IllegalArgumentException::class.java){store.project("a")}
        assertThrows(IllegalArgumentException::class.java){store.saveProject("a",emptyMap())}
    }
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

}
