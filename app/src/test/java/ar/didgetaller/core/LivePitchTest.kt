package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class LivePitchTest {
    @Test fun nonFiniteDetectionsCannotEscapeIntoDisplayState() {
        val previous=LivePitch.State(hz=70.0,consecutive=5,lastMeasuredHz=70.0,storedHz=70.0)
        for(hz in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            val next=LivePitch.next(previous,PitchDetector.Reading(hz,.99),InstrumentProfile.DIDGERIDOO)
            assertEquals(0.0,next.hz,0.0);assertNull(next.storedHz);assertEquals(0,next.consecutive)
            assertEquals(70.0,next.lastMeasuredHz,0.0)
        }
    }
    @Test fun fluteConfidenceBoundaryAndSupportedEndpointsRemainInclusive() {
        val profile=InstrumentProfile.NATIVE_AMERICAN_FLUTE
        for(hz in listOf(profile.validMinHz,440.0,profile.validMaxHz)) {
            assertEquals(hz,LivePitch.next(LivePitch.State(),PitchDetector.Reading(hz,.80),profile).storedHz!!,0.0)
        }
        assertNull(LivePitch.next(LivePitch.State(),PitchDetector.Reading(profile.validMinHz-.001,.99),profile).storedHz)
        assertNull(LivePitch.next(LivePitch.State(),PitchDetector.Reading(profile.validMaxHz+.001,.99),profile).storedHz)
    }
    @Test fun musicalStepsSeparateStabilityFromDisplaySmoothing() {
        val previous=LivePitch.State(70.0,5,70.0,70.0)
        for(cents in listOf(35.0,60.0,90.0)) {
            val hz=70.0*Math.pow(2.0,cents/1200)
            val next=LivePitch.next(previous,PitchDetector.Reading(hz,.98),InstrumentProfile.DIDGERIDOO)
            assertEquals(if(cents<70)70.0*.65+hz*.35 else hz,next.hz,1e-12)
            assertEquals(if(cents<45)6 else 1,next.consecutive)
            assertEquals(hz,next.storedHz!!,0.0)
        }
    }
    @Test fun acceptedDronePeriodicityDoesNotInheritTheFluteConfidenceGate() {
        val next=LivePitch.next(LivePitch.State(),PitchDetector.Reading(70.0,.65),InstrumentProfile.DIDGERIDOO)
        assertEquals(70.0,next.hz,0.0);assertEquals(70.0,next.storedHz!!,0.0)
    }
    @Test fun displaySmoothingDoesNotRewriteStoredHzOrHoldAcrossMissingReadings(){
        val first=LivePitch.next(LivePitch.State(),PitchDetector.Reading(70.0,.95),InstrumentProfile.DIDGERIDOO)
        val next=LivePitch.next(first,PitchDetector.Reading(71.0,.95),InstrumentProfile.DIDGERIDOO)
        assertEquals(70.35,next.hz,1e-8);assertEquals(71.0,next.storedHz!!,0.0)
        val missing=LivePitch.next(next,PitchDetector.Reading(rms=.04),InstrumentProfile.DIDGERIDOO)
        assertEquals(0.0,missing.hz,0.0);assertNull(missing.storedHz)
        assertEquals(70.35,missing.lastMeasuredHz,1e-8)
    }
    @Test fun fluteRetainsItsOwnRangeAndConfidenceGate(){
        for(r in listOf(PitchDetector.Reading(70.0,.99),PitchDetector.Reading(440.0,.79)))
            assertNull(LivePitch.next(LivePitch.State(),r,InstrumentProfile.NATIVE_AMERICAN_FLUTE).storedHz)
        assertEquals(440.0,LivePitch.next(LivePitch.State(),PitchDetector.Reading(440.0,.81),InstrumentProfile.NATIVE_AMERICAN_FLUTE).storedHz!!,0.0)
    }
}
