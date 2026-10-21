package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class LivePitchTest {
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
