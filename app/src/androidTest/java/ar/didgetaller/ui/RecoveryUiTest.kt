package ar.didgetaller.ui

import ar.didgetaller.R

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.SessionPhase
import ar.didgetaller.audio.MicrophoneEngine
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Emulator only, isolated storage. PCM injection tests the real meter callback, not physical audio. */
class RecoveryUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var app:IsolatedApp
    private lateinit var vm:TallerViewModel
    private lateinit var nav:NavHostController
    private val owner=ViewModelStore()
    @Before fun setup(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        app=IsolatedApp(InstrumentationRegistry.getInstrumentation().targetContext);app.clean()
    }
    private fun show(){
        compose.runOnUiThread {vm=TallerViewModel(app);owner.put("recovery",vm)}
        compose.setContent {nav=rememberNavController();TallerApp(vm,{}, {},{_,_,_->},nav)}
    }
    @After fun cleanup(){if(::vm.isInitialized)compose.runOnUiThread{owner.clear()};if(::app.isInitialized)app.clean()}
    private fun capture(name:String){
        compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(350)
        val label=InstrumentationRegistry.getArguments().getString("studioLabel") ?: "v325"
        val file=File(compose.activity.getExternalFilesDir(null),"recovery/$label-$name.png").apply {parentFile!!.mkdirs()}
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun back(){Espresso.pressBackUnconditionally();compose.waitForIdle()}
    private fun library(){compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()}
    private fun open(item:RecordingStore.Session){compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick();compose.onNodeWithTag("time-chart").performScrollTo()}

    @Test fun allSurfacesRenderVariationAndPeriodicityWithOriginalDataIntact(){
        val raw=(0..95).map {i->
            val hz=when(i){in 0..19->70.0;in 20..39->if(i%2==0)70.0 else 71.4;in 40..59->if(i%2==0)70.0 else 74.0;in 60..67->null;else->70.0}
            SessionAnalysis.Point(i*185L,hz,if(hz==null)0.0 else if(i>=68).73 else .97)
        }
        val item=RecordingStore(app).save(WavCodec.encode(ShortArray(44100*18),44100*18,44100),raw)
        show();compose.runOnIdle {vm.renameSession(item,"Variación y periodicidad · serie sintética")}
        library();capture("colours-library");open(item)
        val heading=compose.onNodeWithText(uiText(R.string.ui_explore_audio)).performScrollTo().fetchSemanticsNode()
        assertEquals(heading.size.height.toFloat(),heading.boundsInRoot.height,1f)
        capture("session-heading")
        compose.onNodeWithTag("time-chart").performScrollTo();capture("colours-detail")
        val pixels=compose.onNodeWithTag("time-chart").captureToImage().toPixelMap()
        val found=IntArray(3)
        for(x in 0 until pixels.width)for(y in 0 until pixels.height){
            val c=pixels[x,y]
            listOf(PitchTrace.Tone.STABLE,PitchTrace.Tone.MODERATE,PitchTrace.Tone.REVIEW).forEachIndexed {i,t->
                val target=pitchColor(t)
                if(abs(c.red-target.red)<.01 && abs(c.green-target.green)<.01 && abs(c.blue-target.blue)<.01)found[i]++
            }
        }
        assertTrue("All three colours must reach actual Canvas pixels: ${found.toList()}",found.all {it>5})
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick();capture("colours-fullscreen")
        val general=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow]
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithText(uiText(R.string.ui_fitted_view)).performScrollTo().performClick();back()
        assertTrue(compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotWindow].y.span<general.y.span)
        capture("colours-fitted")
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithTag("show-uncertain").performScrollTo().performClick();capture("colour-help")
        compose.runOnIdle {assertEquals(raw,RecordingStore(app).list().single().timeline)}
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun controlledPcmReachesLiveMeterAndProfilesResetWithoutGraphFiltering(){
        show()
        fun meter(profile:InstrumentProfile,hz:Double,weights:List<Double>,name:String){
            compose.runOnIdle {vm.selectInstrument(profile)}
            val f=TallerViewModel::class.java.getDeclaredField("mic").apply {isAccessible=true}
            val mic=f.get(vm) as MicrophoneEngine
            val callback=MicrophoneEngine::class.java.getDeclaredField("onReading").apply {isAccessible=true}.get(mic) as (PitchDetector.Reading)->Unit
            // The transport is not used: deterministic PCM enters the production reading callback.
            val listening=TallerViewModel::class.java.getDeclaredField("listening\$delegate").apply {isAccessible=true}.get(vm) as androidx.compose.runtime.MutableState<Boolean>
            val phase=TallerViewModel::class.java.getDeclaredField("sessionPhase\$delegate").apply {isAccessible=true}.get(vm) as androidx.compose.runtime.MutableState<SessionPhase>
            val started=TallerViewModel::class.java.getDeclaredField("sessionStartedAtMs\$delegate").apply {isAccessible=true}.get(vm) as androidx.compose.runtime.MutableState<Long>
            val timeline=TallerViewModel::class.java.getDeclaredField("sessionTimeline\$delegate").apply {isAccessible=true}.get(vm) as androidx.compose.runtime.MutableState<List<SessionAnalysis.Point>>
            // A capture UI fixture with no writer or AudioRecord: never claims physical recording.
            compose.runOnIdle {listening.value=true;started.value=android.os.SystemClock.elapsedRealtime();timeline.value=emptyList();phase.value=SessionPhase.CAPTURING}
            val stream=PitchStream(profile,includeVisuals=false)
            val pcm=ShortArray(66150){i->(weights.mapIndexed {h,w->w*sin(2*PI*hz*(h+1)*i/44100)}.sum()/weights.sum()*12000).toInt().toShort()}
            val readings=pcm.asList().chunked(4096).mapNotNull {stream.append(it.toShortArray())}
            readings.forEach {r->compose.runOnIdle {callback(r)};android.os.SystemClock.sleep(profile.analysisChunks*93L)}
            compose.runOnIdle {assertTrue("$profile readings=${readings.size}, hz=${vm.reading.hz}, listening=${vm.listening}",vm.stableReading);assertEquals(hz,vm.filteredHz,1.0);assertEquals(profile,vm.instrument);assertEquals(readings.size,vm.sessionTimeline.size)}
            compose.onNodeWithText(uiText(R.string.ui_stable_note)).assertIsDisplayed()
            compose.onNodeWithText(hzLabel(vm.filteredHz)).assertIsDisplayed()
            capture(name)
            compose.runOnIdle {vm.selectInstrument(if(profile==InstrumentProfile.DIDGERIDOO)InstrumentProfile.NATIVE_AMERICAN_FLUTE else InstrumentProfile.DIDGERIDOO);assertEquals(profile,vm.instrument)}
            compose.runOnIdle {callback(PitchDetector.Reading());assertEquals(0.0,vm.filteredHz,0.0);assertFalse(vm.stableReading)}
            capture("$name-silence")
            compose.runOnIdle {callback(PitchDetector.Reading(confidence=.52,rms=.10));assertEquals(0.0,vm.filteredHz,0.0)}
            compose.onNodeWithText(uiText(R.string.ui_signal_present_uncertain_frequency)).assertIsDisplayed()
            capture("$name-uncertain")
            compose.runOnIdle {phase.value=SessionPhase.IDLE;vm.microphoneStop()}
        }
        meter(InstrumentProfile.DIDGERIDOO,70.0,listOf(1.0,.4,.2),"measure-drone")
        meter(InstrumentProfile.NATIVE_AMERICAN_FLUTE,440.0,listOf(.15,1.0,.2),"measure-flute")
        meter(InstrumentProfile.DIDGERIDOO,70.0,listOf(.04,.1,1.0,.8,.5),"measure-harmonics")
        compose.runOnIdle {assertEquals(35,vm.minHz);assertEquals(140,vm.maxHz)}
    }

    @Test fun historicalAndLatestTimelinesRetainDurationOriginalsAndUsefulCurves(){
        inspectTimelines("v325-real-fixtures.json","real")
    }

    @Test fun privateWavDetectorTimelinesPreserveOriginalsAndTimeExtent(){
        inspectTimelines("v326-real-fixtures.json","wav-output")
    }

    private fun inspectTimelines(name:String,prefix:String){
        val source=File(compose.activity.getExternalFilesDir(null),name)
        assumeTrue("Fixture privado local requerido",source.isFile)
        val raw=JSONArray(source.readText());val store=RecordingStore(app)
        val items=(0 until raw.length()).map {i->
            val s=raw.getJSONObject(i);val a=s.getJSONArray("timeline")
            val points=(0 until a.length()).map {j->val p=a.getJSONArray(j);SessionAnalysis.Point(p.getLong(0),if(p.isNull(1))null else p.getDouble(1),p.getDouble(2))}
            val frames=(s.getLong("durationMs")*44100/1000).toInt()
            s.getString("label") to store.save(WavCodec.encode(ShortArray(frames),frames,44100),points,s.getLong("createdAt"))
        }
        show();library();capture("$prefix-library")
        for((label,item) in items){
            open(item);capture("$prefix-$label-detail")
            val area=compose.onNodeWithTag("time-chart").fetchSemanticsNode().config[PlotWindow]
            assertEquals(item.durationMs.toDouble(),area.x.high,1.0)
            compose.onNodeWithTag("expand-plot").performScrollTo().performClick();capture("$prefix-$label-fullscreen")
            val trace=PitchTrace.build(item.timeline,item.instrument)
            if(InstrumentationRegistry.getArguments().getString("baseline")!="true")
                assertTrue(trace.count {!it.uncertain}>=item.timeline.count {it.hz!=null}*.70)
            assertEquals(item.timeline,store.list().first {it.id==item.id}.timeline)
            back();back()
        }
    }
    private class IsolatedApp(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"recovery-tests").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("recovery-tests-$name",mode)
        fun clean(){listOf("local_data","session_tools").forEach {getSharedPreferences(it,MODE_PRIVATE).edit().clear().commit()};filesDir.deleteRecursively()}
    }
}
