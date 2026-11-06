package ar.didgetaller.ui

import ar.didgetaller.R

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Private test preferences/directory; never user recordings, and never a physical device. */
class GraphIntelligenceUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owner=ViewModelStore()
    private lateinit var app:TestApplication
    private lateinit var vm:TallerViewModel
    private lateinit var nav:NavHostController
    private lateinit var a:RecordingStore.Session
    private lateinit var b:RecordingStore.Session
    private lateinit var c:RecordingStore.Session
    @Before fun setup(){
        val orientation=InstrumentationRegistry.getArguments().getString("directOrientation")
        if(orientation=="landscape"){
            compose.runOnUiThread {compose.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            compose.waitUntil(5000){compose.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE}
        }
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        app=TestApplication(InstrumentationRegistry.getInstrumentation().targetContext);app.clean()
        val store=RecordingStore(app)
        fun fixture(wav:ByteArray,points:List<SessionAnalysis.Point>,profile:InstrumentProfile)=store.save(wav,points,instrument=profile)
        val stable=(0..59).map {SessionAnalysis.Point(it*180L,70+sin(it.toDouble())*.15,.97)}
        a=fixture(DeterministicPitchWav.encode(12000,DeterministicPitchWav.Section(0,12000,70.0)),stable,InstrumentProfile.DIDGERIDOO)
        b=fixture(DeterministicPitchWav.encode(12000,DeterministicPitchWav.Section(0,2700,392.0),
            DeterministicPitchWav.Section(2700,5400,440.0),DeterministicPitchWav.Section(5400,8100,523.0),
            DeterministicPitchWav.Section(8100,12000,440.0)),
            (0..59).map {SessionAnalysis.Point(it*180L,listOf(392.0,440.0,523.0,440.0)[it/15],.94)},InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        c=fixture(DeterministicPitchWav.encode(12000,DeterministicPitchWav.Section(0,12000,129.0)),
            (0..59).map {SessionAnalysis.Point(it*180L,if(it==25)42.9 else 129+sin(it.toDouble())*.15,if(it==25).876 else .92)},InstrumentProfile.DIDGERIDOO)
        compose.runOnUiThread {
            vm=TallerViewModel(app);owner.put("intelligence",vm)
            vm.renameSession(a,"Drone estable · fixture");vm.renameSession(b,"Melodía · fixture");vm.renameSession(c,"Periodicidad ambigua · fixture")
        }
        compose.setContent {nav=rememberNavController();TallerApp(vm,{}, {},{_,_,_->},nav)}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
    }
    @After fun cleanup(){if(::vm.isInitialized)compose.runOnUiThread {owner.clear()};if(::app.isInitialized)app.clean()}
    private fun capture(name:String){
        compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(350)
        val label=InstrumentationRegistry.getArguments().getString("studioLabel") ?: "intelligence"
        val file=File(compose.activity.getExternalFilesDir(null),"stability/$label-$name.png").apply {parentFile!!.mkdirs()}
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun card(item:RecordingStore.Session)=compose.onNodeWithTag("session-${item.id}").performScrollTo()
    private fun open(item:RecordingStore.Session){card(item).performClick();compose.onNodeWithTag("time-chart").performScrollTo()}
    private fun expand(){compose.onNodeWithTag("expand-plot").performScrollTo().performClick();compose.onNodeWithTag("immersive-plot").assertIsDisplayed()}
    private fun back(){
        val previous=nav.currentDestination?.route
        compose.waitUntil(5000){compose.activity.hasWindowFocus()};Espresso.pressBackUnconditionally()
        compose.waitUntil(5000){nav.currentDestination?.route!=previous};compose.waitForIdle()
    }
    private fun tools(action:String){
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithText(action).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        compose.waitUntil(5000){compose.activity.hasWindowFocus()}
    }
    private fun zoom(){
        val graph=compose.onNodeWithTag("immersive-plot");val area=graph.fetchSemanticsNode().config[PlotDataArea]
        val delta=Offset(area.width*.15f,area.height*.1f)
        graph.performTouchInput {pinch(area.center-delta,area.center+delta,area.center-delta*1.5f,area.center+delta*1.5f,500)}
    }
    @Test fun playingGraphSeekContinuesAndConsecutiveRequestsKeepLatestPosition() {
        open(a);expand()
        compose.runOnIdle {vm.togglePlayback(a)}
        compose.waitUntil(5000){vm.playingId==a.id && !vm.playbackPaused}
        val graph=compose.onNodeWithTag("immersive-plot")
        val area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {click(Offset(area.left+area.width*.6f,area.center.y))}
        Thread.sleep(400);compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {assertFalse(vm.playbackPaused);assertEquals(a.durationMs*.6,vm.playbackPositionMs().toDouble(),1000.0)}
        graph.performTouchInput {swipe(Offset(area.left+area.width*.3f,area.center.y),Offset(area.left+area.width*.1f,area.center.y),600)}
        Thread.sleep(300);compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle {assertFalse(vm.playbackPaused);assertEquals(a.durationMs*.1,vm.playbackPositionMs().toDouble(),1000.0)}
        compose.runOnIdle {vm.seekPlayback(a,100);vm.seekPlayback(a,9000);vm.seekPlayback(a,2000)}
        Thread.sleep(500);compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {assertFalse(vm.playbackPaused);assertEquals(2500.0,vm.playbackPositionMs().toDouble(),1000.0)}
        capture("playing-seek")
        back()
        compose.runOnIdle {assertFalse(vm.playbackPaused);vm.seekPlayback(a,a.durationMs-100)}
        compose.waitUntil(5000){vm.playbackPaused}
    }

    @Test fun embeddedGraphScrubsAcrossThePlotAndPinchDoesNotSeek() {
        card(a).performClick()
        capture("embedded-open")
        capture("embedded-before")
        compose.onNodeWithTag("session-playback-seek").assertDoesNotExist()
        val graph=compose.onNodeWithTag("time-chart").performScrollTo()
        var area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {swipe(Offset(area.left+area.width*.2f,area.center.y),Offset(area.left+area.width*.8f,area.center.y),600)}
        compose.waitUntil(5000){vm.playingId==a.id}
        Thread.sleep(300);compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {assertTrue(vm.playbackPaused);assertEquals(a.durationMs*.8,vm.playbackPositionMs().toDouble(),350.0)}
        capture("embedded-selected")
        val position=vm.playbackPositionMs()
        val selection=vm.preview(a.id).selectedMs
        graph.performTouchInput {down(Offset(area.left+area.width*.4f,area.center.y));moveTo(Offset(area.left+area.width*.6f,area.center.y),100)}
        compose.runOnIdle {assertNotNull(vm.preview(a.id).selection.scrubMs)}
        capture("embedded-scrubbing")
        graph.performTouchInput {cancel()}
        compose.runOnIdle {assertNull(vm.preview(a.id).selection.scrubMs);assertEquals(selection,vm.preview(a.id).selectedMs);assertEquals(position,vm.playbackPositionMs())}
        area=graph.fetchSemanticsNode().config[PlotDataArea]
        val delta=Offset(area.width*.18f,area.height*.1f)
        val camera=graph.fetchSemanticsNode().config[PlotWindow]
        graph.performTouchInput {pinch(area.center-delta,area.center+delta,area.center-delta*1.4f,area.center+delta*1.4f,500)}
        compose.runOnIdle {assertEquals(position,vm.playbackPositionMs());assertTrue(vm.preview(a.id).camera(a.durationMs,44100,a.timeline).x.span<camera.x.span)}
        capture("embedded-zoom")
        compose.runOnIdle {vm.togglePlayback(a)}
        compose.waitUntil(5000){!vm.playbackPaused}
        Thread.sleep(200);compose.mainClock.advanceTimeBy(400)
        capture("embedded-playing")
        compose.runOnIdle {vm.togglePlayback(a)}
    }

    @Test fun graphTapAndCursorDragSeekWithoutStartingPausedAudio() {
        open(a);expand()
        compose.waitUntil(15000){vm.musicalTrace(a)!=null}
        val graph=compose.onNodeWithTag("immersive-plot")
        val area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {click(Offset(area.left+area.width*.4f,area.center.y))}
        compose.waitUntil(5000){vm.playingId==a.id}
        compose.runOnIdle {
            assertTrue(vm.playbackPaused)
            assertEquals(a.durationMs*.4,vm.playbackPositionMs().toDouble(),350.0)
        }
        graph.performTouchInput {swipe(Offset(area.left+area.width*.4f,area.center.y),Offset(area.left+area.width*.7f,area.center.y),600)}
        compose.waitUntil(5000){vm.playbackPositionMs()>a.durationMs*.6}
        compose.runOnIdle {assertTrue(vm.playbackPaused)}
        compose.onNodeWithTag("component-readout").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {assertEquals(a.durationMs*.7,vm.playbackPositionMs().toDouble(),350.0)}
        capture("scrub-paused")
        back()
        compose.runOnIdle {assertTrue(vm.playbackPaused);assertTrue(vm.playbackPositionMs()>a.durationMs*.6)}
    }

    @Test fun summaryPreviewAndReopenedSessionNeverInheritExploratoryCamera(){
        card(a);capture("card-before")
        val summary=compose.onNodeWithTag("summary-${a.id}",useUnmergedTree=true).fetchSemanticsNode().config[PlotWindow]
        open(a);val preview=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow]
        expand();repeat(4){zoom()}
        val explored=vm.explorer(a.id)
        compose.runOnIdle {explored.transform(a.durationMs,44100,a.timeline,.5,.5,1.0,1.0,.1,.1)}
        assertTrue(explored.camera(a.durationMs,44100,a.timeline).y.span<preview.y.span/3)
        back();compose.onNodeWithTag("time-chart").performScrollTo()
        assertEquals(preview,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow])
        capture("preview-after-fullscreen")
        back();card(a);assertEquals(summary,compose.onNodeWithTag("summary-${a.id}",useUnmergedTree=true).fetchSemanticsNode().config[PlotWindow]);capture("card-after")
        open(a);expand();assertNotSame(explored,vm.explorer(a.id))
        assertEquals(preview,compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]);capture("reopened-general")
        back();back();open(b);expand()
        val camera=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        assertTrue(camera.y.low<392 && camera.y.high>523);assertEquals(InstrumentProfile.NATIVE_AMERICAN_FLUTE,vm.explorer(b.id).instrument)
        capture("flute-melody")
    }
    @Test fun generalFittedExtremeZoomResetAndGraphKindsStayCoherent(){
        open(a);expand();capture("drone-general")
        compose.waitUntil(15000){vm.savedYinReplay.sessionId==a.id && vm.savedYinReplay.result!=null}
        compose.waitUntil(15000){vm.droneComponent.sessionId==a.id && vm.droneComponent.curve!=null}
        val points=musicianPlotPoints(vm.musicalTrace(a)!!)
        val general=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        tools(uiText(R.string.ui_fitted_view));assertTrue(vm.explorer(a.id).camera(a.durationMs,44100,points).y.span<2);capture("drone-fitted")
        repeat(3){zoom()}
        compose.onNodeWithTag("viewer-tools").performClick()
        repeat(10){compose.onNodeWithText(uiText(R.string.ui_zoom_out),useUnmergedTree=true).performScrollTo().performClick()}
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        assertEquals(general.y,vm.explorer(a.id).camera(a.durationMs,44100,points).y)
        compose.onNodeWithTag("reset-viewport").performClick()
        assertEquals(general,compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]);capture("after-extreme-reset")
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick();compose.onNodeWithText(uiText(R.string.ui_spectrogram)).performClick()
        compose.waitUntil(30000){vm.workspace.state.bitmap!=null && !vm.workspace.state.loading}
        assertEquals(500.0,vm.explorer(a.id).camera(a.durationMs,44100,a.timeline).y.high,0.0)
        compose.onNodeWithTag("immersive-plot").assertIsDisplayed();capture("spectrogram-portrait")
    }
    @Test fun uncertainRawPeriodRemainsInspectableAndZeroAxisHasNoMusicalName(){
        open(c);expand()
        val trace=PitchTrace.build(c.timeline,c.instrument);assertEquals(1,trace.count {it.uncertain})
        compose.waitUntil(15000){vm.savedYinReplay.sessionId==c.id && vm.savedYinReplay.result!=null}
        compose.waitUntil(15000){vm.musicalTrace(c)!=null}
        val s=vm.explorer(c.id);assertTrue(vm.musicalTrace(c)!!.samples.none {it.hz==42.9})
        tools(uiText(R.string.ui_show_all_values));assertTrue(s.showUncertain);capture("uncertain-original")
        val graph=compose.onNodeWithTag("immersive-plot");val area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {longClick(Offset(area.left+area.width*(4500f/c.durationMs),area.center.y),800)}
        assertFalse(s.inspected!!.contains(hzLabel(42.9)))
        assertTrue(s.inspected!!.contains(uiText(R.string.ui_no_nearby_reliable_reading)))
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithTag("complete-reading").assertTextEquals(s.inspected!!);capture("uncertain-reading")
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        // The exact-range tool is the intentional escape hatch outside data-driven zoom limits.
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithText(uiText(R.string.ui_enter_exact_range)).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_from, "Hz")).performTextReplacement("0")
        compose.onNodeWithText(uiText(R.string.ui_to, "Hz")).performTextReplacement("1")
        compose.onNodeWithText(uiText(R.string.ui_apply_range)).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        graph.assertContentDescriptionContains("0 Hz",substring=true)
        val desc=graph.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        assertFalse(desc.contains("C#-14"));assertFalse(desc.contains("aproximadamente"));capture("zero-hz-no-note")
        compose.onNodeWithTag("reset-viewport").performClick()
        assertEquals(vm.musicalTrace(c)!!.frequencyRange(),s.camera(c.durationMs,44100,musicianPlotPoints(vm.musicalTrace(c)!!)).y)
        assertEquals(42.9,RecordingStore(app).list().first {it.id==c.id}.timeline[25].hz!!,0.0)
        back()
        compose.runOnIdle {vm.preview(c.id).selectedMs=4500}
        compose.onNodeWithTag("session-tools").performScrollTo().performClick()
        compose.onNodeWithTag("advanced-diagnostics-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("pitch-original-diagnostic").performScrollTo()
            .assertTextContains(hzLabel(42.9),substring=true)
    }
    private class TestApplication(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"intelligence-tests").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("intelligence-tests-$name",mode)
        fun clean(){getSharedPreferences("local_data",MODE_PRIVATE).edit().clear().commit();getSharedPreferences("session_tools",MODE_PRIVATE).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
