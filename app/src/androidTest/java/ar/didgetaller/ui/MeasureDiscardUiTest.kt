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
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.R
import ar.didgetaller.SessionPhase
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.WavCodec
import ar.didgetaller.data.RecordingStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** Emulator-only recording workflow with a private synthetic fixture. */
class MeasureDiscardUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val owners = ViewModelStore()
    private lateinit var app: IsolatedApp
    private lateinit var vm: TallerViewModel
    private lateinit var store: RecordingStore
    private lateinit var existing: RecordingStore.Session
    private lateinit var original: ByteArray

    @Before fun setup() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.RECORD_AUDIO)
        app = IsolatedApp(instrumentation.targetContext)
        store = RecordingStore(app)
        original = WavCodec.encode(ShortArray(44100) { 1234 }, 44100, 44100)
        existing = store.save(original, createdAt = 123L)
        compose.runOnUiThread { vm = TallerViewModel(app); owners.put("measure", vm) }
        compose.setContent { TallerApp(vm, { vm.startSession() }, {}, { _, _, _ -> }) }
    }

    @After fun cleanup() {
        if (::vm.isInitialized) {
            compose.runOnUiThread { if (vm.sessionPhase == SessionPhase.CAPTURING) vm.discardSession() }
            compose.waitUntil(15000) { vm.sessionPhase == SessionPhase.IDLE }
            compose.runOnUiThread { owners.clear() }
        }
        if (::app.isInitialized) app.clean()
    }

    private fun start() {
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).performClick()
        compose.waitUntil(10000) { vm.sessionPhase == SessionPhase.CAPTURING }
    }

    private fun confirmDiscard() {
        compose.onNodeWithTag("discard-recording").assertHasClickAction().performClick()
        compose.onNodeWithText(uiText(R.string.recording_discard_title)).assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.recording_discard_message)).assertIsDisplayed()
        compose.onNodeWithTag("confirm-discard").assertHasClickAction().performClick()
        compose.waitUntil(15000) { vm.sessionPhase == SessionPhase.IDLE }
    }

    private fun capture(name: String) {
        val label = InstrumentationRegistry.getArguments().getString("evidenceLabel") ?: "measure"
        val file = File(compose.activity.getExternalFilesDir(null), "measure-evidence/$label-$name.png")
        file.parentFile!!.mkdirs()
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun continueDiscardAndSavePreserveExistingAudio() {
        val index = app.getSharedPreferences("local_data", 0).getString("recordings", null)
        start()
        val started = vm.sessionStartedAtMs
        val partial = File(app.filesDir, "recordings").listFiles()!!.single { it.extension == "part" }
        compose.waitUntil(10000) { partial.length() > 44 }
        val before = partial.length()
        compose.onNodeWithTag("discard-recording").performClick()
        compose.onNodeWithText(uiText(R.string.recording_discard_title)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(uiText(R.string.recording_discard_title)).assertDoesNotExist()
        assertEquals(SessionPhase.CAPTURING, vm.sessionPhase)
        compose.onNodeWithTag("discard-recording").performClick()
        compose.onNodeWithTag("continue-recording").performClick()
        compose.waitUntil(10000) { partial.length() > before }
        assertEquals(started, vm.sessionStartedAtMs)
        assertEquals(SessionPhase.CAPTURING, vm.sessionPhase)
        compose.onNodeWithText(uiText(R.string.ui_wide_view)).performClick()
        compose.waitUntil(5000) { vm.fullScreen }
        compose.onNodeWithTag("measure-wide-screen").assertIsDisplayed()
        compose.waitUntil(10000) { compose.activity.window.decorView.hasWindowFocus() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5000) { !vm.fullScreen }
        assertEquals(started, vm.sessionStartedAtMs)
        assertEquals(SessionPhase.CAPTURING, vm.sessionPhase)
        val resources=compose.activity.resources
        val mode=resources.getIdentifier("config_navBarInteractionMode","integer","android")
        if(mode!=0 && resources.getInteger(mode)==2) {
            println("MEASURE_GESTURAL_BACK_EXECUTED")
            compose.onNodeWithText(uiText(R.string.ui_wide_view)).performClick()
            compose.waitUntil(5000) { vm.fullScreen }
            compose.onNodeWithTag("measure-wide-screen").assertIsDisplayed()
            compose.waitForIdle()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            compose.waitUntil(10000) { compose.activity.window.decorView.hasWindowFocus() }
            android.os.SystemClock.sleep(350)
            val metrics=resources.displayMetrics
            val fd=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "input swipe 1 ${metrics.heightPixels/2} ${metrics.widthPixels/3} ${metrics.heightPixels/2} 350")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
            compose.waitUntil(5000) { !vm.fullScreen }
            assertEquals(started,vm.sessionStartedAtMs)
            assertEquals(SessionPhase.CAPTURING,vm.sessionPhase)
        } else println("MEASURE_GESTURAL_BACK_UNAVAILABLE")
        confirmDiscard()
        assertFalse(partial.exists())
        assertEquals(index, app.getSharedPreferences("local_data", 0).getString("recordings", null))
        assertArrayEquals(original, store.file(existing.id).readBytes())
        assertEquals(listOf(existing.id), store.list().map { it.id })
        compose.onNodeWithText(uiText(R.string.ui_library), useUnmergedTree = true).performClick()
        compose.onNodeWithTag("session-${existing.id}").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(existing.id), vm.sessions.map { it.id }) }
        compose.onNodeWithText(uiText(R.string.ui_measure), useUnmergedTree = true).performClick()
        start()
        confirmDiscard() // Immediate discard, including a possible header-only WAV.
        assertEquals(listOf(existing.id), store.list().map { it.id })
        start()
        compose.waitUntil(10000) { File(app.filesDir, "recordings").listFiles()!!.any { it.extension == "part" && it.length() > 44 } }
        compose.runOnUiThread { vm.finishSession(); vm.finishSession(); vm.discardSession() }
        compose.waitUntil(15000) { vm.sessionPhase == SessionPhase.IDLE && store.list().size == 2 }
        assertEquals(2, store.list().size)
        assertArrayEquals(original, store.file(existing.id).readBytes())
        assertTrue(File(app.filesDir, "recordings").listFiles()!!.none { it.extension in listOf("part", "discard") })
    }

    @Test fun activeLayoutAndConfirmationAreAccessible() {
        capture("idle")
        start()
        compose.onNodeWithTag("measurement-frequency").assertIsDisplayed()
        compose.onNodeWithTag("measurement-note-name").assertIsDisplayed()
        compose.onNodeWithText(uiText(R.string.recording_waiting_stable)).assertIsDisplayed()
        compose.onNodeWithTag("acoustic-signal-status").assertIsDisplayed()
        compose.onNodeWithTag("recording-primary").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("discard-recording").assertIsDisplayed().assertHasClickAction()
        val reading = compose.onNodeWithTag("acoustic-signal-status").fetchSemanticsNode().boundsInRoot
        val primary = compose.onNodeWithTag("recording-primary").fetchSemanticsNode().boundsInRoot
        val discard = compose.onNodeWithTag("discard-recording").fetchSemanticsNode().boundsInRoot
        val navigation = compose.onNodeWithText(uiText(R.string.ui_measure),useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        assertTrue(reading.bottom <= primary.top)
        assertTrue(primary.height >= 48 * density && discard.height >= 48 * density)
        assertTrue(primary.right < discard.left)
        assertTrue(primary.bottom <= navigation.top)
        capture("pending")
        compose.onNodeWithTag("discard-recording").performClick()
        compose.onNodeWithText(uiText(R.string.recording_discard_title)).assertIsDisplayed()
        compose.onNodeWithTag("continue-recording").assertHasClickAction()
        compose.onNodeWithTag("confirm-discard").assertHasClickAction()
        capture("confirm")
        compose.onNodeWithTag("confirm-discard").performClick()
        compose.waitUntil(15000) { vm.sessionPhase == SessionPhase.IDLE }
    }

    private class IsolatedApp(context: Context) : Application() {
        private val token = "measure-discard-${UUID.randomUUID()}"
        private val root = File(context.cacheDir, token).apply { check(mkdirs()) }
        init { attachBaseContext(context) }
        override fun getFilesDir(): File = root
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("$token-$name", mode)
        fun clean() {
            for (name in listOf("local_data", "session_tools")) baseContext.deleteSharedPreferences("$token-$name")
            root.deleteRecursively()
        }
    }
}
