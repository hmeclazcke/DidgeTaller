package ar.didgetaller.core

interface SessionAnalysisRequest { val sessionId: String }
data class YinReplayRequest(override val sessionId: String, val expandedRange: Boolean) : SessionAnalysisRequest
data class ComponentRequest(override val sessionId: String) : SessionAnalysisRequest
data class MusicalTraceRequest(override val sessionId: String, val durationMs: Long,
    val captured: List<SessionAnalysis.Point>, val instrument: InstrumentProfile) : SessionAnalysisRequest
data class EvidenceRequest(override val sessionId: String, val positionMs: Long,
    val instrument: InstrumentProfile) : SessionAnalysisRequest
data class RecordingComparisonRequest(val first: RecordingAnalysisRequest, val second: RecordingAnalysisRequest)

/** Library requests have independent grants; removing a key revokes its callbacks before cancellation. */
class SessionAnalyses<R : SessionAnalysisRequest, V : Any>(private val loader: AnalysisLoader<R, V>,
    private val changed: (AnalysisState<R, V>) -> Unit) {
    private val owners = mutableMapOf<String, LatestAnalysis<R, V>>()
    private var closed = false

    fun select(request: R) {
        if (closed) return
        owners.getOrPut(request.sessionId) { create(request.sessionId) }.select(request)
    }

    private fun create(id: String): LatestAnalysis<R, V> {
        lateinit var owner: LatestAnalysis<R, V>
        owner = LatestAnalysis(loader) { if (!closed && owners[id] === owner) changed(it) }
        return owner
    }

    fun remove(id: String) { owners.remove(id)?.close() }

    fun close() {
        if (closed) return
        closed = true
        val previous = owners.values.toList()
        owners.clear()
        previous.forEach { it.close() }
    }
}
