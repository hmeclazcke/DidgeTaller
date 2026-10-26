package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class MusicalTraceTest {
    private fun component(time: Double, hz: Double?) = DroneComponentCurve.Sample(
        time, hz, hz, if (hz == null) 0.0 else .1,
        if (hz == null) DroneComponentCurve.Reason.QUIET else DroneComponentCurve.Reason.COMPONENT,
    )

    @Test fun didgeridooUsesMeasuredComponentsWithoutInsertingConflictingYin() {
        val curve = DroneComponentCurve(listOf(
            component(200.0, 70.0), component(386.0, 69.0),
            component(572.0, null), component(758.0, 35.0),
            component(944.0, 35.0), component(1130.0, 140.0),
            component(1316.0, 140.0),
        ), 186.0)
        val trace = MusicalTrace.didgeridoo(curve, 1500)

        assertEquals(MusicalTrace.Meaning.SPECTRAL_COMPONENT, trace.meaning)
        assertEquals(listOf(70.0, 69.0, null, 35.0, 35.0, 140.0, 140.0),
            trace.samples.map { it.hz })
        assertEquals(listOf(false, true, false, false, true, false, true),
            trace.samples.map { it.linkedToPrevious })
        assertTrue(trace.frequencyRange().low < 35.0)
        assertTrue(trace.frequencyRange().high > 140.0)
    }

    @Test fun fluteKeepsAcceptedPitchAndDoesNotUseDroneComponent() {
        val captured = listOf(
            SessionAnalysis.Point(0, 440.0, .96),
            SessionAnalysis.Point(180, 441.0, .97),
            SessionAnalysis.Point(360, null, .2),
            SessionAnalysis.Point(540, 523.25, .98),
            SessionAnalysis.Point(720, 523.0, .97),
        )
        val trace = MusicalTrace.flute(captured, 900)

        assertEquals(InstrumentProfile.NATIVE_AMERICAN_FLUTE, trace.instrument)
        assertEquals(MusicalTrace.Meaning.ESTIMATED_PITCH, trace.meaning)
        assertTrue(trace.samples.none { it.hz == 70.0 })
        assertTrue(trace.samples.any { it.hz == 440.0 })
        assertTrue(trace.samples.any { it.hz == 523.25 })
        assertFalse(trace.samples.first { it.hz == 523.25 }.linkedToPrevious)
    }

    @Test fun unsupportedIntervalsRemainGapsIncludingARejectedHistoricalTail() {
        val trace = MusicalTrace.didgeridoo(DroneComponentCurve(listOf(
            component(100.0, 65.0), component(286.0, 65.0),
            component(472.0, null), component(658.0, null),
        ), 186.0), 1000)

        assertEquals(2, trace.samples.count { it.hz != null })
        assertFalse(trace.samples[2].linkedToPrevious)
        assertNull(trace.samples[3].hz)
    }

    @Test fun supportedHighRegisterHistoryBecomesAnEventWithoutJoiningTheDroneLine() {
        val curve = DroneComponentCurve((0..12).map { index -> component(index * 186.0, 70.0) }, 186.0)
        val history = listOf(
            SessionAnalysis.Point(930, 70.0, .97),
            SessionAnalysis.Point(1116, 206.0, .97),
            SessionAnalysis.Point(1302, 207.0, .97),
            SessionAnalysis.Point(1488, 70.0, .97),
            SessionAnalysis.Point(1674, 210.0, .82),
        )
        val trace = MusicalTrace.didgeridoo(curve, 2400, history)

        assertEquals(listOf(1116L, 1302L), trace.highRegisterCandidates.map { it.timeMs })
        assertTrue(trace.samples.all {it.hz == 70.0})
        assertFalse(trace.samples.any {it.hz == 206.0 || it.hz == 207.0})
    }
}
