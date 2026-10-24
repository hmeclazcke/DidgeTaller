package ar.didgetaller.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.WavCodec
import ar.didgetaller.core.SessionAnalysis
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.Assume.assumeTrue
import android.os.Build
import ar.didgetaller.core.ProgressiveWavWriter
import ar.didgetaller.core.Pcm16WavReader
import kotlin.math.sin
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Uses isolated test preferences and a test-only directory in the target app. */
@RunWith(AndroidJUnit4::class)
class LocalPersistenceTest {
    @Before fun emulatorOnly(){assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))}
    private fun isolatedContext():Context {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val testFiles=File(target.filesDir,"instrumentation-tests")
        testFiles.deleteRecursively()
        testFiles.mkdirs()
        target.getSharedPreferences("instrumentation_local_data",0).edit().clear().commit()
        return object:ContextWrapper(target) {
            override fun getFilesDir():File=testFiles
            override fun getSharedPreferences(name:String,mode:Int):SharedPreferences =
                target.getSharedPreferences("instrumentation_local_data",mode)
        }
    }
    @Test fun instrumentIsAdditiveAndLegacyReadsNeverRewriteIndexOrAudio(){
        val context=isolatedContext();val prefs=context.getSharedPreferences("local_data",0)
        val store=RecordingStore(context)
        val wav=WavCodec.encode(ShortArray(44100){i->(5000*sin(2*Math.PI*440*i/44100)).toInt().toShort()},44100,44100)
        val old=store.save(wav,listOf(SessionAnalysis.Point(0,69.3,.95)))
        val legacy=JSONArray(prefs.getString("recordings","[]"));legacy.getJSONObject(0).remove("instrument")
        prefs.edit().putString("recordings",legacy.toString()).commit()
        val exact=prefs.getString("recordings",null)
        assertEquals(ar.didgetaller.core.InstrumentProfile.DIDGERIDOO,RecordingStore(context).list().single().instrument)
        assertEquals(exact,prefs.getString("recordings",null));assertArrayEquals(wav,store.file(old.id).readBytes())
        val profile=ar.didgetaller.core.InstrumentProfile.NATIVE_AMERICAN_FLUTE
        val flute=store.save(wav,(0..5).map {SessionAnalysis.Point(it*150L,440.0,.98)},instrument=profile)
        assertEquals(profile,RecordingStore(context).list().first {it.id==flute.id}.instrument)
        store.rename(flute.id,"Flauta de prueba")
        assertEquals(profile,RecordingStore(context).list().first {it.id==flute.id}.instrument)
        assertFalse(JSONArray(prefs.getString("recordings","[]")).getJSONObject(0).has("instrument"))
        val cache=AnalysisCache(context)
        val analysis=cache.analyze(flute.id,store.file(flute.id),instrument=profile)
        assertEquals(440.0,analysis.medianValidHz!!,1.0)
        val cached=AnalysisCache(context).analyze(flute.id,store.file(flute.id),instrument=profile)
        assertEquals(analysis.timeline,cached.timeline);assertArrayEquals(wav,store.file(flute.id).readBytes())
    }
    @Test fun legacyMeasurementsAndDesignsRemainReadable() {
        val context=isolatedContext()
        val prefs=context.getSharedPreferences("local_data",0)
        prefs.edit().putString("measurements","[{\"time\":\"2026-10-20 10:00\",\"hz\":\"66.9\",\"note\":\"Do · C2\"}]")
            .putString("designs","[{\"name\":\"PVC antiguo\",\"length\":1500,\"mouth\":40,\"mid\":40,\"bell\":40}]").commit()
        val store=LocalStore(context)
        assertEquals(66.9,store.measurements().single().hz,0.001)
        assertEquals("PVC antiguo",store.designs().single().name)
        store.saveMeasurement(66.1)
        assertEquals(2,store.measurements().size)
        val legacy=store.measurements().last()
        store.deleteMeasurement(legacy.id)
        assertEquals(66.1,store.measurements().single().hz,0.001)
        store.saveDesign("nuevo",AcousticModel.Shape())
        assertEquals(2,store.designs().size)
        store.deleteDesign(store.designs().last().id)
        assertEquals("nuevo",store.designs().single().name)
        prefs.edit().clear().commit()
    }

    @Test fun wavSurvivesStoreReloadRenameAndDelete() {
        val context=isolatedContext()
        context.getSharedPreferences("local_data",0).edit().remove("recordings").commit()
        val store=RecordingStore(context)
        val bytes=WavCodec.encode(ShortArray(44100){1000},44100,44100)
        val saved=store.save(bytes)
        assertEquals(1000L,saved.durationMs)
        assertArrayEquals(bytes,store.file(saved.id).readBytes())
        val reopened=RecordingStore(context)
        assertEquals(saved.id,reopened.list().single().id)
        reopened.rename(saved.id,"Prueba PVC")
        assertEquals("Prueba PVC",reopened.list().single().name)
        reopened.delete(saved.id)
        assertTrue(reopened.list().isEmpty())
        assertFalse(reopened.file(saved.id).exists())
    }

    @Test fun sessionSummaryTimelineAndLegacyAudioRemainReadable() {
        val context=isolatedContext()
        val prefs=context.getSharedPreferences("local_data",0)
        prefs.edit().remove("recordings").commit()
        val store=RecordingStore(context)
        val bytes=WavCodec.encode(ShortArray(44100){1000},44100,44100)
        val points=(0..5).map {SessionAnalysis.Point(it*150L,66.1+it%2*0.2,0.95)}
        val saved=store.save(bytes,points)
        val reopened=RecordingStore(context).list().single()
        assertEquals(saved.id,reopened.id)
        assertEquals(6,reopened.timeline.size)
        assertEquals(66.2,reopened.summary!!.typicalHz,0.2)
        assertFalse(reopened.legacy)

        val legacyId=UUID.randomUUID().toString()
        store.file(legacyId).writeBytes(bytes)
        val old=JSONArray(prefs.getString("recordings","[]"))
        old.put(JSONObject().put("id",legacyId).put("name","WAV anterior")
            .put("createdAt",123L).put("durationMs",1000L))
        assertTrue(prefs.edit().putString("recordings",old.toString()).commit())
        val legacy=RecordingStore(context).list().single {it.id==legacyId}
        assertEquals("WAV anterior",legacy.name)
        assertTrue(legacy.legacy)
        assertNull(legacy.summary)
        assertTrue(legacy.timeline.isEmpty())
        store.delete(saved.id);store.delete(legacyId)

        val orphanId=UUID.randomUUID().toString()
        store.file(orphanId).writeBytes(bytes)
        val recovered=RecordingStore(context).list().single {it.id==orphanId}
        assertEquals("Audio recuperado",recovered.name)
        assertTrue(recovered.legacy)
        assertTrue(recovered.recovered)
        assertEquals(1000L,recovered.durationMs)
        store.rename(orphanId,"PVC recuperado")
        assertEquals("PVC recuperado",RecordingStore(context).list().single {it.id==orphanId}.name)
        assertTrue(RecordingStore(context).list().single {it.id==orphanId}.recovered)
        store.delete(orphanId)
    }

    @Test fun privateSessionWavCanPlayPauseAndRestart() {
        val context=isolatedContext()
        val store=RecordingStore(context)
        val bytes=WavCodec.encode(ShortArray(44100*3){1000},44100*3,44100)
        val saved=store.save(bytes)
        val player=MediaPlayer()
        try {
            player.setDataSource(store.file(saved.id).absolutePath)
            player.prepare()
            player.start()
            assertTrue(player.isPlaying)
            player.pause()
            assertFalse(player.isPlaying)
            player.seekTo(0)
            player.start()
            assertTrue(player.isPlaying)
        } finally {
            player.release()
            store.delete(saved.id)
        }
    }

    @Test fun durableTrashRestoresAudioAnalysisAndDesignRelationship() {
        val context=isolatedContext();val store=RecordingStore(context);val tools=SessionToolsStore(context)
        val designStore=LocalStore(context);designStore.saveDesign("PVC prueba",AcousticModel.Shape())
        val design=designStore.designs().single()
        val samples=ShortArray(44100*3){(9000*sin(2*Math.PI*70*it/44100)).toInt().toShort()}
        val wav=WavCodec.encode(samples,samples.size,44100);val session=store.save(wav)
        tools.associate(session.id,design.id);tools.addMarker(session.id,500,"Posible toot","")
        val cache=AnalysisCache(context);val result=cache.analyze(session.id,store.file(session.id))
        assertEquals(70.0,result.summary!!.typicalHz,.3)
        val prefs=context.getSharedPreferences("local_data",0);val index=prefs.getString("recordings",null)
        TrashStore(context).put("session",session.id);TrashStore(context).put("design",design.id)
        assertTrue(RecordingStore(context).list().isEmpty());assertTrue(LocalStore(context).designs().isEmpty())
        assertEquals(1,RecordingStore(context).list(true).size);assertArrayEquals(wav,store.file(session.id).readBytes())
        assertEquals(index,prefs.getString("recordings",null));assertEquals(design.id,SessionToolsStore(context).designId(session.id))
        TrashStore(context).restore("session",session.id);TrashStore(context).restore("design",design.id)
        assertEquals(session.id,RecordingStore(context).list().single().id);assertEquals(design.id,LocalStore(context).designs().single().id)
        assertEquals(500,SessionToolsStore(context).markers(session.id).single().timeMs)
        assertEquals(result.summary,AnalysisCache(context).analyze(session.id,store.file(session.id)).summary)
    }

    @Test fun cacheInvalidatesOnChangedSourceAndReusesSelectedIntervals() {
        val context=isolatedContext();val store=RecordingStore(context);val cache=AnalysisCache(context)
        fun wav(hz:Double)=ShortArray(44100*4){(9000*sin(2*Math.PI*hz*it/44100)).toInt().toShort()}.let {WavCodec.encode(it,it.size,44100)}
        val original=wav(70.0);val session=store.save(original);val file=store.file(session.id)
        val first=cache.analyze(session.id,file,1000,3000);assertEquals(70.0,first.summary!!.typicalHz,.3)
        val cached=cache.analyze(session.id,file,1000,3000);assertArrayEquals(first.spectrum.density,cached.spectrum.density,0.0)
        assertEquals(44100,cached.startFrame);assertEquals(132300,cached.endFrame);assertArrayEquals(original,file.readBytes())
        file.writeBytes(wav(73.42));val changed=cache.analyze(session.id,file,1000,3000);assertEquals(73.42,changed.summary!!.typicalHz,.3)
        assertNotEquals(first.summary!!.typicalHz,changed.summary!!.typicalHz)
        val index=context.getSharedPreferences("local_data",0).getString("recordings",null)
        assertTrue(index!!.contains(session.id));assertNull(store.list().single().summary)
    }

    @Test fun previousPitchPolicyCacheIsNotReusedOrMigratedIntoOriginalMeasurements() {
        val context=isolatedContext();val store=RecordingStore(context);val cache=AnalysisCache(context)
        val pcm=ShortArray(44100){(9000*sin(2*Math.PI*70*it/44100)).toInt().toShort()}
        val wav=WavCodec.encode(pcm,pcm.size,44100)
        val originalPoints=listOf(SessionAnalysis.Point(500,51.0,.90))
        val session=store.save(wav,originalPoints)
        val index=context.getSharedPreferences("local_data",0).getString("recordings",null)
        val key="analysis-2-${cache.fingerprint(store.file(session.id))}-0-null-DIDGERIDOO"
        val hash=java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString(""){"%02x".format(it)}
        val oldCache=File(context.filesDir,"analysis-cache/${session.id}-$hash.bin")
        java.io.DataOutputStream(oldCache.outputStream()).use {out->
            out.writeInt(2);out.writeLong(0);out.writeLong(44100);out.writeInt(44100);out.writeInt(1)
            out.writeInt(4096);repeat(2049){out.writeDouble(0.0)}
            out.writeInt(1);out.writeLong(500);out.writeDouble(51.0);out.writeDouble(.90)
            out.writeBoolean(false);out.writeDouble(51.0);out.writeDouble(51.0);out.writeDouble(1.0);out.writeDouble(.90)
        }
        val stale=oldCache.readBytes()
        val fresh=cache.analyze(session.id,store.file(session.id))
        assertTrue("A previous acceptance policy cannot bypass corrected processing",fresh.timeline.filter {it.hz!=null}.all {kotlin.math.abs(it.hz!!-70)<1})
        assertTrue(fresh.timeline.any {it.hz!=null})
        assertEquals(originalPoints,store.list().single().timeline)
        assertEquals(index,context.getSharedPreferences("local_data",0).getString("recordings",null))
        assertArrayEquals(wav,store.file(session.id).readBytes())
        assertArrayEquals(stale,oldCache.readBytes())
    }

    @Test fun progressiveCaptureImportsAndInterruptedCaptureRecoversWithoutInventingSummary() {
        val context=isolatedContext();val store=RecordingStore(context);val part=store.newCapture()
        ProgressiveWavWriter(part).use {it.append(ShortArray(44100){1000},44100)}
        val session=store.importCapture(part,emptyList(),12345L)
        assertFalse(part.exists());assertTrue(store.file(session.id).exists());assertEquals(1000,session.durationMs)
        val interrupted=store.newCapture();ProgressiveWavWriter(interrupted).use {it.append(ShortArray(44100){2000},44100)}
        java.io.RandomAccessFile(interrupted,"rw").use {it.seek(40);it.writeInt(0)}
        assertEquals(1,RecordingStore(context).recoverPending())
        val recovered=RecordingStore(context).list().single {it.id!=session.id}
        assertTrue(recovered.recovered);assertNull(recovered.summary);assertTrue(recovered.timeline.isEmpty())
        Pcm16WavReader(store.file(recovered.id)).use {assertEquals(44100,it.frameCount)}
    }
}
