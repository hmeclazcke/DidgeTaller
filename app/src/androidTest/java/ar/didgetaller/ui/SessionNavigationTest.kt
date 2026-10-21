package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.KeyEvent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.WavCodec
import ar.didgetaller.data.RecordingStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sin

/** Real NavHost and MediaPlayer, with app data isolated from the user's library. Emulator only. */
@RunWith(AndroidJUnit4::class)
class SessionNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val viewModels = ViewModelStore()
    private lateinit var application: NavigationTestApplication
    private lateinit var vm: TallerViewModel
    private lateinit var navController: NavHostController
    private lateinit var session: RecordingStore.Session
    private lateinit var recoveredId: String

    @Before fun setUp() {
        assumeTrue("Ejecutar solo en emulador", Build.HARDWARE in listOf("ranchu", "goldfish"))
        application = NavigationTestApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        application.cleanTestData()
        val store = RecordingStore(application)
        val samples = ShortArray(44100 * 20) { (2000 * sin(2 * Math.PI * 66.1 * it / 44100)).toInt().toShort() }
        val wav = WavCodec.encode(samples, samples.size, 44100)
        val points = (0..100).map { SessionAnalysis.Point(it * 190L, 66.1, 0.95) }
        repeat(12) { index -> session = store.save(wav, points, 1_000_000L + index * 1000L) }
        recoveredId=UUID.randomUUID().toString()
        store.file(recoveredId).writeBytes(wav)
        // Pick an item far down the list so a reset to the top cannot pass unnoticed.
        session = store.list()[9]
        compose.runOnUiThread {
            vm = TallerViewModel(application)
            viewModels.put("navigation-test", vm)
        }
        compose.setContent {
            navController = rememberNavController()
            TallerApp(vm, startMeasurement = {vm.startSession()}, toggleMic = {}, exportFile = { _, _, _ -> },
                navController = navController)
        }
        compose.onNodeWithText("Biblioteca", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo()
    }

    @After fun tearDown() {
        if (::vm.isInitialized) compose.runOnUiThread { viewModels.clear() }
        if (::application.isInitialized) application.cleanTestData()
    }

    private fun choosePlot(title:String){
        compose.onNodeWithContentDescription("Cambiar gráfico").performClick()
        compose.onNodeWithText(title,useUnmergedTree=true).performClick()
        compose.waitUntil(5000){compose.onAllNodes(isPopup()).fetchSemanticsNodes().isEmpty()}
        compose.waitUntil(5000){compose.activity.hasWindowFocus()}
    }
    private fun playAction(title:String){
        if(compose.onAllNodesWithTag("viewer-play").fetchSemanticsNodes().isNotEmpty())compose.onNodeWithTag("viewer-play").performClick()
        else compose.onNodeWithText(title).performClick()
    }
    private fun scrollOffset() = compose.onNodeWithTag("library-list").fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun openAndPlay() {
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.onNodeWithContentDescription("Volver a Sesiones").assertIsDisplayed()
        compose.onNodeWithText("Reproducir").performScrollTo().performClick()
        compose.waitUntil { vm.playingId == session.id }
        compose.runOnIdle {
            assertEquals(TallerRoutes.Session, navController.currentDestination?.route)
            assertFalse(vm.playbackPaused)
        }
    }

    private fun assertReturnedToSameList(offset: Float) {
        awaitDisplayed("library-list")
        compose.onNodeWithTag("library-list").assertIsDisplayed()
        compose.onNodeWithText("Sesiones").assertIsSelected()
        compose.onNodeWithContentDescription("Volver a Sesiones").assertDoesNotExist()
        assertEquals(offset, scrollOffset(), 1f)
        compose.runOnIdle {
            assertEquals(TallerRoutes.Library, navController.currentDestination?.route)
            assertEquals(TallerRoutes.Measure, navController.previousBackStackEntry?.destination?.route)
            assertEquals(Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState)
            assertNull(vm.playingId)
            assertFalse(vm.playbackPaused)
            assertEquals(0, vm.playbackPositionMs())
        }
    }
    private fun awaitDisplayed(tag:String){
        // Android's system/predictive Back transition is not driven by the Compose test clock.
        try {compose.waitUntil(5000){runCatching {compose.onNodeWithTag(tag).assertIsDisplayed()}.isSuccess}}
        catch(e:AssertionError){
            android.util.Log.i("PrecisionBack","failure route=${navController.currentDestination?.route} focus=${compose.activity.hasWindowFocus()} lifecycle=${compose.activity.lifecycle.currentState}")
            val file=File(compose.activity.getExternalFilesDir(null),"stability/back-failure.png").apply {parentFile!!.mkdirs()}
            file.outputStream().use {InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)}
            throw e
        }
    }

    @Test fun systemBackPopsDetailPreservesScrollAndStopsPlayback() {
        val offset = scrollOffset()
        assertTrue(offset > 0)
        openAndPlay()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        assertReturnedToSameList(offset)
        // Reopening must be a fresh, stopped player, with exactly one detail in the stack.
        openAndPlay()
        compose.onNodeWithContentDescription("Volver a Sesiones").performClick()
        assertReturnedToSameList(offset)
    }

    @Test fun toolbarArrowPopsTheSameEntryAndPreservesScroll() {
        compose.runOnIdle { session = vm.sessions.last() }
        compose.onNodeWithTag("session-${session.id}").performScrollTo()
        val offset = scrollOffset()
        openAndPlay()
        compose.onNodeWithContentDescription("Volver a Sesiones").performClick()
        assertReturnedToSameList(offset)
    }

    @Test fun changingSectionReleasesPlaybackAndDoesNotRestoreAnAbandonedDetail() {
        val offset = scrollOffset()
        openAndPlay()
        compose.onNodeWithText("Medir", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("measure-screen").assertIsDisplayed()
        compose.runOnIdle { assertNull(vm.playingId) }
        compose.onNodeWithText("Biblioteca", useUnmergedTree = true).performClick()
        assertReturnedToSameList(offset)
    }

    @Test fun recoveredAudioCanBeAnalyzedWithoutChangingWavOrIndex() {
        val store=RecordingStore(application)
        val prefs=application.getSharedPreferences("local_data",0)
        val beforeIndex=prefs.getString("recordings",null)
        val bytes=store.file(recoveredId).readBytes()
        compose.onNodeWithTag("session-$recoveredId").performScrollTo().performClick()
        compose.onNodeWithText("Analizar grabación").performScrollTo().performClick()
        compose.waitUntil(20000) {vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        compose.runOnIdle {
            val result=vm.recordingAnalysis.result!!
            assertEquals(66.1,result.summary!!.typicalHz,0.3)
            assertTrue(result.harmonics.peaks.any {it.order==1})
            assertEquals(44100,result.sampleRate)
            assertEquals(beforeIndex,prefs.getString("recordings",null))
            assertArrayEquals(bytes,store.file(recoveredId).readBytes())
        }
        compose.onNodeWithText("H1 candidato",substring=true).performScrollTo().assertIsDisplayed()
        val screenshot=File(application.getExternalFilesDir(null),"phase-a-synthetic-66hz.png")
        screenshot.outputStream().use {output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,output)
        }
        compose.onNodeWithText("Reproducir").performScrollTo().performClick()
        compose.waitUntil {vm.playingId==recoveredId && !vm.playbackPaused}
        playAction("Pausar")
        compose.runOnIdle {assertTrue(vm.playbackPaused)}
        assertEquals(beforeIndex,prefs.getString("recordings",null))
        assertArrayEquals(bytes,store.file(recoveredId).readBytes())
    }

    @Test fun progressAndCancelButtonWorkOnLongReadOnlyWav() {
        val store=RecordingStore(application)
        val prefs=application.getSharedPreferences("local_data",0)
        val beforeIndex=prefs.getString("recordings",null)
        val file=store.file(recoveredId)
        val dataSize=44100*2*900
        val header=WavCodec.encode(ShortArray(0),0,44100)
        ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).putInt(4,36+dataSize).putInt(40,dataSize)
        // Extend only this isolated emulator fixture. It is synthetic; no microphone is opened.
        RandomAccessFile(file,"rw").use {wav -> wav.setLength(44L+dataSize);wav.seek(0);wav.write(header)}
        compose.onNodeWithTag("session-$recoveredId").performScrollTo().performClick()
        compose.onNodeWithText("Analizar grabación").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar análisis").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Analizando audio",substring=true).assertIsDisplayed()
        compose.runOnIdle {assertTrue(vm.recordingAnalysis.running);assertTrue(vm.recordingAnalysis.progress in 0..99)}
        compose.onNodeWithText("Cancelar análisis").performClick()
        compose.onNodeWithText("Análisis cancelado.").assertIsDisplayed()
        compose.runOnIdle {assertFalse(vm.recordingAnalysis.running);assertNull(vm.recordingAnalysis.result)}
        assertEquals(beforeIndex,prefs.getString("recordings",null))
        assertEquals(44L+dataSize,file.length())
    }

    @Test fun damagedAudioReportsErrorWithoutChangingMetadata() {
        val store=RecordingStore(application)
        val prefs=application.getSharedPreferences("local_data",0)
        val before=prefs.getString("recordings",null)
        store.file(session.id).writeBytes(ByteArray(64)) // isolated test fixture only
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.onNodeWithText("Analizar grabación").performScrollTo().performClick()
        compose.waitUntil(10000) {vm.recordingAnalysis.error!=null}
        compose.onNodeWithText("No es un WAV RIFF válido.").performScrollTo().assertIsDisplayed()
        assertEquals(before,prefs.getString("recordings",null))
    }

    @Test fun leavingDetailCancelsPendingAnalysisAndRejectsItsCallbacks() {
        val offset=scrollOffset()
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.runOnIdle {
            vm.analyzeRecording(session)
            assertTrue(vm.recordingAnalysis.running)
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        assertReturnedToSameList(offset)
        compose.runOnIdle {
            assertFalse(vm.recordingAnalysis.running)
            assertNull(vm.recordingAnalysis.sessionId)
            assertNull(vm.recordingAnalysis.result)
        }
    }

    @Test fun swipeOnlyDeletesAfterReleaseAndUndoRestoresWav() {
        val store=RecordingStore(application);val bytes=store.file(session.id).readBytes()
        val row=compose.onNodeWithTag("swipe-${session.id}")
        row.performScrollTo()
        // A short drag returns home and preserves the session.
        row.performTouchInput {swipe(center,center.copy(x=center.x-width*.15f),500)}
        compose.runOnIdle {assertTrue(vm.sessions.any {it.id==session.id})}
        row.performTouchInput {swipe(center,center.copy(x=center.x-width*.15f),80)}
        compose.runOnIdle {assertTrue("Un gesto corto no debe borrar por velocidad",vm.sessions.any {it.id==session.id})}
        row.performTouchInput {down(center);moveTo(center.copy(x=width*.02f),400);moveTo(center,500);up()}
        compose.runOnIdle {assertTrue(vm.sessions.any {it.id==session.id});assertArrayEquals(bytes,store.file(session.id).readBytes())}
        // Move well beyond the threshold, but hold: no destructive commit while pressed.
        row.performTouchInput {down(center);moveTo(center.copy(x=width*.02f),500)}
        compose.runOnIdle {assertTrue(vm.sessions.any {it.id==session.id})}
        row.performTouchInput {up()}
        compose.waitUntil(5000){vm.sessions.none {it.id==session.id}}
        assertArrayEquals(bytes,store.file(session.id).readBytes())
        compose.onNodeWithText("Deshacer").performClick()
        compose.waitUntil {vm.sessions.any {it.id==session.id}}
        assertArrayEquals(bytes,store.file(session.id).readBytes())
        assertTrue(vm.trashedSessions.none {it.id==session.id})
    }

    @Test fun singleActionRecordsAndSavesAndLifecycleStopDoesNotKeepRecording() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        instrument.uiAutomation.grantRuntimePermission(application.packageName,android.Manifest.permission.RECORD_AUDIO)
        val original=vm.sessions.map {it.id}.toSet()
        compose.onNodeWithText("Medir",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Iniciar medición").performClick()
        compose.waitUntil {vm.sessionPhase==ar.didgetaller.SessionPhase.CAPTURING}
        Thread.sleep(1200)
        compose.onNodeWithText("Detener y guardar",substring=true).performClick()
        compose.waitUntil(10000){vm.sessionPhase==ar.didgetaller.SessionPhase.IDLE && vm.sessions.size==original.size+1}
        compose.runOnIdle {
            assertFalse(vm.listening)
            val saved=vm.sessions.first {it.id !in original}
            assertTrue(saved.durationMs>=500);assertTrue(vm.sessionFile(saved).length()>44100)
            ar.didgetaller.core.Pcm16WavReader(vm.sessionFile(saved)).use {assertEquals(44100,it.sampleRate)}
        }
        compose.onNodeWithText("Iniciar medición").performClick()
        compose.waitUntil {vm.sessionPhase==ar.didgetaller.SessionPhase.CAPTURING}
        Thread.sleep(700)
        compose.runOnUiThread {vm.onAppStopped()}
        compose.waitUntil(10000){vm.sessionPhase==ar.didgetaller.SessionPhase.IDLE && vm.sessions.size==original.size+2}
        assertFalse(vm.listening)
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
        compose.runOnIdle {assertEquals(original.size+2,RecordingStore(application).list().size)}
    }

    @Test fun designAssociationAndManualMarkersSurviveReopening() {
        compose.runOnIdle {vm.saveDesign("Perfil UI de prueba");assertEquals(1,vm.designs.size)}
        // Let the save snackbar leave; it otherwise intercepts taps near the bottom of the viewport.
        compose.waitUntil(10000){vm.message.isBlank()}
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.onNodeWithText("Asociar un diseño…").performScrollTo().performClick()
        compose.onNodeWithText("Perfil UI de prueba").performClick()
        compose.waitUntil(10000){vm.associatedSimulation!=null}
        compose.onNodeWithText("SIMULACIÓN",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("AUDIO · sesión",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Marcadores").performScrollTo().performClick()
        compose.onNodeWithText("Marcar posible toot").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(1,vm.markers.size);assertEquals("Posible toot",vm.markers.single().type)}
        compose.onNodeWithContentDescription("Volver a Sesiones").performClick()
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.waitUntil {vm.markers.size==1 && vm.associatedSimulation!=null}
        compose.runOnIdle {assertNotNull(vm.associatedDesignId)}
        compose.onNodeWithText("Perfil UI de prueba").performScrollTo().assertIsDisplayed()
    }

    @Test fun explorerRendersRealGraphsSeeksPausesLoopsAndCompares() {
        openAndPlay()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("acoustic-viewer").assertIsDisplayed()
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).assertDoesNotExist()
        assertEquals(session.id,vm.playingId)
        choosePlot("Espectrograma")
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null || vm.workspace.state.error!=null}
        assertNull(vm.workspace.state.error);assertTrue(vm.workspace.state.spectrogram!!.reference>0)
        choosePlot("Forma de onda")
        compose.waitUntil(20000){vm.workspace.state.wave!=null}
        assertTrue(vm.workspace.state.wave!!.max.any {it>0.05})
        compose.onNodeWithContentDescription("Acercar").performClick()
        compose.waitUntil(10000){vm.workspace.state.wave?.let {it.end-it.start<vm.workspace.state.frames}==true}
        val explorer=vm.explorer(session.id)
        val before=explorer.camera(session.durationMs,44100,session.timeline)
        compose.onNodeWithTag("immersive-plot").performTouchInput {swipe(center,center+androidx.compose.ui.geometry.Offset(-width*.15f,height*.15f),500)}
        compose.runOnIdle {val after=explorer.cameras[AcousticPlot.WAVE]!!;assertTrue(after.x.low>before.x.low);assertTrue(after.y.low>before.y.low);assertFalse(explorer.follow)}
        compose.onNodeWithTag("reset-viewport").performClick()
        compose.runOnIdle {assertEquals(-1.0,explorer.cameras[AcousticPlot.WAVE]!!.y.low,0.0)}
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.onNodeWithText("Marcar inicio").performScrollTo().performClick()
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").performTouchInput {click(androidx.compose.ui.geometry.Offset(width*.3f,height*.5f))}
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.onNodeWithText("Marcar fin").performScrollTo().performClick()
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.onNodeWithTag("immersive-plot").performTouchInput {click(androidx.compose.ui.geometry.Offset(width*.65f,height*.5f))}
        compose.runOnIdle {assertNotNull(explorer.region)}
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.onNodeWithText("Analizar fragmento").performScrollTo().performClick()
        compose.waitUntil(20000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        compose.runOnIdle {assertNull(vm.recordingAnalysis.error);assertEquals(66.1,vm.recordingAnalysis.result!!.summary!!.typicalHz,.3)}
        compose.onNodeWithTag("loop-fragment").performScrollTo().performClick()
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.runOnIdle {vm.seekPlayback(session,0)}
        compose.waitUntil(5000){vm.playbackPositionMs().toLong() in explorer.region!!}
        playAction("Pausar")
        compose.waitUntil {vm.playbackPaused};val position=vm.playbackPositionMs();Thread.sleep(180)
        assertTrue(kotlin.math.abs(position-vm.playbackPositionMs())<=80)
        choosePlot("Armónicos")
        compose.onNodeWithTag("immersive-plot").assertIsDisplayed()
        android.util.Log.i("PrecisionBack","before route=${navController.currentDestination?.route} popups=${compose.onAllNodes(isPopup()).fetchSemanticsNodes().size} focus=${compose.activity.hasWindowFocus()} lifecycle=${compose.activity.lifecycle.currentState}")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        android.util.Log.i("PrecisionBack","after route=${navController.currentDestination?.route}")
        awaitDisplayed("session-detail");assertEquals(session.id,vm.playingId)
        compose.onNodeWithContentDescription("Volver a Sesiones").performClick()
        assertNull(vm.playingId)
        compose.onNodeWithText("Comparar dos sesiones").performScrollTo().performClick()
        compose.onNodeWithText("Comparar",useUnmergedTree=true).performScrollTo().performClick()
        compose.waitUntil(30000){vm.comparison.a!=null || vm.comparison.error!=null}
        assertNull(vm.comparison.error);assertEquals(66.1,vm.comparison.a!!.summary!!.typicalHz,.3)
        compose.onNodeWithTag("spectrum-chart").performScrollTo().assertIsDisplayed()
    }

    @Test fun immersiveViewportPinchInspectionResetAndBackPreserveSession() {
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        val graph=compose.onNodeWithTag("immersive-plot")
        val state=vm.explorer(session.id)
        val initial=state.camera(session.durationMs,44100,session.timeline)
        graph.performTouchInput {pinch(start0=center-androidx.compose.ui.geometry.Offset(70f,70f),end0=center-androidx.compose.ui.geometry.Offset(140f,140f),start1=center+androidx.compose.ui.geometry.Offset(70f,70f),end1=center+androidx.compose.ui.geometry.Offset(140f,140f),durationMillis=500)}
        compose.runOnIdle {val next=state.cameras[AcousticPlot.PITCH]!!;assertTrue("Horizontal: $initial → $next",next.x.span<initial.x.span);assertTrue("Vertical: $initial → $next",next.y.span<initial.y.span)}
        graph.performTouchInput {longClick(center,800)}
        compose.runOnIdle {assertNotNull(state.inspected);assertTrue(state.inspected!!.contains("cents"))}
        graph.performTouchInput {doubleClick(center)}
        compose.runOnIdle {assertEquals(initial,state.cameras[AcousticPlot.PITCH])}
        compose.onNodeWithContentDescription("Acercar").performClick()
        compose.runOnIdle {vm.seekPlayback(session,1000)}
        compose.onNodeWithContentDescription("Seguir").performClick()
        compose.waitUntil {val view=state.cameras[AcousticPlot.PITCH]!!;vm.playbackPositionMs().toDouble() in view.x.low..view.x.high}
        graph.performTouchInput {swipe(center,center+androidx.compose.ui.geometry.Offset(-50f,50f),400)}
        compose.runOnIdle {assertFalse(state.follow)}
        val manuallyMoved=state.cameras[AcousticPlot.PITCH]
        Thread.sleep(180)
        compose.runOnIdle {assertEquals(manuallyMoved,state.cameras[AcousticPlot.PITCH])}
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        awaitDisplayed("session-detail")
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(manuallyMoved,state.cameras[AcousticPlot.PITCH])}
    }

    @Test fun longWavViewportCanCancelAndZoomToActualSamplesWithoutChangingSource(){
        val file=RecordingStore(application).file(recoveredId)
        val dataSize=44100*2*900
        val header=WavCodec.encode(ShortArray(0),0,44100)
        ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).putInt(4,36+dataSize).putInt(40,dataSize)
        RandomAccessFile(file,"rw").use {it.setLength(44L+dataSize);it.seek(0);it.write(header)}
        val start=android.os.SystemClock.elapsedRealtime()
        compose.onNodeWithTag("session-$recoveredId").performScrollTo().performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.frames==39_690_000L}
        choosePlot("Espectrograma")
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.onNodeWithText("Cancelar visualización").performScrollTo().performClick()
        compose.runOnIdle {assertFalse(vm.workspace.state.loading);assertEquals("Visualización cancelada.",vm.workspace.state.error)}
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        choosePlot("Forma de onda")
        compose.waitUntil(30000){vm.workspace.state.wave!=null}
        val overviewMs=android.os.SystemClock.elapsedRealtime()-start
        compose.runOnIdle {vm.explorer(recoveredId).cameras[AcousticPlot.WAVE]=ar.didgetaller.core.PlotViewport(ar.didgetaller.core.AxisWindow(600000.0,600010.0),ar.didgetaller.core.AxisWindow(-1.0,1.0))}
        compose.waitUntil(10000){vm.workspace.state.wave?.samples==true}
        assertEquals(900_000L,vm.workspace.state.durationMs)
        assertTrue(vm.workspace.state.wave!!.min.size<=1024)
        assertEquals(44L+dataSize,file.length())
        println("V31_LONG_WAV overview_and_cancel_ms=$overviewMs waveform_points=${vm.workspace.state.wave!!.min.size}")
    }

    @Test fun androidEdgeGesturePopsViewerThenDetailAndReleasesAudio(){
        val resources=compose.activity.resources
        val mode=resources.getIdentifier("config_navBarInteractionMode","integer","android")
        assumeTrue("Requiere navegación por gestos en el emulador",mode!=0 && resources.getInteger(mode)==2)
        val offset=scrollOffset();openAndPlay()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        fun edgeBack(){
            val previous=navController.currentDestination?.route
            val metrics=resources.displayMetrics
            val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input swipe 1 ${metrics.heightPixels/2} ${metrics.widthPixels/3} ${metrics.heightPixels/2} 350")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {it.readBytes()}
            compose.waitUntil(5000){navController.currentDestination?.route!=previous}
            compose.waitForIdle()
        }
        edgeBack()
        compose.onNodeWithTag("session-detail").assertIsDisplayed()
        compose.runOnIdle {assertEquals(session.id,vm.playingId)}
        edgeBack();assertReturnedToSameList(offset)
    }

    @Test fun spectrogramRangesKeepReferenceAndSpectrumOffersRealPeakInspectionAndAudio(){
        compose.onNodeWithTag("session-${session.id}").performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        choosePlot("Espectrograma")
        val state=vm.explorer(session.id)
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null && state.spectrumReference>0}
        val reference=state.spectrumReference
        assertTrue(reference>0)
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.onNodeWithText("100 Hz").performScrollTo().performClick()
        compose.onNodeWithTag("log-frequency").performScrollTo().performClick()
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.spectrogram?.let {it.logarithmic && kotlin.math.abs(it.highHz-100.0)<1e-8}==true}
        assertEquals(reference,state.spectrumReference,0.0);assertTrue(vm.workspace.state.spectrogram!!.lowHz>0)
        choosePlot("Armónicos")
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.onNodeWithTag("viewer-analyze-tools").performScrollTo().performClick()
        compose.onNodeWithText("Volver al gráfico").performScrollTo().performClick()
        compose.waitUntil(20000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        assertNull(vm.recordingAnalysis.error)
        val density=compose.activity.resources.displayMetrics.density
        compose.onNodeWithTag("immersive-plot").performTouchInput {click(androidx.compose.ui.geometry.Offset(58*density+(width-70*density)*66.1f/500,center.y))}
        compose.runOnIdle {assertTrue(state.inspected?.contains("H1 candidato")==true);assertTrue(state.inspected!!.contains("dB rel"))}
        playAction("Reproducir")
        compose.waitUntil {vm.playingId==session.id && !vm.playbackPaused}
        playAction("Pausar");assertTrue(vm.playbackPaused)
    }

    private class NavigationTestApplication(context: Context) : Application() {
        init { attachBaseContext(context) }
        override fun getFilesDir() = File(baseContext.filesDir, "navigation-tests").apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("navigation-tests-$name", mode)
        fun cleanTestData() {
            getSharedPreferences("local_data", MODE_PRIVATE).edit().clear().commit()
            getSharedPreferences("session_tools", MODE_PRIVATE).edit().clear().commit()
            filesDir.deleteRecursively()
        }
    }
}
