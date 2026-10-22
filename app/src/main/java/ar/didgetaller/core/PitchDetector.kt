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
        val comparedSamples:Int=0,val minima:List<Minimum> = emptyList())

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
        // Preserve the proven low-register path. Higher notes need longer periods in samples
        // for accurate interpolation (27.6 samples at 1600Hz instead of 6.9 after /4).
        val ds = if(maxHz>500)1 else 4
        val fs=sampleRate.toDouble()/ds
        val x = DoubleArray(pcm.size/ds) { i ->
            var sum=0.0
            for(j in 0 until ds)sum+=pcm[i*ds+j]-mean
            sum/(ds*32768.0)
        }
        val minLag=max(2, floor(fs / maxHz).toInt()-(if(ds==1)1 else 0))
        val maxLag=min(x.size/2, ceil(fs/minHz).toInt()+(if(ds==1)1 else 0))
        val win=min(2200,x.size-maxLag-1)
        if (win < 1000) {observe?.invoke(Diagnostic(null,"short_comparison"));return Reading(rms=rms,waveform=wave,spectrum=spectrum)}
        val offset=if(ds==1)(x.size-win-maxLag).coerceAtLeast(0)/2 else 0
        val cmndf=DoubleArray(maxLag+1)
        var accumulated=0.0
        cmndf[0]=1.0
        for (lag in 1..maxLag) {
            var squared=0.0
            for (j in 0 until win) { val d=x[offset+j]-x[offset+j+lag]; squared+=d*d }
            accumulated+=squared
            cmndf[lag]=if (accumulated>1e-15) squared*lag/accumulated else 1.0
        }
        var best=-1
        var lag=minLag
        while (lag<=maxLag) {
            if (cmndf[lag]<0.14) {
                while(lag<maxLag && cmndf[lag+1]<cmndf[lag])lag++
                best=lag; break
            }
            lag++
        }
        if(best<0) {
            var smallest=1.0
            for(j in minLag..maxLag) if(cmndf[j]<smallest) { smallest=cmndf[j];best=j }
        }
        if(best<0) {observe?.invoke(Diagnostic(null,"no_minimum"));return Reading(rms=rms,waveform=wave,spectrum=spectrum)}
        // A strong H2 can create a shallow minimum at half the true period.
        // Prefer its double only with a substantial measured improvement, never by continuity.
        if(maxHz>500 && cmndf[best]>.025){
            val doubled=(max(minLag,best*2-2)..min(maxLag,best*2+2)).minByOrNull {cmndf[it]}
            if(doubled!=null && cmndf[doubled]<cmndf[best]*.65)best=doubled
        }
        var delta=0.0
        if(best>minLag && best<maxLag) {
            val a=cmndf[best-1];val b=cmndf[best];val c=cmndf[best+1]
            val denominator=a-2*b+c
            if (abs(denominator)>1e-9) delta=(0.5*(a-c)/denominator).coerceIn(-0.5,0.5)
        }
        val hz=fs/(best+delta)
        val confidence=(1.0-cmndf[best]).coerceIn(0.0,1.0)
        observe?.invoke(Diagnostic(hz,when {
            hz<minHz*.95 || hz>maxHz*1.05 -> "outside_range"
            confidence<.65 -> "low_periodicity"
            else -> "accepted"
        },offset*ds,win*ds,(minLag..maxLag).filter {i ->
            (i==minLag || cmndf[i]<=cmndf[i-1]) && (i==maxLag || cmndf[i]<=cmndf[i+1])
        }.map {Minimum(fs/it,(1-cmndf[it]).coerceIn(0.0,1.0))}))
        return Reading(
            hz=if(hz>=minHz*0.95 && hz<=maxHz*1.05 && confidence>=0.65) hz else 0.0,
            confidence=confidence,rms=rms,waveform=wave,spectrum=spectrum
        )
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
