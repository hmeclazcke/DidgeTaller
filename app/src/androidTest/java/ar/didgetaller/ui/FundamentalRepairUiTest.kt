package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
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
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File

/** Exact read-only exported timelines supplied locally; no real WAV copied into the emulator. */
class FundamentalRepairUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var app:TestApplication
    private lateinit var vm:TallerViewModel
    private lateinit var nav:NavHostController
    private val owner=ViewModelStore()
    private val items=mutableListOf<Pair<String,RecordingStore.Session>>()
    @Before fun setup(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        val source=File(compose.activity.getExternalFilesDir(null),"v324-real-fixtures.json")
        assumeTrue("Fixture privado local requerido para comparación real",source.isFile)
        app=TestApplication(InstrumentationRegistry.getInstrumentation().targetContext);app.clean()
        val store=RecordingStore(app);val raw=JSONArray(source.readText())
        repeat(raw.length()){i->
            val session=raw.getJSONObject(i);val timeline=session.getJSONArray("timeline")
            val points=(0 until timeline.length()).map {j->val a=timeline.getJSONArray(j);SessionAnalysis.Point(a.getLong(0),if(a.isNull(1))null else a.getDouble(1),a.optDouble(2,0.0))}
            val frames=(session.getLong("durationMs")*44100/1000).toInt()
            items+=session.getString("label") to store.save(WavCodec.encode(ShortArray(frames),frames,44100),points,session.getLong("createdAt"))
        }
        compose.runOnUiThread {
            vm=TallerViewModel(app);owner.put("repair",vm)
            items.forEach {(label,item)->vm.renameSession(item,"${if(label=="D2")"Re · D2" else "Mib · Eb2"} · lecturas reales")}
        }
        compose.setContent {nav=rememberNavController();TallerApp(vm,{}, {},{_,_,_->},nav)}
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
    }
    @After fun cleanup(){if(::vm.isInitialized)compose.runOnUiThread {owner.clear()};if(::app.isInitialized)app.clean()}
    private fun capture(name:String){
        compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(350)
        val label=InstrumentationRegistry.getArguments().getString("studioLabel") ?: "repair"
        val file=File(compose.activity.getExternalFilesDir(null),"fundamental/$label-$name.png").apply {parentFile!!.mkdirs()}
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun back(){val route=nav.currentDestination?.route;compose.waitUntil(5000){compose.activity.hasWindowFocus()};Espresso.pressBackUnconditionally();compose.waitUntil(5000){nav.currentDestination?.route!=route};compose.waitForIdle()}
    @Test fun exactReportedRecordingsKeepUsefulCurvesAcrossAllPresentations(){
        val baseline=InstrumentationRegistry.getArguments().getString("baseline")=="true"
        for((label,item) in items){
            compose.onNodeWithTag("session-${item.id}").performScrollTo();capture("$label-card")
            compose.onNodeWithTag("session-${item.id}").performClick()
            val graph=compose.onNodeWithTag("time-chart").performScrollTo();capture("$label-detail")
            val node=graph.fetchSemanticsNode();val area=node.config[PlotDataArea]
            val trace=PitchTrace.build(item.timeline,item.instrument)
            android.util.Log.i("RepairReal","$label points=${item.timeline.size} accepted=${trace.count {!it.uncertain}} links=${trace.count {it.previous!=null}} left=${area.left} total=${node.size.width} camera=${node.config[PlotWindow]}")
            compose.onNodeWithTag("expand-plot").performScrollTo().performClick();capture("$label-fullscreen")
            if(!baseline){
                assertTrue("Useful curve in $label",trace.count {it.previous!=null}>=if(label=="D2")28 else 18)
                val camera=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
                assertTrue(camera.y.low>45 && camera.y.high<100)
                assertEquals(item.timeline,RecordingStore(app).list().first {it.id==item.id}.timeline)
            }
            back();back()
        }
    }
    private class TestApplication(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"repair-tests").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("repair-tests-$name",mode)
        fun clean(){getSharedPreferences("local_data",MODE_PRIVATE).edit().clear().commit();getSharedPreferences("session_tools",MODE_PRIVATE).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
