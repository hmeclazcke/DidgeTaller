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
    private fun shot(name:String,dialog:Boolean=false){
        compose.waitForIdle()
        val directory=File(application.getExternalFilesDir(null),"ui-audit").apply {mkdirs()}
        val node=if(dialog)compose.onNode(isDialog()) else compose.onRoot()
        File(directory,"$prefix-$name.png").outputStream().use {node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun top(tag:String){compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)};compose.waitForIdle()}
    private fun viewerPlot(title:String){
        compose.onNodeWithContentDescription(uiText(R.string.ui_change_chart)).performClick()
        compose.onNodeWithText(title,useUnmergedTree=true).performClick()
    }
    @Test fun chartsInventory(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        graphInventory()
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
        compose.onNodeWithText(uiText(R.string.ui_selection_and_excerpts)).performScrollTo().performClick()
        compose.onNodeWithTag("selection-mode-2").performScrollTo();shot("marcar-fin")
        val chip=compose.onNodeWithTag("selection-mode-2").fetchSemanticsNode().boundsInRoot
        assertTrue("Marcar fin debe tener ancho horizontal útil",chip.width>chip.height*1.3)
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
