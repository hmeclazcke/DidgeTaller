package ar.didgetaller.core

import ar.didgetaller.RecordingAnalysisState
import org.junit.Assert.*
import org.junit.Test

class RecordingAnalysisPresentationTest {
    private val request = RecordingAnalysisRequest("synthetic", InstrumentProfile.DIDGERIDOO, 20, 80, true)
    @Test fun uiFactsComeFromTheSameRequestAndTypedOutcome() {
        val state = RecordingAnalysisState(RecordingAnalysisStatus.Running(request, 40))
        assertEquals("synthetic", state.sessionId); assertEquals(20L, state.startMs); assertEquals(80L, state.endMs)
        assertTrue(state.running); assertEquals(40, state.progress); assertNull(state.result); assertNull(state.error)
        val cancelled = RecordingAnalysisState(RecordingAnalysisStatus.Cancelled(request, 40))
        assertFalse(cancelled.running); assertEquals(40, cancelled.progress); assertEquals("Análisis cancelado.", cancelled.error)
    }
    @Test fun errorsKeepLegacyDiagnosticTranslationInputsAndIdleHasNoSession() {
        assertEquals("invalid WAV", RecordingAnalysisState(RecordingAnalysisStatus.Failed(request, Exception("invalid WAV"))).error)
        assertEquals("Error al leer el WAV.", RecordingAnalysisState(RecordingAnalysisStatus.Failed(request, Exception())).error)
        val idle = RecordingAnalysisState()
        assertNull(idle.sessionId); assertEquals(0L, idle.startMs); assertNull(idle.endMs)
        assertFalse(idle.running); assertEquals(0, idle.progress); assertNull(idle.error)
        assertEquals(idle, RecordingAnalysisState(RecordingAnalysisStatus.Idle))
    }
}
