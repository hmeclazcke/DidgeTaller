package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PrecisionTest {
    @Test fun inverseZoomAndMovingAnchorAreExactForLinearAndLog(){
        for(log in listOf(false,true)){
            val d=AxisDomain(1.0,22050.0,if(log)ln(1.03) else 1.0,log)
            val initial=AxisWindow(200.0,1200.0)
            val zoom=d.transform(initial,.4,2.0,0.0)
            val inverse=d.transform(zoom,.4,.5,0.0)
            assertEquals(initial.low,inverse.low,1e-8);assertEquals(initial.high,inverse.high,1e-8)
            val moved=d.transform(initial,.55,1.37,.15)
            assertEquals(initial.value(.4,log),moved.value(.55,log),1e-8)
        }
    }
    @Test fun continuousPinchSequenceMatchesOneCombinedTransform(){
        val d=AxisDomain(0.0,100000.0,.01);val initial=AxisWindow(20000.0,80000.0)
        var w=initial
        repeat(120){w=d.transform(w,.37,1.006,0.0)}
        val once=d.transform(initial,.37,1.006.pow(120),0.0)
        assertEquals(once.low,w.low,1e-8);assertEquals(once.high,w.high,1e-8)
        repeat(120){w=d.transform(w,.37,1/1.006,0.0)}
        assertEquals(initial.low,w.low,1e-8);assertEquals(initial.high,w.high,1e-8)
    }
    @Test fun geometrySurvivesResizeAndCentroidTranslationInBothAxes(){
        val domain=AxisDomain(0.0,1000.0,.01)
        val v=PlotViewport(AxisWindow(200.0,800.0),AxisWindow(200.0,800.0))
        val changed=v.transform(domain,domain,.6,.45,1.4,1.4,.1,-.05)
        assertEquals(v.x.value(.5),changed.x.value(.6),1e-8)
        assertEquals(v.y.value(.5),changed.y.value(.55),1e-8)
        for((w,h) in listOf(280.0 to 500.0,340.0 to 700.0,1450.0 to 510.0)){
            val p=PlotProjection(changed,w,h,70.0,18.0)
            val px=p.pixels(550.0,520.0);val data=p.data(px.x,px.y)
            assertEquals(550.0,data.x,1e-8);assertEquals(520.0,data.y,1e-8)
        }
    }
    @Test fun repeatedBoundsAndExtremesNeverEscapePhysicalData(){
        for(log in listOf(false,true)){
            val d=AxisDomain(if(log)1.345 else 0.0,22050.0,if(log)ln(1.03) else 1.345,log)
            var w=AxisWindow(100.0,2000.0)
            repeat(100){i->
                w=d.transform(w,(i%10)/10.0,if(i%2==0)1e200 else 1e-200,if(i%3==0)1e200 else -1e200)
                assertTrue(w.low>=d.low-1e-8 && w.high<=d.high+1e-8 && w.high>w.low)
                assertTrue(w.low.isFinite() && w.high.isFinite())
            }
        }
    }
    @Test fun invalidModelInputsAreRejectedAtTheBoundary(){
        fun rejected(block:()->Unit){try{block();fail("invalid accepted")}catch(_:IllegalArgumentException){}}
        rejected{AxisWindow(0.0,0.0)};rejected{AxisWindow(0.0,Double.NaN)}
        rejected{AxisDomain(0.0,Double.POSITIVE_INFINITY,1.0)}
        rejected{AxisDomain(0.0,500.0,Double.NaN)}
        rejected{PlotProjection(PlotViewport(AxisWindow(0.0,1.0),AxisWindow(0.0,1.0)),Double.POSITIVE_INFINITY,30.0)}
        val d=AxisDomain(0.0,500.0,1.0)
        rejected{d.transform(AxisWindow(0.0,200.0),.5,0.0,0.0)}
        rejected{d.transform(AxisWindow(0.0,200.0),Double.NaN,1.0,0.0)}
    }
    private fun p(time:Long,hz:Double?,confidence:Double=.95)=SessionAnalysis.Point(time,hz,confidence)
    @Test fun generalGivesContextAndFittedPreservesSubHzVariation(){
        val points=(0..50).map {p(it*200L,70+sin(it.toDouble())*.1)}
        val general=PlotScales.overview(points);val fitted=PlotScales.pitch(points)
        assertTrue(general.window.span>20);assertTrue(fitted.window.span<2)
        assertEquals(0,general.clipped);assertEquals(points.size,general.valid)
    }
    @Test fun traceBreaksSilenceAndLowConfidenceButKeepsGenuineOutlier(){
        val points=listOf(p(0,70.1),p(200,70.2),p(400,35.0,.3),p(600,70.1),p(800,null),p(1000,35.0,.95),p(1200,70.1))
        val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
        assertEquals(6,trace.size);assertEquals(1,trace.count {it.uncertain})
        assertEquals(1,trace[3].previous);assertNull(trace[4].previous)
        assertEquals(4,trace[5].previous)
        val overview=PlotScales.overview(points);assertTrue(overview.window.low<35)
        assertEquals(35.0,points[2].hz!!,0.0) // Original never changed.
    }
    @Test fun melodyKeepsExactNotesAndTimesWithoutSmoothing(){
        val points=listOf(p(0,392.0),p(100,440.0),p(200,523.0))
        val trace=PitchTrace.build(points,InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        assertEquals(points,trace.map {it.point});assertEquals(listOf(null,0,1),trace.map {it.previous})
        assertFalse(trace.any {it.uncertain})
    }
    @Test fun stftWindowsAreFixedToWavAcrossPanAndHaveNoEmptyCells(){
        val reads=mutableListOf<Long>()
        val source=object:MonoPcmSource {
            override val sampleRate=44100
            override val frameCount=44100L*4
            override fun read(startFrame:Long,destination:ShortArray):Int {
                reads+=startFrame
                val n=min(destination.size.toLong(),frameCount-startFrame).toInt()
                repeat(n){i->destination[i]=(8000*sin(2*PI*440*(startFrame+i)/sampleRate)).toInt().toShort()}
                return n
            }
        }
        val a=AudioVisuals.spectrogram(source,50000,51000,highHz=2000.0,rows=64)
        val b=AudioVisuals.spectrogram(source,50200,51200,highHz=2000.0,rows=64)
        assertEquals(listOf(32768L,32768L),reads)
        assertArrayEquals(a.power,b.power,1e-10f)
        val full=AudioVisuals.spectrogram(source,0,source.frameCount,highHz=2000.0,rows=64)
        for(col in 0 until full.columns)assertTrue((0 until full.rows).any {full.power[col*full.rows+it]>1e-9})
        val silent=object:MonoPcmSource by source {override fun read(startFrame:Long,destination:ShortArray):Int{destination.fill(0);return min(destination.size.toLong(),frameCount-startFrame).toInt()}}
        assertTrue(AudioVisuals.spectrogram(silent,1000,100000).power.all {it==0f})
    }
}
