package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

class AcousticDiagnosticsTest {
    private fun stream(pcm:ShortArray):PitchDetector.Reading {
        val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=false)
        var result:PitchDetector.Reading?=null
        pcm.asList().chunked(4096).forEach {chunk->stream.append(chunk.toShortArray())?.let {result=it}}
        return requireNotNull(result)
    }

    @Test fun normalStreamPreservesRejectedCandidatesWithoutPromotingThem() {
        val random=Random(326)
        val noise=ShortArray(20480){(random.nextInt(12000)-6000).toShort()}
        val reading=stream(noise)
        assertEquals(0.0,reading.hz,0.0)
        assertTrue(reading.rms>=.005)
        assertNotNull("Production stream must carry rejection evidence",reading.diagnostic)
        assertEquals("low_periodicity",reading.diagnostic!!.reason)
        assertNotNull(reading.diagnostic.candidateHz)
        assertNull(LivePitch.next(LivePitch.State(),reading,InstrumentProfile.DIDGERIDOO).storedHz)
    }

    @Test fun silencePeriodicDroneAndEnergeticNoiseHaveSeparateStates() {
        val history=AcousticDiagnostics()
        val random=Random(326)
        val frames=listOf(
            ShortArray(20480),
            ShortArray(20480){(12000*sin(2*PI*70*it/44100)).toInt().toShort()},
            ShortArray(20480){(random.nextInt(12000)-6000).toShort()}
        )
        var live=LivePitch.State()
        val points=frames.mapIndexed {i,pcm->
            val reading=stream(pcm)
            val plain=PitchDetector.analyze(pcm.copyOfRange(4096,20480),includeVisuals=false)
            assertEquals(plain,reading.copy(diagnostic=null))
            live=LivePitch.next(live,reading,InstrumentProfile.DIDGERIDOO)
            history.record(i*2000L,reading,live,InstrumentProfile.DIDGERIDOO)
            SessionAnalysis.Point(i*2000L,live.storedHz,reading.confidence)
        }
        assertEquals(listOf(AcousticDiagnostics.Signal.NO_RELEVANT_SIGNAL,AcousticDiagnostics.Signal.ACCEPTED_PITCH,
            AcousticDiagnostics.Signal.UNCERTAIN_PITCH),history.samples.map {it.signal})
        assertEquals(listOf("low_amplitude",null,"low_periodicity"),history.samples.map {it.rejectionReason})
        assertEquals(70.0,points[1].hz!!,.1)
        assertNull(points[0].hz);assertNull(points[2].hz)
        val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
        assertEquals(1,trace.size);assertSame(points[1],trace.single().point);assertNull(trace.single().previous)
    }

    @Test fun diagnosticEvidenceDoesNotBypassFluteAcceptance() {
        val reading=PitchDetector.Reading(440.0,.79,.04,diagnostic=PitchDetector.Diagnostic(440.0,"accepted"))
        val live=LivePitch.next(LivePitch.State(),reading,InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        val sample=AcousticDiagnostics().record(0,reading,live,InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        assertEquals(AcousticDiagnostics.Signal.UNCERTAIN_PITCH,sample.signal)
        assertEquals("accepted",sample.detectorReason);assertEquals("profile_gate",sample.rejectionReason)
        assertEquals(440.0,sample.candidateHz!!,0.0);assertNull(live.storedHz)
    }

    @Test fun evidenceHasAFixedRetentionLimitAndClearDoesNotMutateSnapshots() {
        val reading=stream(ShortArray(20480))
        val history=AcousticDiagnostics()
        repeat(AcousticDiagnostics.MAX_SAMPLES+9){history.record(it.toLong(),reading,LivePitch.State(),InstrumentProfile.DIDGERIDOO)}
        val snapshot=history.samples
        assertEquals(AcousticDiagnostics.MAX_SAMPLES,snapshot.size)
        assertEquals(9L,snapshot.first().observedAtMs)
        assertEquals((AcousticDiagnostics.MAX_SAMPLES+8).toLong(),snapshot.last().observedAtMs)
        history.clear();assertTrue(history.samples.isEmpty())
        assertEquals(AcousticDiagnostics.MAX_SAMPLES,snapshot.size)
    }

    @Test fun observerAndNormalConsumerReceiveTheSameDiagnostic() {
        var observed:PitchDetector.Diagnostic?=null
        val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=false,observe={observed=it})
        var reading:PitchDetector.Reading?=null
        repeat(5){reading=stream.append(ShortArray(4096))}
        assertSame(observed,reading!!.diagnostic)
        assertEquals("low_amplitude",observed!!.reason)
    }
}
