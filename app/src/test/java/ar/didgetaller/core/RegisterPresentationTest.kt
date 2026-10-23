package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class RegisterPresentationTest {
    @Test fun measuredRegisterChangesKeepBothGroupsWithoutDrawingASweep() {
        val points=listOf(35.0,35.0,70.0,70.0,140.0,140.0).mapIndexed {i,hz->SessionAnalysis.Point(i*200L,hz,.98)}
        val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
        assertEquals(points,trace.map {it.point})
        assertTrue(trace.none {it.uncertain})
        assertNull(trace[2].previous);assertNull(trace[4].previous)
        assertNotNull(trace[1].previous);assertNotNull(trace[3].previous);assertNotNull(trace[5].previous)
    }
    @Test fun briefStrongLowEventsAndAttackReleaseArePreserved() {
        val points=listOf(85.0,75.0,70.0,50.0,70.0,80.0).mapIndexed {i,hz->SessionAnalysis.Point(i*200L,hz,.98)}
        val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
        assertEquals(points,trace.map {it.point})
        assertTrue(trace.none {it.uncertain})
        assertEquals(50.0,trace[3].point.hz!!,0.0)
    }
    @Test fun fluteConnectorsAndBreathingPausesRemainIndependent() {
        val flute=listOf(440.0,440.0,880.0,880.0).mapIndexed {i,hz->SessionAnalysis.Point(i*100L,hz,.98)}
        assertNotNull(PitchTrace.build(flute,InstrumentProfile.NATIVE_AMERICAN_FLUTE)[2].previous)
        val drone=listOf(SessionAnalysis.Point(0,70.0,.98),SessionAnalysis.Point(200,null,0.0),SessionAnalysis.Point(400,70.0,.98))
        assertNull(PitchTrace.build(drone,InstrumentProfile.DIDGERIDOO).last().previous)
    }
}
