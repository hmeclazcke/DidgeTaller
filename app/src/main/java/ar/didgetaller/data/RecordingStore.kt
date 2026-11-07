package ar.didgetaller.data

import android.content.Context
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.Pcm16WavReader
import ar.didgetaller.core.ProgressiveWavWriter
import ar.didgetaller.core.SessionAnalysis
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Owns WAV/index ordering. All instances share the preference file's process-local writer lock. */
class RecordingStore(context: Context) {
    data class Session(
        val id: String, val name: String, val createdAt: Long, val durationMs: Long, val sizeBytes: Long,
        val summary: SessionAnalysis.Summary?, val timeline: List<SessionAnalysis.Point>,
        val legacy: Boolean, val recovered: Boolean = false,
        val instrument: InstrumentProfile = InstrumentProfile.DIDGERIDOO,
        // In-memory provenance only; never serialized or used by WAV recovery.
        val hasStoredName: Boolean = true,
    )

    private val directory = File(context.filesDir, "recordings").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("local_data", Context.MODE_PRIVATE)
    private val index = JsonPreferenceDocument.array(prefs, "recordings")
    private val trash = TrashStore(context)
    val indexDamaged: Boolean get() = index.damaged

    /** Requires a validated backup; quarantines damaged metadata and never deletes audio. */
    fun recoverIndex(): Boolean = synchronized(prefs) { index.recover() }

    fun list(includeTrash: Boolean = false): List<Session> = synchronized(prefs) {
        val entries = index.read()
        val indexed = (0 until entries.length()).mapNotNull { position ->
            entries.optJSONObject(position)?.let { item ->
                val file = runCatching { file(item.optString("id")) }.getOrNull()
                file?.takeIf { it.isFile }?.let { RecordingMetadata.decode(item, it) }
            }
        }.distinctBy { it.id }
        val known = indexed.mapTo(mutableSetOf()) { it.id }
        val recovered = directory.listFiles().orEmpty().mapNotNull { RecordingMetadata.recovered(it, known) }
        val hidden = if (includeTrash) emptySet() else trash.ids("session")
        (indexed + recovered).filter { it.id !in hidden }.sortedByDescending { it.createdAt }
    }

    fun newCapture(): File = File(directory, "${UUID.randomUUID()}.part")

    /** Startup-only recovery. Never call while an AudioRecord worker is capturing into this directory. */
    fun recoverPending(): Int = synchronized(prefs) {
        directory.listFiles().orEmpty().count { partial ->
            if (partial.extension != "part") false else runCatching {
                val destination = file(partial.nameWithoutExtension)
                !destination.exists() && ProgressiveWavWriter.repair(partial) && partial.renameTo(destination)
            }.getOrDefault(false)
        }
    }

    fun importCapture(partial: File, points: List<SessionAnalysis.Point>, createdAt: Long,
                      instrument: InstrumentProfile = InstrumentProfile.DIDGERIDOO): Session = synchronized(prefs) {
        index.requireWritable()
        require(partial.parentFile?.canonicalFile == directory.canonicalFile && partial.extension == "part")
        val id = partial.nameWithoutExtension
        val destination = file(id)
        check(!destination.exists()) { "No se pudo conservar la captura." }
        require(ProgressiveWavWriter.repair(partial)) { "Captura parcial sin muestras completas." }
        val duration = Pcm16WavReader(partial).use { it.frameCount * 1000 / it.sampleRate }
        val session = captured(id, duration, partial.length(), points, createdAt, instrument)
        val metadata = RecordingMetadata.encode(session)
        check(partial.renameTo(destination)) { "No se pudo conservar la captura." }
        publish(session, metadata)
    }

    fun save(bytes: ByteArray, points: List<SessionAnalysis.Point> = emptyList(),
             createdAt: Long = System.currentTimeMillis(), instrument: InstrumentProfile = InstrumentProfile.DIDGERIDOO): Session = synchronized(prefs) {
        index.requireWritable()
        require(bytes.size > 44 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF")
        val id = UUID.randomUUID().toString()
        val temporary = File(directory, "$id.tmp")
        try {
            FileOutputStream(temporary).use { it.write(bytes); it.fd.sync() }
            val session = captured(id, duration(temporary), temporary.length(), points, createdAt, instrument)
            val metadata = RecordingMetadata.encode(session)
            check(temporary.renameTo(file(id))) { "No se pudo conservar el WAV" }
            publish(session, metadata)
        } finally { temporary.delete() }
    }

    private fun captured(id: String, duration: Long, size: Long, points: List<SessionAnalysis.Point>,
                         createdAt: Long, instrument: InstrumentProfile): Session {
        val name = "Sesión ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(createdAt))}"
        return Session(id, name, createdAt, duration, size,
            SessionAnalysis.summarize(points, instrument.validMinHz, instrument.validMaxHz), points, false,
            instrument = instrument)
    }

    private fun publish(session: Session, metadata: JSONObject): Session {
        // A failed metadata publication leaves the complete WAV recoverable; never roll audio back by deleting it.
        index.update { it.put(metadata) }
        return session
    }

    fun rename(id: String, name: String) = synchronized(prefs) {
        val destination = file(id)
        require(name.isNotBlank())
        index.update { entries ->
            var found = false
            for (i in 0 until entries.length()) {
                entries.optJSONObject(i)?.takeIf { it.optString("id") == id }?.let {
                    it.put("name", name.trim().take(80))
                    found = true
                }
            }
            if (!found) {
                check(destination.isFile) { "No se encontró el WAV" }
                entries.put(JSONObject().put("id", id).put("name", name.trim().take(80))
                    .put("createdAt", destination.lastModified()).put("durationMs", duration(destination)).put("recovered", true))
            }
        }
    }

    fun delete(id: String) = synchronized(prefs) {
        val destination = file(id) // Validate before changing any metadata.
        index.update { entries ->
            // Remove only matching rows; unknown fields and malformed unrelated records remain JSON values.
            for (i in entries.length() - 1 downTo 0) {
                if (entries.optJSONObject(i)?.optString("id") == id) entries.remove(i)
            }
        }
        check(!destination.exists() || destination.delete()) { "No se pudo eliminar el audio; permanece en la papelera." }
    }

    fun file(id: String): File {
        require(UUID.fromString(id).toString() == id) { "Identificador inválido" }
        return File(directory, "$id.wav")
    }

    private fun duration(file: File) = runCatching {
        Pcm16WavReader(file).use { it.frameCount * 1000 / it.sampleRate }
    }.getOrDefault(0L)
}
