package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class TracePreparationTest {
    private val points = listOf(SessionAnalysis.Point(0, 70.0, .99), SessionAnalysis.Point(100, 70.0, .99))
    @Test fun aCanonicalGapNeverBorrowsAValidHistoricalMeasurement() {
        val gap = MusicalTrace(InstrumentProfile.DIDGERIDOO, MusicalTrace.Meaning.SPECTRAL_COMPONENT, 1000,
            listOf(MusicalTrace.Sample(0.0, null, false, MusicalTrace.Evidence.Yin(.1, null))))
        val prepared = TraceDrawing.prepare(gap, points, gap.instrument, true, true)
        assertTrue(prepared.marks.isEmpty()); assertTrue(prepared.historical.isEmpty())
        assertEquals(TraceEvidence(0, 0, 0), prepared.evidence)
    }
    @Test fun hiddenTraceRetainsEvidenceWithoutPreparingUnneededDrawingInstructions() {
        val trace = MusicalTrace(InstrumentProfile.NATIVE_AMERICAN_FLUTE, MusicalTrace.Meaning.ESTIMATED_PITCH, 1000,
            listOf(MusicalTrace.Sample(0.0, 440.0, false, MusicalTrace.Evidence.Yin(.99, null))))
        val hidden = TraceDrawing.prepare(trace, points, trace.instrument, false, false)
        assertTrue(hidden.marks.isEmpty()); assertEquals(TraceEvidence(0, 1, 0), hidden.evidence)
        val visible = TraceDrawing.prepare(trace, points, trace.instrument, true, false)
        assertEquals(listOf(TraceDrawing.Dot(TraceDrawing.Vertex(0.0, 440.0))), visible.marks)
    }
    @Test fun historicalFallbackKeepsOriginalSupportedPointsAndTheirTimes() {
        val prepared = TraceDrawing.prepare(null, points, InstrumentProfile.DIDGERIDOO, true, true)
        assertEquals(points, prepared.historical.map { it.point }); assertFalse(prepared.marks.isEmpty())
        assertEquals(TraceEvidence(0, 2, 0), prepared.evidence)
    }
}
