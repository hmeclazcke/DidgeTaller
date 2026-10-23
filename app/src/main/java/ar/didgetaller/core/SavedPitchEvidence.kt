package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.*

/** Read-only, bounded inspection. These alternatives never feed notes or PitchTrace. */
object SavedPitchEvidence {
    enum class Presence { BELOW_THRESHOLD, ENERGY_PRESENT, INSUFFICIENT_INPUT }
    data class Period(val hz:Double,val confidence:Double)
    data class Snapshot(val startFrame:Long,val endFrame:Long,val sampleRate:Int,
        val presence:Presence,val rms:Double,val acceptedHz:Double?,val candidateHz:Double?,
        val periodicity:Double,val alternative:Period?,val families:List<Double>,
        val minHz:Int,val maxHz:Int)

    fun inspect(source:MonoPcmSource,positionMs:Long,instrument:InstrumentProfile,
                cancelled:()->Boolean={false}):Snapshot {
        fun checkCancelled(){if(cancelled())throw CancellationException()}
        checkCancelled()
        val count=min(source.frameCount,instrument.windowSamples.toLong()).toInt()
        if(count<4096)return Snapshot(0,source.frameCount,source.sampleRate,
            Presence.INSUFFICIENT_INPUT,0.0,null,null,0.0,null,emptyList(),instrument.minHz,instrument.initialMaxHz)
        val position=positionMs.coerceIn(0,source.frameCount*1000/source.sampleRate)*source.sampleRate/1000
        val start=(position-count/2).coerceIn(0,source.frameCount-count)
        val pcm=ShortArray(count)
        check(source.read(start,pcm)==count){"Incomplete evidence window"}
        var nominal:PitchDetector.Diagnostic?=null
        val reading=PitchDetector.analyzeWithDiagnostics(pcm,source.sampleRate,instrument.minHz,instrument.initialMaxHz,false){nominal=it}
        val accepted=LivePitch.next(LivePitch.State(),reading,instrument).storedHz
        checkCancelled()
        if(reading.rms<.005)return Snapshot(start,start+count,source.sampleRate,Presence.BELOW_THRESHOLD,
            reading.rms,null,null,reading.confidence,null,emptyList(),instrument.minHz,instrument.initialMaxHz)
        // Expanded search is optional evidence, never a correction or an input to a saved series.
        val alternative=if(instrument==InstrumentProfile.DIDGERIDOO){
            val expanded=PitchDetector.analyze(pcm,source.sampleRate,25,instrument.initialMaxHz,false)
            expanded.hz.takeIf {it>0 && (reading.hz<=0 || abs(1200*log2(it/reading.hz))>80)}
                ?.let {Period(it,expanded.confidence)}
        }else null
        checkCancelled()
        var fftSize=4096
        while(fftSize<count)fftSize*=2
        val spectrum=SpectralAnalysis(fftSize).frame(pcm,source.sampleRate)
        val peaks=HarmonicAnalysis.analyze(spectrum,null).peaks
        // A family is three observed peaks, including its lowest member. Missing H1 is not invented.
        val families=peaks.filter {peak->
            peak.hz in instrument.minHz.toDouble()..instrument.initialMaxHz.toDouble() &&
                (2..3).all {order->
                    val expected=peak.hz*order
                    val tolerance=min(.20*peak.hz,max(spectrum.binHz,expected*(2.0.pow(35.0/1200)-1)))
                    peaks.any {abs(it.hz-expected)<=tolerance}
                }
        }.sortedByDescending {it.relativeDb}.take(3).map {it.hz}
        checkCancelled()
        // A rejected candidate is exposed separately; diagnostic metadata never changes acceptedHz.
        val candidate=nominal?.candidateHz
        return Snapshot(start,start+count,source.sampleRate,Presence.ENERGY_PRESENT,reading.rms,
            accepted,candidate,reading.confidence,alternative,families,instrument.minHz,instrument.initialMaxHz)
    }
}
