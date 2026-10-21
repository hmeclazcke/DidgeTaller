package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.*

object AudioVisuals {
    data class Wave(val start:Long,val end:Long,val rate:Int,val min:FloatArray,val max:FloatArray,val samples:Boolean)
    data class Spectrogram(val start:Long,val end:Long,val rate:Int,val lowHz:Double,val highHz:Double,
                           val logarithmic:Boolean,val columns:Int,val rows:Int,val power:FloatArray,
                           val reference:Double,val fftSize:Int,val hopSize:Int) {
        fun frequency(y:Double)=if(logarithmic)lowHz*(highHz/lowHz).pow(y) else lowHz+(highHz-lowHz)*y
        fun relativeDb(column:Int,row:Int):Double? = if(reference<=1e-16)null else
            10*log10(max(1e-30,power[column*rows+row].toDouble())/reference)
    }
    private fun checkCancel(cancelled:()->Boolean){if(cancelled() || Thread.currentThread().isInterrupted)throw CancellationException()}
    fun waveform(source:MonoPcmSource,start:Long,end:Long,columns:Int=1024,cancelled:()->Boolean={false}):Wave {
        require(start>=0 && end>start && end<=source.frameCount && columns in 2..4096)
        val bins=min(columns.toLong(),end-start).toInt()
        val low=FloatArray(bins){1f};val high=FloatArray(bins){-1f};val pcm=ShortArray(32768)
        var at=start
        while(at<end){checkCancel(cancelled);val count=min(source.read(at,pcm).toLong(),end-at).toInt();check(count>0)
            repeat(count){i ->val bin=((at+i-start)*bins/(end-start)).toInt().coerceAtMost(bins-1);val v=pcm[i]/32768f;low[bin]=min(low[bin],v);high[bin]=max(high[bin],v)}
            at+=count
        }
        return Wave(start,end,source.sampleRate,low,high,end-start<=columns)
    }
    /** All STFT frames in the viewport contribute; time cells average power, never skip hidden frames. */
    fun spectrogram(source:MonoPcmSource,start:Long,end:Long,lowHz:Double=0.0,highHz:Double=500.0,
                    logarithmic:Boolean=false,maxColumns:Int=256,rows:Int=256,cancelled:()->Boolean={false},
                    progress:(Int)->Unit={}):Spectrogram {
        require(start>=0 && end>start && end<=source.frameCount && maxColumns in 2..512 && rows in 16..512)
        val n=32768;val hop=n/2;val low=if(logarithmic)max(source.sampleRate.toDouble()/n,lowHz) else max(0.0,lowHz)
        val high=min(highHz,source.sampleRate/2.0);require(high>low)
        val columns=min(maxColumns.toLong(),max(1L,(end-start+hop-1)/hop)).toInt()
        val sums=DoubleArray(columns*rows);val counts=IntArray(columns)
        val analyzer=SpectralAnalysis(n);val pcm=ShortArray(n)
        val binHz=source.sampleRate.toDouble()/n
        val boundaries=DoubleArray(rows+1){i ->if(logarithmic)low*(high/low).pow(i.toDouble()/rows) else low+(high-low)*i/rows}
        var at=start;var lastProgress=-1
        while(at<end){checkCancel(cancelled);val read=source.read(at,pcm);val count=min(read.toLong(),end-at).toInt();if(count<2)break
            val frame=analyzer.frame(pcm,source.sampleRate,count)
            val center=at+count/2
            val col=((center-start)*columns/(end-start)).toInt().coerceIn(0,columns-1)
            for(row in 0 until rows){
                val first=ceil(boundaries[row]/binHz).toInt().coerceIn(0,frame.density.lastIndex)
                val last=(ceil(boundaries[row+1]/binHz).toInt()-1).coerceAtMost(frame.density.lastIndex)
                // Raster rows narrower than one FFT bin use nearest-bin support, without inventing resolution.
                sums[col*rows+row]+=if(last<first)frame.density[((boundaries[row]+boundaries[row+1])/(2*binHz)).roundToInt().coerceIn(0,frame.density.lastIndex)]
                    else (first..last).sumOf {frame.density[it]}/(last-first+1)
            }
            counts[col]++
            val percent=((at-start)*100/(end-start)).toInt();if(percent!=lastProgress){progress(percent);lastProgress=percent}
            if(at+count>=end)break
            at+=hop
        }
        checkCancel(cancelled)
        // Empty edge cells inherit their nearest actual window; these are window-supported values, not extra observations.
        for(col in 0 until columns)if(counts[col]>0)repeat(rows){sums[col*rows+it]/=counts[col]}
        for(col in 0 until columns)if(counts[col]==0){
            val nearest=counts.indices.filter {counts[it]>0}.minByOrNull {abs(it-col)} ?: continue
            repeat(rows){sums[col*rows+it]=sums[nearest*rows+it]}
        }
        val power=FloatArray(sums.size){sums[it].toFloat()};val reference=power.maxOrNull()?.toDouble() ?: 0.0
        progress(100)
        return Spectrogram(start,end,source.sampleRate,low,high,logarithmic,columns,rows,power,reference,n,hop)
    }
}
