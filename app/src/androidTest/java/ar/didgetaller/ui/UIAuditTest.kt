package ar.didgetaller.ui

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
    private fun shot(name:String){
        compose.waitForIdle()
        val directory=File(application.getExternalFilesDir(null),"ui-audit").apply {mkdirs()}
        File(directory,"$prefix-$name.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun top(tag:String){compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)};compose.waitForIdle()}
    @Test fun chartsInventory(){
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        graphInventory()
    }
    private fun graphInventory(){
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick();shot("visor-fundamental")
        compose.onNodeWithText("Espectrograma",useUnmergedTree=true).performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.bitmap!=null || vm.workspace.state.error!=null}
        assertNull(vm.workspace.state.error);shot("visor-espectrograma")
        compose.onNodeWithText("Forma de onda",useUnmergedTree=true).performScrollTo().performClick()
        compose.waitUntil(20000){vm.workspace.state.wave!=null};shot("visor-onda")
        compose.onNodeWithContentDescription("Volver a la sesión").performClick()
        compose.onNodeWithText("Analizar grabación").performScrollTo().performClick()
        compose.waitUntil(20000){vm.recordingAnalysis.result!=null || vm.recordingAnalysis.error!=null}
        assertNull(vm.recordingAnalysis.error)
        compose.onNodeWithTag("recording-analysis").performScrollTo();shot("analisis")
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithText("Armónicos",useUnmergedTree=true).performScrollTo().performClick();shot("visor-armonicos")
        compose.onNodeWithContentDescription("Volver a la sesión").performClick()
        compose.onNodeWithText("Selección y fragmentos").performScrollTo().performClick()
        compose.onNodeWithTag("selection-mode-2").performScrollTo();shot("marcar-fin")
        val chip=compose.onNodeWithTag("selection-mode-2").fetchSemanticsNode().boundsInRoot
        assertTrue("Marcar fin debe tener ancho horizontal útil",chip.width>chip.height*1.3)
        compose.onNodeWithContentDescription("Volver a Sesiones").performClick()
        compose.onNodeWithText("Comparar dos sesiones").performScrollTo().performClick();shot("comparar")
    }
    @Test fun matrixInventory(){
        shot("medir")
        compose.onNodeWithText("Límite:",substring=true).performClick();shot("ajustes-captura")
        compose.onNodeWithText("Laboratorio",useUnmergedTree=true).performClick();shot("laboratorio")
        compose.onNodeWithText("Diseñar",useUnmergedTree=true).performClick();shot("disenar")
        compose.onNodeWithText("PUNTOS DEL CONDUCTO").performScrollTo();shot("perfil")
        compose.onNodeWithText("RESONANCIAS ESTIMADAS").performScrollTo();shot("simulacion")
        compose.onNodeWithText("Espectro",useUnmergedTree=true).performScrollTo().performClick();shot("espectro-vacio")
        compose.onNodeWithText("Buscar",useUnmergedTree=true).performScrollTo().performClick();shot("buscar")
        compose.onNodeWithText("Guía",useUnmergedTree=true).performScrollTo().performClick();shot("guia")
        compose.onNodeWithContentDescription("Volver al Laboratorio").performClick()
        compose.onNodeWithTag("lab-home").assertIsDisplayed()
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick();shot("biblioteca")
        compose.onNodeWithText("Diseños",useUnmergedTree=true).performClick();shot("disenos")
        compose.onNodeWithText("Sesiones",useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick();shot("detalle")
        compose.onNodeWithText("Audio",useUnmergedTree=true).performScrollTo();shot("reproductor")
        graphInventory()
        compose.onNodeWithContentDescription("Volver a Sesiones").performClick()
        compose.runOnIdle {vm.sessions.toList().forEach {vm.deleteSession(it)};vm.clearUndo();vm.clearMessage()}
        top("library-list");shot("biblioteca-vacia")
    }
    @Test fun measurementStatesAndFinalForms(){
        shot("medir")
        compose.runOnIdle {vm.onPermissionResult(false,true)};shot("permiso-denegado")
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(application.packageName,android.Manifest.permission.RECORD_AUDIO)
        compose.onNodeWithText("Iniciar medición").performClick();compose.waitUntil {vm.sessionPhase==SessionPhase.CAPTURING}
        Thread.sleep(700)
        compose.onNodeWithText("Para medir hace falta permiso",substring=true).assertDoesNotExist()
        shot("medir-activo")
        compose.onNodeWithText("Vista amplia").performScrollTo().performClick()
        compose.onNodeWithText("Detener y guardar").assertIsDisplayed();shot("medir-foco")
        compose.onNodeWithText("Detener y guardar").performClick()
        compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE}
        compose.runOnIdle {vm.fullScreen=false;vm.clearMessage()}
        compose.onNodeWithText("Laboratorio",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Diseñar",useUnmergedTree=true).performClick();shot("disenar")
        frame("PUNTOS DEL CONDUCTO","lab-screen");shot("perfil-controles")
        frame("DIMENSIONES GENERALES","lab-screen");shot("dimensiones")
        frame("RESONANCIAS ESTIMADAS","lab-screen");shot("simulacion")
        compose.onNodeWithText("Buscar",useUnmergedTree=true).performScrollTo().performClick()
        compose.onNodeWithText("Buscar geometría que se aproxime").performScrollTo().performClick();shot("busqueda-progreso")
        compose.runOnIdle {vm.cancelOptimization();vm.clearMessage()}
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick();shot("biblioteca")
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
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Diseños",useUnmergedTree=true).performClick();shot("disenos")
        val card=compose.onNodeWithTag("swipe-design-${vm.designs.first().id}").captureToImage().asAndroidBitmap()
        var redPixels=0
        for(y in 0 until card.height)for(x in 0 until card.width){val pixel=card.getPixel(x,y);val r=android.graphics.Color.red(pixel);val g=android.graphics.Color.green(pixel);val b=android.graphics.Color.blue(pixel)
            if(r>g*1.5 && r>b*2 && r>40)redPixels++}
        assertEquals("El fondo destructivo no debe asomar en reposo",0,redPixels)
        compose.onNodeWithText("Laboratorio",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Diseñar",useUnmergedTree=true).performClick()
        frame("RESONANCIAS ESTIMADAS","lab-screen");shot("simulacion")
    }
    @Test fun inventory(){
        shot("medir")
        compose.runOnIdle {vm.onPermissionResult(false,true)};shot("permiso-denegado")
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(application.packageName,android.Manifest.permission.RECORD_AUDIO)
        compose.onNodeWithText("Iniciar medición").performClick();compose.waitUntil {vm.sessionPhase==SessionPhase.CAPTURING}
        Thread.sleep(700);shot("medir-activo")
        compose.runOnIdle {vm.fullScreen=true};shot("medir-foco")
        compose.runOnIdle {vm.fullScreen=false;vm.finishSession()}
        compose.waitUntil(10000){vm.sessionPhase==SessionPhase.IDLE}
        compose.runOnIdle {vm.clearMessage()}
        compose.onNodeWithText("Laboratorio",useUnmergedTree=true).performClick();shot("laboratorio");compose.onNodeWithText("Diseñar",useUnmergedTree=true).performClick();shot("disenar")
        compose.onNodeWithText("PUNTOS DEL CONDUCTO").performScrollTo();shot("perfil")
        compose.onNodeWithText("RESONANCIAS ESTIMADAS").performScrollTo();shot("simulacion")
        top("lab-screen")
        compose.onNodeWithText("Espectro",useUnmergedTree=true).performClick();compose.waitUntil {vm.labTab==1};shot("espectro-vivo-vacio")
        compose.onNodeWithText("Buscar",useUnmergedTree=true).performScrollTo().performClick();compose.waitUntil {vm.labTab==2};shot("buscar")
        compose.onNodeWithText("Buscar geometría que se aproxime").performScrollTo().performClick();shot("busqueda-progreso")
        compose.runOnIdle {vm.cancelOptimization();vm.clearMessage()}
        top("lab-screen");compose.onNodeWithText("Guía",useUnmergedTree=true).performClick();shot("guia")
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick();shot("biblioteca")
        compose.onNodeWithText("Diseños",useUnmergedTree=true).performClick();shot("disenos")
        compose.onNodeWithText("Sesiones",useUnmergedTree=true).performClick()
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
