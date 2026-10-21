package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class MusicAxisTest {
    @Test fun referenceNotesOctavesAndCents(){
        listOf(69.2957 to "C#2",261.6256 to "C4",392.0 to "G4",440.0 to "A4",880.0 to "A5").forEach {(hz,name)->
            val note=Music.pitch(hz)!!;assertEquals(name,note.note);assertEquals(0.0,note.cents,.025)
            assertEquals(hz,Music.frequency(note.midi),.01)
        }
        assertEquals(7.6,Music.pitch(Music.frequency(37)*2.0.pow(7.6/1200))!!.cents,1e-8)
        assertEquals(-32.4,Music.pitch(440*2.0.pow(-32.4/1200))!!.cents,1e-8)
        assertEquals("C-1",Music.pitch(Music.frequency(0))!!.note)
    }
    @Test fun invalidFrequenciesNeverBecomeNotes(){
        listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY).forEach {
            assertNull(Music.pitch(it));assertEquals("—",Music.note(it))
        }
    }
    @Test fun broadTicksAreTrueMusicalFrequenciesAndSpacedInHz(){
        val range=AxisWindow(260.0,900.0)
        for(max in 2..8){
            val ticks=PitchAxis.ticks(range,max)
            assertTrue(ticks.size<=max);assertTrue(ticks.size>=2)
            ticks.forEach {assertFalse(it.approximate);assertEquals(0.0,Music.pitch(it.hz)!!.cents,1e-8)}
            ticks.zipWithNext().forEach {(a,b)->assertTrue(b.hz-a.hz>=range.span/(max-1)*.9)}
        }
    }
    @Test fun fineZoomUsesNumericTicksAndOneExplicitApproximation(){
        val ticks=PitchAxis.ticks(AxisWindow(69.5,70.5),5)
        assertEquals(1,ticks.count {it.note!=null});assertTrue(ticks.first {it.note!=null}.approximate)
        assertEquals("C#2",ticks.first {it.note!=null}.note)
        for(window in listOf(AxisWindow(0.0,2000.0),AxisWindow(65.3,65.4),AxisWindow(350.0,700.0))){
            val labels=PitchAxis.ticks(window,4)
            assertTrue(labels.isNotEmpty());assertTrue(labels.all {it.hz in window.low..window.high})
        }
    }
}
