package ar.didgetaller.core

import ar.didgetaller.ui.ExplorerState
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class FundamentalPrecisionTest {
    private val didge=InstrumentProfile.DIDGERIDOO
    private fun points(hz:List<Double?>,confidence:Double=.97)=hz.mapIndexed {i,f->SessionAnalysis.Point(i*180L,f,confidence)}
    @Test fun stableDroneExcludesWeakLowAndHighSpikesWithoutRewriting(){
        for(spike in listOf(35.0,60.0,62.0,65.0,77.0,82.5,140.0)){
            val raw=points(List(61){69.0+it%4}).toMutableList()
            val base=PlotScales.overview(raw).window
            raw[30]=raw[30].copy(hz=spike,confidence=.78)
            val trace=PitchTrace.build(raw,didge)
            assertTrue(trace[30].uncertain);assertNull(trace[30].previous);assertEquals(29,trace[31].previous)
            assertSame(raw[30],trace[30].point);assertEquals(spike,trace[30].point.hz!!,0.0)
            val general=PlotScales.overview(raw).window
            assertEquals(base.low,general.low,0.0);assertEquals(base.high,general.high,0.0)
            assertTrue(general.low>50 && general.high<90)
        }
    }
    @Test fun nonHarmonicIsolatedReturnRequiresStableContextAndConfidenceDrop(){
        val raw=points(listOf(70.1,70.2,62.0,70.1,70.2)).toMutableList()
        raw[2]=raw[2].copy(confidence=.89)
        val trace=PitchTrace.build(raw,didge)
        assertEquals(PitchTrace.Reason.ISOLATED_RETURN,trace[2].reason);assertEquals(1,trace[3].previous)
        assertFalse(PitchTrace.build(raw.map {it.copy(confidence=.98)},didge)[2].uncertain)
        // Unequal context is not a supported return: retain an uncertain musical interpretation.
        raw[3]=raw[3].copy(hz=79.0)
        assertFalse(PitchTrace.build(raw,didge)[2].uncertain)
    }
    @Test fun shortAndSustainedTootsSurviveEvenBelowQuantileMass(){
        for(toot in listOf(listOf(205.0,207.0),listOf(205.0,207.0,206.0,204.0))){
            for(confidence in listOf(.88,.97)){
                val raw=points(List(100){70.0}+toot+List(100){70.0},confidence)
                val trace=PitchTrace.build(raw,didge)
                assertFalse(trace.any {it.uncertain});assertEquals(raw,trace.map {it.point})
                val general=PlotScales.overview(raw).window
                assertTrue(general.low<70 && general.high>207)
                // The event and its internal continuity survive. A weak, abrupt register
                // boundary is not evidence of a measured sweep between the two registers.
                assertEquals(confidence>=.90,trace[100].previous!=null)
                assertTrue(trace[101].previous!=null)
            }
        }
    }
    @Test fun actualYinEvidenceForSyntheticShortTootIsPreserved(){
        val raw=listOf(70.0,70.0,205.0,207.0,70.0).mapIndexed {i,hz->
            val pcm=ShortArray(16384){n->(7000*sin(2*PI*hz*n/44100)+2500*sin(4*PI*hz*n/44100)).toInt().toShort()}
            val reading=PitchDetector.analyze(pcm,44100,35,220,includeVisuals=false)
            assertTrue(reading.rms>.005);assertTrue(reading.confidence>.95);assertEquals(hz,reading.hz,.3)
            SessionAnalysis.Point(i*180L,reading.hz,reading.confidence)
        }
        assertFalse(PitchTrace.build(raw,didge).any {it.uncertain})
        assertTrue(PlotScales.overview(raw).window.high>207)
    }
    @Test fun fluteMelodyAndGlissandoRemainUnchanged(){
        for((profile,raw) in listOf(InstrumentProfile.NATIVE_AMERICAN_FLUTE to points(listOf(392.0,440.0,523.0,440.0),.88),
            didge to points(listOf(70.0,72.0,75.0,79.0,83.0),.91))){
            val trace=PitchTrace.build(raw,profile)
            assertEquals(raw,trace.map {it.point});assertFalse(trace.any {it.uncertain})
            assertEquals(raw.size-1,trace.count {it.previous!=null})
            val general=PlotScales.overview(raw,profile).window
            assertTrue(raw.all {it.hz!! in general.low..general.high})
        }
    }
    @Test fun prolongedUncertaintyNeverBecomesReliableByRepetition(){
        val moderate=points(List(100){70.0},.80)
        assertEquals(99,PitchTrace.build(moderate,didge).count {it.previous!=null})
        val raw=points(List(100){70.0},.60)
        val trace=PitchTrace.build(raw,didge)
        assertEquals(100,trace.count {it.uncertain});assertTrue(trace.all {it.previous==null})
        assertEquals(0,PlotScales.overview(raw).valid)
        assertEquals(100,PlotScales.pitch(raw,true).valid)
    }
    @Test fun silenceAndNonMonotonicTimesNeverConnectOrInventZero(){
        val raw=points(listOf(70.0,null,70.0,0.0,70.0,Double.NaN,70.0))
        val trace=PitchTrace.build(raw,didge)
        assertEquals(4,trace.size);assertTrue(trace.all {it.previous==null && it.point.hz==70.0})
        val reversed=points(listOf(70.0,70.0,70.0)).reversed()
        assertTrue(PitchTrace.build(reversed,didge).all {it.previous==null})
    }
    @Test fun realCaptureReleaseAndBreathExamplesRemainVisibleWithoutInventingPitchError(){
        // Read-only Samsung index: these are delivery timestamps, not PCM window positions.
        val raw=listOf(SessionAnalysis.Point(4520,72.1422,.9963),SessionAnalysis.Point(4698,71.6606,.9953),
            SessionAnalysis.Point(4880,66.8456,.7868),SessionAnalysis.Point(5080,null,.5215))
        val trace=PitchTrace.build(raw,didge)
        assertFalse(trace.last().uncertain);assertEquals(3,PlotScales.overview(raw).valid)
        assertEquals(PitchTrace.Tone.REVIEW,trace.last().tone)
        val release=points(listOf(70.7498,71.9726,65.4191,null)).toMutableList()
        release[2]=release[2].copy(confidence=.9047)
        assertFalse(PitchTrace.build(release,didge)[2].uncertain)
        val attack=release.reversed().mapIndexed {i,p->p.copy(timeMs=i*180L)}
        assertFalse(PitchTrace.build(attack,didge).first().uncertain)
        val onset=listOf(SessionAnalysis.Point(14359,null,.3667),SessionAnalysis.Point(14534,91.1151,.8611),
            SessionAnalysis.Point(14735,74.6530,.9026),SessionAnalysis.Point(14919,74.5649,.8546))
        assertFalse(PitchTrace.build(onset,didge).first().uncertain)
    }
    @Test fun allPresentationsKeepDiscontinuitiesAtCardResolution(){
        val raw=points(List(1000){70.0}).toMutableList();raw[450]=raw[450].copy(hz=140.0,confidence=.8)
        // Card and full viewer consume this same unreduced trace; no min/max decimator on F0.
        val trace=PitchTrace.build(raw,didge)
        assertEquals(raw.size,trace.size);assertEquals(449,trace[451].previous)
        assertTrue(trace.filterNot {it.uncertain}.all {it.point.hz==70.0})
    }
    @Test fun fittedSubHzResetExtremesEmptyAndInstrumentSwitch(){
        val raw=points(List(80){70+sin(it.toDouble())*.01})
        val state=ExplorerState();val general=state.camera(15000,44100,raw)
        assertTrue(PlotScales.pitch(raw).window.span<2)
        repeat(8){state.transform(15000,44100,raw,.5,.5,2.0,2.0,0.0,0.0)}
        assertTrue(state.camera(15000,44100,raw).y.span<1)
        state.extendedPitchRange=true;state.reset(15000,44100,raw)
        assertEquals(general,state.camera(15000,44100,raw));assertFalse(state.extendedPitchRange)
        for(profile in InstrumentProfile.entries){
            assertEquals(0,PlotScales.overview(emptyList(),profile).valid)
            assertEquals(0,PlotScales.overview(points(listOf(Double.POSITIVE_INFINITY,Double.MAX_VALUE,-1.0,0.0)),profile).valid)
        }
        assertEquals(0,PlotScales.overview(points(listOf(70.0)),InstrumentProfile.NATIVE_AMERICAN_FLUTE).valid)
        assertEquals(1,PlotScales.overview(points(listOf(440.0)),InstrumentProfile.NATIVE_AMERICAN_FLUTE).valid)
    }
    @Test fun labelsStayFiniteMusicalAndWithinWindow(){
        for(window in listOf(AxisWindow(0.0,1.0),AxisWindow(60.0,85.0),AxisWindow(120.0,140.0),AxisWindow(1000.0,1100.0),AxisWindow(69.1,69.2))){
            val ticks=PitchAxis.ticks(window,5)
            assertTrue(ticks.all {it.hz.isFinite() && it.hz in window.low..window.high})
            for(tick in ticks)if(tick.note!=null)assertEquals(Music.pitch(tick.hz)!!.note,tick.note)
        }
        assertNull(Music.pitch(0.0));assertNull(Music.pitch(Double.NaN))
    }
}
