package ar.didgetaller.data

import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.Pcm16WavReader
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** The legacy wire format is parsed once at the persistence boundary, without rewriting it. */
internal object RecordingMetadata {
    fun recovered(candidate: File, indexed: Set<String>): RecordingStore.Session? {
        if (!candidate.isFile || candidate.extension != "wav" || candidate.length() <= 44) return null
        val id = candidate.nameWithoutExtension
        if (id in indexed || !runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) return null
        val duration = runCatching { Pcm16WavReader(candidate).use { it.frameCount * 1000 / it.sampleRate } }.getOrDefault(0L)
        return RecordingStore.Session(id, "Audio recuperado", candidate.lastModified(), duration, candidate.length(),
            null, emptyList(), true, true, hasStoredName = false)
    }

    fun decode(item: JSONObject, file: File): RecordingStore.Session {
        val summary = item.optJSONObject("summary")?.let(::summary)
        val timeline = item.optJSONArray("timeline")
        val points = if (timeline == null) emptyList() else (0 until timeline.length()).mapNotNull {
            timeline.optJSONArray(it)?.let(::point)
        }
        return RecordingStore.Session(
            file.nameWithoutExtension, item.optString("name", "Grabación"),
            item.optLong("createdAt", file.lastModified()).coerceAtLeast(0),
            item.optLong("durationMs").coerceAtLeast(0), file.length(), summary, points,
            !item.has("timeline"), item.optBoolean("recovered"),
            InstrumentProfile.fromStored(item.optString("instrument")), item.has("name"),
        )
    }

    fun encode(session: RecordingStore.Session): JSONObject {
        val timeline = JSONArray()
        session.timeline.forEach { point ->
            require(point.timeMs >= 0 && point.confidence.isFinite() && point.confidence in 0.0..1.0)
            require(point.hz == null || point.hz.isFinite() && point.hz > 0)
            timeline.put(JSONArray().put(point.timeMs).put(point.hz ?: JSONObject.NULL).put(point.confidence))
        }
        val item = JSONObject().put("id", session.id).put("name", session.name)
            .put("createdAt", session.createdAt).put("durationMs", session.durationMs)
            .put("instrument", session.instrument.name).put("timeline", timeline)
        session.summary?.let { value ->
            item.put("summary", JSONObject().put("typicalHz", value.typicalHz).put("lowHz", value.lowHz)
                .put("highHz", value.highHz).put("stabilityPercent", value.stabilityPercent)
                .put("validPoints", value.validPoints).put("totalPoints", value.totalPoints))
        }
        return item
    }

    private fun point(value: JSONArray): SessionAnalysis.Point? {
        if (value.length() < 3) return null
        val time = value.optLong(0, -1)
        if (time < 0) return null
        val confidence = value.optDouble(2).takeIf { it.isFinite() && it in 0.0..1.0 }
        val hz = value.optDouble(1).takeIf { it.isFinite() && it > 0 && confidence != null }
        return SessionAnalysis.Point(time, hz, confidence ?: 0.0)
    }

    private fun summary(value: JSONObject): SessionAnalysis.Summary? {
        val low = value.optDouble("lowHz")
        val high = value.optDouble("highHz")
        val typical = value.optDouble("typicalHz")
        val stability = value.optInt("stabilityPercent", -1)
        val valid = value.optInt("validPoints", -1)
        val total = value.optInt("totalPoints", -1)
        if (!low.isFinite() || !high.isFinite() || low <= 0 || high < low) return null
        if (typical !in low..high || stability !in 0..100 || valid < 0 || total < valid) return null
        return SessionAnalysis.Summary(typical, low, high, stability, valid, total)
    }
}
