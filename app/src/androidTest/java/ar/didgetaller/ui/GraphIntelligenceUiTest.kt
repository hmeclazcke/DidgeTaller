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
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        app=TestApplication(InstrumentationRegistry.getInstrumentation().targetContext);app.clean()
        val store=RecordingStore(app)
        fun fixture(hz:Double,points:List<SessionAnalysis.Point>,profile:InstrumentProfile)=store.save(
            WavCodec.encode(ShortArray(44100*12){i->(7000*sin(2*PI*hz*i/44100)).toInt().toShort()},44100*12,44100),points,instrument=profile)
        val stable=(0..59).map {SessionAnalysis.Point(it*180L,70+sin(it.toDouble())*.15,.97)}
        a=fixture(70.0,stable,InstrumentProfile.DIDGERIDOO)
        b=fixture(440.0,(0..59).map {SessionAnalysis.Point(it*180L,listOf(392.0,440.0,523.0,440.0)[it/15],.94)},InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        c=fixture(129.0,(0..59).map {SessionAnalysis.Point(it*180L,if(it==25)42.9 else 129+sin(it.toDouble())*.15,if(it==25).876 else .92)},InstrumentProfile.DIDGERIDOO)
        compose.runOnUiThread {
            vm=TallerViewModel(app);owner.put("intelligence",vm)
            vm.renameSession(a,"Drone estable · fixture");vm.renameSession(b,"Melodía · fixture");vm.renameSession(c,"Periodicidad ambigua · fixture")
        }
        compose.setContent {nav=rememberNavController();TallerApp(vm,{}, {},{_,_,_->},nav)}
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
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
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.waitUntil(5000){compose.activity.hasWindowFocus()}
    }
    private fun zoom(){
        val graph=compose.onNodeWithTag("immersive-plot");val area=graph.fetchSemanticsNode().config[PlotDataArea]
        val delta=Offset(area.width*.15f,area.height*.1f)
        graph.performTouchInput {pinch(area.center-delta,area.center+delta,area.center-delta*1.5f,area.center+delta*1.5f,500)}
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
        val general=vm.explorer(a.id).camera(a.durationMs,44100,a.timeline)
        tools("Vista ajustada");assertTrue(vm.explorer(a.id).camera(a.durationMs,44100,a.timeline).y.span<2);capture("drone-fitted")
        repeat(3){zoom()}
        compose.onNodeWithTag("viewer-tools").performClick()
        repeat(10){compose.onNodeWithText("Alejar",useUnmergedTree=true).performScrollTo().performClick()}
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        assertEquals(general.y,vm.explorer(a.id).camera(a.durationMs,44100,a.timeline).y)
        compose.onNodeWithTag("reset-viewport").performClick()
        assertEquals(general,compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]);capture("after-extreme-reset")
        compose.onNodeWithContentDescription("Cambiar gráfico").performClick();compose.onNodeWithText("Espectrograma").performClick()
        compose.waitUntil(30000){vm.workspace.state.bitmap!=null && !vm.workspace.state.loading}
        assertEquals(500.0,vm.explorer(a.id).camera(a.durationMs,44100,a.timeline).y.high,0.0)
        compose.onNodeWithTag("immersive-plot").assertIsDisplayed();capture("spectrogram-portrait")
    }
    @Test fun uncertainRawPeriodRemainsInspectableAndZeroAxisHasNoMusicalName(){
        open(c);expand()
        val trace=PitchTrace.build(c.timeline,c.instrument);assertEquals(1,trace.count {it.uncertain})
        val s=vm.explorer(c.id);assertTrue(s.camera(c.durationMs,44100,c.timeline).y.low>100)
        tools("Mostrar todos los valores");assertTrue(s.showUncertain);capture("uncertain-original")
        val graph=compose.onNodeWithTag("immersive-plot");val area=graph.fetchSemanticsNode().config[PlotDataArea]
        graph.performTouchInput {longClick(Offset(area.left+area.width*(4500f/c.durationMs),area.center.y),800)}
        assertTrue(s.inspected!!.contains("estimación incierta"));assertTrue(s.inspected!!.contains("42,9 Hz"));assertFalse(s.inspected!!.contains("cents"))
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithTag("complete-reading").assertTextEquals(s.inspected!!);capture("uncertain-reading")
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        // The exact-range tool is the intentional escape hatch outside data-driven zoom limits.
        compose.onNodeWithTag("viewer-tools").performClick();compose.onNodeWithText("Ingresar rango exacto…").performScrollTo().performClick()
        compose.onNodeWithText("Desde Hz").performTextReplacement("0")
        compose.onNodeWithText("Hasta Hz").performTextReplacement("1")
        compose.onNodeWithText("Aplicar rango").performScrollTo().performClick()
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        graph.assertContentDescriptionContains("0,00 Hz",substring=true)
        val desc=graph.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        assertFalse(desc.contains("C#-14"));assertFalse(desc.contains("aproximadamente"));capture("zero-hz-no-note")
        compose.onNodeWithTag("reset-viewport").performClick();assertTrue(s.camera(c.durationMs,44100,c.timeline).y.low>100)
        assertEquals(42.9,RecordingStore(app).list().first {it.id==c.id}.timeline[25].hz!!,0.0)
    }
    private class TestApplication(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"intelligence-tests").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("intelligence-tests-$name",mode)
        fun clean(){getSharedPreferences("local_data",MODE_PRIVATE).edit().clear().commit();getSharedPreferences("session_tools",MODE_PRIVATE).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
