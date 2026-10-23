package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.SavedPitchEvidence
import ar.didgetaller.core.WavCodec
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.*

/** Existing WAV/index remain byte-identical. Every mutation is in UUID-owned emulator storage. */
class SavedPitchEvidenceUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val owner=ViewModelStore()
    private lateinit var app:EvidenceApplication
    private val originalLocale=Locale.getDefault()

    @After fun cleanup(){
        if(::app.isInitialized){compose.runOnUiThread{owner.clear()};app.clean()}
        Locale.setDefault(originalLocale)
    }

    @Test fun englishInspectionIsExplicitAndPreservesOriginals()=inspect("en")
    @Test fun spanishInspectionIsExplicitAndPreservesOriginals()=inspect("es-AR")

    private fun inspect(language:String){
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        app=EvidenceApplication(target)
        val locale=Locale.forLanguageTag(language);Locale.setDefault(locale)
        val configuration=Configuration(target.resources.configuration).apply{setLocale(locale)}
        val localized=target.createConfigurationContext(configuration)
        val pcm=ShortArray(44100*2){i->
            if(i>=44100)0 else {
                val t=i/44100.0
                ((.12*sin(2*PI*69.567*t)+.0377*sin(2*PI*139.134*t)+.01495*sin(2*PI*208.701*t)+.0918*sin(2*PI*182.221*t))*32767).roundToInt().toShort()
            }
        }
        val store=RecordingStore(app)
        val session=store.save(WavCodec.encode(pcm,pcm.size,44100),listOf(SessionAnalysis.Point(500,70.0,.98)),123L)
        val original=store.file(session.id).readBytes()
        val index=app.getSharedPreferences("local_data",0).getString("recordings",null)
        lateinit var vm:TallerViewModel
        val position=mutableStateOf(500L)
        compose.runOnUiThread{
            vm=TallerViewModel(app);owner.put("evidence",vm);vm.onDestinationChanged(session.id)
        }
        compose.setContent{
            CompositionLocalProvider(LocalContext provides localized,LocalConfiguration provides configuration){
                MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
                    LabCard("WAV"){SavedPitchEvidencePanel(vm,session,position.value)}
                }}
            }
        }
        compose.onNodeWithTag("saved-evidence-panel").assertDoesNotExist()
        compose.onNodeWithTag("saved-evidence-toggle").performClick()
        compose.runOnIdle{assertNull(vm.savedEvidence.result);assertFalse(vm.savedEvidence.running)}
        compose.onNodeWithTag("saved-evidence-inspect").performClick()
        compose.waitUntil(10000){vm.savedEvidence.result!=null}
        compose.onNodeWithTag("saved-evidence-accepted").assertExists()
        compose.onNodeWithTag("saved-evidence-families").assertExists()
        val first=requireNotNull(vm.savedEvidence.result)
        assertTrue(first.acceptedHz!! in 34.0..38.0)
        assertTrue(first.families.any {it in 68.0..71.0})
        for(tag in listOf("saved-evidence-accepted","saved-evidence-alternative","saved-evidence-families")){
            val layouts=mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
            assertTrue(layouts.isNotEmpty())
            layouts.forEach{
                assertFalse("$tag height: ${it.size.height}/${it.multiParagraph.height}",it.didOverflowHeight)
                // Paragraphs may retain the maximum constraint while Text reports intrinsic width.
                // Check actual line extents, allowing the same one-pixel rounding as localization tests.
                for(line in 0 until it.lineCount){
                    assertFalse(it.isLineEllipsized(line))
                    assertTrue("$tag line right",it.getLineRight(line)<=it.size.width+1f)
                    assertTrue("$tag line left",it.getLineLeft(line)>=-1f)
                }
            }
        }
        compose.onNodeWithTag("saved-evidence-markers").performScrollTo().assertIsDisplayed()
        val capture=File(target.getExternalFilesDir(null),"pitch-evidence-$language.png")
        compose.waitForIdle()
        val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        capture.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.runOnIdle{position.value=1500}
        compose.runOnIdle{assertEquals(first,vm.savedEvidence.result)}
        compose.onNodeWithTag("saved-evidence-inspect").performScrollTo().performClick()
        compose.waitUntil(10000){vm.savedEvidence.result?.presence==SavedPitchEvidence.Presence.BELOW_THRESHOLD}
        assertNull(vm.savedEvidence.result!!.acceptedHz)
        assertTrue(vm.savedEvidence.result!!.families.isEmpty())
        assertArrayEquals(original,store.file(session.id).readBytes())
        assertEquals(index,app.getSharedPreferences("local_data",0).getString("recordings",null))
        assertEquals(70.0,store.list().single().timeline.single().hz!!,0.0)
        compose.runOnIdle{vm.inspectSavedEvidence(session,500);vm.onDestinationChanged()}
        compose.runOnIdle{assertNull(vm.savedEvidence.result);assertFalse(vm.savedEvidence.running)}
    }

    private class EvidenceApplication(private val base:Context):Application(){
        private val id=UUID.randomUUID().toString()
        private val names=mutableSetOf<String>()
        private val root=File(base.cacheDir,"saved-evidence-$id").apply{mkdirs()}
        init{attachBaseContext(base)}
        override fun getFilesDir()=root
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences {
            val scoped="saved-evidence-$id-$name";names+=scoped
            return base.getSharedPreferences(scoped,mode)
        }
        fun clean(){names.forEach{base.getSharedPreferences(it,0).edit().clear().commit()};root.deleteRecursively()}
    }
}
