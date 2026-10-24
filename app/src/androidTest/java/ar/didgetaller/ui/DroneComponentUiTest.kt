package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.*

class DroneComponentUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owner=ViewModelStore()
    private lateinit var app:IsolatedApplication
    private val originalLocale=Locale.getDefault()
    @After fun cleanup(){
        if(::app.isInitialized){compose.runOnUiThread {owner.clear()};app.clean()}
        compose.runOnUiThread {compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED}
        Locale.setDefault(originalLocale)
    }

    @Test fun spanishEstimatedComponentPreservesTheSource()=check("es-AR")
    @Test fun englishEstimatedComponentPreservesTheSource()=check("en")

    @Test fun componentOnlyViewportUsesMeasuredComponentRatherThanYinRegister(){
        val yin=listOf(SessionAnalysis.Point(100,140.0,.95),SessionAnalysis.Point(300,140.0,.95))
        val component=DroneComponentCurve(listOf(
            DroneComponentCurve.Sample(100.0,35.0,35.0,.1,DroneComponentCurve.Reason.COMPONENT),
            DroneComponentCurve.Sample(300.0,35.0,35.0,.1,DroneComponentCurve.Reason.COMPONENT)),200.0)
        val selected=sessionScalePoints(yin,component,ar.didgetaller.PitchLayerMode.COMPONENT)
        val viewport=ExplorerState().camera(1000,44100,selected)
        assertTrue(35.0 in viewport.y.low..viewport.y.high)
        assertFalse(140.0 in viewport.y.low..viewport.y.high)
        assertEquals(yin,sessionScalePoints(yin,component,ar.didgetaller.PitchLayerMode.FUNDAMENTAL))
        assertEquals(4,sessionScalePoints(yin,component,ar.didgetaller.PitchLayerMode.BOTH).size)
    }

    @Test fun privateFourBoutWavShowsIndependentEvidenceWhenPresent()=checkPrivateWav(false)

    @Test fun privateFourBoutWavFitsLandscape()=checkPrivateWav(true)

    private fun checkPrivateWav(landscape:Boolean){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        if(landscape){
            compose.runOnUiThread {compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
        }
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val fixture=File(target.getExternalFilesDir(null),"drone-component-private.wav")
        assumeTrue("Private WAV must be placed only on the emulator",fixture.isFile)
        app=IsolatedApplication(target)
        val store=RecordingStore(app)
        val original=fixture.readBytes()
        val item=store.save(original,emptyList())
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owner.put("private-component",vm);vm.onDestinationChanged(item.id)}
        val locale=Locale.forLanguageTag("es-AR")
        Locale.setDefault(locale)
        val configuration=Configuration(target.resources.configuration).apply {setLocale(locale)}
        val localized=target.createConfigurationContext(configuration)
        val expanded=mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized,LocalConfiguration provides configuration){
                MaterialTheme {
                    if(expanded.value)ImmersiveViewer(vm,item,"PITCH"){expanded.value=false}
                    else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
                        SessionExplorer(vm,item,playbackCursor(vm,item),{expanded.value=true}){}
                    }
                }
            }
        }
        compose.waitUntil(30000){vm.droneComponent.curve!=null && vm.savedYinReplay.result!=null}
        val component=vm.droneComponent.curve!!
        val replay=vm.savedYinReplay.result!!
        fun at(ms:Double)=component.samples.minBy {abs(it.timeMs-ms)}
        assertTrue((at(30730.0).hz ?: 0.0) in 63.0..67.0)
        assertTrue((at(33300.0).hz ?: 0.0) in 63.0..67.0)
        assertNull(at(36000.0).hz)
        assertNull(at(38000.0).hz)
        val stale=replay.frames.minBy {abs(it.centerMs-38174.0)}
        assertEquals("low_comparison_amplitude",stale.reason)
        assertNull(stale.acceptedHz)
        compose.onNodeWithTag("time-chart").performScrollTo()
        val chart=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config
        assertTrue(chart[PlotYinCount]>0)
        assertTrue(chart[PlotDroneComponentCount]>0)
        val suffix=if(landscape) "real-landscape" else "real"
        File(target.getExternalFilesDir(null),"drone-component-$suffix-preview.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").assertExists()
        File(target.getExternalFilesDir(null),"drone-component-$suffix-fullscreen.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        assertArrayEquals(original,fixture.readBytes())
        assertTrue(store.list().first {it.id==item.id}.timeline.isEmpty())
    }

    private fun check(language:String){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        app=IsolatedApplication(target)
        val store=RecordingStore(app)
        val pcm=ShortArray(44100*6){i ->
            if(i in 44100*3 until 44100*4) 0 else
                ((.12*sin(2*PI*70*i/44100)+.075*sin(2*PI*140*i/44100)+
                    .04*sin(2*PI*210*i/44100))*32767).toInt().toShort()
        }
        val points=(0..29).map {i ->
            SessionAnalysis.Point(i*200L,when {i in 10..23->null;i==28->51.0;else->70.0},
                if(i in 10..23)0.0 else .95)
        }
        val item=store.save(WavCodec.encode(pcm,pcm.size,44100),points)
        val before=store.file(item.id).readBytes()
        val index=app.getSharedPreferences("local_data",0).getString("recordings",null)
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owner.put("component",vm);vm.onDestinationChanged(item.id)}
            val locale=Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            val configuration=Configuration(target.resources.configuration).apply {setLocale(locale)}
            val localized=target.createConfigurationContext(configuration)
            val expanded=mutableStateOf(false)
            compose.setContent {
                CompositionLocalProvider(LocalContext provides localized,LocalConfiguration provides configuration){
                    MaterialTheme {
                        if(expanded.value)ImmersiveViewer(vm,item,"PITCH"){expanded.value=false}
                        else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
                            SessionExplorer(vm,item,playbackCursor(vm,item),{expanded.value=true}){}
                        }
                    }
                }
            }
            compose.waitUntil(15000){vm.droneComponent.curve!=null || vm.droneComponent.failed}
            compose.runOnIdle {assertFalse(vm.droneComponent.failed)}
            compose.waitUntil(15000){vm.savedYinReplay.result!=null || vm.savedYinReplay.failed}
            compose.runOnIdle {assertFalse(vm.savedYinReplay.failed)}
            assertTrue(item.timeline.any {it.hz==51.0})
            assertTrue(sessionPlotPoints(vm,item).none {it.hz==51.0})
            compose.onNodeWithTag("pitch-layer-fundamental").performScrollTo().performClick()
            assertEquals(0,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotDroneComponentCount])
            compose.onNodeWithTag("pitch-layer-component").performScrollTo().performClick()
            assertEquals(0,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotYinCount])
            compose.onNodeWithTag("pitch-layer-both").performScrollTo().performClick()
            compose.onNodeWithTag("time-chart").performScrollTo()
            val config=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config
            assertTrue(config[PlotDroneComponentCount]>0)
            assertTrue(config[PlotYinCount]>0)
            compose.onNodeWithText(localized.getString(ar.didgetaller.R.string.drone_component_estimated))
                .assertExists()
            File(target.getExternalFilesDir(null),"drone-component-$language-preview.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
            }
            compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
            compose.onNodeWithTag("immersive-plot").assertExists()
            assertTrue(compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotDroneComponentCount]>0)
            val originalWindow=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
            compose.onNodeWithTag("immersive-plot").performTouchInput {
                pinch(Offset(width*.3f,height*.4f),Offset(width*.7f,height*.6f),
                    Offset(width*.15f,height*.3f),Offset(width*.85f,height*.7f))
            }
            assertNotEquals(originalWindow,
                compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow])
            val zoomed=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
            compose.onNodeWithTag("immersive-plot").performTouchInput {
                swipe(center,Offset(center.x+width*.12f,center.y),500)
            }
            assertNotEquals(zoomed,
                compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow])
            compose.onNodeWithTag("viewer-play").performClick()
            compose.waitUntil(5000){vm.playbackPositionMs()>300}
            compose.waitUntil(5000){
                compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotPlaybackMs]>0
            }
            compose.runOnUiThread {vm.stopPlayback()}
            File(target.getExternalFilesDir(null),"drone-component-$language-fullscreen.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
            }
            compose.runOnUiThread {expanded.value=false;vm.preview(item.id).selectedMs=2500}
            compose.onNodeWithTag("advanced-diagnostics-toggle").performScrollTo().performClick()
            compose.onNodeWithTag("drone-component-diagnostic").assertExists()
            compose.onNodeWithTag("pitch-original-diagnostic").assertExists()
            compose.onNodeWithTag("pitch-replay-diagnostic").assertExists()
        assertArrayEquals(before,store.file(item.id).readBytes())
        assertEquals(index,app.getSharedPreferences("local_data",0).getString("recordings",null))
        assertEquals(points,store.list().first {it.id==item.id}.timeline)
    }

    private class IsolatedApplication(private val base:Context):Application(){
        private val id=UUID.randomUUID().toString()
        private val names=mutableSetOf<String>()
        private val root=File(base.cacheDir,"component-$id").apply {mkdirs()}
        init {attachBaseContext(base)}
        override fun getFilesDir()=root
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences {
            val scoped="component-$id-$name";names+=scoped
            return base.getSharedPreferences(scoped,mode)
        }
        fun clean(){names.forEach {base.getSharedPreferences(it,0).edit().clear().commit()};root.deleteRecursively()}
    }
}
