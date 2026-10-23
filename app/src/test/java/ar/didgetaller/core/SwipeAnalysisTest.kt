package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class SwipeAnalysisTest {
    private class Source(override val frameCount: Long, val silence: Boolean = false, val amplitude:Short=1000) : MonoPcmSource {
        override val sampleRate = 44100
        var largestRead = 0
        override fun read(startFrame: Long, destination: ShortArray): Int {
            largestRead = maxOf(largestRead, destination.size)
            val n = minOf(destination.size.toLong(), frameCount - startFrame).toInt()
            destination.fill(if (silence) 0 else amplitude)
            return n
        }
    }
    @Test fun alignedHypothesesRemainSeparateAndSilenceIsMissing() {
        val source = Source(44100 * 5L)
        val worker = SwipeAnalysis { _, count, _, hop, low, high ->
            assertEquals(35, low); assertEquals(140, high)
            DoubleArray((count + hop - 1) / hop) { 67.0 }
        }
        val curve = worker.analyze(source, InstrumentProfile.DIDGERIDOO)
        assertEquals(12288 * 1000.0 / 44100, curve.samples.first().timeMs, 1e-9)
        assertTrue(curve.samples.all { it.hz == 67.0 })
        assertTrue(curve.linked(8))
        assertTrue(source.largestRead <= 262144)
        assertTrue(worker.analyze(Source(source.frameCount, true), InstrumentProfile.DIDGERIDOO).samples.all { it.hz == null })
        assertTrue(worker.analyze(Source(source.frameCount, amplitude=10), InstrumentProfile.DIDGERIDOO).samples.all { it.hz == null })
    }
    @Test fun gapsAndRegisterChangesAreNeverJoined() {
        val curve = SwipeCurve(listOf(70.0, null, 70.0, 35.0, 35.1).mapIndexed { i, hz -> SwipeCurve.Sample(i * 100.0, hz) },100.0,35,140)
        assertFalse(curve.linked(1)); assertFalse(curve.linked(2)); assertFalse(curve.linked(3)); assertTrue(curve.linked(4))
        assertNull(curve.nearest(1000))
    }
    @Test(expected = CancellationException::class) fun cancellationDoesNotReturnAPartialCurve() {
        SwipeAnalysis { _, _, _, _, _, _ -> error("Must not run") }
            .analyze(Source(44100 * 10L),InstrumentProfile.DIDGERIDOO,cancelled={true})
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedRecordingsAreRejectedBeforeReading() {
        SwipeAnalysis { _, _, _, _, _, _ -> error("Must not run") }
            .analyze(Source(44100 * 10000L),InstrumentProfile.DIDGERIDOO)
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedFluteNeverCallsUnsafeKernel() {
        SwipeAnalysis { _, _, _, _, _, _ -> error("Must not run") }
            .analyze(Source(44100 * 5L),InstrumentProfile.NATIVE_AMERICAN_FLUTE)
    }
    @Test fun shortAudioHasNoFabricatedWindows() {
        val curve=SwipeAnalysis { _, _, _, _, _, _ -> error("Must not run") }
            .analyze(Source(12000),InstrumentProfile.DIDGERIDOO)
        assertTrue(curve.samples.isEmpty())
    }
    @Test fun disablingRestoresOriginalCameraAndRangeAfterComparisonGestures() {
        val state=ar.didgetaller.ui.ExplorerState()
        val points=listOf(SessionAnalysis.Point(100,70.0,.98),SessionAnalysis.Point(300,70.1,.98))
        state.extendedPitchRange=true
        state.cameras[ar.didgetaller.ui.AcousticPlot.PITCH]=PlotViewport(AxisWindow(100.0,1000.0),AxisWindow(68.0,72.0))
        val original=state.camera(5000,44100,points)
        state.configureComparisonRange(AxisWindow(33.25,147.0))
        assertTrue(state.camera(5000,44100,points).y.low<=35)
        state.reset(5000,44100,points)
        state.transform(5000,44100,points,.5,.5,2.0,2.0,.1,.1)
        state.configureComparisonRange(null)
        assertEquals(original,state.camera(5000,44100,points))
        assertTrue(state.extendedPitchRange)
    }
}
