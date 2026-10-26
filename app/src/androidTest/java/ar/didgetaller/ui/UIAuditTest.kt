package ar.didgetaller.ui

import ar.didgetaller.R

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.SessionPhase
import ar.didgetaller.core.*
import ar.didgetaller.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.UUID
import kotlin.math.*

/** Screenshot inventory, real Compose/MediaPlayer/DSP, entirely isolated emulator fixtures. */
class UIAuditTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owners=ViewModelStore()
    private lateinit var application:AuditApplication
    private lateinit var vm:TallerViewModel
    private lateinit var session:RecordingStore.Session
    private val args=InstrumentationRegistry.getArguments()
    private val prefix get()=args.getString("auditPrefix") ?: "v31-audit"
    @Before fun setup(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        application=AuditApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        application.clean()
        val store=RecordingStore(application)
        val rate=44100;val pcm=ShortArray(rate*12){i ->
            val t=i.toDouble()/rate
            if(t in 5.0..6.0)0 else (7000*sin(2*PI*70*t)+4000*sin(2*PI*140*t)+2000*sin(2*PI*210*t)).toInt().toShort()
        }
        val bytes=WavCodec.encode(pcm,pcm.size,rate)
        val file=File(application.filesDir,"reference.wav").apply {writeBytes(bytes)}
        val result=Pcm16WavReader(file).use {WavAnalysis.analyze(it)}
        session=store.save(bytes,result.timeline,1000000)
        store.rename(session.id,"PVC · sesión sintética de auditoría con nombre largo para comprobar texto y controles")
        store.save(WavCodec.encode(ShortArray(rate*2),rate*2,rate),createdAt=500000)
        store.file(UUID.randomUUID().toString()).writeBytes(bytes)
        LocalStore(application).saveDesign("Diseño de prueba geométrica · nombre extenso",AcousticModel.Shape())
        compose.runOnUiThread {vm=TallerViewModel(application);owners.put("audit",vm)}
        compose.setContent {TallerApp(vm,{vm.startSession()},{if(vm.listening)vm.microphoneStop()else vm.microphoneStart()},{_,_,_->})}
    }
    @After fun close(){if(::vm.isInitialized)compose.runOnUiThread {owners.clear()};if(::application.isInitialized)application.clean()}
    private fun shot(name:String,dialog:Boolean=false,nativeWindow:Boolean=false){
        compose.waitForIdle()
        val directory=File(application.getExternalFilesDir(null),"ui-audit").apply {mkdirs()}
        if(nativeWindow){
            // A modal sheet owns a second Compose root; capture the actual window instead.
            Thread.sleep(350) // Wait for the sheet's window animation before visual inspection.
            val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(directory,"$prefix-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
            return
        }
        val node=if(dialog)compose.onNode(isDialog()) else compose.onRoot()
        File(directory,"$prefix-$name.png").outputStream().use {node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun top(tag:String){compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)};compose.waitForIdle()}
    private fun viewerPlot(title:String){
        val expected=AcousticPlot.entries.single {compose.activity.plotLabel(it)==title}
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick()
        compose.onNodeWithText(title,useUnmergedTree=true).performClick()
        compose.waitUntil(10000){
            vm.explorer(session.id).plot==expected && compose.onAllNodes(isPopup()).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("immersive-plot").assertIsDisplayed()
    }
    @Test fun immersiveChartSwitchesHaveSelfContainedAccessibleNames(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithTag("show-uncertain").assertContentDescriptionEquals(uiText(R.string.ui_show_original_uncertain_readings))
        compose.runOnIdle {vm.explorer(session.id).apply {regionStart=1000;regionEnd=2000}}
        compose.onNodeWithTag("loop-fragment").assertContentDescriptionEquals(uiText(R.string.ui_loop_excerpt))
        compose.onNodeWithTag("audio-seek").assertContentDescriptionEquals(uiText(R.string.playback_seek_label))
        compose.onNodeWithText(uiText(R.string.ui_back_to_chart)).performScrollTo().performClick()
        viewerPlot(uiText(R.string.ui_spectrogram))
        compose.onNodeWithTag("viewer-tools").performClick()
        compose.onNodeWithTag("log-frequency").assertContentDescriptionEquals(uiText(R.string.ui_logarithmic_frequency))
    }

    @Test fun pitchLegendIsVisibleBeforeThePlotItExplains(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.musical_drone_description)).performScrollTo()
        val legend=compose.onNodeWithText(uiText(R.string.musical_drone_description)).fetchSemanticsNode().boundsInRoot
        val plot=compose.onNodeWithTag("time-chart").fetchSemanticsNode().boundsInRoot
        assertTrue("The pitch legend must precede the plot",legend.bottom<=plot.top)
        shot("legend-before-plot")
    }

    @Test fun compactLandscapeKeepsTheMeasurementReadingOnScreen(){
        val configuration=compose.activity.resources.configuration
        assumeTrue("Requires short landscape with large text, height=${configuration.screenHeightDp}, font=${configuration.fontScale}",
            configuration.screenHeightDp<500 && configuration.fontScale>=1.5f)
        shot("compact-landscape-reading")
        compose.onNodeWithTag("measurement-frequency").assertIsDisplayed()
        compose.onNodeWithTag("acoustic-signal-status").assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).assertIsDisplayed()
        val statusNode=compose.onNodeWithTag("acoustic-signal-status").fetchSemanticsNode()
        val status=statusNode.boundsInRoot
        val action=compose.onNodeWithText(uiText(R.string.ui_start_measurement)).fetchSemanticsNode().boundsInRoot
        assertTrue("The measurement status must be above the recording action",status.bottom<=action.top)
        assertEquals("The landscape status must not be clipped",statusNode.size.height.toFloat(),status.height,1f)
    }
    @Test fun compactPortraitKeepsTheMeasurementStatusAboveTheRecordAction(){
        val configuration=compose.activity.resources.configuration
        assumeTrue("Requires a height-constrained portrait with large text",
            configuration.screenHeightDp>500 && configuration.fontScale>=1.5f && configuration.screenHeightDp/configuration.fontScale<560f)
        val statusNode=compose.onNodeWithTag("acoustic-signal-status").fetchSemanticsNode()
        val status=statusNode.boundsInRoot
        val action=compose.onNodeWithText(uiText(R.string.ui_start_measurement)).fetchSemanticsNode().boundsInRoot
        assertTrue("The current measurement must precede the fixed recording action",status.bottom<=action.top)
        val content=compose.onNodeWithTag("measure-screen").fetchSemanticsNode().boundsInRoot
        assertTrue("The entire status must fit in the measurement viewport",status.bottom<=content.bottom)
        assertEquals("The status must not be clipped by the fixed action",statusNode.size.height.toFloat(),status.height,1f)
        shot("compact-portrait-reading")
    }
    @Test fun productPolishEvidenceAndTouchTargets(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithTag("swipe-compare-toggle").assertDoesNotExist()
        compose.onNodeWithTag("session-playback-seek").performScrollTo()
            .assertContentDescriptionEquals(uiText(R.string.playback_seek_label))
            .performSemanticsAction(SemanticsActions.SetProgress){it(3000f)}
        compose.waitUntil(10000){kotlin.math.abs(vm.playbackPositionMs()-3000)<80}
        compose.onNodeWithTag("time-chart").performScrollTo();shot("pitch-transport")
        compose.onNodeWithText(uiText(R.string.ui_spectrogram)).performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null && !vm.workspace.state.loading}
        compose.onNodeWithTag("time-chart").performScrollTo();shot("spectrogram-transport")
        compose.onNodeWithTag("plot-tab-pitch").performScrollTo().performClick()
        compose.onNodeWithTag("session-tools").performScrollTo().performClick()
        compose.onNodeWithTag("advanced-diagnostics-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("swipe-compare-toggle").performScrollTo()
            .assertContentDescriptionEquals(uiText(R.string.swipe_compare))
        val bounds=compose.onNodeWithTag("swipe-compare-toggle").fetchSemanticsNode().boundsInRoot
        val density=compose.activity.resources.displayMetrics.density
        assertTrue("Comparison has a 48dp accessible touch target: $bounds, density=$density",
            bounds.width+1>=48*density && bounds.height+1>=48*density)
        compose.onNodeWithTag("swipe-compare-toggle").performClick()
        compose.waitUntil(20000){vm.swipeComparison.curve!=null}
        shot("advanced-comparison",nativeWindow=true)
        compose.onNodeWithTag("saved-evidence-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("saved-evidence-inspect").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("diagnostic-time-chart").performScrollTo().assertIsDisplayed()
        shot("advanced-chart",nativeWindow=true)
    }

    @Test fun chartsInventory(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        graphInventory()
    }
    @Test fun sessionUxScreenshots(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithTag("time-chart").assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.ui_play)).assertIsDisplayed()
        shot("session-portrait")
        compose.onNodeWithText(uiText(R.string.ui_analyze_recording)).performScrollTo().performClick()
        compose.waitUntil(20000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        assertNull(vm.recordingAnalysis.error)
        compose.onNodeWithTag("plot-tab-spectrum").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(AcousticPlot.SPECTRUM,vm.preview(session.id).plot)}
        compose.onNodeWithTag("spectrum-chart").assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.spectrum_axis_title)).assertExists()
        shot("harmonics")
        compose.onNodeWithTag("plot-tab-pitch").performScrollTo().performClick()
        compose.onNodeWithTag("session-tools").performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_scales_and_ranges)).assertIsDisplayed()
        shot("tools",nativeWindow=true)
        compose.onNodeWithTag("advanced-diagnostics-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("diagnostic-time-chart").performScrollTo().assertIsDisplayed()
        shot("advanced",nativeWindow=true)
    }
    private fun graphInventory(){
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick();shot("visor-fundamental")
        val description=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue("Native chart semantics must follow the active language",description.contains(uiText(R.string.ui_time)))
        viewerPlot(uiText(R.string.ui_spectrogram))
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null || vm.workspace.state.error!=null}
        assertNull(vm.workspace.state.error);shot("visor-espectrograma")
        viewerPlot(uiText(R.string.ui_waveform))
        compose.waitUntil(20000){vm.workspace.state.wave!=null};shot("visor-onda")
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        compose.onNodeWithText(uiText(R.string.ui_analyze_recording)).performScrollTo().performClick()
        compose.waitUntil(20000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        assertNull(vm.recordingAnalysis.error)
        compose.onNodeWithTag("recording-analysis").performScrollTo();shot("analisis")
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        viewerPlot(uiText(R.string.ui_harmonics));shot("visor-armonicos")
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        compose.onNodeWithTag("session-tools").performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_selection_and_excerpts)).performScrollTo().performClick()
        compose.onNodeWithTag("selection-mode-2").performScrollTo();shot("marcar-fin",nativeWindow=true)
        val chip=compose.onNodeWithTag("selection-mode-2").fetchSemanticsNode().boundsInRoot
        assertTrue("Marcar fin debe tener ancho horizontal útil",chip.width>chip.height*1.3)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_sessions)).performClick()
        compose.onNodeWithText(uiText(R.string.ui_compare_two_sessions)).performScrollTo().performClick();shot("comparar")
    }
    @Test fun matrixInventory(){
        shot("medir")
        compose.onNodeWithText(uiText(R.string.ui_limit_min_settings, vm.recordingLimitSeconds/60)).performClick();shot("ajustes-captura")
        compose.onNodeWithText(uiText(R.string.ui_laboratory),useUnmergedTree=true).performClick();shot("laboratorio")
        compose.onNodeWithText(uiText(R.string.ui_design),useUnmergedTree=true).performClick();shot("disenar")
        compose.onNodeWithText(uiText(R.string.ui_bore_points)).performScrollTo();shot("perfil")
        compose.onNodeWithText(uiText(R.string.ui_estimated_resonances)).performScrollTo();shot("simulacion")
        compose.onNodeWithText(uiText(R.string.ui_spectrum),useUnmergedTree=true).performScrollTo().performClick();shot("espectro-vacio")
        compose.onNodeWithText(uiText(R.string.ui_search),useUnmergedTree=true).performScrollTo().performClick();shot("buscar")
        compose.onNodeWithText(uiText(R.string.ui_guide),useUnmergedTree=true).performScrollTo().performClick();shot("guia")
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_laboratory)).performClick()
        compose.onNodeWithTag("lab-home").assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick();shot("biblioteca")
        compose.onNodeWithText(uiText(R.string.ui_designs),useUnmergedTree=true).performClick();shot("disenos")
        compose.onNodeWithText(uiText(R.string.ui_sessions),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick();shot("detalle")
        compose.onNodeWithText(uiText(R.string.ui_audio),useUnmergedTree=true).performScrollTo();shot("reproductor")
        graphInventory()
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_sessions)).performClick()
        compose.runOnIdle {vm.sessions.toList().forEach {vm.deleteSession(it)};vm.clearUndo();vm.clearMessage()}
        top("library-list");shot("biblioteca-vacia")
    }
    @Test fun renameDialogIsLocalizedWithoutChangingStoredName(){
        val original=RecordingStore(application).list().first {it.id==session.id}.name
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_rename)).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_rename_session)).assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.ui_name)).assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.ui_save)).assertIsDisplayed()
        shot("rename-dialog",dialog=true)
        compose.onNodeWithText(uiText(R.string.ui_cancel)).performClick()
        assertEquals(original,RecordingStore(application).list().first {it.id==session.id}.name)
    }
    @Test fun measurementStatesAndFinalForms(){
        shot("medir")
        compose.runOnIdle {vm.onPermissionResult(false,true)};shot("permiso-denegado")
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(application.packageName,android.Manifest.permission.RECORD_AUDIO)
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).performClick();compose.waitUntil {vm.sessionPhase==SessionPhase.CAPTURING}
        Thread.sleep(700)
        compose.onNodeWithText(uiText(R.string.diagnostic_measurement_requires_microphone_permission_you_can_grant_it_in_setting)).assertDoesNotExist()
        shot("medir-activo")
        compose.onNodeWithText(uiText(R.string.ui_wide_view)).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_stop_and_save)).assertIsDisplayed();shot("medir-foco")
        compose.onNodeWithText(uiText(R.string.ui_stop_and_save)).performClick()
        compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE}
        compose.runOnIdle {vm.fullScreen=false;vm.clearMessage()}
        compose.onNodeWithText(uiText(R.string.ui_laboratory),useUnmergedTree=true).performClick()
        compose.onNodeWithText(uiText(R.string.ui_design),useUnmergedTree=true).performClick();shot("disenar")
        frame(uiText(R.string.ui_bore_points),"lab-screen");shot("perfil-controles")
        frame(uiText(R.string.ui_overall_dimensions),"lab-screen");shot("dimensiones")
        frame(uiText(R.string.ui_estimated_resonances),"lab-screen");shot("simulacion")
        compose.onNodeWithText(uiText(R.string.ui_search),useUnmergedTree=true).performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_find_an_approximate_geometry)).performScrollTo().performClick();shot("busqueda-progreso")
        compose.runOnIdle {vm.cancelOptimization();vm.clearMessage()}
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick();shot("biblioteca")
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick();shot("detalle")
    }
    private fun frame(text:String,tag:String){
        val target=compose.onNodeWithText(text).performScrollTo().fetchSemanticsNode().boundsInRoot
        val container=compose.onNodeWithTag(tag)
        val offset=target.top-container.fetchSemanticsNode().boundsInRoot.top-24f
        container.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,offset)}
        compose.waitForIdle()
    }
    @Test fun finalDesignCardsAndResonancesAppearance(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithText(uiText(R.string.ui_designs),useUnmergedTree=true).performClick();shot("disenos")
        val card=compose.onNodeWithTag("swipe-design-${vm.designs.first().id}").captureToImage().asAndroidBitmap()
        var redPixels=0
        for(y in 0 until card.height)for(x in 0 until card.width){val pixel=card.getPixel(x,y);val r=android.graphics.Color.red(pixel);val g=android.graphics.Color.green(pixel);val b=android.graphics.Color.blue(pixel)
            if(r>g*1.5 && r>b*2 && r>40)redPixels++}
        assertEquals("El fondo destructivo no debe asomar en reposo",0,redPixels)
        compose.onNodeWithText(uiText(R.string.ui_laboratory),useUnmergedTree=true).performClick()
        compose.onNodeWithText(uiText(R.string.ui_design),useUnmergedTree=true).performClick()
        frame(uiText(R.string.ui_estimated_resonances),"lab-screen");shot("simulacion")
    }
    @Test fun inventory(){
        shot("medir")
        compose.runOnIdle {vm.onPermissionResult(false,true)};shot("permiso-denegado")
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(application.packageName,android.Manifest.permission.RECORD_AUDIO)
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).performClick();compose.waitUntil {vm.sessionPhase==SessionPhase.CAPTURING}
        Thread.sleep(700);shot("medir-activo")
        compose.runOnIdle {vm.fullScreen=true};shot("medir-foco")
        compose.runOnIdle {vm.fullScreen=false;vm.finishSession()}
        compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE}
        compose.runOnIdle {vm.clearMessage()}
        compose.onNodeWithText(uiText(R.string.ui_laboratory),useUnmergedTree=true).performClick();shot("laboratorio");compose.onNodeWithText(uiText(R.string.ui_design),useUnmergedTree=true).performClick();shot("disenar")
        compose.onNodeWithText(uiText(R.string.ui_bore_points)).performScrollTo();shot("perfil")
        compose.onNodeWithText(uiText(R.string.ui_estimated_resonances)).performScrollTo();shot("simulacion")
        top("lab-screen")
        compose.onNodeWithText(uiText(R.string.ui_spectrum),useUnmergedTree=true).performClick();compose.waitUntil {vm.labTab==1};shot("espectro-vivo-vacio")
        compose.onNodeWithText(uiText(R.string.ui_search),useUnmergedTree=true).performScrollTo().performClick();compose.waitUntil {vm.labTab==2};shot("buscar")
        compose.onNodeWithText(uiText(R.string.ui_find_an_approximate_geometry)).performScrollTo().performClick();shot("busqueda-progreso")
        compose.runOnIdle {vm.cancelOptimization();vm.clearMessage()}
        top("lab-screen");compose.onNodeWithText(uiText(R.string.ui_guide),useUnmergedTree=true).performClick();shot("guia")
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick();shot("biblioteca")
        compose.onNodeWithText(uiText(R.string.ui_designs),useUnmergedTree=true).performClick();shot("disenos")
        compose.onNodeWithText(uiText(R.string.ui_sessions),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick();shot("detalle")
        graphInventory()
    }
    private class AuditApplication(context:Context):Application(){
        init{attachBaseContext(context)}
        override fun getFilesDir()=File(baseContext.filesDir,"ui-audit-tests").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("ui-audit-$name",mode)
        fun clean(){getSharedPreferences("local_data",0).edit().clear().commit();getSharedPreferences("session_tools",0).edit().clear().commit();filesDir.deleteRecursively()}
    }
}
