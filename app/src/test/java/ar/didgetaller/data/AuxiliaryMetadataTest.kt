package ar.didgetaller.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AuxiliaryMetadataTest {
    @Test fun legacyMeasurementNumbersKeepStoredMusicalLabelsAndStableFallbackIdentifiers() {
        for (hz in listOf<Any>(66.9, "66.9")) {
            val value = JSONObject().put("hz", hz).put("id", "original").put("time", "historical time")
                .put("note", "Stored note").put("cents", -8)
            val before = value.toString()
            val measured = MeasurementMetadata.decode(value, 5)!!
            assertEquals(LocalStore.Measurement("original", "historical time", 66.9, "Stored note", -8), measured)
            assertEquals(before, value.toString())
        }
        val fallback = MeasurementMetadata.decode(JSONObject().put("hz", 440), 5)!!
        assertEquals("legacy-5", fallback.id)
        assertEquals(ar.didgetaller.core.Music.note(440.0), fallback.note)
        assertEquals(0, fallback.cents)
    }

    @Test fun nonfiniteMissingAndNonpositiveMeasurementsNeverEnterTheDisplayDomain() {
        for (hz in listOf<Any>("NaN", "Infinity", "-Infinity", "unknown", 0, -1)) {
            assertNull(hz.toString(), MeasurementMetadata.decode(JSONObject().put("hz", hz), 0))
        }
        assertNull(MeasurementMetadata.decode(JSONObject(), 0))
    }

    @Test fun malformedMarkersDoNotHideOtherValidMarkersOrLoseTheirSource() {
        val value = JSONObject().put("id", "original").put("timeMs", 0).put("type", "manual").put("label", "Label")
        assertEquals(SessionToolsStore.Marker("original", 0, "manual", "Label"), SessionToolsMetadata.marker(value))
        for ((key, invalid) in listOf("id" to "", "type" to " ", "timeMs" to -1, "timeMs" to "unknown")) {
            val corrupt = JSONObject(value.toString()).put(key, invalid)
            val before = corrupt.toString()
            assertNull(SessionToolsMetadata.marker(corrupt))
            assertEquals(before, corrupt.toString())
        }
        for (key in listOf("id", "type", "timeMs")) {
            assertNull(SessionToolsMetadata.marker(JSONObject(value.toString()).also { it.remove(key) }))
        }
        assertEquals("", SessionToolsMetadata.marker(JSONObject(value.toString()).also { it.remove("label") })!!.label)
    }

    @Test fun metadataUpdateKeepsExistingKnowledgeAndRejectsUnsupportedContainers() {
        val root = JSONObject()
        val value = SessionToolsMetadata.entry(root, "session")
        value.put("future", true)
        assertSame(value, SessionToolsMetadata.entry(root, "session"))
        val markers = SessionToolsMetadata.markersForUpdate(value)
        markers.put("future marker")
        assertSame(markers, SessionToolsMetadata.markersForUpdate(value))
        assertEquals("future marker", root.getJSONObject("session").getJSONArray("markers").getString(0))
        assertTrue(root.getJSONObject("session").getBoolean("future"))
        for (invalid in listOf<Any>("unsupported", JSONObject.NULL)) {
            root.put("other", invalid)
            val before = root.toString()
            assertThrows(IllegalStateException::class.java) { SessionToolsMetadata.entry(root, "other") }
            assertEquals(before, root.toString())
            value.put("markers", invalid)
            assertThrows(IllegalStateException::class.java) { SessionToolsMetadata.markersForUpdate(value) }
        }
        root.put("other", JSONArray())
        assertThrows(IllegalStateException::class.java) { SessionToolsMetadata.entry(root, "other") }
    }
}
