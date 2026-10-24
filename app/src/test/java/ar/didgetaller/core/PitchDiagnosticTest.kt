package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PitchDiagnosticTest {
    @Test fun quietComparisonCannotBorrowEnergyFromAnUncomparedAttack(){
        val pcm=ShortArray(16384){i ->
            val hz=if(i<11000)51.0 else 140.0
            val amplitude=if(i<11000)24.0 else 8000.0
            (amplitude*sin(2*PI*hz*i/44100)).toInt().toShort()
        }
        var detail:PitchDetector.Diagnostic?=null
        val reading=PitchDetector.analyzeWithDiagnostics(pcm,44100,35,140,false){detail=it}
        assertTrue("The whole window contains audible energy",reading.rms>.005)
        assertEquals("A quiet leading comparison must not become an accepted note",0.0,reading.hz,0.0)
        assertEquals("low_comparison_amplitude",detail!!.reason)
        assertNotNull("Retain the rejected diagnostic hypothesis",detail!!.candidateHz)
    }

    @Test fun audibleLowNotesAndBriefAttacksRemainAccepted(){
        for(hz in listOf(35.0,70.0,140.0)){
            val pcm=ShortArray(16384){i -> (4000*sin(2*PI*hz*i/44100)).toInt().toShort()}
            assertEquals(hz,PitchDetector.analyze(pcm,includeVisuals=false).hz,1.1)
        }
        val released=ShortArray(16384){i -> if(i<11000)(4000*sin(2*PI*70*i/44100)).toInt().toShort() else 0}
        assertEquals(70.0,PitchDetector.analyze(released,includeVisuals=false).hz,1.0)
    }
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
