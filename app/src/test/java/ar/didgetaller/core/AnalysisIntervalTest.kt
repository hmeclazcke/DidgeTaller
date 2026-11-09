package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class AnalysisIntervalTest {
    @Test fun wholeFileDoesNotTrustHistoricalDurationMetadataAndUsesOriginalProfile() {
        val range = AnalysisInterval.parse(" ", "", 0)!!
        assertEquals(0L, range.startMs); assertNull(range.endMs)
        assertEquals(RecordingAnalysisRequest("A", InstrumentProfile.NATIVE_AMERICAN_FLUTE),
            range.request("A", InstrumentProfile.NATIVE_AMERICAN_FLUTE))
    }
    @Test fun localizedDecimalsAndOpenEndsPreserveMillisecondTruncation() {
        val range = AnalysisInterval.parse("1,2349", "2.5", 3000)!!
        assertEquals(1234L, range.startMs); assertEquals(2500L, range.endMs)
        assertNull(AnalysisInterval.parse("1", "", 3000)!!.endMs)
        assertEquals(0L, AnalysisInterval.parse("", "2", 3000)!!.startMs)
        assertEquals(3000L, AnalysisInterval.parse("0", "3", 3000)!!.endMs)
    }
    @Test fun malformedNonfiniteOverflowNegativeAndInvertedIntervalsFailAtTheBoundary() {
        listOf("bad", "NaN", "Infinity", "1e308", "-1", "-0.0001", "9e15").forEach {
            assertNull("Invalid start $it", AnalysisInterval.parse(it, "", 3000))
            assertNull("Invalid end $it", AnalysisInterval.parse("", it, 3000))
        }
        assertNull(AnalysisInterval.parse("3", "", 3000))
        assertNull(AnalysisInterval.parse("1", "1", 3000))
        assertNull(AnalysisInterval.parse("2", "1", 3000))
        assertNull(AnalysisInterval.parse("", "3.001", 3000))
        assertNull(AnalysisInterval.parse("0", "", 0))
        assertNull(AnalysisInterval.parse("", "0", 3000))
    }
    @Test fun admissionRequiresTheSelectedProfileAndDoesNotReopenAnAlreadyLoadedWorkspace() {
        assertTrue(AnalysisAdmission.opensWorkspace("A", "A", null))
        assertFalse(AnalysisAdmission.opensWorkspace("A", "A", "A"))
        assertFalse(AnalysisAdmission.opensWorkspace(null, "A", null))
        assertFalse(AnalysisAdmission.opensWorkspace("B", "A", null))
        assertTrue(AnalysisAdmission.acceptsDidgeridoo("A", "A", InstrumentProfile.DIDGERIDOO))
        assertFalse(AnalysisAdmission.acceptsDidgeridoo("A", "A", InstrumentProfile.NATIVE_AMERICAN_FLUTE))
        assertFalse(AnalysisAdmission.acceptsDidgeridoo(null, "A", InstrumentProfile.DIDGERIDOO))
        assertFalse(AnalysisAdmission.acceptsDidgeridoo("B", "A", InstrumentProfile.DIDGERIDOO))
    }
}
