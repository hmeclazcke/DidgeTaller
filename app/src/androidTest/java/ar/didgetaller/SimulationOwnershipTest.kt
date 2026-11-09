package ar.didgetaller

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.SimulationLoader
import ar.didgetaller.core.SimulationTask
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Empty synthetic namespace and controlled callbacks; never records or reads a WAV. */
@RunWith(AndroidJUnit4::class)
class SimulationOwnershipTest {
    private class Pending(val complete: (Result<AcousticModel.Simulation>) -> Unit) : SimulationTask {
        var cancelled = false
        override fun cancel() { cancelled = true }
    }
    private class Loader : SimulationLoader {
        val tasks = mutableListOf<Pending>()
        override fun submit(shape: AcousticModel.Shape, complete: (Result<AcousticModel.Simulation>) -> Unit): Result<SimulationTask> =
            Result.success(Pending(complete).also { tasks += it })
    }
    private class IsolatedApplication(context: Context) : Application() {
        private val namespace = "simulation-owner-${UUID.randomUUID()}"
        init { attachBaseContext(context) }
        override fun getFilesDir() = File(baseContext.filesDir, namespace).apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("$namespace-$name", mode)
    }
    private val first = AcousticModel.Simulation(listOf(1f), listOf(70.0), 70.0)
    private val replacement = AcousticModel.Simulation(listOf(2f), listOf(80.0), 80.0)
    private fun onMain(block: () -> Unit) {
        var result: Result<Unit>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        checkNotNull(result).getOrThrow()
    }

    @Test fun lateSimulationAfterViewModelDisposalCannotPublishOrChangeItsMessage() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = IsolatedApplication(instrumentation.targetContext)
        val loader = Loader(); val owners = ViewModelStore()
        onMain {
            val vm = TallerViewModel(app, loader); owners.put("simulation", vm)
            assertTrue(vm.simBusy)
            val task = loader.tasks.single()
            owners.clear()
            assertTrue(task.cancelled)
            val before = vm.simulation; val message = vm.message
            task.complete(Result.success(first))
            assertSame("Disposed success changed simulation", before, vm.simulation)
            task.complete(Result.failure(IllegalStateException("late synthetic failure")))
            assertSame("Disposed simulation changed", before, vm.simulation)
            assertEquals("Disposed message changed", message, vm.message)
        }
    }

    @Test fun newestShapeOwnsResultsAndFailuresAndKeepsThePreviousSimulationWhileWorking() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = IsolatedApplication(instrumentation.targetContext)
        val loader = Loader(); val owners = ViewModelStore()
        onMain {
            try {
                val vm = TallerViewModel(app, loader); owners.put("simulation", vm)
                loader.tasks[0].complete(Result.success(first)); assertSame(first, vm.simulation)
                vm.updateShape(AcousticModel.Shape(lengthMm = 1600))
                assertSame(first, vm.simulation); assertTrue(vm.simBusy)
                val current = loader.tasks[1]
                current.complete(Result.success(replacement))
                current.complete(Result.failure(IllegalStateException("duplicate failure")))
                loader.tasks[0].complete(Result.failure(IllegalStateException("stale failure")))
                assertSame(replacement, vm.simulation); assertFalse(vm.simBusy)
                assertFalse(vm.message.contains("stale failure"))
            } finally { owners.clear() }
        }
    }

    @Test fun androidViewModelFactoryKeepsTheApplicationConstructorAndDisposesTheDefaultScheduler() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val owners = ViewModelStore()
        onMain {
            try {
                val factory = ViewModelProvider.AndroidViewModelFactory(app)
                val vm = ViewModelProvider(owners, factory)[TallerViewModel::class.java]
                assertSame(app, vm.getApplication<Application>())
                assertTrue(vm.simBusy)
                owners.clear()
                assertFalse(vm.simBusy); assertNull(vm.simulation)
            } finally { owners.clear() }
        }
    }

    @Test fun currentSimulationFailureIsVisibleAndANewShapeCanRecoverWithoutAStaleError() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = IsolatedApplication(InstrumentationRegistry.getInstrumentation().targetContext)
        val loader = Loader(); val owners = ViewModelStore()
        onMain {
            try {
                val vm = TallerViewModel(app, loader); owners.put("simulation", vm)
                loader.tasks.single().complete(Result.failure(IllegalArgumentException("synthetic invalid model")))
                assertFalse(vm.simBusy); assertNull(vm.simulation)
                assertTrue(vm.message.contains("synthetic invalid model"))
                vm.clearMessage(); vm.updateShape(AcousticModel.Shape(lengthMm = 1600))
                assertTrue(vm.simBusy); assertNull(vm.simulation)
                loader.tasks.last().complete(Result.success(replacement))
                loader.tasks.first().complete(Result.failure(IllegalStateException("stale failure")))
                assertSame(replacement, vm.simulation); assertFalse(vm.simBusy); assertEquals("", vm.message)
            } finally { owners.clear() }
        }
    }
}
