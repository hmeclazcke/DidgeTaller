package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class FundamentalRepairTest {
    private val didge=InstrumentProfile.DIDGERIDOO
    private fun points(vararg hz:Double?)=hz.mapIndexed {i,f->SessionAnalysis.Point(i*180L,f,.97)}
    @Test fun preRegressionAcceptanceRestoresModerateEvidenceDrone(){
        val raw=points(70.1,70.2,70.1,70.2,70.3).map {it.copy(confidence=.76)}
        val t=PitchTrace.build(raw,didge)
        assertFalse(t.any {it.uncertain});assertEquals(4,t.count {it.previous!=null})
    }
    @Test fun isolatedLowAndHighOutliersDoNotBreakTheMainCurveOrScale(){
        for(spike in listOf(35.0,62.0,82.4,140.0,200.0)){
            val raw=points(70.1,70.2,spike,70.1,70.2).mapIndexed {i,p->if(i==2)p.copy(confidence=.8) else p}
            val t=PitchTrace.build(raw,didge)
            assertTrue(t[2].uncertain);assertEquals(1,t[3].previous);assertTrue(t[3].bridged)
            assertSame(raw[2],t[2].point);assertEquals(3,t.count {it.previous!=null})
            val range=PlotScales.overview(raw).window;assertTrue(range.low>50 && range.high<90)
        }
    }
    @Test fun hardSilenceRemainsGapButBriefConfidenceDipDoesNotInventSamples(){
        val silence=points(70.0,null,null,70.1).map {if(it.hz==null)it.copy(confidence=0.0) else it}
        assertNull(PitchTrace.build(silence,didge).last().previous)
        val dip=silence.map {if(it.hz==null)it.copy(confidence=.6) else it}
        val t=PitchTrace.build(dip,didge)
        assertEquals(2,t.size);assertEquals(0,t.last().previous);assertTrue(t.last().bridged)
        assertTrue(t.all {sample->dip.any {it===sample.point}})
    }
    @Test fun consistentShortTootAndFluteAreNeverRemovedAsSingletons(){
        val toot=points(70.0,70.0,205.0,207.0,206.0,70.0).map {it.copy(confidence=.88)}
        assertFalse(PitchTrace.build(toot,didge).any {it.uncertain})
        assertTrue(PlotScales.overview(toot).window.high>207)
        val flute=points(392.0,440.0,523.0,440.0).map {it.copy(confidence=.8)}
        val t=PitchTrace.build(flute,InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        assertFalse(t.any {it.uncertain});assertEquals(listOf(null,0,1,2),t.map {it.previous})
    }
    @Test fun shortWeakAlternativePeriodIsInspectableWithoutCuttingDominantDrone(){
        val raw=(List(12){75.8}+listOf(50.2,50.1,49.9)+List(12){75.9}).mapIndexed {i,f->SessionAnalysis.Point(i*180L,f,if(i in 12..14).82 else .8)}
        val t=PitchTrace.build(raw,didge)
        assertTrue((12..14).all {t[it].uncertain});assertEquals(11,t[15].previous)
        assertTrue(PlotScales.overview(raw).window.low>60)
        val sustained=(List(12){75.8}+List(6){50.0}+List(12){75.9}).mapIndexed {i,f->SessionAnalysis.Point(i*180L,f,.8)}
        assertFalse(PitchTrace.build(sustained,didge).any {it.uncertain})
    }
    @Test fun uncertainLongIntervalCannotBridgeOrPromoteNoise(){
        val raw=points(70.0,*Array<Double?>(10){35.0},70.0).mapIndexed {i,p->if(i in 1..10)p.copy(confidence=.3) else p}
        val t=PitchTrace.build(raw,didge)
        assertNull(t.last().previous);assertEquals(10,t.count {it.uncertain})
        assertEquals(raw,t.map {it.point})
    }
    @Test fun cardTraceRetainsOnlyOriginalEndpointsAndUsefulContinuity(){
        val raw=(0..999).map {SessionAnalysis.Point(it*180L,if(it%50==25)62.0 else 70.0,if(it%50==25).72 else .8)}
        val t=PitchTrace.build(raw,didge)
        assertEquals(1000,t.size);assertEquals(20,t.count {it.uncertain})
        assertEquals(979,t.count {it.previous!=null})
        assertTrue(t.all {sample->raw.any {it===sample.point}})
        assertEquals(0,PlotScales.overview(raw).clipped)
    }
}
