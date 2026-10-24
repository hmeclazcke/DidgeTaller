package ar.didgetaller.core

import android.os.Build
import android.os.Debug
import android.os.SystemClock
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.*

class DroneComponentPerformanceTest {
    @Test fun fortySecondSavedAudioIsAnalyzedOnDemandWithinAReasonableBudget(){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val source=object:MonoPcmSource {
            override val sampleRate=44100
            override val frameCount=44100L*40
            override fun read(startFrame:Long,destination:ShortArray):Int {
                val count=minOf(destination.size,(frameCount-startFrame).toInt())
                for(i in 0 until count){
                    val t=(startFrame+i).toDouble()/sampleRate
                    destination[i]=if(t in 12.0..13.0)0 else
                        ((.12*sin(2*PI*70*t)+.075*sin(2*PI*140*t)+.04*sin(2*PI*210*t))*32767)
                            .toInt().toShort()
                }
                return count
            }
        }
        System.gc()
        val runtime=Runtime.getRuntime()
        val managedBefore=runtime.totalMemory()-runtime.freeMemory()
        var managedPeak=managedBefore
        val nativeBefore=Debug.getNativeHeapAllocatedSize()
        var nativePeak=nativeBefore
        val start=SystemClock.elapsedRealtime()
        val curve=DroneComponentAnalysis().analyze(source,progress={
            managedPeak=max(managedPeak,runtime.totalMemory()-runtime.freeMemory())
            nativePeak=max(nativePeak,Debug.getNativeHeapAllocatedSize())
        })
        val elapsed=SystemClock.elapsedRealtime()-start
        val nativeDelta=Debug.getNativeHeapAllocatedSize()-nativeBefore
        println("Drone component emulator: ${elapsed} ms / 40 s PCM; managed sampled high-water +${managedPeak-managedBefore} bytes; native sampled high-water +${nativePeak-nativeBefore} bytes; native end delta ${nativeDelta} bytes; ${curve.samples.size} frames")
        assertTrue(curve.samples.size in 210..220)
        assertTrue(curve.samples.any {it.hz==null})
        assertTrue(curve.samples.count {it.hz!=null}>150)
        assertTrue("Offline analysis must complete well below audio duration",elapsed<20000)
        val yinStart=SystemClock.elapsedRealtime()
        val yin=SavedYinReplay.analyze(source)
        val yinElapsed=SystemClock.elapsedRealtime()-yinStart
        println("Saved YIN replay emulator: ${yinElapsed} ms / 40 s PCM; ${yin.frames.size} frames")
        assertTrue(yin.frames.size in 210..220)
        assertTrue(yinElapsed<20000)
    }

    @Test fun threeMinuteSourceStaysBoundedAndCanBeCancelled(){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val source=object:MonoPcmSource {
            override val sampleRate=44100
            override val frameCount=sampleRate.toLong()*180
            override fun read(startFrame:Long,destination:ShortArray):Int {
                val count=minOf(destination.size,(frameCount-startFrame).toInt())
                for(i in 0 until count){
                    val t=(startFrame+i).toDouble()/sampleRate
                    destination[i]=((.10*sin(2*PI*70*t)+.06*sin(2*PI*140*t)+
                        .03*sin(2*PI*210*t))*32767).toInt().toShort()
                }
                return count
            }
        }
        val start=SystemClock.elapsedRealtime()
        val curve=DroneComponentAnalysis().analyze(source)
        val elapsed=SystemClock.elapsedRealtime()-start
        println("Three-minute component emulator: ${elapsed} ms, ${curve.samples.size} frames")
        assertTrue(curve.samples.size in 960..980)
        assertTrue(elapsed<90000)
        var visits=0
        try {
            SavedYinReplay.analyze(source,cancelled={++visits>10})
            fail("Cancellation was ignored")
        }catch(_:java.util.concurrent.CancellationException){
            assertTrue(visits in 11..12)
        }
    }
}
