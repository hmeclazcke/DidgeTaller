package ar.didgetaller.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PlotTouchTest {
    @Test fun horizontalTimelineDragScrubsInBothDirections(){
        for(dx in listOf(-40f,40f))assertEquals(PlotTouch.SCRUB,plotTouch(dx,2f,10f,1,true))
    }
    @Test fun verticalTimelineDragRemainsAvailableToParentScroll(){
        assertEquals(PlotTouch.SCROLL,plotTouch(2f,40f,10f,1,true))
    }
    @Test fun movementAtTouchSlopDoesNotSeek(){
        assertEquals(PlotTouch.WAIT,plotTouch(10f,0f,10f,1,true))
    }
    @Test fun secondFingerNavigatesEvenBeforeTouchSlop(){
        assertEquals(PlotTouch.VIEWPORT,plotTouch(0f,0f,10f,2,true))
    }
    @Test fun spectrumDragStillNavigates(){
        assertEquals(PlotTouch.VIEWPORT,plotTouch(40f,0f,10f,1,false))
    }
}
