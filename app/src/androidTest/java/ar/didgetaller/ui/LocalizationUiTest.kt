package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import org.junit.*
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Locale
import java.util.UUID

/** Android configuration contexts and real Compose output, with test-owned files/preferences. */
class LocalizationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val originalLocale = Locale.getDefault()
    private val owner = ViewModelStore()
    private lateinit var app: LocalizedApplication

    @Before fun emulatorOnly() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
    }

    @After fun cleanOwnedFixtures() {
        compose.runOnUiThread { owner.clear() }
        if (::app.isInitialized) app.clean()
        Locale.setDefault(originalLocale)
    }

    private fun show(language: String) {
        Locale.setDefault(Locale.forLanguageTag(language))
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(target.resources.configuration).apply { setLocales(LocaleList(Locale.forLanguageTag(language))) }
        val localized = target.createConfigurationContext(config)
        app = LocalizedApplication(localized)
        lateinit var vm: TallerViewModel
        compose.runOnUiThread { vm = TallerViewModel(app); owner.put("localization", vm) }
        val context = object : ContextWrapper(compose.activity) {
            override fun getResources(): Resources = localized.resources
        }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config) {
                TallerApp(vm, {}, {}, { _, _, _ -> })
            }
        }
    }

    @Test fun englishMeasurementAndLibrary() {
        show("en")
        Assert.assertEquals("C2", app.noteLabel(ar.didgetaller.core.Music.frequency(36)))
        Assert.assertEquals("C", app.noteBase(ar.didgetaller.core.Music.frequency(36)))
        Assert.assertEquals("73.4 Hz", hzLabel(73.4))
        Assert.assertEquals("18.5", axisLabel(18.5, 5.0))
        compose.onNodeWithText("Start measurement").assertIsDisplayed()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("No sessions yet").assertIsDisplayed()
    }

    @Test fun spanishMeasurementAndLibrary() {
        show("es-AR")
        Assert.assertEquals("Do · C2", app.noteLabel(ar.didgetaller.core.Music.frequency(36)))
        Assert.assertEquals("Do", app.noteBase(ar.didgetaller.core.Music.frequency(36)))
        Assert.assertEquals("73,4 Hz", hzLabel(73.4))
        Assert.assertEquals("18,5", axisLabel(18.5, 5.0))
        compose.onNodeWithText("Iniciar medición").assertIsDisplayed()
        compose.onNodeWithText("Biblioteca").performClick()
        compose.onNodeWithText("Todavía no hay sesiones").assertIsDisplayed()
    }

    @Test fun unsupportedLanguageFallsBackToEnglish() {
        show("de-DE")
        compose.onNodeWithText("Start measurement").assertIsDisplayed()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("No sessions yet").assertIsDisplayed()
    }

    private class LocalizedApplication(context: Context) : Application() {
        private val token = "localization-${UUID.randomUUID()}"
        private val root = File(context.cacheDir, token).apply { check(mkdirs()) }
        init { attachBaseContext(context) }
        override fun getFilesDir(): File = root
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = baseContext.getSharedPreferences("$token-$name", mode)
        fun clean() {
            for (name in listOf("local_data", "session_tools")) baseContext.deleteSharedPreferences("$token-$name")
            root.deleteRecursively()
        }
    }
}
