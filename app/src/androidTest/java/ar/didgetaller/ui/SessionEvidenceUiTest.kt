package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.requirePrivateFixtures
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Emulator-only, opt-in external PCM; never packages private audio or touches phone storage. */
class SessionEvidenceUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owners=ViewModelStore()
    private lateinit var app:EvidenceApplication
    private lateinit var vm:TallerViewModel
    private lateinit var item:RecordingStore.Session
    private val args get()=InstrumentationRegistry.getArguments()
    @Before fun setup(){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        args.getString("evidenceExpectedLanguage")?.let { expected ->
            assertEquals(expected, compose.activity.resources.configuration.locales[0].language)
            assertEquals(1.5f, compose.activity.resources.configuration.fontScale, .01f)
        }
        app=EvidenceApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val path=args.getString("evidenceWav")
        val externalPoints=args.getString("evidencePoints")
        if(path!=null || externalPoints!=null)requirePrivateFixtures()
        val bytes=if(path!=null)File(path).readBytes() else {
            val pcm=ShortArray(44100*8){i->if(i in 88200..132300)0 else (6000*sin(2*PI*70*i/44100)).toInt().toShort()}
            WavCodec.encode(pcm,pcm.size,44100)
        }
        val local=File(app.filesDir,"input.wav").apply {writeBytes(bytes)}
        val points=externalPoints?.let {input ->
            File(input).readLines().drop(1).filter {it.isNotBlank()}.map {line->
                val f=line.split(',');SessionAnalysis.Point((f[2].toDouble()*1000).toLong(),f[4].toDoubleOrNull(),f[5].toDouble())
            }
        } ?: Pcm16WavReader(local).use {reader->
            val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=false)
            val chunk=ShortArray(4096);val result=mutableListOf<SessionAnalysis.Point>();var at=0L
            while(at<reader.frameCount){val n=reader.read(at,chunk);at+=n;stream.append(chunk,n)?.let {r->
                result+=SessionAnalysis.Point(at*1000/44100,r.hz.takeIf {it>0},r.confidence)
            }};result
        }
        item=RecordingStore(app).save(bytes,points,if(args.getString("evidenceLabel")?.startsWith("current-")==true)1793923200000L else 1000000)
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("evidence",vm)}
        compose.setContent {TallerApp(vm,{}, {}, {_,_,_->})}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.waitUntil(20000){!vm.workspace.state.loading && vm.workspace.state.frames>0}
    }
    @After fun close(){if(::app.isInitialized){compose.runOnUiThread {owners.clear()};app.clean()}}
    private fun screenshot(name:String){
        compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        // The accessibility connection is lazy; UI-thread idleness does not await its first window.
        automation.waitForIdle(100,5000)
        val dir=File(app.getExternalFilesDir(null),"session-evidence").apply {mkdirs()}
        try {
            compose.waitUntil(5000) { automation.rootInActiveWindow?.packageName?.toString() == compose.activity.packageName }
        } catch (failure:ComposeTimeoutException) {
            val observed=automation.rootInActiveWindow?.packageName?.toString()
            val rejected=checkNotNull(automation.takeScreenshot())
            File(dir,"rejected-$name.png").outputStream().use { rejected.compress(Bitmap.CompressFormat.PNG,100,it) }
            rejected.recycle()
            throw AssertionError("Invalid visual evidence: expected ${compose.activity.packageName}, observed $observed",failure)
        }
        val bitmap=checkNotNull(automation.takeScreenshot())
        File(dir,"${args.getString("evidenceLabel") ?: "synthetic"}-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Test fun readmeShowcaseFromSyntheticWav(){
        compose.runOnUiThread {vm.renameSession(item,if(compose.activity.resources.configuration.locales[0].language=="es")"Estudio sintético del dron" else "Synthetic drone study")}
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("library-list").assertIsDisplayed()
        screenshot("library")
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.waitUntil(20000){vm.musicalTrace(item)!=null}
        compose.onNodeWithTag("time-chart").assertIsDisplayed()
        val chart=compose.onNodeWithTag("time-chart")
        val area=chart.fetchSemanticsNode().config[PlotDataArea]
        chart.performTouchInput {click(androidx.compose.ui.geometry.Offset(area.left+area.width*.36f,area.center.y))}
        compose.waitUntil(5000){vm.playingId==item.id}
        Thread.sleep(250);compose.mainClock.advanceTimeBy(500)
        assertEquals(item.durationMs*.36,chart.fetchSemanticsNode().config[PlotPlaybackMs].toDouble(),100.0)
        screenshot("session")
        compose.onNodeWithTag("plot-tab-spectrogram").performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null && !vm.workspace.state.loading}
        compose.onNodeWithTag("time-chart").assertIsDisplayed()
        screenshot("spectrogram")
        compose.onNodeWithText(uiText(R.string.ui_analyze_recording)).performScrollTo().performClick()
        compose.waitUntil(30000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        assertNull(vm.recordingAnalysis.error)
        // Analysis scrolls vertically below the card; first restore the tab row's vertical owner.
        compose.onNodeWithTag("expand-plot").performScrollTo()
        compose.onNodeWithTag("plot-tab-spectrum").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("spectrum-chart").performScrollTo().assertIsDisplayed()
        screenshot("harmonics")
    }
    @Test fun captureAndPreserveOriginals(){
        val store=RecordingStore(app);val original=store.file(item.id).readBytes()
        val index=app.getSharedPreferences("local_data",0).all.toMap()
        compose.onNodeWithTag("time-chart").performScrollTo();screenshot("pitch")
        compose.onNodeWithTag("time-chart").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress){it(2000f)}
        compose.waitUntil(10000){abs(vm.playbackPositionMs()-2000)<100}
        screenshot("seek")
        // Scroll the vertical parent first; the tab's nearest scroller is horizontal.
        compose.onNodeWithTag("expand-plot").performScrollTo()
        compose.onNodeWithText(uiText(R.string.ui_spectrogram)).performScrollTo().performClick()
        screenshot("spectrogram-selected")
        compose.runOnIdle {assertEquals("Tab selection",AcousticPlot.SPECTROGRAM,vm.preview(item.id).plot)}
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null || vm.workspace.state.error!=null}
        compose.runOnIdle {assertNull("Spectrogram calculation",vm.workspace.state.error)}
        compose.onNodeWithTag("time-chart").performScrollTo();screenshot("spectrogram")
        // A tab's nearest scroll owner is horizontal; first reveal its row vertically.
        compose.onNodeWithTag("expand-plot").performScrollTo()
        compose.onNodeWithTag("plot-tab-pitch").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle {assertEquals("Selected chart before fullscreen",AcousticPlot.PITCH,vm.preview(item.id).plot)}
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").assertIsDisplayed()
        compose.runOnIdle {assertEquals("Selected chart in fullscreen",AcousticPlot.PITCH,vm.explorer(item.id).plot)}
        try {
            compose.waitUntil(5000) { compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config.contains(PlotDataArea) }
        } catch (failure: ComposeTimeoutException) {
            screenshot("wide-no-drawable-frame")
            throw AssertionError("Fullscreen must retain a drawable plot at the selected font and navigation size", failure)
        }
        val fullscreenChart = compose.onNodeWithTag("immersive-plot").fetchSemanticsNode()
        if (fullscreenChart.config[PlotDroneComponentCount] == 0) {
            val emptyEvidence = compose.onNodeWithText(uiText(R.string.musical_trace_unavailable)).assertIsDisplayed()
            val bounds = emptyEvidence.fetchSemanticsNode().boundsInRoot
            val area = fullscreenChart.config[PlotDataArea]
            val left = fullscreenChart.boundsInRoot.left + area.left
            val top = fullscreenChart.boundsInRoot.top + area.top
            val right = fullscreenChart.boundsInRoot.left + area.right
            val bottom = fullscreenChart.boundsInRoot.top + area.bottom
            if (bounds.left < left || bounds.top < top || bounds.right > right || bounds.bottom > bottom)
                screenshot("wide-empty-overlay-overlap")
            assertTrue("Empty evidence text must stay right of the Y-axis labels", bounds.left >= left)
            assertTrue("Empty evidence text must stay below the plot top", bounds.top >= top)
            assertTrue("Empty evidence text must stay inside the plot right", bounds.right <= right)
            assertTrue("Empty evidence text must stay above the time labels", bounds.bottom <= bottom)
            val hintLayout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            emptyEvidence.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(hintLayout) }
            val renderedHint = hintLayout.single()
            if (renderedHint.hasVisualOverflow) screenshot("wide-empty-text-overflow")
            assertFalse("Empty evidence explanation remains complete: width=${renderedHint.didOverflowWidth}, " +
                "height=${renderedHint.didOverflowHeight}, size=${renderedHint.size}, lines=${renderedHint.lineCount}, area=$area",
                renderedHint.hasVisualOverflow)
        }
        screenshot("wide-pitch")
        val scientificText = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val explanationNode = compose.onNodeWithTag("viewer-scientific-description")
            .performScrollTo().assertIsDisplayed().assertTextEquals(uiText(R.string.musical_drone_description))
        explanationNode
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(scientificText) }
        assertTrue("Scientific explanation exposes its rendered layout", scientificText.isNotEmpty())
        val explanationLayout = scientificText.single()
        val descriptionDir = File(app.getExternalFilesDir(null), "session-evidence").apply { mkdirs() }
        File(descriptionDir, "${args.getString("evidenceLabel") ?: "synthetic"}-text-layout.txt").writeText(
            "text=${explanationLayout.layoutInput.text.text}\nsize=${explanationLayout.size}\nbounds=${explanationNode.fetchSemanticsNode().boundsInRoot}\nlines=${explanationLayout.lineCount}\nlastLineBottom=${explanationLayout.getLineBottom(explanationLayout.lineCount-1)}\nlastLineEnd=${explanationLayout.getLineEnd(explanationLayout.lineCount-1,true)}\ncharacters=${explanationLayout.layoutInput.text.length}\nmaxLines=${explanationLayout.layoutInput.maxLines}\n")
        if (explanationLayout.hasVisualOverflow) screenshot("wide-text-overflow")
        assertFalse("Scientific explanation must wrap without truncation: width=${explanationLayout.didOverflowWidth}, height=${explanationLayout.didOverflowHeight}, size=${explanationLayout.size}, lines=${explanationLayout.lineCount}, maxLines=${explanationLayout.layoutInput.maxLines}", explanationLayout.hasVisualOverflow)
        assertEquals("The last scientific word must be laid out", explanationLayout.layoutInput.text.length,
            explanationLayout.getLineEnd(explanationLayout.lineCount-1,true))
        assertTrue("Parent clipping must not hide a scientific line", explanationNode.fetchSemanticsNode().boundsInRoot.height + .5f >= explanationLayout.size.height)
        screenshot("wide-pitch-reading")
        assertArrayEquals(original,store.file(item.id).readBytes())
        assertEquals(index,app.getSharedPreferences("local_data",0).all)
        assertEquals(item.timeline,store.list().first {session->session.id==item.id}.timeline)
    }
    @Test fun scrollableFullscreenPreservesPinchPanAndPausedScrubbing(){
        compose.onNodeWithTag("plot-tab-pitch").performScrollTo().performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        val graph=compose.onNodeWithTag("immersive-plot")
        compose.waitUntil(5000){graph.fetchSemanticsNode().config.contains(PlotDataArea)}
        val initial=graph.fetchSemanticsNode()
        val area=initial.config[PlotDataArea]
        val window=initial.config[PlotWindow]
        val cursor=initial.config[PlotPlaybackMs]
        val delta=androidx.compose.ui.geometry.Offset(area.width*.2f,0f)
        graph.performTouchInput {
            pinch(start0=area.center-delta,start1=area.center+delta,
                end0=area.center-delta*1.5f,end1=area.center+delta*1.5f,durationMillis=500)
        }
        val zoomed=graph.fetchSemanticsNode().config[PlotWindow]
        assertEquals(window.x.span/1.5,zoomed.x.span,window.x.span*.02)
        assertEquals(cursor,graph.fetchSemanticsNode().config[PlotPlaybackMs])
        assertNull("Pinching must not prepare audio",vm.playingId)
        graph.panViewport(androidx.compose.ui.geometry.Offset(area.width*.08f,0f))
        val panned=graph.fetchSemanticsNode().config[PlotWindow]
        assertTrue("Two-finger pan must move the viewport",abs(panned.x.low-zoomed.x.low)>1)
        assertEquals(cursor,graph.fetchSemanticsNode().config[PlotPlaybackMs])
        assertNull("Panning must not prepare audio",vm.playingId)
        val currentArea=graph.fetchSemanticsNode().config[PlotDataArea]
        val target=panned.x.value(.75)
        graph.performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(currentArea.left+currentArea.width*.25f,currentArea.center.y),
                androidx.compose.ui.geometry.Offset(currentArea.left+currentArea.width*.75f,currentArea.center.y),500)
        }
        compose.waitUntil(5000){vm.playingId==item.id && abs(vm.playbackPositionMs()-target)<=80}
        assertTrue("Scrubbing before playback must stay paused",vm.playbackPaused)
        assertEquals(target,graph.fetchSemanticsNode().config[PlotPlaybackMs].toDouble(),80.0)
        assertEquals("Scrubbing must not pan",panned,graph.fetchSemanticsNode().config[PlotWindow])
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("session-detail").assertIsDisplayed()
        assertTrue(vm.playbackPaused)
        assertEquals(target,vm.playbackPositionMs().toDouble(),80.0)
    }
    @Test fun audioLevelExplainsGapsAndFollowsTheSameViewport(){
        compose.onNodeWithTag("time-chart").performScrollTo()
        val node=compose.onNodeWithTag("time-chart").fetchSemanticsNode()
        assertTrue(node.config[PlotAudioLevelCount]>0)
        compose.onNodeWithText(uiText(R.string.musical_drone_description)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").performTouchInput {doubleClick()}
        assertTrue(compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[PlotAudioLevelCount]>0)
    }
    @Test fun boundedLevelAndActualStreamPerformance(){
        val file=RecordingStore(app).file(item.id)
        val levelTimes=mutableListOf<Double>();val streamTimes=mutableListOf<Double>()
        var levelBytes=0;var duration=0.0
        repeat(4){iteration->
            Pcm16WavReader(file).use {reader->
                duration=reader.frameCount.toDouble()/reader.sampleRate
                val start=SystemClock.elapsedRealtimeNanos();val level=AudioLevel.read(reader)
                if(iteration>0)levelTimes+=(SystemClock.elapsedRealtimeNanos()-start)/1e6
                levelBytes=level.rms.size*4+level.hop*2
                val stream=PitchStream(InstrumentProfile.DIDGERIDOO,includeVisuals=true)
                val chunk=ShortArray(4096);var at=0L
                while(at<reader.frameCount){val count=reader.read(at,chunk);at+=count
                    val before=SystemClock.elapsedRealtimeNanos();val result=stream.append(chunk,count)
                    if(iteration>0 && result!=null)streamTimes+=(SystemClock.elapsedRealtimeNanos()-before)/1e6
                }
            }
        }
        assertTrue(levelBytes<160000)
        val dir=File(app.getExternalFilesDir(null),"session-evidence").apply {mkdirs()}
        File(dir,"performance.json").writeText("""{"duration_s":$duration,"level_ms":$levelTimes,"level_array_bytes":$levelBytes,"stream_mean_ms":${streamTimes.average()},"stream_max_ms":${streamTimes.maxOrNull()},"stream_p95_ms":${streamTimes.sorted()[(streamTimes.size*.95).toInt()]}}""")
    }
    private class EvidenceApplication(context:Context):Application(){
        init {attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"session-evidence-test").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("session-evidence-$name",mode)
        fun clean(){getSharedPreferences("local_data",0).edit().clear().commit();getSharedPreferences("session_tools",0).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
