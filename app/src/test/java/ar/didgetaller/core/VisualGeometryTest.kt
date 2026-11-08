package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class VisualGeometryTest {
    private fun projection(x: AxisWindow = AxisWindow(0.0, 1000.0), y: AxisWindow = AxisWindow(-1.0, 1.0),
                           width: Double = 1000.0, height: Double = 200.0, log: Boolean = false) =
        PlotProjection(PlotViewport(x, y), width, height, logY = log)
    private fun tile(log: Boolean = false) = AudioVisuals.Spectrogram(100, 500, 1000, 100.0, 400.0,
        log, 2, 2, floatArrayOf(1f, 2f, 3f, 4f), 4.0, 1024, 512)
    private fun assertPlacement(expected: RasterPlacement, actual: RasterPlacement?) {
        assertNotNull(actual)
        assertEquals(expected.origin.x, actual!!.origin.x, 1e-10)
        assertEquals(expected.origin.y, actual.origin.y, 1e-10)
        assertEquals(expected.width, actual.width, 1e-10)
        assertEquals(expected.height, actual.height, 1e-10)
    }

    @Test fun rawSamplesUseTheirActualFramesAndTheExclusiveEndIsNotAnotherSample() {
        val low = floatArrayOf(-1f, 0f, 1f, .5f); val original = low.copyOf()
        val wave = AudioVisuals.Wave(100, 104, 1000, low, low.copyOf(), true)
        val drawing = WaveGeometry.prepare(wave, projection(AxisWindow(100.0, 104.0), width = 400.0))
        assertEquals(WaveGeometry.Samples(listOf(PlotPoint(0.0, 200.0), PlotPoint(100.0, 100.0),
            PlotPoint(200.0, 0.0), PlotPoint(300.0, 50.0))), drawing)
        assertArrayEquals(original, wave.min, 0f)
    }

    @Test fun nonIntegralSampleMillisecondsDoNotCollapseOrStretchToTheWindowEnd() {
        val values = floatArrayOf(0f, 0f, 0f, 0f)
        val wave = AudioVisuals.Wave(44100, 44104, 44100, values, values.copyOf(), true)
        val drawing = WaveGeometry.prepare(wave,
            projection(AxisWindow(1000.0, 1000.0 + 4000.0 / 44100), width = 400.0)) as WaveGeometry.Samples
        for ((index, point) in drawing.points.withIndex()) {
            assertEquals(index * 100.0, point.x, 1e-8)
            assertEquals(100.0, point.y, 0.0)
        }
    }

    @Test fun envelopesUseBinCentersAndKeepMinMaxAmplitudeIndependentOfTime() {
        val wave = AudioVisuals.Wave(100, 500, 1000, floatArrayOf(-.5f, -1f), floatArrayOf(.5f, .75f), false)
        assertEquals(WaveGeometry.Envelope(listOf(WaveGeometry.Column(200.0, 50.0, 150.0),
            WaveGeometry.Column(400.0, 25.0, 200.0)), 200.0), WaveGeometry.prepare(wave, projection()))
        val zoomed = WaveGeometry.prepare(wave, projection(AxisWindow(250.0, 350.0)))
        assertEquals(WaveGeometry.Envelope(listOf(WaveGeometry.Column(-500.0, 50.0, 150.0),
            WaveGeometry.Column(1500.0, 25.0, 200.0)), 2000.0), zoomed)
    }

    @Test fun emptyEnvelopesAndSubpixelBinsNeverProduceNonfiniteWidths() {
        val wave = AudioVisuals.Wave(100, 500, 1000, floatArrayOf(0f, 0f), floatArrayOf(0f, 0f), false)
        val small = WaveGeometry.prepare(wave, projection(width = 1.0)) as WaveGeometry.Envelope
        assertEquals(1.0, small.strokeWidth, 0.0)
        assertEquals(WaveGeometry.Envelope(emptyList(), 1.0),
            WaveGeometry.prepare(wave.copy(min = floatArrayOf(), max = floatArrayOf()), projection()))
        assertThrows(IllegalArgumentException::class.java) { WaveGeometry.prepare(wave.copy(rate = 0), projection()) }
        assertThrows(IllegalArgumentException::class.java) { WaveGeometry.prepare(wave.copy(max = floatArrayOf()), projection()) }
    }

    @Test fun completedRasterRetainsItsOwnBoundsDuringZoomAndPan() {
        val source = tile(); val original = source.power.copyOf()
        assertPlacement(RasterPlacement(PlotPoint(100.0, 20.0), 400.0, 60.0),
            RasterPlacement.of(source, projection(y = AxisWindow(0.0, 500.0), height = 100.0)))
        assertPlacement(RasterPlacement(PlotPoint(-300.0, -50.0), 1200.0, 150.0),
            RasterPlacement.of(source, projection(AxisWindow(200.0, 400.0), AxisWindow(100.0, 300.0), 600.0, 100.0)))
        assertArrayEquals(original, source.power, 0f)
    }

    @Test fun logarithmicPlacementUsesFrequencySpaceAndRejectsIncompatibleOrDegenerateTiles() {
        val log = projection(y = AxisWindow(100.0, 1600.0), height = 400.0, log = true)
        val placement = RasterPlacement.of(tile(true), log)!!
        assertEquals(200.0, placement.origin.y, 1e-10)
        assertEquals(200.0, placement.height, 1e-10)
        assertNull(RasterPlacement.of(tile(false), log))
        assertNull(RasterPlacement.of(tile(true), projection(y = AxisWindow(0.0, 500.0))))
        assertNull(RasterPlacement.of(tile(true).copy(lowHz = 0.0), log))
        assertNull(RasterPlacement.of(tile().copy(start = 500, end = 100), projection()))
        assertNull(RasterPlacement.of(tile().copy(lowHz = 500.0, highHz = 100.0), projection()))
    }

    @Test fun zeroAreaAndNonfiniteCoordinatesAreNeverPassedToTheRasterizer() {
        assertNull(RasterPlacement.of(tile().copy(start = 100, end = 100), projection()))
        assertNull(RasterPlacement.of(tile().copy(lowHz = 200.0, highHz = 200.0), projection()))
        assertNull(RasterPlacement.of(tile().copy(highHz = Double.NaN), projection()))
        assertNull(RasterPlacement.of(tile().copy(rate = 0), projection()))
        val extreme = projection(AxisWindow(0.0, 1e-300), width = Double.MAX_VALUE)
        assertNull(RasterPlacement.of(tile().copy(start = 0), extreme))
    }

    @Test fun largestFiniteExtentsRemainFiniteRatherThanBeingConfusedWithInfinity() {
        val exact = projection(AxisWindow(100.0, 500.0), AxisWindow(100.0, 400.0), Double.MAX_VALUE, Double.MAX_VALUE)
        val placement = RasterPlacement.of(tile(), exact)!!
        assertEquals(PlotPoint(0.0, 0.0), placement.origin)
        assertEquals(Double.MAX_VALUE, placement.width, 0.0)
        assertEquals(Double.MAX_VALUE, placement.height, 0.0)
    }
}
