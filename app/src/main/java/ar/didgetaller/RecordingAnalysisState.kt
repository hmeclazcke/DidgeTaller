package ar.didgetaller

import ar.didgetaller.core.RecordingAnalysisStatus

/** Read-only legacy presentation API; all facts derive from the single analysis owner. */
data class RecordingAnalysisState(private val owned: RecordingAnalysisStatus = RecordingAnalysisStatus.Idle) {
    val sessionId = owned.request?.sessionId
    val running = owned.running
    val progress = owned.progress
    val result = owned.result
    val error = owned.failure?.let { it.message ?: "Error al leer el WAV." }
        ?: if (owned.cancelled) "Análisis cancelado." else null
    val startMs = owned.request?.startMs ?: 0
    val endMs = owned.request?.endMs
}
