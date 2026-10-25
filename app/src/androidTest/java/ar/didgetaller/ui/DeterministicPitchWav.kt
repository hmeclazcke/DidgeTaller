package ar.didgetaller.ui

import ar.didgetaller.core.WavCodec
import kotlin.math.PI
import kotlin.math.sin

/** Test-only PCM whose audible sections match the pitch events asserted by UI tests. */
internal object DeterministicPitchWav {
    data class Section(val startMs:Long,val endMs:Long,val hz:Double?)

    fun encode(durationMs:Long,vararg sections:Section,sampleRate:Int=44100):ByteArray {
        require(durationMs>0 && sampleRate>0)
        require(sections.isNotEmpty() && sections.first().startMs==0L)
        require(sections.asList().zipWithNext().all {(a,b)->a.endMs==b.startMs})
        require(sections.last().endMs==durationMs)
        require(sections.all {it.endMs>it.startMs && (it.hz==null || it.hz>0)})
        val pcm=ShortArray((durationMs*sampleRate/1000).toInt())
        var sectionIndex=0
        var phase=0.0
        for(i in pcm.indices){
            val timeMs=i.toLong()*1000/sampleRate
            while(timeMs>=sections[sectionIndex].endMs)sectionIndex++
            val hz=sections[sectionIndex].hz ?: continue
            phase+=2*PI*hz/sampleRate
            pcm[i]=(7000*sin(phase)+2200*sin(2*phase)).toInt().toShort()
        }
        return WavCodec.encode(pcm,pcm.size,sampleRate)
    }
}
