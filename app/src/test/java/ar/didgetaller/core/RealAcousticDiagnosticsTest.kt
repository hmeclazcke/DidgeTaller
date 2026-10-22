package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Opt-in six-window regression; private PCM stays outside the repository and APK. */
class RealAcousticDiagnosticsTest {
    @Test fun verifiedPrivateWindowsKeepAcceptedHzAndExposeRejectedEvidence() {
        val directory=System.getenv("DIDGE_TALLER_FIXTURES")?.let(::File)
        assumeTrue("Private WAV fixtures are opt-in",directory?.isDirectory==true)
        val file=File(directory!!,"Sesion_2026-10-21_19-17.wav")
        fun hash()=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(Locale.ROOT,it)}
        val expectedHash="7e877517a57bff4f5222d8d73dbc54494768fc208e14e24530ae65c9ca13eeb5"
        assertEquals(expectedHash,hash())
        // Golden outputs measured before metadata transport was added. Times are PCM window ends.
        val indices=listOf(13,24,44,52,73,90)
        val hz=listOf(67.79880791179887,35.70698605607956,0.0,0.0,0.0,62.0138884789146)
        val periodicity=listOf(.7036990968137065,.7152045694359421,0.0,.5268841739737335,.5382073542920041,.8164401543245147)
        val reasons=listOf("accepted","accepted","low_amplitude","low_periodicity","low_periodicity","accepted")
        val history=AcousticDiagnostics()
        val points=mutableListOf<SessionAnalysis.Point>()
        Pcm16WavReader(file).use {source->
            assertEquals(44100,source.sampleRate);assertEquals(1,source.channels)
            indices.forEachIndexed {i,index->
                val end=20480L+8192L*index
                val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=false)
                val chunk=ShortArray(4096)
                var reading:PitchDetector.Reading?=null
                repeat(5){part->
                    assertEquals(chunk.size,source.read(end-20480+part*4096,chunk))
                    reading=stream.append(chunk)
                }
                val incoming=requireNotNull(reading)
                assertEquals(hz[i],incoming.hz,1e-10);assertEquals(periodicity[i],incoming.confidence,1e-10)
                assertEquals(reasons[i],incoming.diagnostic!!.reason)
                val live=LivePitch.next(LivePitch.State(),incoming,InstrumentProfile.DIDGERIDOO)
                assertEquals(LivePitch.next(LivePitch.State(),incoming.copy(diagnostic=null),InstrumentProfile.DIDGERIDOO),live)
                val sample=history.record(end*1000/44100,incoming,live,InstrumentProfile.DIDGERIDOO)
                if(reasons[i]=="low_periodicity") {
                    assertEquals(AcousticDiagnostics.Signal.UNCERTAIN_PITCH,sample.signal)
                    assertTrue(sample.rms>.1);assertNotNull(sample.candidateHz);assertNull(live.storedHz)
                }
                points+=SessionAnalysis.Point(end*1000/44100,live.storedHz,incoming.confidence)
            }
        }
        val trace=PitchTrace.build(points,InstrumentProfile.DIDGERIDOO)
        assertEquals(3,trace.size);assertTrue(trace.all {s->points.any {it===s.point}})
        assertTrue(trace.all {it.previous==null})
        assertEquals(6,history.samples.size);assertEquals(expectedHash,hash())
    }
}
