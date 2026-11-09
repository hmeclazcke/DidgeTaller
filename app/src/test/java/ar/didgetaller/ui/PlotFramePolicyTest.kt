package ar.didgetaller.ui

import ar.didgetaller.core.*
import org.junit.Assert.*
import org.junit.Test

class PlotFramePolicyTest {
    @Test fun plotIdentifiersKeepTheirPriorityAcrossUnmeasuredAndReadyLayouts() {
        assertEquals("diagnostic-time-chart", plotSurfaceTag(AcousticPlot.PITCH, true, true))
        assertEquals("immersive-plot", plotSurfaceTag(AcousticPlot.SPECTRUM, true, false))
        assertEquals("spectrum-chart", plotSurfaceTag(AcousticPlot.SPECTRUM, false, false))
        assertEquals("time-chart", plotSurfaceTag(AcousticPlot.PITCH, false, false))
        assertEquals("time-chart", plotSurfaceTag(AcousticPlot.WAVE, false, false))
    }
    @Test fun axisMarginsUseActualNumericAndNoteWidthsIncludingApproximationPrefix() {
        val ticks = listOf(50.0, 150.0)
        val notes = listOf(PitchAxis.Tick(100.0, "WWWWWW", true))
        val measure: (String) -> Float = { it.length * 10f }
        assertEquals(76f, plotLeftMargin(true, notes, ticks, 100.0, 20f, 4f, 56f, measure), 1e-5f)
        assertEquals(38f, plotLeftMargin(true, emptyList(), ticks, 100.0, 20f, 4f, 56f, measure), 0f)
        assertEquals(8f, plotLeftMargin(true, emptyList(), emptyList(), 100.0, 20f, 4f, 56f, measure), 0f)
        assertEquals(64f, plotLeftMargin(false, notes, ticks, 100.0, 20f, 4f, 56f, measure), 0f)
        assertEquals(80f, plotLeftMargin(false, notes, ticks, 100.0, 20f, 4f, 80f, measure), 0f)
        assertEquals(38f, plotLeftMargin(true, listOf(PitchAxis.Tick(100.0)), ticks, 100.0, 20f, 4f, 56f, measure), 0f)
        assertEquals(84.5f, plotLeftMargin(true, listOf(PitchAxis.Tick(100.0, "LONG NOTE", false)), ticks, 100.0, 20f, 4f, 56f, measure), 1e-5f)
        val differentWidths = listOf(PitchAxis.Tick(80.0, "A"), PitchAxis.Tick(100.0, "WWWWWW", true))
        assertEquals(76f, plotLeftMargin(true, differentWidths, ticks, 100.0, 20f, 4f, 56f, measure), 1e-5f)
        assertEquals(76f, plotLeftMargin(true, differentWidths.reversed(), ticks, 100.0, 20f, 4f, 56f, measure), 1e-5f)
    }
    @Test fun musicalTicksAndSupportedComponentTicksRemainDifferentPresentations() {
        val window = AxisWindow(50.0, 150.0)
        val musical = verticalPlotTicks(true, window, 300f, 20f, false)
        assertTrue(musical.musical.isNotEmpty()); assertEquals(musical.musical.map { it.hz }, musical.numeric)
        val component = verticalPlotTicks(false, window, 200f, 20f, false)
        assertTrue(component.musical.isEmpty()); assertEquals(PlotScales.ticks(window, 3), component.numeric)
        val large = verticalPlotTicks(false, window, 400f, 20f, false)
        assertEquals(PlotScales.ticks(window, 5), large.numeric)
        val logarithmic = verticalPlotTicks(false, AxisWindow(10.0, 1000.0), 400f, 20f, true)
        assertEquals(PlotScales.ticks(AxisWindow(10.0, 1000.0), 5, true), logarithmic.numeric)
    }
}
