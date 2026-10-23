package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException
import kotlin.math.*

class SavedPitchEvidenceTest {
    private fun source(vararg tones:Pair<Double,Double>,seconds:Int=2):MonoPcmSource {
        val pcm=ShortArray(44100*seconds){i->
            (tones.sumOf {(hz,a)->a*sin(2*PI*hz*i/44100)}*32767).roundToInt().toShort()
        }
        return object:MonoPcmSource {
            override val sampleRate=44100
            override val frameCount=pcm.size.toLong()
            override fun read(startFrame:Long,destination:ShortArray):Int {
                val count=min(destination.size.toLong(),frameCount-startFrame).toInt()
                pcm.copyInto(destination,0,startFrame.toInt(),startFrame.toInt()+count)
                return count
            }
        }
    }
    private fun inspect(source:MonoPcmSource,ms:Long=700,profile:InstrumentProfile=InstrumentProfile.DIDGERIDOO)=
        SavedPitchEvidence.inspect(source,ms,profile)

    @Test fun acceptedValuesRemainTheActualProfileDecisionForBothInstruments() {
        for((profile,frequencies) in listOf(
            InstrumentProfile.DIDGERIDOO to listOf(35.0,70.0,140.0),
            InstrumentProfile.NATIVE_AMERICAN_FLUTE to listOf(220.0,440.0,880.0,1600.0))) {
            for(hz in frequencies){
                val input=source(hz to .2);val evidence=inspect(input,profile=profile)
                assertTrue(abs(1200*log2(evidence.acceptedHz!!/hz))<5)
                val pcm=ShortArray((evidence.endFrame-evidence.startFrame).toInt())
                input.read(evidence.startFrame,pcm)
                val raw=PitchDetector.analyze(pcm,44100,profile.minHz,profile.initialMaxHz,false)
                assertEquals(LivePitch.next(LivePitch.State(),raw,profile).storedHz,evidence.acceptedHz)
                if(profile==InstrumentProfile.NATIVE_AMERICAN_FLUTE)assertNull(evidence.alternative)
            }
        }
    }

    @Test fun competingFamilyIsSeparateFromAcceptedWholeMixturePeriodicity() {
        val evidence=inspect(source(69.567 to .12,139.134 to .0377,208.701 to .01495,182.221 to .0918))
        assertTrue(evidence.acceptedHz!! in 34.0..38.0)
        assertTrue(evidence.families.any {it in 68.0..71.0})
        assertTrue(evidence.families.none {it in 48.0..58.0})
        assertEquals(SavedPitchEvidence.Presence.ENERGY_PRESENT,evidence.presence)
    }

    @Test fun missingLowFundamentalIsNotPromotedToItsVisibleSecondHarmonic() {
        val evidence=inspect(source(70.0 to .24,175.0 to .15))
        assertEquals(35.0,evidence.acceptedHz!!,.1)
        assertFalse(evidence.families.any {it in 68.0..72.0})
    }

    @Test fun energyWithoutAcceptedPitchIsDifferentFromLowSignal() {
        val random=java.util.Random(731)
        val input=object:MonoPcmSource {
            override val sampleRate=44100;override val frameCount=44100L
            override fun read(startFrame:Long,destination:ShortArray):Int {
                destination.indices.forEach {destination[it]=((random.nextDouble()*2-1)*6000).toInt().toShort()}
                return destination.size
            }
        }
        val noise=inspect(input)
        assertEquals(SavedPitchEvidence.Presence.ENERGY_PRESENT,noise.presence)
        assertNull(noise.acceptedHz)
        val silence=inspect(source())
        assertEquals(SavedPitchEvidence.Presence.BELOW_THRESHOLD,silence.presence)
        assertNull(silence.acceptedHz);assertNull(silence.alternative);assertTrue(silence.families.isEmpty())
    }

    @Test fun aNewNoteAndPauseNeverReuseThePreviousInspection() {
        val first=inspect(source(70.0 to .2))
        val changed=inspect(source(98.0 to .2))
        val pause=inspect(source())
        assertEquals(70.0,first.acceptedHz!!,.1)
        assertEquals(98.0,changed.acceptedHz!!,.1)
        assertNull(pause.acceptedHz)
    }

    @Test fun boundariesAreClampedWithoutPaddingAndShortInputIsNotDeclaredSilence() {
        val input=source(70.0 to .2)
        val first=inspect(input,0);val last=inspect(input,100000)
        assertEquals(0L,first.startFrame);assertEquals(input.frameCount,last.endFrame)
        val short=object:MonoPcmSource {
            override val sampleRate=44100;override val frameCount=100L
            override fun read(startFrame:Long,destination:ShortArray)=error("Insufficient input must not be analyzed")
        }
        assertEquals(SavedPitchEvidence.Presence.INSUFFICIENT_INPUT,inspect(short).presence)
    }

    @Test(expected=CancellationException::class) fun cancelledInspectionDoesNotReadAudio() {
        SavedPitchEvidence.inspect(source(70.0 to .2),700,InstrumentProfile.DIDGERIDOO){true}
    }
}
