package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class TraceEvidenceTest {
    private val historical = listOf(PitchTrace.Sample(SessionAnalysis.Point(0, 70.0, 1.0), null, false),
        PitchTrace.Sample(SessionAnalysis.Point(1, 140.0, .4), null, true))
    private val samples = listOf(MusicalTrace.Sample(0.0, 70.0, false, MusicalTrace.Evidence.Yin(1.0, null)),
        MusicalTrace.Sample(20.0, null, false, MusicalTrace.Evidence.Yin(.1, null)))

    @Test fun spectralComponentsAreCountedWithoutInventingYinMeasurements() {
        val trace = MusicalTrace(InstrumentProfile.DIDGERIDOO, MusicalTrace.Meaning.SPECTRAL_COMPONENT, 100, samples,
            listOf(MusicalTrace.HighRegisterCandidate(0, 280.0)))
        assertEquals(TraceEvidence(1, 0, 1), TraceEvidence.of(trace, historical))
    }
    @Test fun melodicTraceAndHistoricalFallbackRetainTheirOwnMeaningAndGaps() {
        val trace = MusicalTrace(InstrumentProfile.NATIVE_AMERICAN_FLUTE, MusicalTrace.Meaning.ESTIMATED_PITCH, 100, samples)
        assertEquals(TraceEvidence(0, 1, 0), TraceEvidence.of(trace, historical))
        assertEquals(TraceEvidence(0, 1, 0), TraceEvidence.of(null, historical))
        assertEquals(TraceEvidence(0, 0, 0), TraceEvidence.of(null, emptyList()))
    }
}
