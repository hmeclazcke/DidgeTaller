package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class RegisterTransitionTest {
    @Test fun weakBackedRegistersRemainVisibleWithoutAnUnsupportedSweep(){
        val frequencies=List(6){67.8}+List(8){35.6}+List(6){66.0}
        val raw=frequencies.mapIndexed {i,hz->SessionAnalysis.Point(i*186L,hz,.76)}
        val trace=PitchTrace.build(raw,InstrumentProfile.DIDGERIDOO)
        assertEquals(raw.size,trace.size);assertTrue(trace.none {it.uncertain})
        assertNull(trace[6].previous);assertNull(trace[14].previous)
        assertEquals(raw.size-3,trace.count {it.previous!=null})
        trace.forEachIndexed {i,s->assertSame(raw[i],s.point)}
        val range=PlotScales.overview(raw).window
        assertTrue(35.6 in range.low..range.high);assertTrue(67.8 in range.low..range.high)
    }
    @Test fun fluteMelodyAndSupportedTootKeepTheirMeasuredRegisters(){
        val raw=(List(6){70.0}+List(6){205.0}+List(6){70.0}).mapIndexed {i,hz->SessionAnalysis.Point(i*185L,hz,.87)}
        val toot=PitchTrace.build(raw,InstrumentProfile.DIDGERIDOO)
        assertTrue(toot.none {it.uncertain});assertNull(toot[6].previous);assertNotNull(toot[7].previous)
        assertTrue(PlotScales.overview(raw).window.high>205)
        val melody=(List(6){392.0}+List(6){784.0}).mapIndexed {i,hz->SessionAnalysis.Point(i*93L,hz,.87)}
        assertEquals(11,PitchTrace.build(melody,InstrumentProfile.NATIVE_AMERICAN_FLUTE).count {it.previous!=null})
    }
}
