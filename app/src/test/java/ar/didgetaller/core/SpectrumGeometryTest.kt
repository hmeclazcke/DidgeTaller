package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class SpectrumGeometryTest {
    private fun analysis(power: DoubleArray = DoubleArray(51) { if (it == 10) 1.0 else .01 },
                         harmonics: HarmonicAnalysis.Result = HarmonicAnalysis.Result(100.0,
                             listOf(HarmonicAnalysis.Peak(100.0, 0.0, 1, 0.0),
                                 HarmonicAnalysis.Peak(150.0, -20.0, null, null)), null)) =
        WavAnalysis.Result(0, 1000, 1000, 1, PowerSpectrum(1000, 100, power), harmonics,
            emptyList(), null, null, null, 0.0, null)
    private val projection = PlotProjection(PlotViewport(AxisWindow(0.0, 500.0), AxisWindow(-60.0, 0.0)), 500.0, 120.0)

    @Test fun spectrumKeepsMeasuredBinPeaksRelativePowerAndHarmonicCandidates() {
        val input = analysis(); val original = input.spectrum.density.copyOf()
        val geometry = checkNotNull(SpectrumGeometry.prepare(input, projection, 500))
        assertEquals(51, geometry.curve.size)
        assertEquals(PlotPoint(100.0, 0.0), geometry.curve[10])
        assertEquals(40.0, geometry.curve[0].y, 1e-10)
        assertEquals(listOf(100.0, 200.0, 300.0, 400.0, 500.0), geometry.guides)
        assertEquals(150.0, geometry.peaks[1].position.x, 1e-10)
        assertEquals(40.0, geometry.peaks[1].position.y, 1e-10)
        assertNull(geometry.peaks[1].order); assertEquals(1, geometry.peaks[0].order)
        assertArrayEquals(original, input.spectrum.density, 0.0)
    }

    @Test fun zoomAndPanKeepTheFullRecordedSpectrumInsteadOfStretchingIt() {
        val view = projection.copy(viewport = PlotViewport(AxisWindow(125.0, 225.0), AxisWindow(-60.0, 0.0)))
        val geometry = checkNotNull(SpectrumGeometry.prepare(analysis(), view, 100))
        assertEquals(listOf(375.0), geometry.guides)
        assertEquals(listOf(150.0), geometry.peaks.map { view.data(it.position.x, it.position.y).x })
        assertEquals(125.0, geometry.peaks.single().position.x, 1e-10)
        assertTrue(geometry.curve.first().x >= 0); assertTrue(geometry.curve.last().x <= view.width)
    }

    @Test fun absentOrSilentSpectrumHasNoInventedCurveOrHarmonicGrid() {
        assertNull(SpectrumGeometry.prepare(null, projection, 100))
        assertNull(SpectrumGeometry.prepare(analysis(DoubleArray(51)), projection, 100))
        assertNull(SpectrumGeometry.prepare(analysis(DoubleArray(51) { 1e-16 }), projection, 100))
        val unassigned = analysis(harmonics = HarmonicAnalysis.Result(null, emptyList(), null))
        assertTrue(checkNotNull(SpectrumGeometry.prepare(unassigned, projection, 100)).guides.isEmpty())
    }

    @Test fun guidesRespectTheExisting128OrderBudgetAndNeverCreateNotes() {
        val input = analysis(harmonics = HarmonicAnalysis.Result(1.0, emptyList(), null))
        val geometry = checkNotNull(SpectrumGeometry.prepare(input, projection, 100))
        assertEquals(128, geometry.guides.size); assertEquals(128.0, geometry.guides.last(), 0.0)
        assertTrue(geometry.peaks.isEmpty())
    }

    @Test fun relativeSpectrumIsInvariantUnderMeasuredPowerGainAndIncludesVisibleBoundaryPeaks() {
        val normal = analysis()
        val louder = analysis(normal.spectrum.density.map { it * 100 }.toDoubleArray())
        val first = checkNotNull(SpectrumGeometry.prepare(normal, projection, 100))
        val second = checkNotNull(SpectrumGeometry.prepare(louder, projection, 100))
        first.curve.zip(second.curve).forEach { (a, b) ->
            assertEquals(a.x, b.x, 1e-10); assertEquals(a.y, b.y, 1e-10)
        }
        val edges = analysis(harmonics = HarmonicAnalysis.Result(100.0,
            listOf(HarmonicAnalysis.Peak(0.0, -20.0, null, null), HarmonicAnalysis.Peak(500.0, 0.0, 5, 0.0)), null))
        assertEquals(listOf(0.0, 500.0), checkNotNull(SpectrumGeometry.prepare(edges, projection, 100)).peaks.map { it.position.x })
        assertNull(SpectrumGeometry.prepare(analysis(doubleArrayOf()), projection, 100))
    }
}
