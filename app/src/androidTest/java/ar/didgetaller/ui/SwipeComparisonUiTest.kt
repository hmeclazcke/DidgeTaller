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
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.waitUntil(10000){vm.swipeComparison.curve!=null}
        compose.onNodeWithTag("time-chart").performScrollTo()
        File(target.getExternalFilesDir(null),"swipe-actual-app.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("swipe-legend").assertExists()
        compose.onNodeWithTag("viewer-play").performClick()
        compose.waitUntil(5000){vm.playbackPositionMs()>300}
        File(target.getExternalFilesDir(null),"swipe-actual-fullscreen.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        compose.runOnIdle {assertTrue(vm.swipeComparison.enabled);assertNotNull(vm.swipeComparison.curve)}
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
        compose.waitUntil(10000){vm.workspace.state.frames>0 && !vm.workspace.state.loading}
        compose.onNodeWithTag("swipe-legend").assertDoesNotExist()
        compose.onNodeWithTag("time-chart").performScrollTo()
        val camera=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow]
        val before=compose.onNodeWithTag("time-chart").captureToImage().asAndroidBitmap()
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.waitUntil(10000){vm.swipeComparison.curve!=null || vm.swipeComparison.failed}
        compose.runOnIdle {assertFalse(vm.swipeComparison.failed);assertTrue(vm.swipeComparison.curve!!.samples.any {it.hz==null})}
        compose.onNodeWithTag("swipe-legend").assertExists()
        compose.onNodeWithTag("time-chart").performScrollTo()
        val compared=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config
        assertTrue(compared[PlotSwipeCount]>0)
        assertTrue(compared[PlotWindow].y.low<=35)
        File(target.getExternalFilesDir(null),"swipe-$language-preview.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        // Preview and fullscreen share hypotheses/selection, with independent cameras.
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").assertExists()
        val full=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        compose.onNodeWithTag("immersive-plot").performTouchInput {
            pinch(Offset(width*.3f,height*.4f),Offset(width*.7f,height*.6f),Offset(width*.1f,height*.2f),Offset(width*.9f,height*.8f))
        }
        compose.waitForIdle()
        assertNotEquals(full,compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow])
        compose.onNodeWithTag("viewer-play").performClick()
        compose.waitUntil(5000){vm.playbackPositionMs()>300}
        compose.waitUntil(5000){compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotPlaybackMs]>0}
        compose.onNodeWithTag("immersive-plot").performTouchInput {swipe(center,Offset(center.x+width*.15f,center.y),500)}
        compose.onNodeWithTag("immersive-plot").performTouchInput {longClick(center)}
        compose.runOnIdle {assertTrue(vm.explorer(item.id).inspected.orEmpty().contains("SWIPE"));vm.stopPlayback()}
        compose.onNodeWithTag("reset-viewport").performClick()
        File(target.getExternalFilesDir(null),"swipe-$language-fullscreen.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        compose.runOnIdle {expanded.value=false;vm.preview(item.id).selectedMs=0}
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("swipe-legend").assertDoesNotExist()
        compose.onNodeWithTag("time-chart").performScrollTo()
        assertEquals(camera,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow])
        assertEquals(0,compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotSwipeCount])
        val after=compose.onNodeWithTag("time-chart").captureToImage().asAndroidBitmap()
        assertTrue("Original chart pixels restored",before.sameAs(after))
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
