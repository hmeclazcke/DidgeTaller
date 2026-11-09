package ar.didgetaller

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled callbacks and an empty synthetic namespace; never opens a user's recording. */
@RunWith(AndroidJUnit4::class)
class AnalysisOwnershipTest {
    private class Pending(val request: RecordingAnalysisRequest, val progress: (Int) -> Unit,
                          val complete: (Result<WavAnalysis.Result>) -> Unit) : RecordingAnalysisTask {
        var cancelled = false
        override fun cancel() { cancelled = true }
    }
    private class Loader : RecordingAnalysisLoader {
        val tasks = mutableListOf<Pending>()
        override fun submit(request: RecordingAnalysisRequest, progress: (Int) -> Unit,
                            complete: (Result<WavAnalysis.Result>) -> Unit): Result<RecordingAnalysisTask> =
            Result.success(Pending(request, progress, complete).also { tasks += it })
    }
    private class SwipePending(val request: SwipeComparisonRequest, val progress: (Int) -> Unit,
        val complete: (Result<SwipeCurve>) -> Unit) : SwipeComparisonTask {
        var cancelled = false
        override fun cancel() { cancelled = true }
    }
    private class SwipeLoader : SwipeComparisonLoader {
        val tasks = mutableListOf<SwipePending>()
        override fun submit(request: SwipeComparisonRequest, progress: (Int) -> Unit,
            complete: (Result<SwipeCurve>) -> Unit): Result<SwipeComparisonTask> =
            Result.success(SwipePending(request, progress, complete).also { tasks += it })
    }
    private class IsolatedApplication(context: Context) : Application() {
        private val namespace = "analysis-owner-${UUID.randomUUID()}"
        init { attachBaseContext(context) }
        override fun getFilesDir() = File(baseContext.filesDir, namespace).apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("$namespace-$name", mode)
    }
    private fun onMain(block: () -> Unit) {
        var result: Result<Unit>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        checkNotNull(result).getOrThrow()
    }
    private fun session() = RecordingStore.Session(UUID.randomUUID().toString(), "Synthetic analysis", 1, 1000, 0,
        null, emptyList(), false)
    private val result = WavAnalysis.Result(0, 4096, 44100, 1, PowerSpectrum(44100, 4096, DoubleArray(2049)),
        HarmonicAnalysis.Result(null, emptyList(), null), emptyList(), null, null, null, 0.0, null)

    @Test fun terminalFailureCannotAcceptLateProgressAndDisposalCannotStartANewAnalysis() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val loader = Loader(); val owners = ViewModelStore()
        onMain {
            try {
                val vm = TallerViewModel(app, null, loader); owners.put("analysis", vm)
                vm.analyzeRecording(session()); val task = loader.tasks.single()
                task.progress(40)
                task.complete(Result.failure(IllegalArgumentException("synthetic invalid input")))
                val terminal = vm.recordingAnalysis
                assertFalse(terminal.running); assertEquals("synthetic invalid input", terminal.error)
                task.progress(12)
                assertEquals("Terminal progress was overwritten", terminal, vm.recordingAnalysis)
                owners.clear(); val disposed = vm.recordingAnalysis
                vm.analyzeRecording(session())
                assertEquals(1, loader.tasks.size)
                assertEquals(disposed, vm.recordingAnalysis)
            } finally { owners.clear() }
        }
    }

    @Test fun replacementOwnsRangeProgressAndTerminalResultWhileNavigationRevokesPublication() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val loader = Loader(); val owners = ViewModelStore()
        onMain {
            try {
                val vm = TallerViewModel(app, null, loader); owners.put("analysis", vm)
                val a = session(); val b = session()
                vm.analyzeRecording(a); val old = loader.tasks.single(); old.progress(40)
                vm.analyzeRecording(b, 200, 900, true); val current = loader.tasks.last()
                assertTrue(old.cancelled); assertEquals(b.id, current.request.sessionId)
                assertEquals(200L, current.request.startMs); assertEquals(900L, current.request.endMs)
                assertTrue(current.request.force)
                current.progress(20); val waiting = vm.recordingAnalysis
                old.progress(90); old.complete(Result.failure(IllegalStateException("old")))
                assertEquals(waiting, vm.recordingAnalysis)
                current.complete(Result.success(result)); current.progress(30)
                current.complete(Result.failure(IllegalStateException("duplicate")))
                assertSame(result, vm.recordingAnalysis.result); assertEquals(100, vm.recordingAnalysis.progress)
                vm.cancelRecordingAnalysis(); assertSame(result, vm.recordingAnalysis.result)
                vm.analyzeRecording(a); val leaving = loader.tasks.last()
                vm.onAppStopped(); assertTrue(leaving.cancelled)
                assertFalse(vm.recordingAnalysis.running)
                val cancelled = vm.recordingAnalysis
                leaving.progress(80); leaving.complete(Result.success(result))
                assertEquals(cancelled, vm.recordingAnalysis)
                vm.cancelRecordingAnalysis(clear = true); assertNull(vm.recordingAnalysis.sessionId)
            } finally { owners.clear() }
        }
    }

    @Test fun clearingDuringWorkCancelsTheHandleAndLateEventsCannotChangeDisposedState() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val loader = Loader(); val owners = ViewModelStore()
        onMain {
            val vm = TallerViewModel(app, null, loader); owners.put("analysis", vm)
            vm.analyzeRecording(session()); val pending = loader.tasks.single()
            owners.clear(); assertTrue(pending.cancelled)
            val disposed = vm.recordingAnalysis
            pending.progress(90); pending.complete(Result.success(result))
            pending.complete(Result.failure(IllegalArgumentException("late")))
            vm.analyzeRecording(session()); vm.cancelRecordingAnalysis(true)
            assertEquals(disposed, vm.recordingAnalysis); assertEquals(1, loader.tasks.size)
        }
    }
    @Test fun aClearedModelCannotEnableOrStartASwipeComparison() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val owners = ViewModelStore()
        onMain {
            val vm = TallerViewModel::class.java.getConstructor(Application::class.java).newInstance(app)
            owners.put("analysis", vm)
            owners.clear()
            val disposed = vm.swipeComparison
            vm.setSwipeComparison(session(), true)
            assertEquals("A cleared owner must not publish new SWIPE work", disposed, vm.swipeComparison)
        }
    }

    @Test fun swipeCacheAndProgressPreserveTheCorrectComparisonCameraAndInstrumentBoundary() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val swipe = SwipeLoader(); val owners = ViewModelStore()
        val curve = SwipeCurve(listOf(SwipeCurve.Sample(20.0, 70.0)), 10.0, 35, 140)
        onMain {
            try {
                val vm = TallerViewModel(app, null, Loader(), swipe); owners.put("analysis", vm)
                val item = session(); vm.onDestinationChanged(item.id)
                val explorer = vm.explorer(item.id)
                val original = PlotViewport(AxisWindow(0.0, 1000.0), AxisWindow(60.0, 100.0))
                explorer.cameras[ar.didgetaller.ui.AcousticPlot.PITCH] = original
                vm.setSwipeComparison(item, true); val task = swipe.tasks.single()
                assertEquals(AxisWindow(33.25, 147.0), explorer.camera(1000, 44100, emptyList()).y)
                val panned = original.copy(x = AxisWindow(100.0, 500.0), y = AxisWindow(50.0, 120.0))
                explorer.cameras[ar.didgetaller.ui.AcousticPlot.PITCH] = panned
                task.progress(40); task.complete(Result.success(curve))
                assertEquals(panned, explorer.camera(1000, 44100, emptyList()))
                vm.setSwipeComparison(item, false)
                assertEquals(original, explorer.camera(1000, 44100, emptyList()))
                vm.setSwipeComparison(item, true)
                assertEquals(1, swipe.tasks.size); assertSame(curve, vm.swipeComparison.curve)
                val ready = vm.swipeComparison
                vm.setSwipeComparison(item.copy(instrument = InstrumentProfile.NATIVE_AMERICAN_FLUTE), true)
                assertEquals(ready, vm.swipeComparison)
                vm.onAppStopped(); assertFalse(vm.swipeComparison.enabled)
                assertEquals(original, explorer.camera(1000, 44100, emptyList()))
            } finally { owners.clear() }
        }
    }

    @Test fun swipeReplacementNavigationAndDisposalRevokeLateNativeCallbacks() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val swipe = SwipeLoader(); val owners = ViewModelStore()
        onMain {
            val vm = TallerViewModel(app, null, Loader(), swipe); owners.put("analysis", vm)
            val a = session(); val b = session()
            vm.onDestinationChanged(a.id); vm.setSwipeComparison(a, true); val old = swipe.tasks.single()
            vm.onDestinationChanged(b.id); assertTrue(old.cancelled)
            vm.setSwipeComparison(b, true); val current = swipe.tasks.last(); current.progress(25)
            val selected = vm.swipeComparison
            old.progress(90); old.complete(Result.failure(IllegalArgumentException("late")))
            assertEquals(selected, vm.swipeComparison)
            owners.clear(); assertTrue(current.cancelled); val disposed = vm.swipeComparison
            current.progress(95); current.complete(Result.success(SwipeCurve(emptyList(), 10.0, 35, 140)))
            vm.setSwipeComparison(a, true); vm.setSwipeComparison(b, false)
            assertEquals(disposed, vm.swipeComparison); assertEquals(2, swipe.tasks.size)
        }
    }

}
