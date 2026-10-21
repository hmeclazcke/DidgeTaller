package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.*

/** Opt-in private fixtures: DIDGE_TALLER_FIXTURES. No audio is copied or packaged.
 * CSVs stay in build/reports; window times are PCM positions, not Android delivery times. */
class RealAudioDiagnosticsTest {
    @Test fun replayPrivateWavsThroughProductionStreamAndLiveDecision() {
        val directory=System.getenv("DIDGE_TALLER_FIXTURES")?.let(::File)
        assumeTrue("Private WAV diagnostics are opt-in",directory?.isDirectory==true)
        val root=directory!!
        val readme=File(root,"README.txt").readText()
        val files=root.listFiles()!!.filter {it.extension.lowercase()=="wav"}.sortedBy {it.name}
        assertEquals("Expected the three supplied fixtures",3,files.size)
        val report=File("build/reports/real-audio/"+(System.getenv("DIDGE_DIAGNOSTIC_LABEL")?:"current")).apply {mkdirs()}
        for(file in files) {
            val before=sha(file)
            assertTrue("Hash missing from private manifest: ${file.name}",readme.lowercase().contains(before))
            val points=mutableListOf<SessionAnalysis.Point>()
            val rows=mutableListOf<String>()
            var diagnostic:PitchDetector.Diagnostic?=null
            val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=false,observe={diagnostic=it})
            var live=LivePitch.State()
            var quiet=0;var uncertain=0;var valid=0
            Pcm16WavReader(file).use {source ->
                assertEquals(44100,source.sampleRate);assertEquals(1,source.channels)
                var position=0L
                val chunk=ShortArray(4096);val window=ShortArray(16384)
                while(position<source.frameCount) {
                    val count=source.read(position,chunk);if(count==0)break
                    position+=count
                    val reading=stream.append(chunk,count)?:continue
                    val d=diagnostic!!
                    live=LivePitch.next(live,reading,InstrumentProfile.DIDGERIDOO)
                    points+=SessionAnalysis.Point(position*1000/44100,live.storedHz,reading.confidence)
                    if(reading.rms<.005)quiet++ else if(reading.hz==0.0)uncertain++ else valid++
                    source.read(position-window.size,window)
                    val reference=autocorrelation(window)
                    rows+=listOf(position/44100.0,reading.rms,d.candidateHz?:0.0,reading.confidence,d.reason,
                        reading.hz,live.hz,Music.pitch(live.hz)?.midi?:"",live.storedHz?:"",
                        d.offsetSamples,d.comparedSamples,reference.first,reference.second,
                        d.minima.joinToString("|"){"%.3f:%.5f".format(Locale.ROOT,it.hz,it.confidence)}).joinToString(",")
                }
                println("REAL ${file.name} duration=${source.frameCount/44100.0} windows=${points.size} accepted=$valid lowAmplitude=$quiet lowPeriodicity=$uncertain")
            }
            val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
            val samples=trace.associateBy {it.point.timeMs}
            val header="end_s,rms,candidate_hz,confidence,reason,detector_hz,ui_hz,midi,stored_hz,comparison_offset,comparison_samples,reference_hz,reference_ac,minima,graph_reason,previous_ms,tone"
            File(report,file.nameWithoutExtension+".csv").printWriter().use {out ->
                out.println(header)
                rows.forEachIndexed {i,row ->val s=samples[points[i].timeMs]
                    out.println(row+","+(s?.reason?:"")+","+(s?.previous?.let {trace[it].point.timeMs}?:"")+","+(s?.tone?:""))}
            }
            println("TRACE samples=${trace.size} uncertain=${trace.count {it.uncertain}} links=${trace.count {it.previous!=null}} tones=${trace.groupingBy {it.tone}.eachCount()}")
            assertTrue(points.any {it.hz!=null})
            assertTrue(trace.all {s ->points.any {it===s.point}})
            assertEquals("Original audio changed",before,sha(file))
        }
    }

    private fun sha(file:File)=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it)}

    /** Independent normalized correlation over the full frame; a competing period, not truth.
     * Different support from YIN's leading comparison helps expose nonstationary windows. */
    private fun autocorrelation(pcm:ShortArray):Pair<Double,Double> {
        val mean=pcm.sumOf {it.toDouble()}/pcm.size
        val x=DoubleArray(pcm.size/4){i ->(0..3).sumOf {j ->pcm[i*4+j]-mean}/4}
        var best=-1.0;var bestLag=0
        for(lag in 50..334) {
            var sum=0.0;var a=0.0;var b=0.0
            for(i in 0 until x.size-lag){val p=x[i];val q=x[i+lag];sum+=p*q;a+=p*p;b+=q*q}
            val value=if(a*b>1e-12)sum/sqrt(a*b) else 0.0
            if(value>best){best=value;bestLag=lag}
        }
        return 11025.0/bestLag to best
    }
}
