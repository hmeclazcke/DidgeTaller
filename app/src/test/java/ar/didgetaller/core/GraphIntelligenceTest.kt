package ar.didgetaller.core

import ar.didgetaller.ui.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GraphIntelligenceTest {
    private fun p(i:Int,hz:Double?,confidence:Double=.97)=SessionAnalysis.Point(i*180L,hz,confidence)
    @Test fun stableDroneHasContextAndInteractiveZoomOutStopsAtGeneral(){
        val points=(0..60).map {p(it,70+sin(it.toDouble()))}
        val state=ExplorerState();val general=state.camera(11000,44100,points)
        assertTrue(general.y.low in 50.0..65.0);assertTrue(general.y.high in 75.0..90.0)
        repeat(200){state.transform(11000,44100,points,.2,.7,.01,.01,0.0,0.0)}
        assertEquals(general,state.camera(11000,44100,points))
        assertTrue(PlotScales.pitch(points).window.span<5)
    }
    @Test fun resetAfterLongExplorationRestoresDataFitAndKeepsSelection(){
        val points=(0..60).map {p(it,70+sin(it.toDouble())*.1)};val s=ExplorerState()
        val initial=s.camera(11000,44100,points);s.selectedMs=6300;s.inspected="raw"
        repeat(100){s.transform(11000,44100,points,.3,.4,1.02,1.02,-.02,.01)}
        val y=s.camera(11000,44100,points).y
        s.transform(11000,44100,points,.5,.5,1.0,1.0,.1,0.0)
        assertEquals(y,s.camera(11000,44100,points).y)
        s.extendedPitchRange=true;s.reset(11000,44100,points)
        assertFalse(s.extendedPitchRange);assertEquals(initial,s.camera(11000,44100,points));assertEquals(6300,s.selectedMs);assertEquals("raw",s.inspected)
    }
    @Test fun lowConfidenceAndOutOfProfileDataRemainRawAndDoNotDriveScale(){
        val points=listOf(p(0,70.0),p(1,70.1),p(2,35.0,.3),p(3,70.2),p(4,900.0,.2),p(5,70.1))
        val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
        assertEquals(2,trace.count {it.uncertain});assertEquals(1,trace[3].previous)
        assertTrue(PlotScales.overview(points).window.low>50);assertTrue(PlotScales.overview(points).window.high<90)
        assertTrue(PlotScales.pitch(points,true).window.high>900)
        assertSame(points[2],trace[2].point);assertSame(points[4],trace[4].point)
    }
    @Test fun isolatedPeriodWithContextIsUncertainButNotRewritten(){
        for(hz in listOf(35.0,140.0,70.0/3)){
            val instrument=if(hz<35)InstrumentProfile.NATIVE_AMERICAN_FLUTE else InstrumentProfile.DIDGERIDOO
            val points=if(hz<35)listOf(p(0,660.0),p(1,220.0,.88),p(2,662.0)) else listOf(p(0,70.1),p(1,hz,.88),p(2,70.2))
            val t=PitchTrace.build(points,instrument)
            assertTrue(t[1].uncertain);assertEquals(PitchTrace.Reason.ISOLATED_PERIOD,t[1].reason);assertEquals(0,t[2].previous)
            assertSame(points[1],t[1].point)
        }
        val real=listOf(p(0,126.8567,.8506),p(1,42.9031,.8762),p(2,128.7177,.9123))
        assertEquals(PitchTrace.Reason.ISOLATED_PERIOD,PitchTrace.build(real,InstrumentProfile.DIDGERIDOO)[1].reason)
    }
    @Test fun genuineTransitionsAndCleanOrSustainedOctavesArePreserved(){
        for(hz in listOf(listOf(70.0,75.0,82.0,90.0),listOf(70.0,140.0,140.0,70.0),listOf(70.0,35.0,35.0,70.0))){
            val points=hz.mapIndexed {i,f->p(i,f)};val t=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
            assertFalse(t.any {it.uncertain});assertEquals(points,t.map {it.point})
        }
        val melody=listOf(392.0,440.0,523.0,440.0).mapIndexed {i,hz->p(i,hz,.88)}
        val trace=PitchTrace.build(melody,InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        assertFalse(trace.any {it.uncertain});assertEquals(listOf(null,0,1,2),trace.map {it.previous})
        val range=PlotScales.overview(melody,InstrumentProfile.NATIVE_AMERICAN_FLUTE).window
        assertTrue(range.low<392 && range.high>523)
    }
    @Test fun missingAndDiscontinuousTimesBreakTheCurve(){
        val points=listOf(p(0,70.0),p(1,null),p(2,70.0),SessionAnalysis.Point(3000,70.0,.99),SessionAnalysis.Point(2900,70.0,.99))
        assertTrue(PitchTrace.build(points,InstrumentProfile.DIDGERIDOO).all {it.previous==null})
    }
    @Test fun noReliableDataProducesFiniteEmptyRange(){
        val points=listOf(p(0,null),p(1,0.0),p(2,Double.NaN),p(3,70.0,.2))
        for(profile in InstrumentProfile.entries){val r=PlotScales.overview(points,profile);assertEquals(0,r.valid);assertTrue(r.window.low>=0 && r.window.high.isFinite())}
    }
    @Test fun musicalBoundariesAndZeroAxisNeverInventNotes(){
        for(hz in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,Double.MIN_VALUE,1e-20,.001,1.0,1e20)){
            assertNull(Music.pitch(hz));assertEquals("—",Music.note(hz));assertTrue(Music.midi(hz).isNaN())
        }
        for((hz,name) in listOf(65.406 to "C2",69.296 to "C#2",130.813 to "C3",261.626 to "C4",440.0 to "A4",880.0 to "A5"))assertEquals(name,Music.pitch(hz)!!.note)
        assertTrue(PitchAxis.ticks(AxisWindow(0.0,1.0),5).all {it.note==null})
        for(range in listOf(AxisWindow(0.0,500.0),AxisWindow(0.0,.01),AxisWindow(.001,1.0)))
            PitchAxis.ticks(range,5).forEach {tick->if(tick.note!=null)assertTrue(tick.hz>=Music.frequency(0))}
    }
    @Test fun camerasAreIndependentAcrossKindsAndInverseZoomStillHolds(){
        val points=(0..30).map {p(it,70.0)};val s=ExplorerState();val initial=s.camera(6000,44100,points)
        s.transform(6000,44100,points,.5,.5,2.0,2.0,0.0,0.0)
        s.transform(6000,44100,points,.5,.5,.5,.5,0.0,0.0)
        assertEquals(initial.y.low,s.camera(6000,44100,points).y.low,1e-8)
        s.plot=AcousticPlot.SPECTROGRAM;assertEquals(500.0,s.camera(6000,44100,points).y.high,0.0)
        s.plot=AcousticPlot.PITCH;assertEquals(initial.y.high,s.camera(6000,44100,points).y.high,1e-8)
    }
    @Test fun rareReliableExtremeIsVisibleInGeneralAndExplicitFullRange(){
        val points=(0..50).map {p(it,if(it in 25..26)200.0 else 70.0)}
        val overview=PlotScales.overview(points)
        assertEquals(0,overview.clipped);assertTrue(overview.window.high>200)
        assertTrue(PlotScales.pitch(points,true).window.high>200)
        assertFalse(PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)[25].uncertain)
        assertEquals(0,PlotScales.overview(listOf(p(0,Double.MAX_VALUE))).valid)
    }
}
