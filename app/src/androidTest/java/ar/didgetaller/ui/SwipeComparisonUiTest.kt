package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.graphics.Bitmap
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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.R
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.*

class SwipeComparisonUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owner=ViewModelStore()
    private lateinit var app:IsolatedApplication
    private val originalLocale=Locale.getDefault()
    @After fun cleanup(){
        if(::app.isInitialized){compose.runOnUiThread {owner.clear()};app.clean()}
        Locale.setDefault(originalLocale)
    }
    @Test fun englishComparisonPlaybackGesturesAndExactRestore()=compare("en")
    @Test fun spanishComparisonPlaybackGesturesAndExactRestore()=compare("es-AR")

    private fun openComparison() {
        if(compose.onAllNodesWithTag("advanced-diagnostics-toggle").fetchSemanticsNodes().isEmpty())
            compose.onNodeWithTag("session-tools").performScrollTo().performClick()
        if(compose.onAllNodesWithTag("swipe-compare-toggle").fetchSemanticsNodes().isEmpty())
            compose.onNodeWithTag("advanced-diagnostics-toggle").performScrollTo().performClick()
    }

    @Test fun comparisonIsReachableThroughTheActualApplicationNavigation() {
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        app=IsolatedApplication(target)
        val store=RecordingStore(app)
        val pcm=ShortArray(44100*5){i->(6000*sin(2*PI*70*i/44100)).toInt().toShort()}
        val item=store.save(WavCodec.encode(pcm,pcm.size,44100),(0..24).map {SessionAnalysis.Point(it*200L,70.0,.99)})
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owner.put("navigation",vm)}
        compose.setContent {TallerApp(vm,{}, {}, {_,_,_->})}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        openComparison()
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.waitUntil(10000){vm.swipeComparison.curve!=null}
        compose.onNodeWithTag("diagnostic-time-chart").performScrollTo().assertIsDisplayed()
        File(target.getExternalFilesDir(null),"swipe-actual-app.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        assertTrue(compose.onNodeWithTag("diagnostic-time-chart").fetchSemanticsNode().config[PlotSwipeCount]>0)
        assertEquals(0,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotSwipeCount])
        compose.runOnIdle {assertTrue(vm.swipeComparison.enabled);assertNotNull(vm.swipeComparison.curve)}
        compose.onNodeWithTag("expand-diagnostics").performScrollTo().performClick()
        assertTrue(compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotSwipeCount]>0)
        compose.onNodeWithTag("viewer-play").performClick()
        compose.waitUntil(5000){vm.playbackPositionMs()>300}
        compose.onNodeWithTag("viewer-play").performClick()
        val position=vm.playbackPositionMs()
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        compose.runOnIdle {assertEquals(item.id,vm.playingId);assertTrue(vm.playbackPaused);assertEquals(position.toDouble(),vm.playbackPositionMs().toDouble(),80.0)}
        openComparison()
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.runOnIdle {assertFalse(vm.swipeComparison.enabled)}
    }

    private fun compare(language:String){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        app=IsolatedApplication(target)
        val locale=Locale.forLanguageTag(language);Locale.setDefault(locale)
        val configuration=Configuration(target.resources.configuration).apply {setLocale(locale)}
        val localized=target.createConfigurationContext(configuration)
        val pcm=ShortArray(44100*6){i->if(i in 150000..200000)0 else (6000*sin(2*PI*70*i/44100)+2500*sin(2*PI*140*i/44100)).toInt().toShort()}
        val store=RecordingStore(app)
        val points=(0..29).map {SessionAnalysis.Point(it*200L,if(it in 17..22)null else 70.0,.98)}
        val item=store.save(WavCodec.encode(pcm,pcm.size,44100),points,123)
        val flute=store.save(WavCodec.encode(pcm,pcm.size,44100),emptyList(),124,InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        val original=store.file(item.id).readBytes()
        val index=app.getSharedPreferences("local_data",0).getString("recordings",null)
        lateinit var vm:TallerViewModel
        val expanded=mutableStateOf(false)
        compose.runOnUiThread {vm=TallerViewModel(app);owner.put("swipe",vm);vm.onDestinationChanged(item.id)}
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
        compose.waitUntil(15000){vm.workspace.state.frames>0 && !vm.workspace.state.loading &&
            vm.droneComponent.curve!=null && vm.savedYinReplay.result!=null}
        compose.onNodeWithTag("swipe-legend").assertDoesNotExist()
        compose.onNodeWithTag("time-chart").performScrollTo()
        val camera=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow]
        val before=compose.onNodeWithTag("time-chart").captureToImage().asAndroidBitmap()
        val originalLevel=vm.workspace.state.level
        openComparison()
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.waitUntil(10000){vm.swipeComparison.curve!=null || vm.swipeComparison.failed}
        compose.runOnIdle {assertFalse(vm.swipeComparison.failed);assertTrue(vm.swipeComparison.curve!!.samples.any {it.hz==null})}
        compose.onNodeWithTag("swipe-legend").assertExists()
        compose.onNodeWithTag("diagnostic-time-chart").performScrollTo()
        val compared=compose.onNodeWithTag("diagnostic-time-chart").fetchSemanticsNode().config
        assertTrue(compared[PlotSwipeCount]>0)
        assertTrue(compared[PlotWindow].y.low<=35)
        assertEquals(0,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotSwipeCount])
        File(target.getExternalFilesDir(null),"swipe-$language-preview.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("swipe-legend").assertDoesNotExist()
        compose.onNodeWithTag("time-chart").performScrollTo()
        assertEquals(camera,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow])
        assertEquals(0,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotSwipeCount])
        val after=compose.onNodeWithTag("time-chart").captureToImage().asAndroidBitmap()
        assertNotNull("Source level remains available after comparison",vm.workspace.state.level)
        assertSame("Comparison must not replace source evidence",originalLevel,vm.workspace.state.level)
        assertTrue("Source level still contains sound",vm.workspace.state.level!!.rms.any {it>.005})
        if(!before.sameAs(after))for((name,image) in listOf("before" to before,"after" to after)){
            File(target.getExternalFilesDir(null),"swipe-restore-$language-$name.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG,100,it)
            }
        }
        // The emulator can round a blended grid-line channel by one byte between draws.
        assertTrue("Original chart pixels restored within one channel level",sameChartPixels(before,after))
        compose.runOnIdle {
            vm.selectInstrument(InstrumentProfile.NATIVE_AMERICAN_FLUTE)
            vm.setSwipeComparison(item,true) // Session profile, not the current live instrument.
            assertTrue(vm.swipeComparison.enabled)
            vm.onDestinationChanged(flute.id)
            assertNull(vm.swipeComparison.curve)
            vm.setSwipeComparison(flute,true)
            assertFalse(vm.swipeComparison.enabled)
            vm.onDestinationChanged(item.id);vm.setSwipeComparison(item,true);vm.setSwipeComparison(item,false)
            assertFalse(vm.swipeComparison.running);assertFalse(vm.swipeComparison.enabled)
            vm.onDestinationChanged()
        }
        assertArrayEquals(original,store.file(item.id).readBytes())
        assertEquals(index,app.getSharedPreferences("local_data",0).getString("recordings",null))
        assertEquals(points,store.list().first {it.id==item.id}.timeline)
    }

    private fun sameChartPixels(before:Bitmap,after:Bitmap):Boolean {
        if(before.width!=after.width || before.height!=after.height)return false
        for(y in 0 until before.height)for(x in 0 until before.width){
            val a=before.getPixel(x,y);val b=after.getPixel(x,y)
            if(kotlin.math.abs(android.graphics.Color.red(a)-android.graphics.Color.red(b))>1 ||
                kotlin.math.abs(android.graphics.Color.green(a)-android.graphics.Color.green(b))>1 ||
                kotlin.math.abs(android.graphics.Color.blue(a)-android.graphics.Color.blue(b))>1)return false
        }
        return true
    }

    private class IsolatedApplication(private val base:Context):Application(){
        private val id=UUID.randomUUID().toString()
        private val names=mutableSetOf<String>()
        private val root=File(base.cacheDir,"swipe-$id").apply {mkdirs()}
        init {attachBaseContext(base)}
        override fun getFilesDir()=root
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences {
            val scoped="swipe-$id-$name";names+=scoped;return base.getSharedPreferences(scoped,mode)
        }
        fun clean(){names.forEach {base.getSharedPreferences(it,0).edit().clear().commit()};root.deleteRecursively()}
    }
}
