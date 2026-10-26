package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Emulator-only, opt-in external PCM; never packages private audio or touches phone storage. */
class SessionEvidenceUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owners=ViewModelStore()
    private lateinit var app:EvidenceApplication
    private lateinit var vm:TallerViewModel
    private lateinit var item:RecordingStore.Session
    private val args get()=InstrumentationRegistry.getArguments()
    @Before fun setup(){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        app=EvidenceApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val path=args.getString("evidenceWav")
        val bytes=if(path!=null)File(path).readBytes() else {
            val pcm=ShortArray(44100*8){i->if(i in 88200..132300)0 else (6000*sin(2*PI*70*i/44100)).toInt().toShort()}
            WavCodec.encode(pcm,pcm.size,44100)
        }
        val local=File(app.filesDir,"input.wav").apply {writeBytes(bytes)}
        val points=args.getString("evidencePoints")?.let {input ->
            File(input).readLines().drop(1).filter {it.isNotBlank()}.map {line->
                val f=line.split(',');SessionAnalysis.Point((f[2].toDouble()*1000).toLong(),f[4].toDoubleOrNull(),f[5].toDouble())
            }
        } ?: Pcm16WavReader(local).use {reader->
            val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=false)
            val chunk=ShortArray(4096);val result=mutableListOf<SessionAnalysis.Point>();var at=0L
            while(at<reader.frameCount){val n=reader.read(at,chunk);at+=n;stream.append(chunk,n)?.let {r->
                result+=SessionAnalysis.Point(at*1000/44100,r.hz.takeIf {it>0},r.confidence)
            }};result
        }
        item=RecordingStore(app).save(bytes,points,1000000)
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("evidence",vm)}
        compose.setContent {TallerApp(vm,{}, {}, {_,_,_->})}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.waitUntil(20000){!vm.workspace.state.loading && vm.workspace.state.frames>0}
    }
    @After fun close(){if(::app.isInitialized){compose.runOnUiThread {owners.clear()};app.clean()}}
    private fun screenshot(name:String){
        compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(250)
        val dir=File(app.getExternalFilesDir(null),"session-evidence").apply {mkdirs()}
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(dir,"${args.getString("evidenceLabel") ?: "synthetic"}-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Test fun readmeShowcaseFromSyntheticWav(){
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("library-list").assertIsDisplayed()
        screenshot("library")
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.waitUntil(20000){vm.musicalTrace(item)!=null}
        compose.onNodeWithTag("time-chart").assertIsDisplayed()
        screenshot("session")
        compose.onNodeWithTag("plot-tab-spectrogram").performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null && !vm.workspace.state.loading}
        compose.onNodeWithTag("time-chart").assertIsDisplayed()
        screenshot("spectrogram")
        compose.onNodeWithText(uiText(R.string.ui_analyze_recording)).performScrollTo().performClick()
        compose.waitUntil(30000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        assertNull(vm.recordingAnalysis.error)
        compose.onNodeWithTag("plot-tab-spectrum").performScrollTo().performClick()
        compose.onNodeWithTag("spectrum-chart").assertIsDisplayed()
        screenshot("harmonics")
    }
    @Test fun captureAndPreserveOriginals(){
        val store=RecordingStore(app);val original=store.file(item.id).readBytes()
        val index=app.getSharedPreferences("local_data",0).all.toMap()
        compose.onNodeWithTag("time-chart").performScrollTo();screenshot("pitch")
        compose.onNodeWithTag("session-playback-seek").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress){it(2000f)}
        compose.waitUntil(10000){abs(vm.playbackPositionMs()-2000)<100}
        screenshot("seek")
        // Scroll the vertical parent first; the tab's nearest scroller is horizontal.
        compose.onNodeWithTag("expand-plot").performScrollTo()
        compose.onNodeWithText(uiText(R.string.ui_spectrogram)).performScrollTo().performClick()
        screenshot("spectrogram-selected")
        compose.runOnIdle {assertEquals("Tab selection",AcousticPlot.SPECTROGRAM,vm.preview(item.id).plot)}
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null || vm.workspace.state.error!=null}
        compose.runOnIdle {assertNull("Spectrogram calculation",vm.workspace.state.error)}
        compose.onNodeWithTag("time-chart").performScrollTo();screenshot("spectrogram")
        compose.onNodeWithTag("plot-tab-pitch").performScrollTo().performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick();screenshot("wide-pitch")
        assertArrayEquals(original,store.file(item.id).readBytes())
        assertEquals(index,app.getSharedPreferences("local_data",0).all)
        assertEquals(item.timeline,store.list().first {session->session.id==item.id}.timeline)
    }
    @Test fun audioLevelExplainsGapsAndFollowsTheSameViewport(){
        compose.onNodeWithTag("time-chart").performScrollTo()
        val node=compose.onNodeWithTag("time-chart").fetchSemanticsNode()
        assertTrue(node.config[PlotAudioLevelCount]>0)
        compose.onNodeWithText(uiText(R.string.musical_drone_description)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").performTouchInput {doubleClick()}
        assertTrue(compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotAudioLevelCount]>0)
    }
    @Test fun boundedLevelAndActualStreamPerformance(){
        val file=RecordingStore(app).file(item.id)
        val levelTimes=mutableListOf<Double>();val streamTimes=mutableListOf<Double>()
        var levelBytes=0;var duration=0.0
        repeat(4){iteration->
            Pcm16WavReader(file).use {reader->
                duration=reader.frameCount.toDouble()/reader.sampleRate
                val start=SystemClock.elapsedRealtimeNanos();val level=AudioLevel.read(reader)
                if(iteration>0)levelTimes+=(SystemClock.elapsedRealtimeNanos()-start)/1e6
                levelBytes=level.rms.size*4+level.hop*2
                val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=true)
                val chunk=ShortArray(4096);var at=0L
                while(at<reader.frameCount){val count=reader.read(at,chunk);at+=count
                    val before=SystemClock.elapsedRealtimeNanos();val result=stream.append(chunk,count)
                    if(iteration>0 && result!=null)streamTimes+=(SystemClock.elapsedRealtimeNanos()-before)/1e6
                }
            }
        }
        assertTrue(levelBytes<160000)
        val dir=File(app.getExternalFilesDir(null),"session-evidence").apply {mkdirs()}
        File(dir,"performance.json").writeText("""{"duration_s":$duration,"level_ms":$levelTimes,"level_array_bytes":$levelBytes,"stream_mean_ms":${streamTimes.average()},"stream_max_ms":${streamTimes.maxOrNull()},"stream_p95_ms":${streamTimes.sorted()[(streamTimes.size*.95).toInt()]}}""")
    }
    private class EvidenceApplication(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"session-evidence-test").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("session-evidence-$name",mode)
        fun clean(){getSharedPreferences("local_data",0).edit().clear().commit();getSharedPreferences("session_tools",0).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
