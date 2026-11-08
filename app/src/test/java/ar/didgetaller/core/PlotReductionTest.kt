package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class PlotReductionTest {
    private val level = AudioLevel(1000, 1000, 100, floatArrayOf(.1f, .9f, .2f, .4f, .8f, .3f, .2f, .1f, .6f, .5f))

    @Test fun levelPeaksKeepSourceTimeAndViewportOffsetWithoutInventingPitch() {
        val original = level.rms.copyOf()
        assertArrayEquals(floatArrayOf(.9f, .9f, .8f, .8f, .6f), PlotReduction.levelPixels(level, AxisWindow(0.0, 1000.0), 5f), 0f)
        assertArrayEquals(floatArrayOf(.2f, .4f, .8f, .8f), PlotReduction.levelPixels(level, AxisWindow(200.0, 600.0), 4f), 0f)
        assertArrayEquals(original, level.rms, 0f)
    }

    @Test fun collapsedAndEmptyViewsProduceNoInvalidGeometry() {
        val window = AxisWindow(0.0, 1000.0)
        assertTrue(PlotReduction.levelPixels(null, window, 100f).isEmpty())
        assertTrue(PlotReduction.levelPixels(level, window, 0f).isEmpty())
        assertArrayEquals(FloatArray(3), PlotReduction.levelPixels(level, AxisWindow(2000.0, 3000.0), 3f), 0f)
        assertTrue(PlotReduction.spectrum(null, window, 100).isEmpty())
        assertTrue(PlotReduction.spectrum(PowerSpectrum(1000, 100, doubleArrayOf()), window, 100).isEmpty())
    }

    @Test fun zoomedSpectrumRetainsExactBinsAndNeverMutatesMeasuredDensity() {
        val density = DoubleArray(51) { it.toDouble() }; val original = density.copyOf()
        val spectrum = PowerSpectrum(1000, 100, density)
        val points = PlotReduction.spectrum(spectrum, AxisWindow(100.0, 130.0), 10)
        assertEquals(listOf(100.0 to 10.0, 110.0 to 11.0, 120.0 to 12.0, 130.0 to 13.0), points)
        assertArrayEquals(original, density, 0.0)
    }

    @Test fun narrowSpectrumRetainsCellMaximaAndUsesTheActualPlotColumns() {
        val spectrum = PowerSpectrum(1000, 100, DoubleArray(51) { if (it == 11) 10.0 else if (it == 35) 20.0 else if (it == 50) 30.0 else 0.0 })
        assertEquals(listOf(0.0 to 10.0, 250.0 to 20.0, 500.0 to 30.0), PlotReduction.spectrum(spectrum, AxisWindow(0.0, 500.0), 3))
        val wider = PlotReduction.spectrum(spectrum, AxisWindow(0.0, 500.0), 20)
        assertEquals(20, wider.size); assertEquals(30.0, wider.maxOf { it.second }, 0.0)
        assertEquals(2, PlotReduction.spectrum(spectrum, AxisWindow(0.0, 500.0), 1).size)
        assertTrue(PlotReduction.spectrum(spectrum, AxisWindow(0.0, 500.0), 0).isEmpty())
    }

    @Test fun viewportLevelNeverBorrowsALouderPeakFromFutureAudio() {
        val audio = AudioLevel(1000, 1000, 100, FloatArray(10) { if (it == 8) 1f else .1f })
        assertArrayEquals(FloatArray(4) { .1f }, PlotReduction.levelPixels(audio, AxisWindow(200.0, 600.0), 4f), 0f)
    }

    @Test fun binBudgetBoundaryUsesExactBinsOnlyWhenTheyFit() {
        val spectrum = PowerSpectrum(1000, 100, DoubleArray(51) { it.toDouble() })
        val window = AxisWindow(100.0, 130.0)
        assertEquals(listOf(100.0 to 11.0, 115.0 to 12.0, 130.0 to 13.0), PlotReduction.spectrum(spectrum, window, 3))
        assertEquals(listOf(100.0 to 10.0, 110.0 to 11.0, 120.0 to 12.0, 130.0 to 13.0), PlotReduction.spectrum(spectrum, window, 4))
    }
}
