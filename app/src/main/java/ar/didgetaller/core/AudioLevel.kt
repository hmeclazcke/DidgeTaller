package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.*

/** Source-time digital RMS, independent of pitch and never persisted as a measurement. */
data class AudioLevel(val rate:Int,val frames:Long,val hop:Int,val rms:FloatArray) {
    companion object {
        fun read(source:MonoPcmSource,cancelled:()->Boolean={false}):AudioLevel {
            val hop=(source.sampleRate/10).coerceAtLeast(1)
            val count=(source.frameCount+hop-1)/hop
            require(count<=36000) { "Audio level overview exceeds one hour" }
            val result=FloatArray(count.toInt());val pcm=ShortArray(hop)
            for(i in result.indices){
                if(cancelled() || Thread.currentThread().isInterrupted)throw CancellationException()
                val expected=min(hop.toLong(),source.frameCount-i.toLong()*hop).toInt()
                check(source.read(i.toLong()*hop,pcm)==expected) { "Incomplete audio level read" }
                val mean=(0 until expected).sumOf {pcm[it].toDouble()}/expected
                result[i]=(sqrt((0 until expected).sumOf {(pcm[it]-mean).pow(2)}/expected)/32768).toFloat()
            }
            return AudioLevel(source.sampleRate,source.frameCount,hop,result)
        }
    }
}
