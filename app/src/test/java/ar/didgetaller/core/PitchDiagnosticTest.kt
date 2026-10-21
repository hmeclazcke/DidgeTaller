package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PitchDiagnosticTest {
    @Test fun observationDoesNotChangeEstimatorAndExplainsRejection(){
        val pcm=ShortArray(16384){(12000*sin(2*PI*70*it/44100)).toInt().toShort()}
        var detail:PitchDetector.Diagnostic?=null
        val plain=PitchDetector.analyze(pcm,includeVisuals=false)
        val observed=PitchDetector.analyzeWithDiagnostics(pcm,44100,35,140,false){detail=it}
        assertEquals(plain,observed);assertEquals("accepted",detail!!.reason)
        assertEquals(plain.hz,detail!!.candidateHz!!,0.0)
        assertTrue(detail!!.minima.any {abs(it.hz-70)<1})
        PitchDetector.analyzeWithDiagnostics(ShortArray(16384),44100,35,140,false){detail=it}
        assertEquals("low_amplitude",detail!!.reason);assertNull(detail!!.candidateHz)
        val noise=java.util.Random(326)
        val air=ShortArray(16384){(noise.nextInt(12000)-6000).toShort()}
        val rejected=PitchDetector.analyzeWithDiagnostics(air,44100,35,140,false){detail=it}
        assertEquals(0.0,rejected.hz,0.0);assertEquals("low_periodicity",detail!!.reason)
        assertNotNull(detail!!.candidateHz)
    }
}
