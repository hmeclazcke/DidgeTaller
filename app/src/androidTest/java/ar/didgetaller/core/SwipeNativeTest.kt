package ar.didgetaller.core

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import kotlin.math.*

/** Numerical fidelity, including known failures, rather than a claim of pitch accuracy. */
class SwipeNativeTest {
    private fun emulator() = assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
    private data class Case(val name:String,val f0:Double,val harmonics:List<Double>)
    @Test fun androidMatchesIndependentSptkGoldenIncludingKnownErrors() {
        emulator()
        val reference=InstrumentationRegistry.getInstrumentation().context.assets.open("swipe-reference.tsv")
            .bufferedReader().use {it.readLines().drop(1).map {line->line.split('\t')}}.groupBy {it[0]}
        val cases=listOf(Case("pure35",35.0,listOf(1.0)),Case("pure70",70.0,listOf(1.0)),
            Case("pure140",140.0,listOf(1.0)),Case("missing35",35.0,listOf(0.0,1.0,0.0,0.0,.6)),
            Case("weak35",35.0,listOf(.03,1.0,.3)),
            Case("silence",70.0,listOf(0.0)))
        for(case in cases){
            val pcm=ShortArray(88200){i->(9000*case.harmonics.mapIndexed {h,a->a*sin(2*PI*case.f0*(h+1)*i/44100)}.sum()).toInt().toShort()}
            val source=object:MonoPcmSource {
                override val sampleRate=44100;override val frameCount=pcm.size.toLong()
                override fun read(startFrame:Long,destination:ShortArray):Int {
                    val n=minOf(destination.size,pcm.size-startFrame.toInt())
                    pcm.copyInto(destination,0,startFrame.toInt(),startFrame.toInt()+n);return n
                }
            }
            val curve=SwipeAnalysis().analyze(source,InstrumentProfile.DIDGERIDOO)
            val rows=reference.getValue(case.name)
            assertEquals(rows.size,curve.samples.size)
            rows.zip(curve.samples).forEach {(row,sample)->
                assertEquals(row[1].toDouble()*1000/44100,sample.timeMs,1e-8)
                val expected=row[2].toDouble()
                if(expected==0.0)assertNull(sample.hz) else assertEquals("${case.name} ${row[1]}",expected,sample.hz!!,.01)
            }
        }
        // Deterministic broadband noise must not gain pitch from the presence gate.
        var seed=1L
        val noise=ShortArray(88200){seed=(1664525*seed+1013904223) and 0xffffffffL;((seed ushr 16).toInt()-32768).toShort()}
        assertTrue(SwipeNative.estimate(noise,noise.size,44100,8192,35,140).all {it==0.0})
        try {SwipeNative.estimate(noise,noise.size,44100,4096,200,1600);fail("Unsafe flute range must be rejected")}
        catch(expected:IllegalArgumentException){ /* Guard runs before the upstream code. */ }
    }

    @Test fun optionalPrivateFixtureMatchesWholeSignalReferenceWithoutMutation() {
        emulator()
        val args=InstrumentationRegistry.getArguments()
        val path=args.getString("swipeFixture")
        assumeTrue("Optional protected fixture not supplied",path!=null)
        val wav=File(path!!)
        fun hash()=MessageDigest.getInstance("SHA-256").digest(wav.readBytes()).joinToString(""){"%02x".format(it)}
        val before=hash()
        val rows=File(args.getString("swipeReference")!!).readLines().drop(1).map {it.split('\t')}
        val start=SystemClock.elapsedRealtimeNanos()
        var peak=0L
        val curve=Pcm16WavReader(wav).use {source->SwipeAnalysis {pcm,count,rate,hop,lo,hi->
            SwipeNative.estimate(pcm,count,rate,hop,lo,hi).also {peak=maxOf(peak,SwipeNative.peakBytes())}
        }.analyze(source,InstrumentProfile.DIDGERIDOO)}
        val elapsed=(SystemClock.elapsedRealtimeNanos()-start)/1e6
        assertEquals(rows.size,curve.samples.size)
        rows.zip(curve.samples).forEach {(row,sample)->
            assertEquals(row[0].toDouble()*1000/44100,sample.timeMs,1e-8)
            val expected=row[1].toDouble().takeIf {row[2].toDouble()>=.005 && it>0}
            if(expected==null)assertNull(sample.hz) else assertEquals("frame ${row[0]}",expected,sample.hz!!,.01)
        }
        assertEquals(before,hash())
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"swipe-android-results.tsv")
        out.writeText("time_ms\thz\n"+curve.samples.joinToString("\n"){"${it.timeMs}\t${it.hz ?: 0.0}"})
        Log.i("SwipeValidation","private points=${curve.samples.size} ms=$elapsed nativePeakBytes=$peak sha256=$before")
    }

    @Test fun minuteAnalysisIsBoundedAndLongRecordingCanBeCancelled() {
        emulator()
        var largestRead=0;var peak=0L;var calls=0
        val worker=SwipeAnalysis {pcm,count,rate,hop,lo,hi->
            calls++
            SwipeNative.estimate(pcm,count,rate,hop,lo,hi).also {peak=maxOf(peak,SwipeNative.peakBytes())}
        }
        fun source(seconds:Int)=object:MonoPcmSource {
            override val sampleRate=44100;override val frameCount=seconds*44100L
            override fun read(startFrame:Long,destination:ShortArray):Int {
                largestRead=maxOf(largestRead,destination.size)
                destination.indices.forEach {i->destination[i]=(6000*sin(2*PI*70*(startFrame+i)/44100)).toInt().toShort()}
                return destination.size
            }
        }
        val start=SystemClock.elapsedRealtimeNanos()
        val curve=worker.analyze(source(60),InstrumentProfile.DIDGERIDOO)
        assertTrue(curve.samples.size>300);assertTrue(largestRead<=262144);assertTrue(peak<32*1024*1024)
        Log.i("SwipeValidation","minute ms=${(SystemClock.elapsedRealtimeNanos()-start)/1e6} points=${curve.samples.size} nativePeakBytes=$peak maxPcmBytes=${largestRead*2}")
        val before=calls
        try {worker.analyze(source(1800),InstrumentProfile.DIDGERIDOO,cancelled={calls>before});fail("Cancellation required")}
        catch(expected:java.util.concurrent.CancellationException){assertEquals(before+1,calls)}
    }
}
