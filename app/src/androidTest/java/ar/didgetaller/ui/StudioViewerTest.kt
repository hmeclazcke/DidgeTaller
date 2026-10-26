package ar.didgetaller.ui

import ar.didgetaller.R

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import ar.didgetaller.MainActivity
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

class StudioViewerTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:TallerViewModel
    private lateinit var item:RecordingStore.Session
    private lateinit var store:RecordingStore
    private val extraFixtures=mutableListOf<String>()
    private var originalInstrument=InstrumentProfile.DIDGERIDOO
    @Before fun setup(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        store=RecordingStore(compose.activity.application)
        val pcm=ShortArray(44100*8){i->(6000*sin(2*PI*69.6*i/44100)+3500*sin(2*PI*139.2*i/44100)+
            1800*sin(2*PI*208.8*i/44100)).toInt().toShort()}
        item=store.save(WavCodec.encode(pcm,pcm.size,44100),(0..39).map {SessionAnalysis.Point(it*200L,69.6+.15*sin(it.toDouble()),.97)})
        compose.runOnUiThread {vm=ViewModelProvider(compose.activity)[TallerViewModel::class.java];originalInstrument=vm.instrument;vm.renameSession(item,"Sintética · Acoustic Studio")}
    }
    @After fun cleanup(){
        if(::vm.isInitialized)compose.runOnUiThread {vm.microphoneStop();vm.stopPlayback();vm.workspace.clear();vm.selectInstrument(originalInstrument);compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED}
        if(::item.isInitialized)store.delete(item.id)
        extraFixtures.forEach {store.delete(it)}
    }
    private fun capture(name:String){
        val label=androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("studioLabel") ?: "default"
        val path=File(compose.activity.getExternalFilesDir(null),"stability/$label-$name.png").apply {parentFile!!.mkdirs()}
        path.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun landscapeAndPortraitEvidence(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.runOnIdle {vm.preview(item.id).plot=AcousticPlot.SPECTROGRAM}
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.waitUntil(30000){vm.workspace.state.spectrogram!=null && !vm.workspace.state.loading}
        capture("spectrogram-portrait")
        compose.onNodeWithTag("rotate-viewer").performClick()
        compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
        compose.waitForIdle()
        val plot=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().boundsInRoot
        val root=compose.onNodeWithTag("acoustic-viewer").fetchSemanticsNode().boundsInRoot
        val area=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotDataArea]
        Log.i("StudioLayout","root=$root plot=$plot fraction=${plot.height/root.height} dataFraction=${area.height/root.height} density=${compose.activity.resources.displayMetrics.density} font=${compose.activity.resources.configuration.fontScale}")
        assertTrue("The graph must dominate",plot.height/root.height>=.70f)
        assertTrue("Data, excluding axes",area.height/root.height>=if(compose.activity.resources.configuration.fontScale>1.2f).60f else .65f)
        capture("spectrogram-landscape")
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick()
        compose.onNodeWithText(uiText(R.string.musical_drone_title)).performClick()
        capture("fundamental-landscape")
        compose.onNodeWithTag("immersive-plot").assertContentDescriptionContains("Hz",substring=true)
        val graph=compose.onNodeWithTag("immersive-plot")
        val pitchArea=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {longClick(androidx.compose.ui.geometry.Offset(pitchArea.left+pitchArea.width*.25f,pitchArea.center.y),800)}
        compose.runOnIdle {
            assertEquals(2000.0,vm.explorer(item.id).selectedMs.toDouble(),20.0)
            assertTrue(vm.explorer(item.id).inspected!!.contains("Hz"))
        }
        capture("fundamental-cursor-landscape")
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick()
        compose.onNodeWithText(uiText(R.string.ui_waveform)).performClick()
        compose.runOnIdle {assertNull("A reading belongs to its graph",vm.explorer(item.id).inspected)}
        compose.waitUntil(30000){vm.workspace.state.wave!=null && !vm.workspace.state.loading}
        capture("wave-landscape")
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick()
        compose.onNodeWithText(uiText(R.string.ui_harmonics)).performClick()
        compose.runOnIdle {assertNull(vm.explorer(item.id).inspected);vm.analyzeRecording(item)}
        compose.waitUntil(30000){vm.recordingAnalysis.result!=null}
        capture("harmonics-landscape")
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        compose.onNodeWithTag("session-detail").assertIsDisplayed()
    }
    @Test fun fluteCapturePersistsProfileAndSessionUsesItsOwnProfile(){
        compose.onNodeWithTag("instrument-selector").performClick()
        compose.onNodeWithText(uiText(R.string.instrument_native_flute)).performClick()
        capture("measure-flute")
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,android.Manifest.permission.RECORD_AUDIO)
        val previous=vm.sessions.map {it.id}.toSet()
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).performClick()
        compose.waitUntil(10000){vm.listening && vm.sessionTimeline.size>=3}
        compose.onNodeWithTag("instrument-selector").assertIsNotEnabled()
        compose.onNodeWithText(uiText(R.string.ui_stop_and_save)).performClick()
        compose.waitUntil(10000){vm.sessionPhase==ar.didgetaller.SessionPhase.IDLE && vm.sessions.any {it.id !in previous}}
        val captured=vm.sessions.first {it.id !in previous};extraFixtures+=captured.id
        assertEquals(InstrumentProfile.NATIVE_AMERICAN_FLUTE,captured.instrument)
        // Real generated PCM + detector timeline, exclusively in the emulator.
        val pcm=ShortArray(44100*4){i->
            val hz=when(i/44100){0->440.0;1->392.0;2->0.0;else->659.26}
            (6000*sin(2*PI*hz*i/44100)).toInt().toShort()
        }
        val points=(0..(pcm.size-8192) step 4096).map {start->
            val r=PitchDetector.analyze(pcm.copyOfRange(start,start+8192),44100,200,1600,false)
            SessionAnalysis.Point((start+4096)*1000L/44100,r.hz.takeIf {it>0 && r.confidence>=.8},r.confidence)
        }
        val flute=store.save(WavCodec.encode(pcm,pcm.size,44100),points,instrument=InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        extraFixtures+=flute.id
        compose.runOnIdle {vm.renameSession(flute,"Melodía sintética · Flauta");vm.selectInstrument(InstrumentProfile.DIDGERIDOO)}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${flute.id}").performScrollTo().performClick()
        compose.onNodeWithTag("session-detail").assertIsDisplayed()
        capture("flute-detail")
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("acoustic-viewer").assertIsDisplayed()
        compose.runOnIdle {
            val state=vm.explorer(flute.id)
            assertEquals(InstrumentProfile.NATIVE_AMERICAN_FLUTE,state.instrument)
            val range=state.camera(flute.durationMs,44100,flute.timeline).y
            assertTrue(range.high>659);assertTrue(range.low<392)
        }
        capture("flute-fundamental-portrait")
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick()
        compose.onNodeWithText(uiText(R.string.ui_spectrogram)).performClick()
        compose.waitUntil(30000){vm.workspace.state.spectrogram!=null && !vm.workspace.state.loading}
        compose.runOnIdle {assertEquals(2000.0,vm.explorer(flute.id).camera(flute.durationMs,44100,flute.timeline).y.high,0.0)}
        capture("flute-spectrogram-portrait")
        val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag("viewer-time").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){it(layouts)}
        assertFalse("Transport time must remain complete",layouts.single().isLineEllipsized(0))
        compose.onNodeWithTag("immersive-plot").assertContentDescriptionContains(uiText(R.string.ui_spectrogram),substring=true)
        assertFalse(compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString().contains(uiText(R.string.ui_hz_axis_and_base_note)))
    }
}
