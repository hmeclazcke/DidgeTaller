package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class OfflineStatisticsTest {
    private fun source(rate:Int=44100,frames:Long=2000)=object:MonoPcmSource {
        override val sampleRate=rate
        override val frameCount=frames
        override fun read(startFrame:Long,destination:ShortArray):Int=error("Statistics must not read audio")
    }
    private val settings=WavAnalysis.Settings(4096,2048,maxPitchHz=140)
    private val spectrum=PowerSpectrum(44100,4096,DoubleArray(2049))
    private fun result(points:List<SessionAnalysis.Point>,config:WavAnalysis.Settings=settings)=
        OfflineStatistics.result(RecordingWindow.of(source(),100,1100,config),config,spectrum,7,points)

    @Test fun summariesUseAcceptedPointsAndRetainAbsoluteRecordingMetadata() {
        val points=listOf(SessionAnalysis.Point(1,70.0,.8),SessionAnalysis.Point(2,70.1,.9),
            SessionAnalysis.Point(3,69.9,1.0),SessionAnalysis.Point(4,90.0,.95),
            SessionAnalysis.Point(5,null,0.0),SessionAnalysis.Point(6,200.0,.9))
        val actual=result(points)
        assertEquals(100L,actual.startFrame);assertEquals(1100L,actual.endFrame)
        assertEquals(44100,actual.sampleRate);assertEquals(7,actual.windows)
        assertEquals(1000.0/44100,actual.durationSeconds,0.0)
        assertSame(spectrum,actual.spectrum);assertEquals(points,actual.timeline)
        assertEquals(70.05,actual.medianValidHz!!,1e-10)
        assertEquals(75.0,actual.meanValidHz!!,1e-10)
        assertEquals(4.0/6,actual.validWindowFraction,0.0)
        assertEquals(.925,actual.medianPeriodicityConfidence!!,1e-10)
        val summary=checkNotNull(actual.summary)
        assertEquals(70.0,summary.typicalHz,1e-10);assertEquals(3,summary.validPoints)
        assertEquals(6,summary.totalPoints)
    }
    @Test fun absentOrRejectedEvidenceDoesNotInventSummaryStatistics() {
        for(points in listOf(emptyList(),listOf(SessionAnalysis.Point(1,70.0,.69),SessionAnalysis.Point(2,null,.9)))) {
            val actual=result(points)
            assertNull(actual.summary);assertNull(actual.medianValidHz);assertNull(actual.meanValidHz)
            assertNull(actual.medianPeriodicityConfidence);assertEquals(0.0,actual.validWindowFraction,0.0)
        }
    }
    @Test fun fluteRangeGuardIncludesOnlyTheEstablishedHalfPercentBoundary() {
        val config=settings.copy(minPitchHz=100,maxPitchHz=600,instrument=InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        val points=listOf(99.6,602.4,99.4,603.6).mapIndexed { i,hz->SessionAnalysis.Point(i.toLong(),hz,.9) }
        val actual=result(points,config)
        assertEquals(351.0,actual.medianValidHz!!,1e-10);assertEquals(351.0,actual.meanValidHz!!,1e-10)
        assertEquals(.5,actual.validWindowFraction,0.0);assertEquals(.9,actual.medianPeriodicityConfidence!!,0.0)
    }
    @Test fun sourceRateAndFrameBudgetBoundariesAreInclusiveAndValidatedBeforeIo() {
        for(rate in listOf(8000,192000))assertEquals(2L,RecordingWindow.of(source(rate),0,2,settings).end)
        for(rate in listOf(7999,192001))assertThrows(IllegalArgumentException::class.java){RecordingWindow.of(source(rate),0,2,settings)}
        for((start,end) in listOf(-1L to 2L,0L to 1L,0L to 2001L,1999L to 2000L))
            assertThrows(IllegalArgumentException::class.java){RecordingWindow.of(source(),start,end,settings)}
        val budget=settings.copy(maxWindows=1)
        assertEquals(2048L,RecordingWindow.of(source(frames=4096),0,2048,budget).end)
        assertThrows(IllegalArgumentException::class.java){RecordingWindow.of(source(frames=4096),0,2049,budget)}
    }
}
