package ar.didgetaller.ui

import ar.didgetaller.R

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.MainActivity
import ar.didgetaller.TallerViewModel
import ar.didgetaller.SessionPhase
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Real Activity, system Back and multi-pointer injection; fixtures exclusively on emulator. */
class PrecisionUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:TallerViewModel
    private lateinit var store:RecordingStore
    private val owned=mutableListOf<String>()
    @Before fun setup(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        compose.runOnUiThread {vm=ViewModelProvider(compose.activity)[TallerViewModel::class.java]}
        store=RecordingStore(compose.activity.application)
    }
    @After fun cleanup(){
        if(::vm.isInitialized)compose.runOnUiThread {vm.microphoneStop();vm.stopPlayback();vm.workspace.clear();compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED}
        owned.forEach {store.delete(it)}
    }
    private fun capture(name:String){
        val label=InstrumentationRegistry.getArguments().getString("studioLabel") ?: "precision"
        val file=File(compose.activity.getExternalFilesDir(null),"stability/$label-$name.png").apply {parentFile!!.mkdirs()}
        compose.waitForIdle()
        // Native destination/window drawing can trail Compose semantics for a frame.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    private fun tapLabeledControl(label:String){
        val root=checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
        val visible=mutableListOf<String>()
        fun find(node:AccessibilityNodeInfo):AccessibilityNodeInfo? {
            val value=node.text?.toString() ?: node.contentDescription?.toString()
            if(value!=null)visible+=value
            if(value==label){
                var target:AccessibilityNodeInfo?=node
                while(target!=null && !target.isClickable)target=target.parent
                if(target!=null)return target
            }
            for(index in 0 until node.childCount){
                val child=node.getChild(index) ?: continue
                find(child)?.let {return it}
            }
            return null
        }
        val target=checkNotNull(find(root)){"Labeled control not found: $label; visible=$visible"}
        val bounds=Rect().also {target.getBoundsInScreen(it)}
        assertFalse("Accessible action has no visible bounds: $label",bounds.isEmpty)
        val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "input tap ${bounds.centerX()} ${bounds.centerY()}"
        )
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {it.readBytes()}
    }
    private fun accessibleTextVisible(label:String):Boolean {
        val root=InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow ?: return false
        fun contains(node:AccessibilityNodeInfo):Boolean {
            if(node.text?.toString()==label || node.contentDescription?.toString()==label)return true
            for(index in 0 until node.childCount){
                val child=node.getChild(index) ?: continue
                if(contains(child))return true
            }
            return false
        }
        return contains(root)
    }
    private fun fixture():RecordingStore.Session {
        val pcm=ShortArray(44100*20){i->if(i in 44100*7..44100*8)0 else (7000*sin(2*PI*70*i/44100)).toInt().toShort()}
        val points=(0..99).map {i->SessionAnalysis.Point(i*200L,when(i){in 35..40->null;25->35.0;else->70+sin(i.toDouble())*.15},if(i==25).3 else .97)}
        val item=store.save(WavCodec.encode(pcm,pcm.size,44100),points);owned+=item.id
        compose.runOnIdle {vm.renameSession(item,"Estabilidad · prueba sintética")}
        return item
    }
    private fun detail(item:RecordingStore.Session){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo()
        capture("library-same-session")
        compose.onNodeWithTag("session-${item.id}").performClick()
        compose.onNodeWithTag("time-chart").performScrollTo()
        capture("fundamental-detail")
    }
    private fun pick(title:String){
        val expected=AcousticPlot.entries.single {compose.activity.plotLabel(it)==title}
        val sessionId=checkNotNull(vm.workspace.state.sessionId)
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick();compose.onNodeWithText(title).performClick()
        compose.waitUntil(10000){
            vm.explorer(sessionId).plot==expected && compose.onAllNodes(isPopup()).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("immersive-plot").assertIsDisplayed()
    }
    @Test fun systemBackFromWideReturnsToMeasureRepeatedlyWithoutFinishingActivity(){
        capture("measure-normal")
        repeat(3){
            compose.onNodeWithText(uiText(R.string.ui_wide_view)).performScrollTo().performClick()
            compose.onNodeWithTag("measure-wide-screen").assertIsDisplayed()
            if(it==0)capture("measure-wide")
            val resources=compose.activity.resources
            val mode=resources.getIdentifier("config_navBarInteractionMode","integer","android")
            if(it==2 && mode!=0 && resources.getInteger(mode)==2){
                val metrics=resources.displayMetrics
                val fd=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input swipe 1 ${metrics.heightPixels/2} ${metrics.widthPixels/3} ${metrics.heightPixels/2} 350")
                android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use {stream->stream.readBytes()}
            }else Espresso.pressBackUnconditionally()
            compose.waitUntil(5000){!vm.fullScreen}
            compose.onNodeWithTag("measure-screen").assertIsDisplayed()
            compose.runOnIdle {assertFalse(vm.fullScreen);assertFalse(compose.activity.isFinishing);assertEquals(SessionPhase.IDLE,vm.sessionPhase);assertFalse(vm.listening)}
        }
    }
    @Test fun wideBackKeepsListeningWithoutCreatingARecording(){
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,Manifest.permission.RECORD_AUDIO)
        val before=vm.sessions.map {it.id}.toSet()
        compose.runOnIdle {vm.microphoneStart()}
        compose.waitUntil(10000){vm.listening}
        compose.onNodeWithText(uiText(R.string.ui_wide_view)).performScrollTo().performClick()
        Espresso.pressBackUnconditionally()
        compose.waitUntil(5000){!vm.fullScreen}
        compose.onNodeWithTag("measure-screen").assertIsDisplayed()
        compose.runOnIdle {assertTrue(vm.listening);assertEquals(SessionPhase.IDLE,vm.sessionPhase);assertEquals(before,vm.sessions.map {it.id}.toSet())}
    }
    @Test fun widePresentationToggleKeepsSameActiveCaptureTimelineAndWav(){
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,Manifest.permission.RECORD_AUDIO)
        val wideLabel=uiText(R.string.ui_wide_view)
        val normalLabel=uiText(R.string.ui_normal_view)
        if(vm.fullScreen){
            tapLabeledControl(normalLabel)
            compose.waitUntil(5000){!vm.fullScreen}
        }
        val previous=vm.sessions.map {it.id}.toSet()
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).performClick()
        compose.waitUntil(10000){vm.listening && vm.sessionTimeline.size>=4}
        val start=vm.sessionStartedAtMs
        val prefix=vm.sessionTimeline.toList()
        val partial=File(compose.activity.filesDir,"recordings").listFiles()!!.single {it.extension=="part"}
        val size=partial.length()
        tapLabeledControl(wideLabel)
        compose.waitUntil(5000){vm.fullScreen}
        capture("measure-wide-capturing")
        tapLabeledControl(normalLabel)
        compose.waitUntil(5000){
            !vm.fullScreen && accessibleTextVisible(wideLabel)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(SessionPhase.CAPTURING,vm.sessionPhase);assertTrue(vm.listening)
        assertEquals(start,vm.sessionStartedAtMs);assertEquals(previous,vm.sessions.map {it.id}.toSet())
        assertEquals(prefix,vm.sessionTimeline.take(prefix.size));assertTrue(partial.exists() && partial.length()>=size)
        tapLabeledControl(uiText(R.string.ui_stop_and_save))
        compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE && vm.sessions.any {it.id !in previous}}
        val saved=vm.sessions.filter {it.id !in previous};owned+=saved.map {it.id}
        assertEquals(1,saved.size);assertTrue(saved.single().timeline.size>=prefix.size)
    }
    @Test fun rotationAndSystemBackPreserveCaptureButBackgroundStillSavesOnce(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(compose.activity.packageName,Manifest.permission.RECORD_AUDIO)
        val previous=store.list().map {it.id}.toSet()
        try {
            compose.onNodeWithText(uiText(R.string.ui_start_measurement)).performClick()
            compose.waitUntil(10000){vm.sessionPhase==SessionPhase.CAPTURING && vm.sessionTimeline.size>=3}
            val start=vm.sessionStartedAtMs
            val prefix=vm.sessionTimeline.toList()
            val partial=File(compose.activity.filesDir,"recordings").listFiles()!!.single {it.extension=="part"}
            compose.onNodeWithText(uiText(R.string.ui_wide_view)).performScrollTo().performClick()
            compose.runOnUiThread {compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
            compose.onNodeWithTag("measure-wide-screen").assertIsDisplayed()
            compose.runOnIdle {
                assertSame(vm,ViewModelProvider(compose.activity)[TallerViewModel::class.java])
                assertEquals(SessionPhase.CAPTURING,vm.sessionPhase)
                assertEquals(start,vm.sessionStartedAtMs)
                assertEquals(prefix,vm.sessionTimeline.take(prefix.size))
                assertTrue(partial.exists())
            }
            compose.onNodeWithTag("acoustic-signal-status").assertIsDisplayed()
            compose.onNodeWithTag("measurement-frequency").assertIsDisplayed()
            val statusBounds=compose.onNodeWithTag("acoustic-signal-status").fetchSemanticsNode().boundsInRoot
            val frequencyBounds=compose.onNodeWithTag("measurement-frequency").fetchSemanticsNode().boundsInRoot
            val transportBounds=compose.onNodeWithText(uiText(R.string.ui_stop_and_save)).fetchSemanticsNode().boundsInRoot
            assertTrue("Frequency and signal status fit above transport",maxOf(statusBounds.bottom,frequencyBounds.bottom)<=transportBounds.top)
            val noteName=compose.onNodeWithTag("measurement-note-name").getUnclippedBoundsInRoot()
            val transport=compose.onNodeWithText(uiText(R.string.ui_stop_and_save)).getUnclippedBoundsInRoot()
            assertTrue("The complete note label remains visible above transport",noteName.bottom<=transport.top)
            capture("capture-landscape")
            // The recording contract uses a deterministic system Back event. An
            // actual edge gesture remains covered by the dedicated gesture tests.
            Espresso.pressBackUnconditionally()
            compose.waitUntil(10000){!vm.fullScreen}
            compose.onNodeWithTag("measure-screen").assertIsDisplayed()
            compose.runOnIdle {assertEquals(SessionPhase.CAPTURING,vm.sessionPhase);assertEquals(start,vm.sessionStartedAtMs)}
            compose.onNodeWithTag("recording-primary").assertIsDisplayed()
            compose.onNodeWithTag("discard-recording").assertIsDisplayed()
            val normalNote=compose.onNodeWithTag("measurement-note-name").getUnclippedBoundsInRoot()
            val normalStatus=compose.onNodeWithTag("acoustic-signal-status").getUnclippedBoundsInRoot()
            val normalControls=compose.onNodeWithTag("recording-primary").getUnclippedBoundsInRoot()
            assertTrue("The normal landscape reading must fit above the controls",maxOf(normalNote.bottom,normalStatus.bottom)<=normalControls.top)
            capture("capture-landscape-normal")
            compose.onNodeWithTag("discard-recording").performClick()
            compose.onNodeWithText(uiText(R.string.recording_discard_message)).assertIsDisplayed()
            val discardMessage=compose.onNodeWithText(uiText(R.string.recording_discard_message)).fetchSemanticsNode()
            assertEquals("The complete discard message must be visible",discardMessage.size.height.toFloat(),discardMessage.boundsInRoot.height,1f)
            val discardLayouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(uiText(R.string.recording_discard_message)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){it(discardLayouts)}
            assertFalse("The discard message must not overflow",discardLayouts.single().didOverflowHeight)
            compose.onNodeWithTag("confirm-discard").assertIsDisplayed()
            compose.onNodeWithTag("continue-recording").assertIsDisplayed()
            capture("capture-landscape-confirm")
            compose.onNodeWithTag("continue-recording").performClick()
            compose.runOnIdle {assertEquals(SessionPhase.CAPTURING,vm.sessionPhase);assertEquals(start,vm.sessionStartedAtMs)}
            compose.runOnUiThread {compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT}
            compose.runOnIdle {assertEquals(SessionPhase.CAPTURING,vm.sessionPhase);assertTrue(vm.listening)}
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE && store.list().any {it.id !in previous}}
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            compose.runOnIdle {assertFalse(vm.listening);assertFalse(compose.activity.isFinishing)}
            assertEquals(1,store.list().count {it.id !in previous})
        } finally {
            compose.runOnUiThread {vm.microphoneStop()}
            compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE}
            owned+=store.list().filter {it.id !in previous}.map {it.id}
        }
    }
    @Test fun canonicalPitchAndBothScalesAndCompleteInspectionRemainAvailable(){
        val item=fixture();detail(item)
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        val state=vm.explorer(item.id)
        compose.runOnIdle {assertTrue(state.camera(item.durationMs,44100,item.timeline).y.span>20);vm.analyzeRecording(item)}
        compose.waitUntil(30000){vm.recordingAnalysis.result!=null}
        compose.waitUntil(30000){vm.savedYinReplay.sessionId==item.id && vm.savedYinReplay.result!=null}
        compose.waitUntil(30000){vm.droneComponent.sessionId==item.id && vm.droneComponent.curve!=null}
        val points=sessionPlotPoints(vm,item)
        val scalePoints=musicianPlotPoints(vm.musicalTrace(item)!!)
        compose.runOnIdle {
            assertSame(vm.savedYinReplay.result!!.points,points)
            assertEquals(item.timeline,store.list().first {it.id==item.id}.timeline)
            assertTrue(points.any {it.hz!=null && it.hz in 67.0..73.0})
            assertTrue(points.count {it.timeMs in 7400..8000 && it.hz==null}>=2)
        }
        capture("fundamental-general")
        val originalSpan=state.camera(item.durationMs,44100,scalePoints).y.span
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithText(uiText(R.string.ui_fitted_view)).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        compose.runOnIdle {assertTrue(state.camera(item.durationMs,44100,scalePoints).y.span<originalSpan)}
        capture("fundamental-fitted")
        val graph=compose.onNodeWithTag("immersive-plot")
        val area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {longClick(Offset(area.left+area.width*.2f,area.center.y),800)}
        val reading=state.inspected!!
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithTag("complete-reading").assertTextEquals(reading)
        val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag("complete-reading").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){it(layouts)}
        capture("tools-complete-reading")
        android.util.Log.i("PrecisionLayout","reading size=${layouts.single().size} lines=${layouts.single().lineCount} widthOverflow=${layouts.single().didOverflowWidth} heightOverflow=${layouts.single().didOverflowHeight} text=$reading")
        assertFalse(layouts.single().hasVisualOverflow)
        compose.onNodeWithText(uiText(R.string.ui_overview)).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        compose.onNodeWithTag("reset-viewport").performClick()
        compose.runOnIdle {assertTrue(state.camera(item.durationMs,44100,scalePoints).y.span>20);assertEquals(reading,state.inspected)}
    }
    @Test fun continuousRadialPinchPanRotationAndResetUseDataCoordinates(){
        val item=fixture();detail(item)
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        pick(uiText(R.string.ui_spectrogram))
        compose.waitUntil(90000){vm.workspace.state.spectrogram!=null && !vm.workspace.state.loading}
        capture("spectrogram-portrait")
        val graph=compose.onNodeWithTag("immersive-plot");val state=vm.explorer(item.id)
        val initial=state.camera(item.durationMs,44100,item.timeline)
        val reference=state.spectrumReference
        fun pinch(horizontal:Boolean,ratio:Float){
            val a=graph.fetchSemanticsNode().config[PlotDataArea]
            val center=a.center
            val delta=if(horizontal)Offset(a.width*.20f,0f) else Offset(0f,a.height*.20f)
            graph.performTouchInput {pinch(start0=center-delta,start1=center+delta,end0=center-delta*ratio,end1=center+delta*ratio,durationMillis=600)}
        }
        pinch(true,1.5f)
        var camera=state.camera(item.durationMs,44100,item.timeline)
        assertEquals(initial.x.span/1.5,camera.x.span,initial.x.span*.02)
        assertEquals(initial.y.span/1.5,camera.y.span,initial.y.span*.02)
        assertEquals(initial.x.value(.5),camera.x.value(.5),20.0)
        assertEquals(initial.y.value(.5),camera.y.value(.5),1.0)
        val beforePan=camera
        val area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {swipe(area.center,area.center+Offset(area.width*.08f,-area.height*.08f),500)}
        camera=state.camera(item.durationMs,44100,item.timeline)
        assertTrue(camera.x.low<beforePan.x.low);assertTrue(camera.y.low<beforePan.y.low)
        val preserved=camera
        compose.onNodeWithTag("rotate-viewer").performClick()
        compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
        compose.runOnIdle {assertEquals(preserved,state.camera(item.durationMs,44100,item.timeline))}
        pinch(false,1.3f)
        val after=state.camera(item.durationMs,44100,item.timeline)
        assertEquals(preserved.x.span/1.3,after.x.span,preserved.x.span*.02)
        assertEquals(preserved.y.span/1.3,after.y.span,preserved.y.span*.02)
        pinch(false,1/1.3f)
        compose.onNodeWithTag("reset-viewport").performClick()
        compose.waitUntil(30000){!vm.workspace.state.loading}
        compose.runOnIdle {assertEquals(initial,state.camera(item.durationMs,44100,item.timeline));assertEquals(reference,state.spectrumReference,1e-16)}
        val root=compose.onNodeWithTag("acoustic-viewer").fetchSemanticsNode().boundsInRoot
        val data=graph.fetchSemanticsNode().config[PlotDataArea]
        assertTrue("Preserve dominant data area",data.height/root.height>=.65f)
        capture("spectrogram-landscape")
        graph.performTouchInput {longClick(data.center,800)}
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithTag("complete-reading").assertTextEquals(state.inspected!!)
        capture("spectrogram-reading-tools")
        // The previous logarithmic toggle crashed at the lowest possible linear bin.
        compose.runOnIdle {state.transform(item.durationMs,44100,item.timeline,.5,1.0,1.0,1e20,0.0,0.0)}
        compose.onNodeWithTag("log-frequency").performScrollTo().performClick()
        compose.runOnIdle {val y=state.camera(item.durationMs,44100,item.timeline).y;assertTrue(y.low>0 && y.high>y.low && y.high<=22050.00001)}
        compose.onNodeWithTag("log-frequency").performClick()
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        compose.onNodeWithTag("reset-viewport").performClick()
        pick(uiText(R.string.ui_waveform));compose.waitUntil(30000){vm.workspace.state.wave!=null && !vm.workspace.state.loading}
        pinch(true,1.3f)
        assertTrue(state.camera(item.durationMs,44100,item.timeline).y.span<2)
        pick(uiText(R.string.ui_harmonics));compose.runOnIdle {vm.analyzeRecording(item)}
        compose.waitUntil(30000){vm.recordingAnalysis.result!=null}
        pinch(true,1.3f)
        assertTrue(state.camera(item.durationMs,44100,item.timeline).x.span<500)
        compose.onNodeWithTag("rotate-viewer").performClick()
        compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT}
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        compose.onNodeWithTag("session-detail").assertIsDisplayed()
    }
}
