package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/** Display math tolerances: 1e-9 coordinates, 1e-6 Hz on logarithmic transforms. */
class PlotViewportTest {
    @Test fun pixelProjectionRespectsMarginsAndInvertsVerticalDirection(){
        val p=PlotProjection(PlotViewport(AxisWindow(3000.0,7000.0),AxisWindow(65.0,75.0)),800.0,400.0,60.0,20.0)
        assertEquals(PlotPoint(3000.0,75.0),p.data(60.0,20.0));assertEquals(PlotPoint(7000.0,65.0),p.data(860.0,420.0))
        val pixel=p.pixels(4200.0,69.0);val value=p.data(pixel.x,pixel.y)
        assertEquals(4200.0,value.x,1e-9);assertEquals(69.0,value.y,1e-9)
        assertEquals(PlotPoint(3000.0,75.0),p.data(-1000.0,-1000.0))
    }
    @Test fun logarithmicPixelProjectionUsesPositivePhysicalFrequencies(){
        val p=PlotProjection(PlotViewport(AxisWindow(0.0,12000.0),AxisWindow(1.0,10000.0)),400.0,200.0,logY=true)
        assertEquals(100.0,p.data(200.0,100.0).y,1e-9)
        val back=p.pixels(6000.0,100.0);assertEquals(100.0,back.y,1e-9)
    }
    @Test fun linearAndLogCoordinatesRoundTrip(){
        for(log in listOf(false,true))for(window in listOf(AxisWindow(1.0,22050.0),AxisWindow(65.0,75.0)))
            for(f in listOf(0.0,.01,.3,.5,.99,1.0))assertEquals(f,window.fraction(window.value(f,log),log),1e-9)
    }
    @Test fun anchoredZoomPreservesThePointBetweenFingers(){
        val domain=AxisDomain(0.0,1000.0,1.0);val original=AxisWindow(200.0,600.0)
        val next=domain.transform(original,.3,2.5,0.0)
        assertEquals(original.value(.3),next.value(.3),1e-9);assertEquals(160.0,next.span,1e-9)
    }
    @Test fun dragFollowsFingerAndVerticalAxisIsInverted(){
        val domain=AxisDomain(0.0,100.0,.001)
        val view=PlotViewport(AxisWindow(20.0,60.0),AxisWindow(20.0,60.0))
        val next=view.transform(domain,domain,.5,.5,1.0,1.0,.25,.25)
        assertEquals(10.0,next.x.low,1e-9);assertEquals(30.0,next.y.low,1e-9)
        assertEquals(.25,next.x.fraction(view.x.value(0.0)),1e-9)
    }
    @Test fun enormousDragCannotLeaveTheData(){
        val d=AxisDomain(0.0,900000.0,2.0)
        assertEquals(AxisWindow(0.0,100.0),d.transform(AxisWindow(50.0,150.0),.5,1.0,1e20))
        assertEquals(AxisWindow(899900.0,900000.0),d.transform(AxisWindow(50.0,150.0),.5,1.0,-1e20))
    }
    @Test fun zoomIsBoundedAtBothExtremes(){
        val d=AxisDomain(0.0,100.0,.2)
        assertEquals(.2,d.transform(AxisWindow(20.0,60.0),.5,1e200,0.0).span,1e-9)
        assertEquals(AxisWindow(0.0,100.0),d.transform(AxisWindow(20.0,60.0),.5,1e-200,0.0))
    }
    @Test fun logarithmicZoomIsAnchoredAndFinite(){
        val d=AxisDomain(1.0,22050.0,ln(1.03),true);val w=AxisWindow(20.0,2000.0)
        val zoom=d.transform(w,.4,2.0,0.0)
        assertEquals(w.value(.4,true),zoom.value(.4,true),1e-6)
        val edge=d.transform(zoom,.5,1.0,-1e200)
        assertTrue(edge.high.isFinite());assertEquals(22050.0,edge.high,1e-6)
    }
    @Test fun logNeverUsesZeroAsFrequency(){
        val d=AxisDomain(1.345,22050.0,ln(1.03),true)
        val range=d.constrain(AxisWindow(0.0,500.0));assertTrue(range.low>0)
    }
    @Test fun robustPitchScaleFitsMusicalVariationAndReportsOutliers(){
        val points=(0..99).map {SessionAnalysis.Point(it*100L,70.0+sin(it.toDouble())*.7,.98)}+SessionAnalysis.Point(10001,210.0,.95)
        val result=PlotScales.pitch(points)
        assertTrue(result.window.span<5);assertEquals(1,result.clipped)
        assertTrue(PlotScales.pitch(points,true).window.high>210)
        assertEquals(210.0,points.last().hz!!,0.0)
    }
    @Test fun pitchSilenceAndSingleReadingHaveUsableRanges(){
        assertEquals(0,PlotScales.pitch(listOf(SessionAnalysis.Point(0,null,0.0))).valid)
        val single=PlotScales.pitch(listOf(SessionAnalysis.Point(0,70.0,.95)))
        assertTrue(single.window.low<70);assertTrue(single.window.high>70);assertEquals(0,single.clipped)
    }
    @Test fun noLowConfidenceOrInvalidPitchAffectsScale(){
        val p=listOf(SessionAnalysis.Point(0,70.0,.95),SessionAnalysis.Point(1,5000.0,.1),SessionAnalysis.Point(2,Double.NaN,.95),SessionAnalysis.Point(3,-1.0,.95))
        val result=PlotScales.pitch(p);assertEquals(1,result.valid);assertTrue(result.window.high<72)
    }
    @Test fun labelsStayInsideVisibleLinearAndLogRanges(){
        for(log in listOf(false,true))for(w in listOf(AxisWindow(1.0,22050.0),AxisWindow(65.3,65.4),AxisWindow(68.0,72.0))){
            val ticks=PlotScales.ticks(w,5,log);assertTrue(ticks.isNotEmpty());assertTrue(ticks.all {it>=w.low && it<=w.high+1e-8})
            assertEquals(ticks.distinct().size,ticks.size)
        }
    }
}
