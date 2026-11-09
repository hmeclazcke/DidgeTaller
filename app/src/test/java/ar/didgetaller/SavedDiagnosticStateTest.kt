package ar.didgetaller

import ar.didgetaller.core.*
import org.junit.Assert.*
import org.junit.Test

class SavedDiagnosticStateTest {
    @Test fun diagnosticsPreserveRequestFactsProgressResultsAndFailures() {
        val yin = YinReplayRequest("A", true)
        val result = SavedYinReplay.Result(1, emptyList(), emptyList())
        assertEquals(SavedYinReplayState("A", true), AnalysisState.Loading<YinReplayRequest, SavedYinReplay.Result>(yin).yinReplayState())
        assertSame(result, AnalysisState.Ready(yin, result).yinReplayState().result)
        assertTrue(AnalysisState.Failed<YinReplayRequest, SavedYinReplay.Result>(yin, 20, Exception()).yinReplayState().failed)
        val component = ComponentRequest("B"); val curve = DroneComponentCurve(emptyList(), 10.0)
        assertEquals(32, AnalysisState.Loading<ComponentRequest, DroneComponentCurve>(component, 32).componentState().progress)
        assertSame(curve, AnalysisState.Ready(component, curve).componentState().curve)
        assertTrue(AnalysisState.Failed<ComponentRequest, DroneComponentCurve>(component, 40, Exception()).componentState().failed)
        val evidence = EvidenceRequest("C", 500, InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        assertEquals(SavedEvidenceState("C", true), AnalysisState.Loading<EvidenceRequest, SavedPitchEvidence.Snapshot>(evidence).evidenceState())
        assertTrue(AnalysisState.Failed<EvidenceRequest, SavedPitchEvidence.Snapshot>(evidence, 0, Exception()).evidenceState().failed)
        val snapshot = SavedPitchEvidence.Snapshot(0, 1000, 44100, SavedPitchEvidence.Presence.INSUFFICIENT_INPUT,
            0.0, null, null, 0.0, null, emptyList(), 35, 140)
        assertSame(snapshot, AnalysisState.Ready(evidence, snapshot).evidenceState().result)
    }
    @Test fun disposedAndCancelledProjectionsNeverInventRunningOrFailure() {
        assertEquals(SavedYinReplayState(), AnalysisState.Disposed.yinReplayState())
        assertEquals(DroneComponentState(), AnalysisState.Disposed.componentState())
        assertEquals(SavedEvidenceState(), AnalysisState.Disposed.evidenceState())
        assertEquals(ComparisonState(), AnalysisState.Disposed.comparisonState())
        val request = EvidenceRequest("A", 0, InstrumentProfile.DIDGERIDOO)
        assertEquals(SavedEvidenceState("A"), AnalysisState.Cancelled<EvidenceRequest, SavedPitchEvidence.Snapshot>(request, 0).evidenceState())
    }
    @Test fun comparisonProjectionPreservesBothResultsAndHistoricalErrorFallback() {
        val request = RecordingComparisonRequest(RecordingAnalysisRequest("A", InstrumentProfile.DIDGERIDOO),
            RecordingAnalysisRequest("B", InstrumentProfile.NATIVE_AMERICAN_FLUTE))
        assertEquals(ComparisonState(true, 55), AnalysisState.Loading<RecordingComparisonRequest, Pair<WavAnalysis.Result, WavAnalysis.Result>>(request, 55).comparisonState())
        assertEquals("Error de comparación.", AnalysisState.Failed<RecordingComparisonRequest, Pair<WavAnalysis.Result, WavAnalysis.Result>>(request, 0, Exception()).comparisonState().error)
        assertEquals("invalid", AnalysisState.Failed<RecordingComparisonRequest, Pair<WavAnalysis.Result, WavAnalysis.Result>>(request, 0, Exception("invalid")).comparisonState().error)
        val a = WavAnalysis.Result(0, 4096, 44100, 1, PowerSpectrum(44100, 4096, DoubleArray(2049)),
            HarmonicAnalysis.Result(null, emptyList(), null), emptyList(), null, null, null, 0.0, null)
        val b = a.copy(endFrame = 88200)
        val projected = AnalysisState.Ready(request, a to b).comparisonState()
        assertSame(a, projected.a); assertSame(b, projected.b); assertEquals(100, projected.progress)
    }
    @Test fun idleComponentUsesOnlySelectedCanonicalEvidenceAndDisposalHidesIt() {
        val request = MusicalTraceRequest("A", 1000, emptyList(), InstrumentProfile.DIDGERIDOO)
        val curve = DroneComponentCurve(emptyList(), 10.0)
        val view = TraceSnapshot(mapOf("A" to TraceProduct(request, curve,
            MusicalTrace.didgeridoo(curve, request.durationMs, request.captured))), setOf("B"))
        assertSame(curve, AnalysisState.Idle.componentState("A", view).curve)
        assertEquals(100, AnalysisState.Idle.componentState("A", view).progress)
        assertEquals(DroneComponentState("B", failed = true), AnalysisState.Idle.componentState("B", view))
        assertEquals(DroneComponentState(), AnalysisState.Idle.componentState(null, view))
        assertEquals(DroneComponentState(), AnalysisState.Disposed.componentState("A", view))
    }
}
