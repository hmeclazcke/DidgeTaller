package ar.didgetaller.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import ar.didgetaller.data.SessionToolsStore
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.Locale
import kotlin.math.*

class HarmonicDetectorUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val owners = ViewModelStore()
    private lateinit var app: SwipeComparisonUiTest.IsolatedApplication
    private val previousLocale = Locale.getDefault()
    private var previousAppLocales: android.os.LocaleList? = null

    @Before fun isolate() {
        Assume.assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish"))
        app = SwipeComparisonUiTest.IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
    }
    @After fun cleanup() {
        if (::app.isInitialized) { compose.runOnUiThread { owners.clear() }; app.clean() }
        if (Build.VERSION.SDK_INT >= 33) previousAppLocales?.let { original ->
            compose.runOnUiThread { app.getSystemService(android.app.LocaleManager::class.java).applicationLocales = original }
        }
        Locale.setDefault(previousLocale)
    }

    @Test fun spanishLargeText() = compare("es", 1.5f)
    @Test fun englishNormalText() = compare("en", 1f)

    @Test fun bothNavigationRoutesReachTheSameReplay() {
        val bytes = WavCodec.encode(ShortArray(44100), 44100, 44100)
        val item = RecordingStore(app).save(bytes)
        val vm = compose.runOnUiThread { TallerViewModel(app).also { owners.put("routes", it) } }
        compose.setContent { TallerApp(vm, {}, {}, { _,_,_ -> }) }
        compose.onNodeWithTag("pitch-algorithm").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(uiText(R.string.ui_laboratory)).performClick()
        compose.onNodeWithContentDescription(uiText(R.string.ui_close)).performClick()
        compose.onNodeWithTag("compare-detectors").performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.detector_choose)).performClick()
        val name = app.sessionName(item)
        compose.onNodeWithText(name).performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithTag("detector-graph").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(uiText(R.string.ui_close)).performClick()
        compose.onNodeWithContentDescription(uiText(R.string.ui_library)).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.onNodeWithTag("compare-detectors").performScrollTo().performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithTag("detector-graph").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(name) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        assertArrayEquals(bytes, RecordingStore(app).file(item.id).readBytes())
    }

    @Test fun experimentalDisplayCannotChangeCaptureIdentityOrStandardTimeline() {
        compose.runOnUiThread {
            val vm = TallerViewModel(app).also { owners.put("capture", it) }
            vm.selectPitchAlgorithm(PitchAlgorithm.HARMONIC)
            val recording = vm.javaClass.getDeclaredField("recording").apply { isAccessible = true }.get(vm) as RecordingCoordinator
            val id = CaptureId()
            recording.monitorStarted(id)
            vm.selectPitchAlgorithm(PitchAlgorithm.STANDARD)
            assertEquals(PitchAlgorithm.HARMONIC, vm.pitchAlgorithm)
            recording.begin(id, 1L, InstrumentProfile.DIDGERIDOO)
            val deliver = vm.javaClass.getDeclaredMethod("onLiveReading", PitchDetector.Reading::class.java).apply { isAccessible = true }
            deliver.invoke(vm, PitchDetector.Reading(hz = 102.0, confidence = .95, harmonic = HarmonicEstimate(68.0, .9)))
            assertEquals(68.0, vm.filteredHz, 0.0)
            assertEquals(102.0, recording.state.points.single().hz!!, 0.0)
            recording.saving()
            vm.selectPitchAlgorithm(PitchAlgorithm.STANDARD)
            assertEquals(PitchAlgorithm.HARMONIC, SessionToolsStore(app).pitchAlgorithm())
        }
    }

    private fun compare(lang: String, scale: Float) {
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = app.getSystemService(android.app.LocaleManager::class.java)
            previousAppLocales = manager.applicationLocales
            compose.runOnUiThread { manager.applicationLocales = android.os.LocaleList(locale) }
            compose.waitForIdle()
        }
        val configuration = Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang)); fontScale = scale }
        val context = app.createConfigurationContext(configuration)
        val pcm = ShortArray(44100 * 3) { i ->
            val t = 2 * PI * 68 * i / 44100
            (10000 * sin(3*t) + 800 * sin(4*t) + 300 * sin(t)).toInt().toShort()
        }
        val wav = WavCodec.encode(pcm, pcm.size, 44100)
        val store = RecordingStore(app)
        val item = store.save(wav)
        val vm = compose.runOnUiThread { TallerViewModel(app).also { owners.put("comparison", it) } }
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val activityContext = androidx.compose.runtime.remember(context) {
                object : android.content.ContextWrapper(compose.activity) {
                    override fun getResources() = context.resources
                }
            }
            CompositionLocalProvider(LocalContext provides activityContext, LocalConfiguration provides configuration,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, scale)) {
                MaterialTheme(colorScheme = appColors) { Column { DetectorSelector(vm); DetectorComparisonAction(vm, item) } }
            }
        }
        assertEquals(PitchAlgorithm.STANDARD, vm.pitchAlgorithm)
        compose.onNodeWithTag("pitch-algorithm").performClick()
        compose.onNodeWithText(context.getString(R.string.detector_experimental)).performClick()
        assertEquals(PitchAlgorithm.HARMONIC, SessionToolsStore(app).pitchAlgorithm())
        compose.onNodeWithTag("compare-detectors").performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithTag("detector-graph").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.detector_both)).assertIsSelected()
        compose.onNodeWithContentDescription(context.getString(R.string.detector_cursor)).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(1200f) }
        compose.onAllNodesWithText("Hz", substring = true).assertCountEquals(2)
        File(app.getExternalFilesDir(null), "harmonic-$lang.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("YIN", substring = false).performClick()
        compose.onAllNodesWithText("Hz", substring = true).assertCountEquals(1)
        compose.onNodeWithText(context.getString(R.string.detector_harmonic), substring = false).performClick()
        compose.onAllNodesWithText("Hz", substring = true).assertCountEquals(1)
        compose.onNodeWithText(context.getString(R.string.ui_close)).performClick()
        assertArrayEquals(wav, store.file(item.id).readBytes())
        assertEquals(item.timeline, store.list().single().timeline)
    }

    @Test fun streamingCost() {
        for (instrument in InstrumentProfile.entries) for (algorithm in PitchAlgorithm.entries) {
            val stream = PitchStream(instrument, algorithm = algorithm)
            val freq = if (instrument == InstrumentProfile.DIDGERIDOO) 68 else 245
            val chunks = Array(24) { n -> ShortArray(4096) { i ->
                val a = 2 * PI * freq * (i + n * 4096) / 44100
                (12000 * sin(a * 3) + 1600 * sin(a * 4)).toInt().toShort()
            } }
            repeat(64) { stream.append(chunks[it % chunks.size]) }
            val cpu = mutableListOf<Double>()
            repeat(96) {
                val start = Debug.threadCpuTimeNanos()
                val reading = stream.append(chunks[it % chunks.size])
                if (reading != null) cpu += (Debug.threadCpuTimeNanos() - start) / 1e6
            }
            cpu.sort()
            Log.i("DidgePitchCost", "$instrument $algorithm frames=${cpu.size} medianMs=${cpu[cpu.size/2]} p95Ms=${cpu[(cpu.size*.95).toInt()]}")
        }
    }
}
