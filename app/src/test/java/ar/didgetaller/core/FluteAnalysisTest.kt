package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import kotlin.math.*

class FluteAnalysisTest {
    private fun signal(hz:Double,n:Int=8192,weak:Boolean=false,noise:Double=0.0):ShortArray {
        val rng=Random(41)
        return ShortArray(n){i->
            val t=2*PI*hz*i/44100
            val v=if(weak).13*sin(t)+.70*sin(2*t)+.10*sin(3*t) else .5*sin(t)+.20*sin(2*t)+.08*sin(3*t)
            ((v+noise*(rng.nextDouble()*2-1))*16000).toInt().coerceIn(-32768,32767).toShort()
        }
    }
    private fun reading(hz:Double,weak:Boolean=false,noise:Double=0.0)=PitchDetector.analyze(signal(hz,weak=weak,noise=noise),44100,200,1600,false)
    @Test fun fluteRegistersAndOctavesAreCorrect(){
        listOf(200.0 to "G3",261.63 to "C4",329.63 to "E4",392.0 to "G4",440.0 to "A4",659.26 to "E5",880.0 to "A5",1600.0 to "G6").forEach {(hz,note)->
            val r=reading(hz);assertEquals("frequency $hz",hz,r.hz,hz*.003)
            assertEquals(note,Music.pitch(r.hz)!!.note);assertTrue(r.confidence>.90)
        }
    }
    @Test fun louderSecondHarmonicAndAirDoNotSystematicallyDoublePitch(){
        for(hz in listOf(261.63,330.0,392.0,440.0,659.26,880.0)){
            val r=reading(hz,weak=true,noise=.04)
            assertEquals("weak H1 $hz",hz,r.hz,hz*.005);assertTrue(r.confidence>.80)
        }
    }
    @Test fun silenceNoiseAndPausesNeverReusePreviousNote(){
        assertTrue(reading(440.0).hz>0)
        assertEquals(0.0,PitchDetector.analyze(ShortArray(8192),44100,200,1600,false).hz,0.0)
        val rng=Random(39);val air=ShortArray(8192){((rng.nextDouble()*2-1)*5000).toInt().toShort()}
        val r=PitchDetector.analyze(air,44100,200,1600,false)
        assertEquals(0.0,r.hz,0.0);assertTrue(r.confidence<.65)
        assertEquals(392.0,reading(392.0).hz,1.0)
    }
    @Test fun didgeridooLowRegisterRegression(){
        for(hz in listOf(35.5,50.0,65.4,69.3,70.0,98.0,125.0,210.0)){
            val r=PitchDetector.analyze(signal(hz,16384,noise=.02),44100,35,220,false)
            assertEquals(hz,r.hz,.4);assertTrue(r.confidence>.90)
        }
    }
    @Test fun profileRangesAndAutoScaleUseActualSessionInstrument(){
        val flute=InstrumentProfile.NATIVE_AMERICAN_FLUTE
        assertEquals(200,flute.minHz);assertEquals(1600,flute.maxHz)
        assertEquals(InstrumentProfile.DIDGERIDOO,InstrumentProfile.fromStored(null))
        assertEquals(InstrumentProfile.DIDGERIDOO,InstrumentProfile.fromStored("unknown"))
        val points=listOf(350.0,440.0,700.0).mapIndexed {i,hz->SessionAnalysis.Point(i*200L,hz,.98)}
        assertEquals(0,PlotScales.pitch(points).valid)
        val range=PlotScales.pitch(points,instrument=flute)
        assertEquals(3,range.valid);assertTrue(range.window.low<350);assertTrue(range.window.high>700)
        assertEquals(2000.0,flute.spectralMaxHz,0.0)
    }
    @Test fun melodicReanalysisPreservesNotesAndSilenceWithoutChangingSpectrumResolution(){
        val hz=listOf(261.63,329.63,392.0,0.0,440.0,659.26,880.0)
        val segment=22050
        val pcm=hz.flatMap {f->if(f==0.0)ShortArray(segment).toList() else signal(f,segment,noise=.025).toList()}.toShortArray()
        val source=object:MonoPcmSource {
            override val sampleRate=44100
            override val frameCount=pcm.size.toLong()
            override fun read(startFrame:Long,destination:ShortArray):Int {
                val count=min(destination.size,pcm.size-startFrame.toInt()).coerceAtLeast(0)
                pcm.copyInto(destination,0,startFrame.toInt(),startFrame.toInt()+count);return count
            }
        }
        val result=WavAnalysis.analyze(source,settings=WavAnalysis.Settings(minPitchHz=200,maxPitchHz=1600,instrument=InstrumentProfile.NATIVE_AMERICAN_FLUTE))
        assertEquals(32768,result.spectrum.fftSize)
        assertTrue(result.timeline.size>30);assertNull(result.summary)
        hz.forEachIndexed {i,f->
            val middle=result.timeline.filter {it.timeMs in (i*500+150L)..(i*500+350L)}
            assertTrue(middle.isNotEmpty())
            if(f==0.0)assertTrue(middle.all {it.hz==null})
            else assertTrue("segment $i",middle.any {it.hz!=null && abs(it.hz-f)<f*.005})
        }
        assertTrue(result.harmonics.peaks.all {it.order==null}) // No fixed F0 invented for a melody.
    }
}
