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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.TallerViewModel
import ar.didgetaller.R
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.WavCodec
import ar.didgetaller.data.RecordingStore
import ar.didgetaller.data.SessionToolsStore
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
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
    private lateinit var viewModel: TallerViewModel
    private lateinit var changeLanguage: (String) -> Unit

    @Before fun emulatorOnly() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
    }

    @After fun cleanOwnedFixtures() {
        compose.runOnUiThread { owner.clear() }
        if (::app.isInitialized) app.clean()
        Locale.setDefault(originalLocale)
    }

    private fun show(language: String, widthDp: Int? = null, fontScale: Float = 1f, prepare: (Context) -> Unit = {}) {
        Locale.setDefault(Locale.forLanguageTag(language))
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(target.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(language)))
            this.fontScale = fontScale
        }
        val localized = target.createConfigurationContext(config)
        app = LocalizedApplication(localized)
        prepare(app)
        lateinit var vm: TallerViewModel
        compose.runOnUiThread { vm = TallerViewModel(app); owner.put("localization", vm) }
        viewModel = vm
        val configuration = mutableStateOf(config)
        changeLanguage = { tag ->
            Locale.setDefault(Locale.forLanguageTag(tag))
            configuration.value = Configuration(config).apply { setLocales(LocaleList(Locale.forLanguageTag(tag))) }
        }
        compose.setContent {
            val active = configuration.value
            val context = remember(active) {
                val resources = target.createConfigurationContext(active).resources
                object : ContextWrapper(compose.activity) {
                    override fun getResources(): Resources = resources
                }
            }
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides active,
                LocalDensity provides Density(target.resources.displayMetrics.density, fontScale)) {
                Box(if (widthDp == null) Modifier else Modifier.width(widthDp.dp).fillMaxHeight()) {
                    TallerApp(vm, {}, {}, { _, _, _ -> })
                }
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
        Assert.assertEquals("Do♯ · C#2", app.noteLabel(ar.didgetaller.core.Music.frequency(37)))
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

    @Test fun largeEnglishNavigationFitsAtDoubleFontScale() {
        assertLargeFontNavigation("en", listOf("Measure", "Laboratory", "Library"))
    }

    @Test fun largeSpanishNavigationFitsAtDoubleFontScale() {
        assertLargeFontNavigation("es-AR", listOf("Medir", "Laboratorio", "Biblioteca"))
    }

    @Test fun compactEnglishNavigationKeepsEveryLabelAtLargeFontScale() {
        assertCompactNavigation("en", listOf("Measure", "Laboratory", "Library"))
    }

    @Test fun compactSpanishNavigationKeepsEveryLabelAtLargeFontScale() {
        assertCompactNavigation("es-AR", listOf("Medir", "Laboratorio", "Biblioteca"))
    }

    private fun assertCompactNavigation(language:String, labels:List<String>) {
        show(language, widthDp = 343, fontScale = 1.5f)
        labels.forEach { label ->
            val results = mutableListOf<TextLayoutResult>()
            val nodes=compose.onAllNodesWithText(label, useUnmergedTree = true)
            nodes[nodes.fetchSemanticsNodes().lastIndex]
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertCompleteTextFits(results.single())
        }
    }

    @Test fun spanishDroneTerminologyMatchesTheComponentLayer() {
        show("es-AR")
        Assert.assertTrue(app.getString(R.string.ui_drone, "Do2", "66 Hz").startsWith("Dron:"))
        Assert.assertTrue(app.getString(R.string.swipe_caution).contains("del dron"))
        Assert.assertFalse(app.getString(R.string.saved_evidence_help).contains("drone"))
    }

    private fun assertLargeFontNavigation(language: String, labels: List<String>) {
        show(language, fontScale = 2f)
        compose.onNodeWithText("DIDGE TALLER").assertDoesNotExist()
        val bounds = labels.map { compose.onAllNodesWithText(it, useUnmergedTree = true).fetchSemanticsNodes().last().boundsInRoot }
        Assert.assertTrue("Navigation labels must not overlap at 2x font scale", bounds.zipWithNext().all { (a, b) -> a.right <= b.left })
        labels.forEach { label ->
            val nodes = compose.onAllNodesWithText(label, useUnmergedTree = true)
            nodes[nodes.fetchSemanticsNodes().lastIndex].assertIsDisplayed()
        }
    }

    @Test fun englishBackendFailureIsLocalizedAtPresentation() {
        show("en")
        compose.runOnUiThread { viewModel.notify("No se pudo cerrar el WAV: Captura parcial no compatible.. La captura queda pendiente de recuperación.") }
        compose.onNodeWithText("Could not close the WAV: Unsupported partial capture.. The capture is pending recovery.").assertExists()
    }

    @Test fun generatedRecoveryLabelIsLocalizedButAnIdenticalStoredNameIsPreserved() {
        lateinit var orphan: RecordingStore.Session
        show("en", prepare = { context ->
            val store = RecordingStore(context)
            store.file(UUID.randomUUID().toString()).writeBytes(WavCodec.encode(ShortArray(4410), 4410, 44100))
            orphan = store.list().single()
        })
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Audio recuperado").assertDoesNotExist()
        compose.onNodeWithText("Recovered audio").assertIsDisplayed()
        compose.runOnUiThread { changeLanguage("es-AR") }
        compose.onNodeWithText("Audio recuperado").assertIsDisplayed()
        compose.runOnUiThread { changeLanguage("en"); viewModel.renameSession(orphan, "Audio recuperado") }
        compose.onNodeWithText("Audio recuperado").assertIsDisplayed()
        Assert.assertEquals("Audio recuperado", RecordingStore(app).list().single().name)
    }

    @Test fun longEnglishInstrumentLabelFitsNarrowLargeText() {
        show("en", widthDp = 320, fontScale = 1.3f)
        compose.runOnUiThread { viewModel.selectInstrument(ar.didgetaller.core.InstrumentProfile.NATIVE_AMERICAN_FLUTE) }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Instrument: Native American flute ▾", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertCompleteTextFits(results.single())
        capture("en-measure-320-font130")
    }

    @Test fun longSpanishInstrumentLabelFitsNarrowLargeText() {
        show("es-AR", widthDp = 320, fontScale = 1.3f)
        compose.runOnUiThread { viewModel.selectInstrument(InstrumentProfile.NATIVE_AMERICAN_FLUTE) }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Instrumento: Flauta nativa ▾", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        capture("es-measure-320-font130")
        assertCompleteTextFits(results.single())
    }

    @Test fun allCompiledStringFormatsAndPluralBoundariesWorkInBothLanguages() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val format = Regex("%(\\d+)\\$[-+ 0,(]*\\d*(?:\\.\\d+)?([sdf])")
        for (language in listOf("en", "es-AR", "de-DE")) {
            val config = Configuration(target.resources.configuration).apply { setLocales(LocaleList(Locale.forLanguageTag(language))) }
            val context = target.createConfigurationContext(config)
            for (field in R.string::class.java.fields) {
                val id = field.getInt(null)
                val text = context.getString(id)
                val specs = format.findAll(text).toList()
                if (specs.isEmpty()) continue
                val arguments = Array<Any>(specs.maxOf { it.groupValues[1].toInt() }) { "value" }
                specs.forEach { m -> arguments[m.groupValues[1].toInt() - 1] = when (m.groupValues[2]) { "d" -> 2; "f" -> 1.25; else -> "value" } }
                context.getString(id, *arguments) // Real compiled Android formatting must not throw.
            }
            val spanish = language.startsWith("es")
            Assert.assertEquals(if (spanish) "1 medición sin audio" else "1 measurement without audio", context.resources.getQuantityString(R.plurals.measurement_count, 1, 1))
            Assert.assertEquals(if (spanish) "2 mediciones sin audio" else "2 measurements without audio", context.resources.getQuantityString(R.plurals.measurement_count, 2, 2))
            Assert.assertTrue(context.resources.getQuantityString(R.plurals.trend_reading_count, 1, 1).startsWith(if (spanish) "1 lectura en" else "1 reading in"))
            Assert.assertTrue(context.resources.getQuantityString(R.plurals.trend_reading_count, 2, 2).startsWith(if (spanish) "2 lecturas en" else "2 readings in"))
            Assert.assertEquals(if (spanish) "Se recuperó 1 grabación interrumpida. Está en Biblioteca." else "Recovered 1 interrupted recording. It is in Library.", context.localizedDiagnostic("Se recuperaron 1 grabaciones interrumpidas. Están en Biblioteca."))
            Assert.assertEquals("Audio -38/7", context.localizedDiagnostic("Audio -38/7"))
            Assert.assertEquals("Se recuperaron unknown grabaciones interrumpidas. Están en Biblioteca.", context.localizedDiagnostic("Se recuperaron unknown grabaciones interrumpidas. Están en Biblioteca."))
        }
    }

    @Test fun configurationChangeRelocalizesWithoutRewritingSessionData() {
        lateinit var session: RecordingStore.Session
        val pcm = WavCodec.encode(ShortArray(44100) { 1234 }, 44100, 44100)
        show("en", prepare = { context ->
            val store = RecordingStore(context)
            session = store.save(pcm, listOf(SessionAnalysis.Point(0, 440.0, .98)), 123L, InstrumentProfile.NATIVE_AMERICAN_FLUTE)
            store.rename(session.id, "Mi sesión personal · preserve")
            SessionToolsStore(context).associate(session.id, "unchanged-design")
        })
        val index = app.getSharedPreferences("local_data", 0).getString("recordings", null)
        val extras = app.getSharedPreferences("session_tools", 0).getString("extras", null)
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Mi sesión personal · preserve").assertExists()
        compose.runOnUiThread { changeLanguage("es-AR") }
        compose.onAllNodesWithText("Biblioteca").assertCountEquals(2)
        compose.onNodeWithText("Mi sesión personal · preserve").assertExists()
        compose.runOnUiThread { changeLanguage("de-DE") }
        compose.onAllNodesWithText("Library").assertCountEquals(2)
        Assert.assertEquals(index, app.getSharedPreferences("local_data", 0).getString("recordings", null))
        Assert.assertEquals(extras, app.getSharedPreferences("session_tools", 0).getString("extras", null))
        Assert.assertArrayEquals(pcm, RecordingStore(app).file(session.id).readBytes())
        compose.onNodeWithTag("session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        val state = viewModel.explorer(session.id)
        compose.runOnIdle { state.inspected = "English inspection"; state.selectedMs = 321 }
        val camera = state.cameras.toMap()
        compose.runOnUiThread { changeLanguage("es-AR") }
        compose.waitForIdle()
        Assert.assertNull("A cached inspection must not retain the previous UI language", state.inspected)
        Assert.assertEquals(321L, state.selectedMs)
        Assert.assertEquals(camera, state.cameras.toMap())
    }

    private fun capture(name: String) {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(target.getExternalFilesDir(null), "localization-evidence").apply { check(exists() || mkdirs()) }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertCompleteTextFits(layout: TextLayoutResult) {
        Assert.assertFalse("Instrument label height must fit", layout.didOverflowHeight)
        for (line in 0 until layout.lineCount) {
            Assert.assertFalse("Instrument label must not be ellipsized", layout.isLineEllipsized(line))
            // TextLayoutResult rounds the layout size to integer pixels; glyph bounds are floats.
            Assert.assertTrue("Instrument label width must fit", layout.getLineRight(line) <= layout.size.width + 1f)
        }
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
