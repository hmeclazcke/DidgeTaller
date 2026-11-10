package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.WavCodec
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Locale
import java.util.UUID

class LibraryRecoveryUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: IsolatedApp
    private val owner = ViewModelStore()
    private val wav = WavCodec.encode(ShortArray(8000) { 123 }, 8000, 8000)

    @Before fun isolate() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        app = IsolatedApp(InstrumentationRegistry.getInstrumentation().targetContext)
    }
    @After fun cleanup() {
        compose.runOnUiThread { owner.clear() }
        if (::app.isInitialized) app.clean()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(100, 5000)
        check(automation.rootInActiveWindow?.packageName?.toString() == compose.activity.packageName) {
            "A system window obscures the application; visual evidence is invalid"
        }
        val label = InstrumentationRegistry.getArguments().getString("libraryEvidenceLabel", "default")!!
        require(label in setOf("default", "en-platform150", "es-platform150"))
        val directory = File(compose.activity.getExternalFilesDir(null), "rebuild-library-evidence/$label").apply { mkdirs() }
        val bitmap = checkNotNull(automation.takeScreenshot())
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun libraryRecoveryRequiresConfirmationAndPreservesOriginalIndexAndWav() {
        InstrumentationRegistry.getArguments().getString("libraryExpectedLanguage")?.let { expected ->
            assertEquals(expected, compose.activity.resources.configuration.locales[0].language)
            assertEquals(expected, app.resources.configuration.locales[0].language)
            assertEquals(1.5f, compose.activity.resources.configuration.fontScale, .01f)
        }
        val store = RecordingStore(app)
        val saved = store.save(wav)
        val prefs = app.getSharedPreferences("local_data", 0)
        val damaged = "[{\"synthetic damaged metadata\":"
        check(prefs.edit().putString("recordings", damaged).commit())
        lateinit var vm: TallerViewModel
        compose.runOnUiThread { vm = TallerViewModel(app); owner.put("library", vm) }
        compose.setContent { TallerApp(vm, {}, {}, { _, _, _ -> }, rememberNavController()) }
        compose.onNodeWithContentDescription(uiText(R.string.ui_library)).performClick()
        compose.onNodeWithTag("recover-session-index").performScrollTo().assertIsDisplayed()
        assertTrue(vm.sessionIndexDamaged)
        assertEquals(damaged, prefs.getString("recordings", null))
        capture("damaged-library")
        compose.onNodeWithTag("recover-session-index").performClick()
        compose.onNodeWithText(uiText(R.string.ui_cancel)).performClick()
        assertEquals(damaged, prefs.getString("recordings", null))
        compose.onNodeWithTag("recover-session-index").performClick()
        compose.onNodeWithTag("confirm-index-recovery").assertIsDisplayed()
        capture("recovery-confirmation")
        compose.onNodeWithTag("confirm-index-recovery").performClick()
        compose.onNodeWithTag("library-index-warning").assertDoesNotExist()
        assertFalse(vm.sessionIndexDamaged)
        assertEquals(listOf(saved.id), vm.sessions.map { it.id })
        assertTrue(prefs.all.any { (key, value) -> key.startsWith("recordings.quarantine.") && value == damaged })
        assertArrayEquals(wav, store.file(saved.id).readBytes())
        capture("restored-library")
    }

    @Test fun libraryTransitionsPublishListsAndCountersTogether() {
        val store = RecordingStore(app)
        val saved = store.save(wav)
        lateinit var vm: TallerViewModel
        compose.runOnUiThread { vm = TallerViewModel(app); owner.put("library", vm) }
        compose.runOnUiThread {
            vm.deleteSession(saved)
            assertTrue(vm.sessions.isEmpty())
            assertEquals(listOf(saved.id), vm.trashedSessions.map { it.id })
            assertEquals(1, vm.trashCount)
            vm.undoDelete()
            assertEquals(listOf(saved.id), vm.sessions.map { it.id })
            assertTrue(vm.trashedSessions.isEmpty())
            assertEquals(0, vm.trashCount)
            vm.renameSession(saved, "Synthetic renamed session")
            assertEquals("Synthetic renamed session", vm.sessions.single().name)
            vm.saveDesign("Synthetic design")
            assertEquals(1, vm.designs.size)
            vm.deleteDesign(vm.designs.single())
            assertTrue(vm.designs.isEmpty())
            assertEquals(1, vm.trashedDesigns.size)
            assertEquals(1, vm.trashCount)
            vm.undoDelete()
            assertEquals(1, vm.designs.size)
            assertTrue(vm.trashedDesigns.isEmpty())
            assertEquals(0, vm.trashCount)
        }
        assertArrayEquals(wav, store.file(saved.id).readBytes())
    }

    @Test fun damagedMetadataCommandsReportFailureWithoutCrashingOrOverwritingEvidence() {
        val store = RecordingStore(app)
        val saved = store.save(wav)
        val tools = app.getSharedPreferences("session_tools", 0)
        val local = app.getSharedPreferences("local_data", 0)
        val damaged = "{\"damaged synthetic metadata\":"
        check(tools.edit().putString("extras", damaged).commit())
        check(local.edit().putString("designs", damaged).commit())
        lateinit var vm: TallerViewModel
        compose.runOnUiThread { vm = TallerViewModel(app); owner.put("library", vm) }
        compose.runOnUiThread {
            vm.addMarker(saved, 0, "manual")
            assertEquals(app.getString(R.string.library_metadata_write_failed), vm.message)
            vm.removeMarker(saved, "missing")
            vm.associate(saved, "synthetic-design")
            vm.saveDesign("Synthetic new design")
            assertEquals(app.getString(R.string.library_metadata_write_failed), vm.message)
            assertEquals(listOf(saved.id), vm.sessions.map { it.id })
            assertTrue(vm.designs.isEmpty())
            assertEquals(0, vm.trashCount)
        }
        assertEquals(damaged, tools.getString("extras", null))
        assertEquals(damaged, local.getString("designs", null))
        assertArrayEquals(wav, store.file(saved.id).readBytes())
    }

    @Test fun recoveryNoticeAndDialogUseTheCallerLocale() {
        var language by mutableStateOf("en")
        var recovered = false
        compose.setContent {
            val base = LocalContext.current
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(language)) }
            val localized = base.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                MaterialTheme {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
                        LibraryRecoveryNotice { recovered = true }
                    }
                }
            }
        }
        for (code in listOf("en", "es")) {
            compose.runOnIdle { language = code }
            val configuration = Configuration(compose.activity.resources.configuration).apply { setLocale(Locale.forLanguageTag(code)) }
            val resources = compose.activity.createConfigurationContext(configuration).resources
            compose.onNodeWithTag("recover-session-index").assertIsDisplayed().performClick()
            compose.onNodeWithTag("confirm-index-recovery").assertIsDisplayed().assertHasClickAction()
                .assertTextContains(resources.getString(R.string.library_index_recover))
            compose.onNodeWithText(resources.getString(R.string.library_index_recovery_confirmation)).assertIsDisplayed()
            compose.onNodeWithText(resources.getString(R.string.ui_cancel)).assertIsDisplayed()
            capture("$code-caller-locale-dialog")
            compose.onNodeWithTag("confirm-index-recovery").performClick()
            assertTrue(recovered)
            recovered = false
            capture("$code-caller-locale-warning")
        }
    }

    private class IsolatedApp(context: Context) : Application() {
        private val token = "library-recovery-${UUID.randomUUID()}"
        private val root = File(context.cacheDir, token).apply { check(mkdirs()) }
        init { attachBaseContext(context) }
        override fun getFilesDir() = root
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("$token-$name", mode)
        fun clean() {
            listOf("local_data", "session_tools").forEach { baseContext.deleteSharedPreferences("$token-$it") }
            root.deleteRecursively()
        }
    }
}
