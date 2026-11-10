package ar.didgetaller.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
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
    private var captureTransportFailure = false
    @Before fun setup() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish"))
        app = IsolatedApp(InstrumentationRegistry.getInstrumentation().targetContext)
        compose.runOnUiThread { metronome = PracticeMetronome(app) }
    }
    @After fun cleanup() {
        if (captureTransportFailure) screenshot("-failure")
        compose.runOnUiThread { stores.forEach { it.clear() } }
        if (::metronome.isInitialized) compose.runOnUiThread { metronome.close() }
        if (::app.isInitialized) app.clean()
    }
    @Test fun transportSettingsAndVisualCaptureInterlock() {
        captureTransportFailure = true
        val owners = ViewModelStore().also { stores += it }
        lateinit var vm: TallerViewModel
        compose.runOnUiThread {
            metronome.close(); vm = TallerViewModel(app); owners.put("screen", vm); metronome = vm.metronome
        }
        compose.setContent { TallerApp(vm, {}, {}, { _, _, _ -> }) }
        compose.onNodeWithTag("metronome-screen").assertDoesNotExist()
        val label = compose.activity.getString(ar.didgetaller.R.string.metronome_tab)
        compose.onNode(hasContentDescription(label) and hasAnyAncestor(hasTestTag("main-navigation"))).performClick()
        compose.onNodeWithTag("metronome-screen").assertIsDisplayed()
        val configuration = compose.activity.resources.configuration
        val narrow = configuration.screenWidthDp < 340 * minOf(configuration.fontScale, 1.2f)
        val labels = listOf(ar.didgetaller.R.string.ui_measure,
            if (narrow) ar.didgetaller.R.string.metronome_tab_short else ar.didgetaller.R.string.metronome_tab,
            if (narrow) ar.didgetaller.R.string.ui_lab_short else ar.didgetaller.R.string.ui_laboratory,
            if (narrow) ar.didgetaller.R.string.ui_library_short else ar.didgetaller.R.string.ui_library)
        val labelBounds = labels.map { resource ->
            val node = compose.onNode(hasText(compose.activity.getString(resource)) and hasAnyAncestor(hasTestTag("main-navigation")), useUnmergedTree = true)
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("Navigation label height must fit", layout.didOverflowHeight)
            assertFalse("Navigation labels must not be ellipsized", layout.isLineEllipsized(0))
            assertTrue("Navigation glyphs must fit (integer-pixel rounding)", layout.getLineRight(0) <= layout.size.width + 1f)
            node.fetchSemanticsNode().boundsInRoot
        }
        assertTrue("Four labels must not overlap", labelBounds.zipWithNext().all { (a, b) -> a.right <= b.left })
        compose.onNodeWithTag("metronome-transport").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("metronome-sound").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("metronome-bpm").assertDoesNotExist()
        val initialTempo = metronome.state.settings.bpm
        compose.onNodeWithTag("metronome-faster").performScrollTo().performClick()
        compose.waitUntil { metronome.state.settings.bpm == initialTempo + 1 }
        compose.onNodeWithTag("metronome-slower").performScrollTo().performClick()
        compose.waitUntil { metronome.state.settings.bpm == initialTempo }
        compose.onNodeWithTag("metronome-transport").performScrollTo().performClick()
        compose.waitUntil { metronome.state.pulse != null }
        compose.onNodeWithTag("metronome-beats").assertExists()
        compose.onNodeWithTag("metronome-pause").performClick()
        compose.waitUntil { metronome.state.mode == MetronomeMode.PAUSED }
        compose.onNodeWithTag("metronome-edit").performScrollTo().performClick()
        compose.onNodeWithTag("metronome-bpm").performTextReplacement("0")
        compose.onNodeWithTag("metronome-apply").assertIsNotEnabled()
        compose.onNodeWithTag("metronome-bpm").performTextReplacement("137")
        compose.onNodeWithTag("metronome-apply").performClick()
        compose.onNodeWithTag("metronome-bpm").assertDoesNotExist()
        for (meter in listOf(2, 4, 3)) {
            compose.onNodeWithTag("metronome-meter-$meter").performScrollTo().performClick()
            compose.waitUntil { metronome.state.settings.beatsPerBar == meter }
            compose.onNodeWithTag("metronome-beats").onChildren().assertCountEquals(meter)
        }
        compose.onNodeWithTag("metronome-expand").performScrollTo().performClick()
        compose.onNodeWithTag("metronome-subdivision-TRIPLET").performScrollTo().performClick()
        compose.onNodeWithTag("metronome-sound").performScrollTo().performClick()
        compose.waitUntil { metronome.state.settings.audible }
        compose.runOnUiThread { metronome.captureActive(true) }
        compose.onNodeWithTag("metronome-sound").assertIsNotEnabled()
        compose.onNodeWithTag("metronome-notice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("metronome-transport").performScrollTo().performClick()
        compose.waitUntil { metronome.state.mode == MetronomeMode.RUNNING }
        compose.onNodeWithTag("metronome-transport").performScrollTo().performClick()
        compose.waitUntil { metronome.state.mode == MetronomeMode.STOPPED }
        assertNull(metronome.state.pulse)
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
        compose.runOnUiThread { metronome.captureActive(false); metronome.requestSound(false) }
        compose.onNodeWithTag("metronome-expand").performScrollTo().performClick()
        compose.onNodeWithTag("metronome-edit").performScrollTo()
        compose.onNodeWithTag("metronome-transport").performScrollTo().assertIsDisplayed()
        screenshot("-transport")
        compose.onNodeWithTag("metronome-edit").performScrollTo()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag("metronome-tempo-value", useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val tempo = layouts.single()
        assertFalse("Tempo must not be ellipsized", tempo.isLineEllipsized(0))
        assertFalse("Tempo height must fit", tempo.didOverflowHeight)
        assertTrue("All tempo digits must fit", tempo.getLineRight(0) <= tempo.size.width + 1f)
        screenshot()
        captureTransportFailure = false
    }
    @Test fun fourSectionsAndBackPreserveNavigationAndPauseMetronome() {
        val owners = ViewModelStore().also { stores += it }
        lateinit var vm: TallerViewModel
        lateinit var nav: androidx.navigation.NavHostController
        compose.runOnUiThread { vm = TallerViewModel(app); owners.put("navigation", vm) }
        compose.setContent { nav = androidx.navigation.compose.rememberNavController(); TallerApp(vm, {}, {}, { _, _, _ -> }, nav) }
        fun section(resource: Int) = compose.onNode(hasContentDescription(compose.activity.getString(resource)) and hasAnyAncestor(hasTestTag("main-navigation")))
        section(ar.didgetaller.R.string.metronome_tab).performClick()
        compose.onNodeWithTag("metronome-transport").performScrollTo().performClick()
        compose.waitUntil { vm.metronome.state.mode == MetronomeMode.RUNNING }
        section(ar.didgetaller.R.string.ui_laboratory).performClick()
        compose.waitUntil { nav.currentDestination?.route == TallerRoutes.Lab }
        compose.waitUntil { vm.metronome.state.mode == MetronomeMode.PAUSED }
        section(ar.didgetaller.R.string.ui_library).performClick()
        compose.waitUntil { nav.currentDestination?.route == TallerRoutes.Library }
        section(ar.didgetaller.R.string.ui_measure).performClick()
        compose.onNodeWithTag("measure-screen").assertExists()
        compose.onNodeWithTag("metronome-screen").assertDoesNotExist()
        section(ar.didgetaller.R.string.metronome_tab).performClick()
        compose.waitUntil { compose.activity.hasWindowFocus() }
        androidx.test.espresso.Espresso.pressBackUnconditionally()
        compose.waitUntil { nav.currentDestination?.route == TallerRoutes.Measure }
        assertEquals(MetronomeMode.PAUSED, vm.metronome.state.mode)
    }
    @Test fun mediaMuteIsReportedAndNativeTrackConsumesBothBuffers() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val audio = compose.activity.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val previous = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val click = ar.didgetaller.audio.AndroidMetronomeClick()
        automation.adoptShellPermissionIdentity(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        try {
            audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
            compose.runOnUiThread { metronome.refreshAudioEnvironment() }
            assertTrue(metronome.systemMuted)
            audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) / 2, 0)
            compose.runOnUiThread { metronome.refreshAudioEnvironment() }
            assertFalse(metronome.systemMuted)
            assertTrue(click.play(false, .5f))
            compose.waitUntil(5000) { click.renderedFrames() >= 576 }
            assertTrue(click.play(true, .5f))
            compose.waitUntil(5000) { click.renderedFrames() >= 1152 }
            android.util.Log.i("MetronomeEvidence", "Consumed PCM frames=${click.renderedFrames()}, output device types=${click.routedDeviceTypes()}")
            click.silence()
            assertEquals(0, click.renderedFrames())
        } finally {
            click.close()
            audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, previous, 0)
            automation.dropShellPermissionIdentity()
        }
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
    private fun screenshot(suffix: String = "") {
        compose.waitForIdle()
        val label = InstrumentationRegistry.getArguments().getString("evidenceLabel") ?: "default"
        val file = File(compose.activity.getExternalFilesDir(null), "metronome-evidence/$label$suffix.png")
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
