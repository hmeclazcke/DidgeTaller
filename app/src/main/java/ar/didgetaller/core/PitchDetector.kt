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
        val spectrum: List<Float> = emptyList()
    )

    fun analyze(pcm: ShortArray, sampleRate: Int = 44100, minHz: Int = 35, maxHz: Int = 140,
                includeVisuals: Boolean = true): Reading {
        require(sampleRate > 0 && minHz >= 20 && maxHz > minHz)
        if (pcm.size < 4096) return Reading()
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
        if (rms < 0.005) return Reading(rms = rms, waveform = wave, spectrum = spectrum)
        val ds = 4
        val fs=sampleRate.toDouble()/ds
        val x = DoubleArray(pcm.size/ds) { i ->
            ((pcm[i*ds]-mean)+(pcm[i*ds+1]-mean)+(pcm[i*ds+2]-mean)+(pcm[i*ds+3]-mean)) / (ds*32768.0)
        }
        val minLag=max(2, floor(fs / maxHz).toInt())
        val maxLag=min(x.size/2, ceil(fs/minHz).toInt())
        val win=min(2200,x.size-maxLag-1)
        if (win < 1000) return Reading(rms=rms,waveform=wave,spectrum=spectrum)
        val cmndf=DoubleArray(maxLag+1)
        var accumulated=0.0
        cmndf[0]=1.0
        for (lag in 1..maxLag) {
            var squared=0.0
            for (j in 0 until win) { val d=x[j]-x[j+lag]; squared+=d*d }
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
        if(best<0) return Reading(rms=rms,waveform=wave,spectrum=spectrum)
        var delta=0.0
        if(best>minLag && best<maxLag) {
            val a=cmndf[best-1];val b=cmndf[best];val c=cmndf[best+1]
            val denominator=a-2*b+c
            if (abs(denominator)>1e-9) delta=(0.5*(a-c)/denominator).coerceIn(-0.5,0.5)
        }
        val hz=fs/(best+delta)
        val confidence=(1.0-cmndf[best]).coerceIn(0.0,1.0)
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
