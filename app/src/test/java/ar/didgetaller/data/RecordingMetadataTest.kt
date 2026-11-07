package ar.didgetaller.data

import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.WavCodec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecordingMetadataTest {
    @get:Rule val files = TemporaryFolder()
    private fun audio() = files.newFile("12345678-1234-1234-1234-123456789abc.wav").apply {
        writeBytes(byteArrayOf(1, 2, 3))
        check(setLastModified(123456L))
    }
    private fun summary() = JSONObject("""{"typicalHz":100,"lowHz":90,"highHz":110,"stabilityPercent":80,"validPoints":3,"totalPoints":5}""")

    @Test fun historicalFormatKeepsIdentityNamesTimestampsAndMissingTraceSemantics() {
        val file = audio()
        val json = JSONObject("""{"name":"Synthetic historical session","createdAt":123,"durationMs":456,"unknown":{"preserve":true}}""")
        val original = json.toString()
        val session = RecordingMetadata.decode(json, file)
        assertEquals(file.nameWithoutExtension, session.id)
        assertEquals("Synthetic historical session", session.name)
        assertEquals(123L, session.createdAt)
        assertEquals(456L, session.durationMs)
        assertEquals(3L, session.sizeBytes)
        assertTrue(session.legacy)
        assertFalse(session.recovered)
        assertTrue(session.hasStoredName)
        assertEquals(InstrumentProfile.DIDGERIDOO, session.instrument)
        assertTrue(session.timeline.isEmpty())
        assertNull(session.summary)
        assertEquals(original, json.toString())
    }

    @Test fun currentFormatRoundTripsEveryStoredFieldAndRetainsRealGaps() {
        val file = audio()
        val points = listOf(SessionAnalysis.Point(0, 440.0, 1.0), SessionAnalysis.Point(100, null, 0.0))
        val session = RecordingStore.Session(file.nameWithoutExtension, "Demo", 123, 456, 3,
            SessionAnalysis.Summary(440.0, 439.0, 441.0, 100, 3, 4), points, false,
            instrument = InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        val encoded = RecordingMetadata.encode(session)
        assertEquals(setOf("id", "name", "createdAt", "durationMs", "instrument", "timeline", "summary"), encoded.keys().asSequence().toSet())
        assertEquals("NATIVE_AMERICAN_FLUTE", encoded.getString("instrument"))
        assertEquals(file.nameWithoutExtension, encoded.getString("id"))
        assertTrue(encoded.getJSONArray("timeline").getJSONArray(1).isNull(1))
        assertEquals(session, RecordingMetadata.decode(encoded, file))
    }

    @Test fun unknownProfileAndAbsentNameKeepLegacyProvenanceWithoutRewriting() {
        val file = audio()
        val json = JSONObject().put("instrument", "future-profile").put("durationMs", -10).put("createdAt", -20)
            .put("recovered", true).put("timeline", JSONArray())
        val session = RecordingMetadata.decode(json, file)
        assertFalse(session.hasStoredName)
        assertFalse(session.legacy)
        assertTrue(session.recovered)
        assertEquals(0L, session.createdAt)
        assertEquals(0L, session.durationMs)
        assertEquals(InstrumentProfile.DIDGERIDOO, session.instrument)
        assertEquals("future-profile", json.getString("instrument"))
    }

    @Test fun validTimestampWithInvalidFrequencyOrConfidenceIsAnExplicitGap() {
        val file = audio()
        val bad = listOf("[1,0,0.9]", "[2,-1,0.9]", "[3,\"NaN\",0.9]", "[4,100,\"bad\"]",
            "[5,100,-0.1]", "[6,100,1.1]", "[7,null,1.0]")
        for (raw in bad) {
            val json = JSONObject().put("timeline", JSONArray().put(JSONArray(raw)))
            val point = RecordingMetadata.decode(json, file).timeline.single()
            assertNull(raw, point.hz)
            assertTrue(point.confidence in 0.0..1.0)
        }
        val values = JSONArray("""[null,{},[],[0],[0,100],[-1,100,1],["bad",100,1],[0,100,0],[1,100,1]]""")
        val points = RecordingMetadata.decode(JSONObject().put("timeline", values), file).timeline
        assertEquals(listOf(SessionAnalysis.Point(0, 100.0, 0.0), SessionAnalysis.Point(1, 100.0, 1.0)), points)
    }

    @Test fun invalidSummaryNeverDiscardsOtherSessionMetadata() {
        val file = audio()
        val invalid = listOf("lowHz" to 0, "lowHz" to -1, "lowHz" to "NaN", "highHz" to 80,
            "highHz" to "Infinity", "typicalHz" to 111, "typicalHz" to 89, "typicalHz" to "NaN",
            "stabilityPercent" to -1, "stabilityPercent" to 101, "validPoints" to -1,
            "totalPoints" to 2, "totalPoints" to -1)
        for ((key, value) in invalid) {
            val json = JSONObject().put("name", "Preserved").put("summary", summary().put(key, value))
            val session = RecordingMetadata.decode(json, file)
            assertNull("$key=$value", session.summary)
            assertEquals("Preserved", session.name)
        }
        for (missing in summary().keys().asSequence().toList()) {
            val value = summary().also { it.remove(missing) }
            assertNull(missing, RecordingMetadata.decode(JSONObject().put("summary", value), file).summary)
        }
    }

    @Test fun summaryBoundaryValuesRemainValid() {
        val file = audio()
        for (stability in listOf(0, 100)) {
            for (hz in listOf(90.0, 110.0)) {
                val value = summary().put("stabilityPercent", stability).put("typicalHz", hz)
                    .put("validPoints", 0).put("totalPoints", 0)
                val parsed = RecordingMetadata.decode(JSONObject().put("summary", value), file).summary!!
                assertEquals(hz, parsed.typicalHz, 0.0)
                assertEquals(stability, parsed.stabilityPercent)
                assertEquals(0, parsed.validPoints)
                assertEquals(0, parsed.totalPoints)
            }
        }
        val same = summary().put("lowHz", 100.0).put("highHz", 100.0).put("typicalHz", 100.0)
        assertNotNull(RecordingMetadata.decode(JSONObject().put("summary", same), file).summary)
        val maximum = summary().put("lowHz", Double.MAX_VALUE).put("highHz", Double.MAX_VALUE).put("typicalHz", Double.MAX_VALUE)
        assertNotNull(RecordingMetadata.decode(JSONObject().put("summary", maximum), file).summary)
    }

    @Test fun invalidNewPointsAreRejectedBeforePublication() {
        val file = audio()
        val base = RecordingStore.Session(file.nameWithoutExtension, "Demo", 0, 0, 3, null, emptyList(), false)
        val invalid = listOf(SessionAnalysis.Point(-1, 100.0, 1.0), SessionAnalysis.Point(0, 0.0, 1.0),
            SessionAnalysis.Point(0, -1.0, 1.0), SessionAnalysis.Point(0, Double.NaN, 1.0),
            SessionAnalysis.Point(0, Double.POSITIVE_INFINITY, 1.0), SessionAnalysis.Point(0, null, Double.NaN),
            SessionAnalysis.Point(0, null, -0.1), SessionAnalysis.Point(0, null, 1.1))
        invalid.forEach { point -> assertThrows(IllegalArgumentException::class.java) {
            RecordingMetadata.encode(base.copy(timeline = listOf(point)))
        } }
        assertFalse(RecordingMetadata.encode(base).has("summary"))
    }

    @Test fun orphanRecoveryIsReadOnlyAndRequiresCanonicalOwnedAudioName() {
        val file = audio()
        file.writeBytes(WavCodec.encode(ShortArray(8000) { 123 }, 8000, 8000))
        val before = file.readBytes()
        val recovered = RecordingMetadata.recovered(file, emptySet())!!
        assertEquals(file.nameWithoutExtension, recovered.id)
        assertEquals(1000L, recovered.durationMs)
        assertEquals(file.lastModified(), recovered.createdAt)
        assertTrue(recovered.recovered)
        assertTrue(recovered.legacy)
        assertFalse(recovered.hasStoredName)
        assertTrue(recovered.timeline.isEmpty())
        assertNull(recovered.summary)
        assertArrayEquals(before, file.readBytes())
        assertNull(RecordingMetadata.recovered(file, setOf(recovered.id)))
        assertNull(RecordingMetadata.recovered(files.root, emptySet()))
        for (name in listOf("invalid.wav", "ABCDEF01-1234-1234-1234-123456789abc.wav", "demo.part")) {
            val candidate = files.newFile(name).apply { writeBytes(before) }
            assertNull(name, RecordingMetadata.recovered(candidate, emptySet()))
        }
        file.writeBytes(ByteArray(44))
        assertNull(RecordingMetadata.recovered(file, emptySet()))
        file.writeBytes(ByteArray(45))
        assertEquals(0L, RecordingMetadata.recovered(file, emptySet())!!.durationMs)
    }
}
