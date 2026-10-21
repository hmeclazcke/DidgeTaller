package ar.didgetaller.ui

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
        fun fixture(points:List<SessionAnalysis.Point>,profile:InstrumentProfile)=store.save(
            WavCodec.encode(ShortArray(44100*12){i->(7000*sin(2*PI*70*i/44100)).toInt().toShort()},44100*12,44100),points,instrument=profile)
        drone=fixture((0..59).map {SessionAnalysis.Point(it*180L,spikes[it] ?: (71.2+sin(it*.3)*.6),if(it in spikes).82 else .97)},InstrumentProfile.DIDGERIDOO)
        toot=fixture((0..59).map {SessionAnalysis.Point(it*180L,when(it){29->205.0;30->207.0;else->70.0},.97)},InstrumentProfile.DIDGERIDOO)
        flute=fixture((0..59).map {SessionAnalysis.Point(it*180L,listOf(392.0,440.0,523.0,440.0)[it/15],.94)},InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        compose.runOnUiThread {
            vm=TallerViewModel(app);owner.put("fundamental",vm)
            vm.renameSession(drone,"Drone con retornos · fixture");vm.renameSession(toot,"Toot breve · fixture");vm.renameSession(flute,"Flauta melódica · fixture")
        }
        compose.setContent {TallerApp(vm,{}, {},{_,_,_->},rememberNavController())}
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
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
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick();compose.waitUntil(5000){compose.activity.hasWindowFocus()}
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
        val preview=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow]
        assertEquals(summary,preview)
        if(InstrumentationRegistry.getArguments().getString("baseline")!="true"){
            compose.onNodeWithText("Verde: estable · Amarillo: variación · Naranja: revisar").performScrollTo();capture("detail-legend")
        }
        expand();measure("fullscreen","immersive-plot");capture("fullscreen")
        if(InstrumentationRegistry.getArguments().getString("baseline")=="true")return
        val trace=PitchTrace.build(drone.timeline,drone.instrument)
        assertEquals(4,trace.count {it.uncertain});spikes.keys.forEach {assertEquals(it-1,trace[it+1].previous)}
        assertTrue(preview.y.low>59);assertTrue(preview.y.high<86)
        // Original readings remain byte-for-byte equivalent after opening every presentation.
        assertEquals(drone.timeline,RecordingStore(app).list().first {it.id==drone.id}.timeline)
    }
    @Test fun originalInspectionFittedPinchAndReset(){
        open(drone);expand();val general=vm.explorer(drone.id).camera(drone.durationMs,44100,drone.timeline)
        tools("Vista ajustada");assertTrue(vm.explorer(drone.id).camera(drone.durationMs,44100,drone.timeline).y.span<4);capture("fitted")
        val graph=compose.onNodeWithTag("immersive-plot");val area=graph.fetchSemanticsNode().config[PlotDataArea];val delta=Offset(area.width*.12f,area.height*.12f)
        graph.performTouchInput {pinch(area.center-delta,area.center+delta,area.center-delta*1.7f,area.center+delta*1.7f,450)}
        assertTrue(vm.explorer(drone.id).camera(drone.durationMs,44100,drone.timeline).x.span<general.x.span)
        compose.onNodeWithTag("reset-viewport").performClick();assertEquals(general,graph.fetchSemanticsNode().config[PlotWindow])
        tools("Mostrar todos los valores");capture("original")
        val expanded=graph.fetchSemanticsNode().config[PlotDataArea]
        val x=vm.explorer(drone.id).camera(drone.durationMs,44100,drone.timeline).x.fraction(2160.0).toFloat()
        graph.performTouchInput {longClick(Offset(expanded.left+expanded.width*x,expanded.center.y),800)}
        assertTrue(vm.explorer(drone.id).inspected!!.contains("estimación incierta"));assertTrue(vm.explorer(drone.id).inspected!!.contains("60,0 Hz"))
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithTag("complete-reading").assertTextEquals(vm.explorer(drone.id).inspected!!);capture("inspection")
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithTag("show-uncertain").performScrollTo().performClick()
        assertFalse(vm.explorer(drone.id).showUncertain)
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
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
