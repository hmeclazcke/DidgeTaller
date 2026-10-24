package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException
import kotlin.math.*

class AudioLevelTest {
    private fun source(pcm:ShortArray)=object:MonoPcmSource {
        override val sampleRate=44100
        override val frameCount=pcm.size.toLong()
        override fun read(startFrame:Long,destination:ShortArray):Int {
            val n=min(destination.size,pcm.size-startFrame.toInt())
            pcm.copyInto(destination,0,startFrame.toInt(),startFrame.toInt()+n)
            return n
        }
    }
    @Test fun showsSoundAndPausesIndependentlyOfPitchWithoutSmearingShortEvents(){
        val pcm=ShortArray(44100){i -> if(i in 4410 until 8820 || i>=22050)(8000*sin(2*PI*70*i/44100)).toInt().toShort() else 0}
        val before=pcm.copyOf();val level=AudioLevel.read(source(pcm))
        assertEquals(10,level.rms.size)
        assertArrayEquals(before,pcm)
        assertTrue(level.rms[1]>.1)
        for(i in listOf(0,2,3,4))assertEquals(0f,level.rms[i],0f)
        for(i in 5..9)assertTrue(level.rms[i]>.1)
    }
    @Test fun removesDcAndRetainsTheActualPartialTail(){
        val level=AudioLevel.read(source(ShortArray(5000){1200}))
        assertEquals(2,level.rms.size)
        assertTrue(level.rms.all {it==0f})
        assertEquals(5000L,level.frames)
    }
    @Test(expected=CancellationException::class) fun cancellationStopsBeforeReading(){
        AudioLevel.read(source(ShortArray(44100))){true}
    }
}
