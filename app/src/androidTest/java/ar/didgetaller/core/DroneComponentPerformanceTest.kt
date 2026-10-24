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
                for(i in destination.indices){
                    val t=(startFrame+i).toDouble()/sampleRate
                    destination[i]=if(t in 12.0..13.0)0 else
                        ((.12*sin(2*PI*70*t)+.075*sin(2*PI*140*t)+.04*sin(2*PI*210*t))*32767)
                            .toInt().toShort()
                }
                return destination.size
            }
        }
        val nativeBefore=Debug.getNativeHeapAllocatedSize()
        val start=SystemClock.elapsedRealtime()
        val curve=DroneComponentAnalysis().analyze(source)
        val elapsed=SystemClock.elapsedRealtime()-start
        val nativeDelta=Debug.getNativeHeapAllocatedSize()-nativeBefore
        println("Drone component emulator: ${elapsed} ms / 40 s PCM; native heap delta ${nativeDelta} bytes; ${curve.samples.size} frames")
        assertTrue(curve.samples.size in 210..220)
        assertTrue(curve.samples.any {it.hz==null})
        assertTrue(curve.samples.count {it.hz!=null}>150)
        assertTrue("Offline analysis must complete well below audio duration",elapsed<20000)
    }
}
