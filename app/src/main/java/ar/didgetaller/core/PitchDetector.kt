package ar.didgetaller.core

import kotlin.math.*

/** Microphone-independent signal processing; tests run on the JVM.
 * Difference function/CMNDF (YIN-like), with a short spectrum for visualization.
 * The detector can mistake subharmonics/octaves when phone microphones filter lows.
 */
object PitchDetector {
    data class Reading(
        val hz: Double = 0.0,
        val confidence: Double = 0.0,
        val rms: Double = 0.0,
        val waveform: List<Float> = emptyList(),
        val spectrum: List<Float> = emptyList(),
        val diagnostic: Diagnostic? = null
    )

    /** Diagnostic evidence, not a second estimator or a confidence calibration. */
    data class Minimum(val hz:Double,val confidence:Double)
    data class Diagnostic(val candidateHz:Double?,val reason:String,val offsetSamples:Int=0,
        val comparedSamples:Int=0,val minima:List<Minimum> = emptyList(),val comparisonRms:Double?=null)

    fun analyze(pcm: ShortArray, sampleRate: Int = 44100, minHz: Int = 35, maxHz: Int = 140,
                includeVisuals: Boolean = true): Reading = analyzeWithDiagnostics(pcm,sampleRate,minHz,maxHz,includeVisuals,null)

    fun analyzeWithDiagnostics(pcm:ShortArray,sampleRate:Int,minHz:Int,maxHz:Int,
        includeVisuals:Boolean,observe:((Diagnostic)->Unit)?):Reading {
        require(sampleRate > 0 && minHz >= 20 && maxHz > minHz)
        if (pcm.size < 4096) {observe?.invoke(Diagnostic(null,"short_window"));return Reading()}
        val mean = pcm.sumOf { it.toDouble() } / pcm.size
        val rms = sqrt(pcm.sumOf { (it - mean).pow(2) } / pcm.size) / 32768.0
        val wave = if (!includeVisuals) emptyList() else List(128) { i ->
            val a = i * pcm.size / 128
            val b = (i+1)*pcm.size/128
            var peak=0.0
            for (j in a until b) peak=max(peak,abs(pcm[j]-mean)/32768.0)
            peak.toFloat()
        }
        val spectrum = if (includeVisuals) spectrum(pcm, sampleRate) else emptyList()
        if (rms < 0.005) {observe?.invoke(Diagnostic(null,"low_amplitude"));return Reading(rms = rms, waveform = wave, spectrum = spectrum)}
        val search=YinPeriodSearch.prepare(pcm,mean,sampleRate,minHz..maxHz)
        if(search==null) {
            observe?.invoke(Diagnostic(null,"short_comparison"))
            return Reading(rms=rms,waveform=wave,spectrum=spectrum)
        }
        val result=search.evaluate(pcm,rms,minHz..maxHz,observe!=null)
        observe?.invoke(result.diagnostic)
        return Reading(hz=result.hz,confidence=result.confidence,rms=rms,waveform=wave,spectrum=spectrum)
    }

    /** Hann-windowed radix-2 FFT, 0-600 Hz rendered as 160 normalized bins. */
    private fun spectrum(pcm: ShortArray, sampleRate: Int): List<Float> {
        val n=8192
        val re=DoubleArray(n); val im=DoubleArray(n)
        val start=(pcm.size-n).coerceAtLeast(0)
        val size=min(pcm.size,n)
        val avg=(0 until size).sumOf { pcm[start+it].toDouble() } / size
        repeat(size) { i -> re[i]=(pcm[start+i]-avg)*(0.5-0.5*cos(2*PI*i/(n-1))) }
        Radix2Fft.forward(re,im)
        val out=FloatArray(160)
        var top=1.0
        for(i in 1 until out.size) {
            val freq=i*600.0/(out.size-1)
            val center=(freq*n/sampleRate).roundToInt().coerceIn(1,n/2-2)
            var amp=0.0
            for(k in center-1..center+1) if (k>0) amp=max(amp,hypot(re[k],im[k]))
            out[i]=amp.toFloat();top=max(top,amp)
        }
        return out.map { (ln(1.0+5.0*it/top)/ln(6.0)).toFloat() }
    }
}
