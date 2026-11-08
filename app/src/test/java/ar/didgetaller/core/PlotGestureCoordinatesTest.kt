package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class PlotGestureCoordinatesTest {
    private val viewport = PlotViewport(AxisWindow(40000.0, 80000.0), AxisWindow(50.0, 150.0))
    private val geometry = PlotGestureCoordinates(viewport, 600.0, 300.0, 80.0, 20.0, false)
    @Test fun seekingUsesRealMarginsAndZoomedViewportInsteadOfTheWhole117SecondRecording() {
        val point = geometry.data(PlotPoint(380.0, 170.0))
        assertEquals(60000.0, point.x, 1e-10); assertEquals(100.0, point.y, 1e-10)
        assertEquals(40000.0, geometry.data(PlotPoint(0.0, 0.0)).x, 0.0)
        assertEquals(80000.0, geometry.data(PlotPoint(900.0, 170.0)).x, 0.0)
        assertTrue(geometry.contains(PlotPoint(80.0, 20.0))); assertTrue(geometry.contains(PlotPoint(680.0, 320.0)))
        assertFalse(geometry.contains(PlotPoint(79.0, 20.0))); assertFalse(geometry.contains(PlotPoint(681.0, 20.0)))
        assertFalse(geometry.contains(PlotPoint(80.0, 19.0))); assertFalse(geometry.contains(PlotPoint(80.0, 321.0)))
    }
    @Test fun twoFingerTransformsNormalizeAgainstTheSameRectangle() {
        val motion = geometry.transform(PlotPoint(380.0, 170.0), PlotPoint(60.0, -30.0), 2.0, 3.0)
        assertEquals(PlotGestureCoordinates.Transform(.5, .5, 2.0, 3.0, .1, -.1), motion)
        val beyond = geometry.transform(PlotPoint(900.0, 0.0), PlotPoint(0.0, 0.0), 1.0, 1.0)
        assertEquals(1.0, beyond.anchorX, 0.0); assertEquals(0.0, beyond.anchorY, 0.0)
    }
    @Test fun logarithmicFrequencyAndCollapsedTransitionFramesKeepFiniteMapping() {
        val logarithmic = PlotGestureCoordinates(PlotViewport(viewport.x, AxisWindow(10.0, 1000.0)), 600.0, 300.0, 80.0, 20.0, true)
        assertEquals(100.0, logarithmic.data(PlotPoint(380.0, 170.0)).y, 1e-10)
        val collapsed = PlotGestureCoordinates(viewport, -2.0, 0.0, 80.0, 20.0, false)
        assertFalse(collapsed.contains(PlotPoint(80.0, 20.0)))
        assertTrue(collapsed.data(PlotPoint(80.0, 20.0)).x.isFinite())
    }
}
