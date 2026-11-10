package ar.didgetaller.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.audio.PracticeMetronome
import ar.didgetaller.core.MetronomeMode
import ar.didgetaller.core.MetronomeSettings
import ar.didgetaller.core.MetronomeSubdivision
import ar.didgetaller.data.MetronomePreferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** Clean-emulator tests. Unique preferences/files never touch historical sessions or microphone data. */
class MetronomeUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: IsolatedApp
    private lateinit var metronome: PracticeMetronome
    private val stores = mutableListOf<ViewModelStore>()
    @Before fun setup() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish"))
        app = IsolatedApp(InstrumentationRegistry.getInstrumentation().targetContext)
        compose.runOnUiThread { metronome = PracticeMetronome(app) }
    }
    @After fun cleanup() {
        compose.runOnUiThread { stores.forEach { it.clear() } }
        if (::metronome.isInitialized) compose.runOnUiThread { metronome.close() }
        if (::app.isInitialized) app.clean()
    }
    @Test fun transportSettingsAndVisualCaptureInterlock() {
        compose.setContent { MaterialTheme { Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState())) { MetronomePanel(metronome) } } }
        compose.onNodeWithTag("metronome-start").performClick()
        compose.waitUntil { metronome.state.pulse != null }
        compose.onNodeWithTag("metronome-pause").performClick()
        compose.waitUntil { metronome.state.mode == MetronomeMode.PAUSED }
        compose.onNodeWithTag("metronome-expand").performClick()
        compose.onNodeWithTag("metronome-bpm").performScrollTo().performTextReplacement("137")
        compose.onNodeWithTag("metronome-meter-3").performScrollTo().performClick()
        compose.onNodeWithTag("metronome-subdivision-TRIPLET").performScrollTo().performClick()
        compose.onNodeWithTag("metronome-sound").performScrollTo().performClick()
        compose.waitUntil { metronome.state.settings.audible }
        compose.runOnUiThread { metronome.captureActive(true) }
        compose.onNodeWithTag("metronome-sound").assertIsNotEnabled()
        compose.onNodeWithTag("metronome-interlock").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("metronome-start").performScrollTo()
        compose.onNodeWithTag("metronome-start").performClick()
        compose.waitUntil { metronome.state.mode == MetronomeMode.RUNNING }
        compose.onNodeWithTag("metronome-stop").performScrollTo().performClick()
        compose.waitUntil { metronome.state.mode == MetronomeMode.STOPPED }
        assertNull(metronome.state.pulse)
        compose.onNodeWithTag("metronome-bpm").assertIsNotFocused()
        compose.waitUntil(5000) {
            var hidden = false
            compose.runOnUiThread {
                hidden = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) != true
            }
            hidden
        }
        val settings = MetronomePreferences(app).load()
        assertEquals(137, settings.bpm); assertEquals(3, settings.beatsPerBar)
        assertEquals(MetronomeSubdivision.TRIPLET, settings.subdivision)
        compose.onNodeWithTag("metronome-expand").performScrollTo()
        screenshot()
    }
    @Test fun preferencesSurviveNewOwnerAndInvalidValuesAreBounded() {
        val preferences = MetronomePreferences(app)
        val expected = MetronomeSettings(240, 2, MetronomeSubdivision.SIXTEENTH, false, true, 0.23f)
        preferences.save(expected)
        compose.runOnUiThread { metronome.close(); metronome = PracticeMetronome(app) }
        assertEquals(expected, metronome.state.settings)
        assertEquals(MetronomeMode.STOPPED, metronome.state.mode)
        app.getSharedPreferences("practice_metronome", 0).edit().putInt("bpm", 900).putInt("beats", 0)
            .putString("subdivision", "unknown").putFloat("volume", Float.NaN).commit()
        val bounded = preferences.load()
        assertEquals(240, bounded.bpm); assertEquals(2, bounded.beatsPerBar)
        assertEquals(0.5f, bounded.volume); assertEquals(MetronomeSubdivision.QUARTER, bounded.subdivision)
        app.getSharedPreferences("practice_metronome", 0).edit().putString("bpm", "invalid type").commit()
        assertEquals(MetronomeSettings(), preferences.load())
    }
    @Test fun viewModelNavigationBackgroundAndClearOwnTheTransport() {
        val owners = ViewModelStore().also { stores += it }
        lateinit var vm: TallerViewModel
        compose.runOnUiThread {
            vm = TallerViewModel(app); owners.put("metronome", vm)
            vm.metronome.start(); vm.onDestinationChanged(measurement = false)
        }
        compose.waitUntil { vm.metronome.state.mode == MetronomeMode.PAUSED }
        compose.runOnUiThread { vm.metronome.start(); vm.onAppStopped() }
        compose.waitUntil { vm.metronome.state.mode == MetronomeMode.PAUSED }
        compose.runOnUiThread { vm.metronome.start(); owners.clear() }
        compose.runOnUiThread { vm.metronome.requestSound(true); vm.metronome.start() }
        assertNotEquals(MetronomeMode.RUNNING, vm.metronome.state.mode)
    }
    @Test fun realAudioOutputAndMicrophoneAdmissionAreMutuallyExclusive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.RECORD_AUDIO)
        val owners = ViewModelStore().also { stores += it }
        lateinit var vm: TallerViewModel
        compose.runOnUiThread { vm = TallerViewModel(app); owners.put("mic-interlock", vm) }
        compose.runOnUiThread { vm.metronome.requestSound(true); vm.metronome.start() }
        compose.waitUntil(5000) { vm.metronome.state.pulse?.beat == 2 }
        assertFalse(vm.metronome.state.audioFailed)
        compose.runOnUiThread { vm.microphoneStart() }
        compose.waitUntil { vm.metronome.state.captureActive }
        assertTrue(vm.listening)
        compose.runOnUiThread { vm.metronome.requestSound(true) }
        assertTrue(vm.metronome.state.captureActive)
        compose.runOnUiThread { vm.microphoneStop(); owners.clear() }
    }
    private fun screenshot() {
        compose.waitForIdle()
        val label = InstrumentationRegistry.getArguments().getString("evidenceLabel") ?: "default"
        val file = File(compose.activity.getExternalFilesDir(null), "metronome-evidence/$label.png")
        file.parentFile!!.mkdirs()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { image ->
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
        }
    }
    private class IsolatedApp(context: Context) : Application() {
        private val token = "metronome-test-${UUID.randomUUID()}"
        private val root = File(context.cacheDir, token).apply { check(mkdirs()) }
        init { attachBaseContext(context) }
        override fun getFilesDir() = root
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = baseContext.getSharedPreferences("$token-$name", mode)
        fun clean() {
            listOf("practice_metronome", "local_data", "session_tools").forEach { baseContext.deleteSharedPreferences("$token-$it") }
            root.deleteRecursively()
        }
    }
}
