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
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Equivalent to the reported scenario, not a recording of a real performance. */
class FundamentalPrecisionUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owner=ViewModelStore()
    private lateinit var app:TestApplication
    private lateinit var vm:TallerViewModel
    private lateinit var drone:RecordingStore.Session
    private lateinit var toot:RecordingStore.Session
    private lateinit var flute:RecordingStore.Session
    private val spikes=mapOf(12 to 60.0,24 to 65.0,36 to 77.0,48 to 82.5)
    @Before fun setup(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        app=TestApplication(InstrumentationRegistry.getInstrumentation().targetContext);app.clean()
        val store=RecordingStore(app)
        fun fixture(points:List<SessionAnalysis.Point>,profile:InstrumentProfile,wav:ByteArray)=store.save(wav,points,instrument=profile)
        drone=fixture((0..59).map {SessionAnalysis.Point(it*180L,spikes[it] ?: (71.2+sin(it*.3)*.6),if(it in spikes).82 else .97)},InstrumentProfile.DIDGERIDOO,
            DeterministicPitchWav.encode(12000,DeterministicPitchWav.Section(0,12000,71.2)))
        toot=fixture((0..59).map {SessionAnalysis.Point(it*180L,when(it){29->205.0;30->207.0;else->70.0},.97)},InstrumentProfile.DIDGERIDOO,
            DeterministicPitchWav.encode(12000,DeterministicPitchWav.Section(0,4800,70.0),
                DeterministicPitchWav.Section(4800,5700,206.0),DeterministicPitchWav.Section(5700,12000,70.0)))
        flute=fixture((0..59).map {SessionAnalysis.Point(it*180L,listOf(392.0,440.0,523.0,440.0)[it/15],.94)},InstrumentProfile.NATIVE_AMERICAN_FLUTE,
            DeterministicPitchWav.encode(12000,DeterministicPitchWav.Section(0,2700,392.0),
                DeterministicPitchWav.Section(2700,5400,440.0),DeterministicPitchWav.Section(5400,8100,523.0),
                DeterministicPitchWav.Section(8100,12000,440.0)))
        compose.runOnUiThread {
            vm=TallerViewModel(app);owner.put("fundamental",vm)
            vm.renameSession(drone,"Drone con retornos · fixture");vm.renameSession(toot,"Toot breve · fixture");vm.renameSession(flute,"Flauta melódica · fixture")
        }
        compose.setContent {TallerApp(vm,{}, {},{_,_,_->},rememberNavController())}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
    }
    @After fun cleanup(){if(::vm.isInitialized)compose.runOnUiThread {owner.clear()};if(::app.isInitialized)app.clean()}
    private fun capture(name:String){
        compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(300)
        val label=InstrumentationRegistry.getArguments().getString("studioLabel") ?: "precision"
        val file=File(compose.activity.getExternalFilesDir(null),"fundamental/$label-$name.png").apply {parentFile!!.mkdirs()}
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun open(item:RecordingStore.Session){compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick();compose.onNodeWithTag("time-chart").performScrollTo()}
    private fun expand(){compose.onNodeWithTag("expand-plot").performScrollTo().performClick();compose.onNodeWithTag("immersive-plot").assertIsDisplayed()}
    private fun tools(action:String){
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithText(action).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick();compose.waitUntil(5000){compose.activity.hasWindowFocus()}
    }
    private fun measure(name:String,tag:String){
        val node=compose.onNodeWithTag(tag).fetchSemanticsNode();val area=node.config[PlotDataArea]
        android.util.Log.i("FundamentalLayout","$name total=${node.size.width} left=${area.left} data=${area.width} right=${node.size.width-area.right}")
        assertTrue(area.left>0);assertTrue(area.width>node.size.width*.55)
    }
    @Test fun reportedScenarioCardDetailAndFullscreen(){
        compose.onNodeWithTag("session-${drone.id}").performScrollTo();capture("card")
        val summary=compose.onNodeWithTag("summary-${drone.id}",useUnmergedTree=true).fetchSemanticsNode().config[PlotWindow]
        open(drone);measure("detail","time-chart");capture("detail")
        compose.waitUntil(15000){vm.savedYinReplay.sessionId==drone.id && vm.savedYinReplay.result!=null}
        compose.waitUntil(15000){vm.droneComponent.sessionId==drone.id && vm.droneComponent.curve!=null}
        val replayPreview=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow]
        assertTrue(summary.y.low<60.0) // The original 60 Hz reading remains in the library card.
        val accepted=sessionPlotPoints(vm,drone).mapNotNull {it.hz}
        assertTrue(accepted.isNotEmpty() && accepted.all {it in 69.0..73.0})
        if(InstrumentationRegistry.getArguments().getString("baseline")!="true"){
            compose.onNodeWithText(uiText(R.string.ui_frequency_in_hz_green_stable_yellow_variation_orange_review),substring=true).performScrollTo();capture("detail-legend")
        }
        expand();measure("fullscreen","immersive-plot");capture("fullscreen")
        if(InstrumentationRegistry.getArguments().getString("baseline")=="true")return
        val trace=PitchTrace.build(drone.timeline,drone.instrument)
        assertEquals(4,trace.count {it.uncertain});spikes.keys.forEach {assertEquals(it-1,trace[it+1].previous)}
        assertTrue(replayPreview.y.high<86)
        // Original readings remain byte-for-byte equivalent after opening every presentation.
        assertEquals(drone.timeline,RecordingStore(app).list().first {it.id==drone.id}.timeline)
    }
    @Test fun originalInspectionFittedPinchAndReset(){
        open(drone);compose.waitUntil(15000){vm.savedYinReplay.sessionId==drone.id && vm.savedYinReplay.result!=null}
        compose.waitUntil(15000){vm.droneComponent.sessionId==drone.id && vm.droneComponent.curve!=null}
        expand();val points=sessionScalePoints(sessionPlotPoints(vm,drone),vm.droneComponent.curve,vm.pitchLayerMode)
        val general=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        tools(uiText(R.string.ui_fitted_view));assertTrue(vm.explorer(drone.id).camera(drone.durationMs,44100,points).y.span<4);capture("fitted")
        val graph=compose.onNodeWithTag("immersive-plot");val area=graph.fetchSemanticsNode().config[PlotDataArea];val delta=Offset(area.width*.12f,area.height*.12f)
        graph.performTouchInput {pinch(area.center-delta,area.center+delta,area.center-delta*1.7f,area.center+delta*1.7f,450)}
        assertTrue(vm.explorer(drone.id).camera(drone.durationMs,44100,points).x.span<general.x.span)
        compose.onNodeWithTag("reset-viewport").performClick();assertEquals(general,graph.fetchSemanticsNode().config[PlotWindow])
        tools(uiText(R.string.ui_show_all_values));capture("original")
        val expanded=graph.fetchSemanticsNode().config[PlotDataArea]
        val x=vm.explorer(drone.id).camera(drone.durationMs,44100,points).x.fraction(2160.0).toFloat()
        graph.performTouchInput {longClick(Offset(expanded.left+expanded.width*x,expanded.center.y),800)}
        assertTrue(vm.explorer(drone.id).inspected!!.contains("Hz"))
        assertFalse(vm.explorer(drone.id).inspected!!.contains(hzLabel(60.0)))
        assertEquals(60.0,RecordingStore(app).list().first {it.id==drone.id}.timeline[12].hz!!,0.0)
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithTag("complete-reading").assertTextEquals(vm.explorer(drone.id).inspected!!);capture("inspection")
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithTag("show-uncertain").performScrollTo().performClick()
        assertFalse(vm.explorer(drone.id).showUncertain)
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        compose.onNodeWithTag("reset-viewport").performClick();assertEquals(general,graph.fetchSemanticsNode().config[PlotWindow]);capture("reset")
    }
    @Test fun shortTootAndFluteRemainVisible(){
        open(toot);expand();val camera=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        assertTrue(camera.y.high>207);assertTrue(camera.y.low<70);capture("toot")
        assertFalse(PitchTrace.build(toot.timeline,toot.instrument).any {it.uncertain})
        // Switch session through the real navigation stack.
        androidx.test.espresso.Espresso.pressBackUnconditionally();compose.waitForIdle()
        androidx.test.espresso.Espresso.pressBackUnconditionally();compose.waitForIdle()
        open(flute);expand();measure("flute","immersive-plot");capture("flute")
        val fluteCamera=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        assertTrue(fluteCamera.y.low<392 && fluteCamera.y.high>523)
    }
    private class TestApplication(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"fundamental-tests").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("fundamental-tests-$name",mode)
        fun clean(){getSharedPreferences("local_data",MODE_PRIVATE).edit().clear().commit();getSharedPreferences("session_tools",MODE_PRIVATE).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
