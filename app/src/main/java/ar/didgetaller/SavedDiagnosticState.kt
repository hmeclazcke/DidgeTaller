package ar.didgetaller

import ar.didgetaller.core.*

data class ComparisonState(val running: Boolean = false, val progress: Int = 0,
    val a: WavAnalysis.Result? = null, val b: WavAnalysis.Result? = null, val error: String? = null)
data class SavedEvidenceState(val sessionId: String? = null, val running: Boolean = false,
    val result: SavedPitchEvidence.Snapshot? = null, val failed: Boolean = false)
data class DroneComponentState(val sessionId: String? = null, val running: Boolean = false, val progress: Int = 0,
    val curve: DroneComponentCurve? = null, val failed: Boolean = false)
data class SavedYinReplayState(val sessionId: String? = null, val running: Boolean = false,
    val result: SavedYinReplay.Result? = null, val failed: Boolean = false)

/** Read-only compatibility projections; operation owners are the only state writers. */
internal fun AnalysisState<YinReplayRequest, SavedYinReplay.Result>.yinReplayState() =
    SavedYinReplayState(request?.sessionId, this is AnalysisState.Loading, value, failure != null)
internal fun AnalysisState<ComponentRequest, DroneComponentCurve>.componentState(
    selected: String? = null, traces: TraceSnapshot = TraceSnapshot()): DroneComponentState {
    if (this === AnalysisState.Disposed) return DroneComponentState()
    if (request != null) return DroneComponentState(request.sessionId, this is AnalysisState.Loading, progress, value, failure != null)
    val cached = traces.products[selected]
    return if (cached != null) DroneComponentState(cached.request.sessionId, progress = 100, curve = cached.curve)
        else DroneComponentState(selected.takeIf { it in traces.failures }, failed = selected in traces.failures)
}
internal fun AnalysisState<EvidenceRequest, SavedPitchEvidence.Snapshot>.evidenceState() =
    SavedEvidenceState(request?.sessionId, this is AnalysisState.Loading, value, failure != null)
internal fun AnalysisState<RecordingComparisonRequest, Pair<WavAnalysis.Result, WavAnalysis.Result>>.comparisonState() =
    ComparisonState(this is AnalysisState.Loading, progress, value?.first, value?.second,
        failure?.let { it.message ?: "Error de comparación." })

/** Typed I/O seams, not another analysis engine or a persisted format. */
internal class SavedDiagnosticLoaders(val yin: AnalysisLoader<YinReplayRequest, SavedYinReplay.Result>,
    val component: AnalysisLoader<ComponentRequest, DroneComponentCurve>,
    val evidence: AnalysisLoader<EvidenceRequest, SavedPitchEvidence.Snapshot>)
