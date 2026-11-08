package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class AxisDrawingTest {
    private val projection = PlotProjection(PlotViewport(AxisWindow(0.0, 1000.0), AxisWindow(50.0, 150.0)), 400.0, 200.0)
    @Test fun numericFrequencyTicksKeepRealPositionsAndStayInsideTheirLabelSpace() {
        val text = AxisDrawing.vertical(listOf(50.0, 100.0, 150.0), emptyList(), projection, 20f, 4f) { it.toInt().toString() }
        assertEquals(listOf("50", "100", "150"), text.map { it.value })
        assertEquals(listOf(195f, 100f, 20f), text.map { it.y })
        assertTrue(text.all { it.x == -4f && it.rightAligned && it.scale == 1f && it.alpha == 255 })
    }
    @Test fun notesAreOnlyAddedForExplicitPitchTicksWithRoomForTheirSecondLine() {
        val notes = listOf(PitchAxis.Tick(50.0, "A", true), PitchAxis.Tick(100.0, "G", false))
        val text = AxisDrawing.vertical(listOf(50.0, 100.0, 150.0), notes, projection, 20f, 4f) { "Hz:$it" }
        assertEquals(listOf("Hz:50.0", "≈ A", "Hz:100.0", "G", "Hz:150.0"), text.map { it.value })
        assertEquals(177f, text[0].y, 0f); assertEquals(197f, text[1].y, 0f)
        assertEquals(.85f, text[1].scale, 0f); assertEquals(195, text[1].alpha)
    }
    @Test fun horizontalLabelsUseAlreadyMeasuredCentersAndNeverRecomputeTimeFromScreenWidth() {
        val text = AxisDrawing.horizontal(listOf("20 s" to 10f, "25 s" to 300f), 200f, 20f)
        assertEquals(listOf(10f, 300f), text.map { it.x }); assertEquals(listOf(230f, 230f), text.map { it.y })
        assertTrue(text.all { !it.rightAligned }); assertTrue(AxisDrawing.horizontal(emptyList(), 20f, 10f).isEmpty())
    }
}
