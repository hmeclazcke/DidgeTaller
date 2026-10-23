package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Explicit opt-in, read-only. The fixture is never bundled with the app or repository. */
class RealSavedPitchEvidenceTest {
    @Test fun separateCandidatesPreserveUnresolvedPrivateAudioAndBaselineDecisions() {
        val directory=System.getenv("DIDGE_TALLER_FIXTURES")
        assumeTrue("Private audio is optional",!directory.isNullOrBlank())
        val file=File(directory!!,"Sesion_2026-10-21_19-17.wav")
        fun hash()=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it)}
        val expected="7e877517a57bff4f5222d8d73dbc54494768fc208e14e24530ae65c9ca13eeb5"
        assertEquals(expected,hash())
        Pcm16WavReader(file).use {source->
            val normal=SavedPitchEvidence.inspect(source,5108,InstrumentProfile.DIDGERIDOO)
            assertTrue(normal.acceptedHz!! in 35.0..38.0)
            assertTrue(normal.families.any {it in 68.0..71.0})
            val vocal=SavedPitchEvidence.inspect(source,13839,InstrumentProfile.DIDGERIDOO)
            assertEquals(SavedPitchEvidence.Presence.ENERGY_PRESENT,vocal.presence)
            assertNull(vocal.acceptedHz)
            assertTrue(vocal.alternative!!.hz in 33.0..34.5)
            assertTrue(vocal.families.any {it in 66.0..69.0})
            val pause=SavedPitchEvidence.inspect(source,8452,InstrumentProfile.DIDGERIDOO)
            assertEquals(SavedPitchEvidence.Presence.BELOW_THRESHOLD,pause.presence)
            assertNull(pause.acceptedHz);assertNull(pause.alternative);assertTrue(pause.families.isEmpty())
        }
        assertEquals(expected,hash())
    }
}
