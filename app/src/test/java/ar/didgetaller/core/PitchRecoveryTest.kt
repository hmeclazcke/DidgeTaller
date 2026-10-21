package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class PitchRecoveryTest {
    private val didge=InstrumentProfile.DIDGERIDOO
    private fun points(hz:List<Double?>,confidence:Double=.96)=hz.mapIndexed {i,f->SessionAnalysis.Point(i*185L,f,confidence)}
    @Test fun colourSeparatesTemporalVariationFromPeriodicity(){
        assertEquals(PitchTrace.Tone.STABLE,PitchTrace.tone(.95,10.0))
        assertEquals(PitchTrace.Tone.MODERATE,PitchTrace.tone(.95,35.0))
        assertEquals(PitchTrace.Tone.REVIEW,PitchTrace.tone(.95,90.0))
        assertEquals(PitchTrace.Tone.MODERATE,PitchTrace.tone(.85,0.0))
        assertEquals(PitchTrace.Tone.REVIEW,PitchTrace.tone(.72,0.0))
    }
    @Test fun renderedSegmentsHaveAllThreeMeaningfulColours(){
        val raw=points(listOf(70.0,70*2.0.pow(10.0/1200),70*2.0.pow(45.0/1200),70*2.0.pow(130.0/1200)))
        val trace=PitchTrace.build(raw,didge)
        assertEquals(listOf(PitchTrace.Tone.MODERATE,PitchTrace.Tone.STABLE,PitchTrace.Tone.MODERATE,PitchTrace.Tone.REVIEW),trace.map {it.tone})
        assertTrue(trace.all {!it.uncertain})
    }
    @Test fun detectorAcceptedWeakPeriodicityIsVisibleInOrangeWithoutPromotion(){
        val raw=points(List(30){70.0},.67)
        val trace=PitchTrace.build(raw,didge)
        assertEquals(29,trace.count {it.previous!=null});assertTrue(trace.all {it.tone==PitchTrace.Tone.REVIEW && !it.uncertain})
        assertNull(SessionAnalysis.summarize(raw)) // Summary quality threshold is deliberately unchanged.
        assertTrue(PitchTrace.build(points(List(30){70.0},.64),didge).all {it.uncertain && it.previous==null})
    }
    @Test fun noGlobalModeOrMissingNeighbourCanDeleteSupportedOnsetsAndLowerNotes(){
        val raw=points(List(20){75.0}+listOf(null,50.0,50.0,50.0,null)+List(20){75.0},.82)
        val trace=PitchTrace.build(raw,didge)
        assertEquals(43,trace.size);assertTrue(trace.all {!it.uncertain})
        assertTrue(PlotScales.overview(raw).window.low<50)
    }
    @Test fun briefAliasedReturnIsAccessibleWithDistinctBoundedPresentationLink(){
        val raw=points(listOf(70.0,70.1,35.0,70.0,70.1)).mapIndexed {i,p->if(i==2)p.copy(confidence=.8) else p}
        val trace=PitchTrace.build(raw,didge)
        assertTrue(trace[2].uncertain);assertSame(raw[2],trace[2].point)
        assertEquals(1,trace[3].previous);assertTrue(trace[3].bridged)
        assertTrue(PlotScales.overview(raw).window.low>50)
        assertFalse(PitchTrace.build(raw.map {it.copy(confidence=.99)},didge)[2].uncertain)
    }
    @Test fun hardSilenceAndLongMissingIntervalsNeverGetAConnection(){
        val raw=points(listOf(70.0,null,70.0)).map {if(it.hz==null)it.copy(confidence=0.0) else it}
        assertNull(PitchTrace.build(raw,didge).last().previous)
        val long=listOf(raw.first(),SessionAnalysis.Point(1500,35.0,.4),SessionAnalysis.Point(1800,70.0,.96))
        assertNull(PitchTrace.build(long,didge).last().previous)
    }
    @Test fun weakerEndpointDeterminesSegmentQualityAndOriginalsAreUnchanged(){
        val raw=points(listOf(70.0,70.1,70.0)).mapIndexed {i,p->if(i==0)p.copy(confidence=.72) else p}
        val trace=PitchTrace.build(raw,didge)
        assertEquals(PitchTrace.Tone.REVIEW,trace[1].tone)
        assertEquals(PitchTrace.Tone.STABLE,trace[2].tone)
        raw.indices.forEach {assertSame(raw[it],trace[it].point)}
    }
    @Test fun generalUsesCorroborationWithoutCroppingBackedTootsOrDeletingLoneOriginals(){
        val raw=points(List(60){70.0}+listOf(null,44.0,null)+List(10){70.0}+listOf(205.0,207.0),.84)
        val general=PlotScales.overview(raw)
        assertEquals(PlotScales.overview(raw.filter {it.hz!=44.0}).window,general.window)
        assertTrue(general.window.high>207)
        assertEquals(1,general.clipped)
        assertTrue(PlotScales.pitch(raw,true).window.low<44)
        assertTrue(PitchTrace.build(raw,didge).any {it.point.hz==44.0})
    }
}
