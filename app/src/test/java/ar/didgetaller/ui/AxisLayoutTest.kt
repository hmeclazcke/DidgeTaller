package ar.didgetaller.ui

import ar.didgetaller.core.*
import org.junit.Assert.*
import org.junit.Test

class AxisLayoutTest {
    @Test fun longAndOffsetTimeWindowsAlwaysOfferAtLeastTwoMarks(){
        for(window in listOf(AxisWindow(0.0,47900.0),AxisWindow(0.0,51460.0),AxisWindow(39000.0,42000.0),AxisWindow(.21,.24)))
            for(maximum in 2..5){val ticks=PlotScales.ticks(window,maximum)
                assertTrue(ticks.size>=2);assertTrue(ticks.all {it in window.low..window.high})}
    }
    @Test fun actualTextWidthsKeepClampedTimeLabelsInsideAndSeparate(){
        val window=AxisWindow(0.0,47900.0)
        for(width in listOf(180f,260f,420f)){
            val placed=placeAxisLabels(PlotScales.ticks(window,3),window,width,8f,{"%.2f s".format(it/1000)},{it.length*9f})
            assertTrue(placed.size>=2)
            assertTrue(placed.all {it.center-it.width/2>=0 && it.center+it.width/2<=width})
            placed.zipWithNext().forEach {(a,b)->assertTrue(b.center-b.width/2>=a.center+a.width/2+8)}
        }
        assertEquals("70",axisLabel(70.0,40.0));assertEquals("70,25",axisLabel(70.25,1.0))
    }
}
