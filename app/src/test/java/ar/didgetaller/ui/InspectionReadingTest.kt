package ar.didgetaller.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionReadingTest {
    @Test fun detailSheetPreservesExplicitEvidenceEvenWhenTheCanonicalTraceHasAGap() {
        assertEquals("Original estimate: 137 Hz",inspectionReading("Original estimate: 137 Hz","No nearby reliable reading"))
    }

    @Test fun detailSheetUsesTheTimelineUntilTheUserInspectsEvidence() {
        assertEquals("22.40 s | Component: 137.6 Hz",inspectionReading(null,"22.40 s | Component: 137.6 Hz"))
    }
}
