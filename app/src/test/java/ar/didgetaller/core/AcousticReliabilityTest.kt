package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import kotlin.math.*

/** Feeds the same rolling window/cadence used by AudioRecord, with reproducible PCM. */
class AcousticReliabilityTest {
    private val didge=InstrumentProfile.DIDGERIDOO
    private fun pcm(hz:Double,weights:List<Double>,seconds:Double=.9,amplitude:Double=.65,noise:Double=.015):ShortArray {
        val rng=Random(325)
        val norm=weights.sum().coerceAtLeast(1.0)
        return ShortArray((44100*seconds).toInt()){i->
            val phase=2*PI*hz*i/44100
            val v=weights.mapIndexed {h,w->w*sin((h+1)*phase)}.sum()/norm
            ((v*amplitude+noise*(rng.nextDouble()*2-1))*25000).roundToInt().coerceIn(-32768,32767).toShort()
        }
    }
    private fun feed(stream:PitchStream,pcm:ShortArray,chunk:Int=4096)=
        pcm.asList().chunked(chunk).mapNotNull {stream.append(it.toShortArray())}

    @Test fun harmonicBalancesAndVowelLikeTimbresRetainPeriodicDrone(){
        // Spectral envelopes only: these are not physical recordings or a vocal identification test.
        val vowels=listOf(listOf(1.0,.5,.3),listOf(.08,1.0,.8),listOf(.04,.1,1.0,.8,.5),
            listOf(.04,.03,.1,.2,.8,1.0,.7,.4),listOf(.7,.3,.1),listOf(.4,.7,.6,.2))
        vowels.forEachIndexed {i,weights->
            val readings=feed(PitchStream(didge,includeVisuals=false),pcm(70.0,weights))
            assertTrue(readings.isNotEmpty())
            readings.forEach {r->assertEquals("balance $i",70.0,r.hz,.65);assertTrue(r.confidence>=.80)}
        }
    }
    @Test fun changingHarmonicsAndAmplitudeOnlyLoseAmbiguousTransitionWindows(){
        val stream=PitchStream(didge,includeVisuals=false)
        val blocks=listOf(listOf(1.0,.3,.2),listOf(.05,.2,1.0,.7,.4),listOf(.7,.4,.3))
        for((i,weights) in blocks.withIndex()){
            val results=feed(stream,pcm(71.0,weights,seconds=1.3,amplitude=if(i==1).16 else .65,noise=.008))
            assertTrue(results.takeLast(4).all {abs(it.hz-71.0)<.7 && it.confidence>=.8})
        }
    }
    @Test fun silenceAirAndResumeDoNotHoldOrInventANote(){
        val stream=PitchStream(didge,includeVisuals=false)
        assertTrue(feed(stream,pcm(70.0,listOf(1.0,.6))).last().hz>0)
        val silence=feed(stream,ShortArray(44100))
        assertTrue(silence.takeLast(3).all {it.hz==0.0 && it.rms==0.0})
        val noise=pcm(0.0,listOf(0.0),noise=.25)
        assertTrue(feed(stream,noise).takeLast(3).all {it.hz==0.0})
        assertEquals(70.0,feed(stream,pcm(70.0,listOf(.1,1.0,.7))).last().hz,.6)
    }
    @Test fun extendedDidgeRangeAllowsSupportedTootWithoutChangingDrone(){
        val stream=PitchStream(didge,maxHz=220,includeVisuals=false)
        for(hz in listOf(70.0,205.0,70.0)){
            val readings=feed(stream,pcm(hz,listOf(1.0,.4,.2)))
            assertTrue(readings.takeLast(3).all {abs(it.hz-hz)<1.0})
        }
        // Default 35–140Hz is intentionally narrower: never assert a 205Hz toot there.
        assertEquals(140,didge.initialMaxHz)
    }
    @Test fun fluteMelodyDominantHarmonicsAndSilenceRemainIndependent(){
        val flute=InstrumentProfile.NATIVE_AMERICAN_FLUTE
        val stream=PitchStream(flute,includeVisuals=false)
        for(hz in listOf(200.0,392.0,440.0,880.0,1600.0)){
            val results=feed(stream,pcm(hz,listOf(.15,1.0,.2),seconds=.55,noise=.01))
            assertTrue("flute $hz",results.takeLast(3).all {abs(it.hz-hz)<hz*.006 && it.confidence>=.8})
        }
        assertTrue(feed(stream,ShortArray(22050)).takeLast(3).all {it.hz==0.0})
        assertEquals(70.0,feed(PitchStream(didge,includeVisuals=false),pcm(70.0,listOf(1.0,.3))).last().hz,.5)
    }
    @Test fun windowWarmupCadenceAndPartialReadsMatchTheCaptureContract(){
        for(profile in InstrumentProfile.entries){
            val stream=PitchStream(profile,includeVisuals=false)
            val pcm=pcm(if(profile==didge)70.0 else 440.0,listOf(1.0,.3),seconds=1.0)
            val actual=feed(stream,pcm)
            val chunks=ceil(pcm.size/4096.0).toInt()
            assertEquals((chunks-profile.windowSamples/4096+1)/profile.analysisChunks,actual.size)
            val irregular=feed(PitchStream(profile,includeVisuals=false),pcm,3000)
            assertEquals(actual.last().hz,irregular.last().hz,.2)
        }
    }
}
