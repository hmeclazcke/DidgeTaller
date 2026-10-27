package ar.didgetaller.audio

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.SessionPhase
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.ProgressiveWavWriter
import ar.didgetaller.core.WavCodec
import ar.didgetaller.data.RecordingStore
import ar.didgetaller.data.SessionToolsStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Actual writer/engine/Handler callbacks; no AudioRecord or microphone permission is used. */
@RunWith(AndroidJUnit4::class)
class RecordingHandoffTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var app: IsolatedApplication

    @Before fun setUpOwnedEmulatorFiles() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        app = IsolatedApplication(instrumentation.targetContext)
    }

    @After fun removeOnlyOwnedFixtures() {
        if (::app.isInitialized) app.clean()
    }

    // Test-only access keeps fault injection out of the production API. Names are debug fields.
    private fun field(target: Any, name: String) = target.javaClass.getDeclaredField(name).apply { isAccessible = true }
    private fun attach(engine: MicrophoneEngine, writer: ProgressiveWavWriter) {
        field(engine, "writer").set(engine, writer)
        field(engine, "recording").setBoolean(engine, true)
    }
    private fun finish(engine: MicrophoneEngine) {
        engine.javaClass.getDeclaredMethod("finishRecording", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(engine, true)
    }
    private fun breakOwnedHandle(writer: ProgressiveWavWriter) {
        // Real IOException on finalization, without mocking close or changing file bytes.
        (field(writer, "output").get(writer) as RandomAccessFile).close()
    }

    @Test fun closeFailureDoesNotHandOffAnUnfinalizedCapture() = assertFailedHandoff(8)

    @Test fun closeFailureDoesNotHandOffEvenWhenCheckpointHeaderIsReadable() = assertFailedHandoff(44100)

    private fun assertFailedHandoff(frames: Int) {
        val store = RecordingStore(app)
        val partial = store.newCapture()
        val pcm = ShortArray(frames) { if (it % 2 == 0) Short.MIN_VALUE else Short.MAX_VALUE }
        val writer = ProgressiveWavWriter(partial)
        writer.append(pcm, pcm.size)
        val original = partial.readBytes()
        val ready = AtomicInteger()
        val stopped = AtomicInteger()
        val errors = AtomicInteger()
        val engine = MicrophoneEngine({}, { stopped.incrementAndGet() }, { ready.incrementAndGet() }, { errors.incrementAndGet() })
        attach(engine, writer)
        breakOwnedHandle(writer)

        repeat(2) { finish(engine) }
        instrumentation.waitForIdleSync()
        assertEquals("A failed close must not authorize persistence", 0, ready.get())
        assertEquals(1, stopped.get())
        assertEquals(1, errors.get())
        assertArrayEquals(original, partial.readBytes())
        assertTrue(store.list().isEmpty())
        assertEquals(1, store.recoverPending())
        assertEquals(0, store.recoverPending())
        assertArrayEquals(WavCodec.encode(pcm, pcm.size, 44100), store.file(partial.nameWithoutExtension).readBytes())
        assertEquals(partial.nameWithoutExtension, store.list().single().id)
    }

    @Test fun normalFinalizationAndRepeatedFinishHandOffExactlyOnce() {
        val store = RecordingStore(app)
        val partial = store.newCapture()
        val pcm = shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE)
        val writer = ProgressiveWavWriter(partial)
        writer.append(pcm, pcm.size)
        val ready = AtomicInteger()
        val stopped = AtomicInteger()
        val errors = AtomicInteger()
        val engine = MicrophoneEngine({}, { stopped.incrementAndGet() }, { file ->
            assertEquals(partial, file)
            assertArrayEquals(WavCodec.encode(pcm, pcm.size, 44100), file.readBytes())
            ready.incrementAndGet()
        }, { errors.incrementAndGet() })
        attach(engine, writer)
        repeat(2) { finish(engine) }
        instrumentation.waitForIdleSync()
        assertEquals(1, ready.get())
        assertEquals(1, stopped.get())
        assertEquals(0, errors.get())
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun discardRemovesOnlyTheCurrentPartialAndPreservesSavedData() {
        val store = RecordingStore(app)
        val audio = WavCodec.encode(ShortArray(44100) { 1234 }, 44100, 44100)
        val existing = store.save(audio, createdAt = 123L)
        val index = app.getSharedPreferences("local_data", 0).getString("recordings", null)
        val owner = ViewModelStore()
        lateinit var vm: TallerViewModel
        instrumentation.runOnMainSync { vm = TallerViewModel(app); owner.put("capture", vm) }
        try {
            val partial = store.newCapture()
            val writer = ProgressiveWavWriter(partial)
            writer.append(shortArrayOf(1, 2, 3), 3)
            val engine = field(vm, "mic").get(vm) as MicrophoneEngine
            attach(engine, writer)
            instrumentation.runOnMainSync {
                (field(vm, "sessionPhase\$delegate").get(vm) as MutableState<SessionPhase>).value = SessionPhase.CAPTURING
                vm.discardSession()
                vm.discardSession()
            }
            repeat(2) { finish(engine) }
            instrumentation.waitForIdleSync()
            assertEquals(SessionPhase.IDLE, vm.sessionPhase)
            assertFalse(partial.exists())
            assertEquals(index, app.getSharedPreferences("local_data", 0).getString("recordings", null))
            assertEquals(listOf(existing.id), store.list().map { it.id })
            assertArrayEquals(audio, store.file(existing.id).readBytes())
        } finally {
            instrumentation.runOnMainSync { owner.clear() }
        }
    }

    @Test fun saveAndDiscardRaceHasExactlyOneOutcome() {
        repeat(20) {
            val partial = RecordingStore(app).newCapture()
            val writer = ProgressiveWavWriter(partial)
            writer.append(shortArrayOf(10, 20, 30), 3)
            val ready = AtomicInteger()
            val discarded = AtomicInteger()
            val errors = AtomicInteger()
            val engine = MicrophoneEngine({}, {}, { ready.incrementAndGet() }, { errors.incrementAndGet() },
                onRecordingDiscarded = { success -> if (success) discarded.incrementAndGet() else errors.incrementAndGet() })
            attach(engine, writer)
            val start = java.util.concurrent.CountDownLatch(1)
            val saving = Thread { start.await(); finish(engine) }
            val cancelling = Thread { start.await(); if (engine.discardRecording()) finish(engine) }
            saving.start(); cancelling.start(); start.countDown(); saving.join(); cancelling.join()
            instrumentation.waitForIdleSync()
            assertEquals(1, ready.get() + discarded.get())
            assertEquals(0, errors.get())
            assertEquals(ready.get() == 1, partial.exists())
            if (partial.exists()) assertTrue(partial.delete())
        }
    }

    @Test fun claimedDiscardIsNotRecoveredBeforeCleanupCompletes() {
        val store = RecordingStore(app)
        val partial = store.newCapture()
        val writer = ProgressiveWavWriter(partial)
        writer.append(shortArrayOf(1, 2, 3), 3)
        val engine = MicrophoneEngine({}, {}, {}, {})
        attach(engine, writer)
        assertTrue(engine.discardRecording())
        val discarded = File(partial.parentFile, "${partial.nameWithoutExtension}.discard")
        assertFalse(partial.exists())
        assertTrue(discarded.exists())
        assertEquals(0, store.recoverPending())
        assertTrue(store.list().isEmpty())
        finish(engine)
        instrumentation.waitForIdleSync()
        assertFalse(discarded.exists())
    }

    @Test fun failedDiscardReportsFailureAndDoesNotTouchAnotherFile() {
        val store = RecordingStore(app)
        val partial = store.newCapture()
        val writer = ProgressiveWavWriter(partial)
        writer.append(shortArrayOf(1, 2, 3), 3)
        val blocker = File(partial.parentFile, "${partial.nameWithoutExtension}.discard").apply { writeText("owned sentinel") }
        val ready = AtomicInteger()
        val discarded = AtomicInteger()
        val failures = AtomicInteger()
        val engine = MicrophoneEngine({}, {}, { ready.incrementAndGet() }, {},
            onRecordingDiscarded = { if (it) discarded.incrementAndGet() else failures.incrementAndGet() })
        attach(engine, writer)
        assertTrue(engine.discardRecording())
        assertFalse(engine.discardRecording())
        finish(engine)
        instrumentation.waitForIdleSync()
        assertEquals(0, ready.get())
        assertEquals(0, discarded.get())
        assertEquals(1, failures.get())
        assertTrue(partial.exists())
        assertEquals("owned sentinel", blocker.readText())
    }

    @Test fun discardDoesNotReportSuccessIfMicrophoneReleaseFails() {
        val partial = RecordingStore(app).newCapture()
        val writer = ProgressiveWavWriter(partial)
        writer.append(shortArrayOf(1, 2, 3), 3)
        val failures = AtomicInteger()
        val successes = AtomicInteger()
        val engine = MicrophoneEngine({}, {}, {}, {},
            onRecordingDiscarded = { if (it) successes.incrementAndGet() else failures.incrementAndGet() })
        attach(engine, writer)
        assertTrue(engine.discardRecording())
        engine.javaClass.getDeclaredMethod("finishRecording", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(engine, false)
        instrumentation.waitForIdleSync()
        assertEquals(0, successes.get())
        assertEquals(1, failures.get())
        assertFalse(partial.exists())
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun failedDiscardIsVisibleToTheUserAndDoesNotImport() {
        val store = RecordingStore(app)
        val audio = WavCodec.encode(ShortArray(44100) { 1234 }, 44100, 44100)
        val existing = store.save(audio, createdAt = 123L)
        val index = app.getSharedPreferences("local_data", 0).getString("recordings", null)
        val owner = ViewModelStore()
        lateinit var vm: TallerViewModel
        instrumentation.runOnMainSync { vm = TallerViewModel(app); owner.put("capture", vm) }
        try {
            val partial = store.newCapture()
            val writer = ProgressiveWavWriter(partial)
            writer.append(shortArrayOf(1, 2, 3), 3)
            val blocker = File(partial.parentFile, "${partial.nameWithoutExtension}.discard").apply { writeText("owned sentinel") }
            val engine = field(vm, "mic").get(vm) as MicrophoneEngine
            attach(engine, writer)
            instrumentation.runOnMainSync {
                (field(vm, "sessionPhase\$delegate").get(vm) as MutableState<SessionPhase>).value = SessionPhase.CAPTURING
                vm.discardSession()
            }
            finish(engine)
            instrumentation.waitForIdleSync()
            assertEquals(SessionPhase.IDLE, vm.sessionPhase)
            assertNotNull(vm.microphoneError)
            assertEquals(vm.microphoneError, vm.message)
            assertTrue(partial.exists())
            assertEquals("owned sentinel", blocker.readText())
            assertEquals(index, app.getSharedPreferences("local_data", 0).getString("recordings", null))
            assertArrayEquals(audio, store.file(existing.id).readBytes())
            assertEquals(listOf(existing.id), store.list().map { it.id })
        } finally {
            instrumentation.runOnMainSync { owner.clear() }
        }
    }

    @Test fun actualTimeoutAndDiscardHaveOneTerminalOutcome() {
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, android.Manifest.permission.RECORD_AUDIO)
        for (delay in listOf(650L, 1400L)) {
            val partial = RecordingStore(app).newCapture()
            val done = java.util.concurrent.CountDownLatch(1)
            val ready = AtomicInteger()
            val discarded = AtomicInteger()
            val errors = AtomicInteger()
            val engine = MicrophoneEngine({}, {}, { ready.incrementAndGet(); done.countDown() },
                { errors.incrementAndGet(); done.countDown() },
                onRecordingDiscarded = { success ->
                    if (success) discarded.incrementAndGet() else errors.incrementAndGet()
                    done.countDown()
                })
            assertTrue(engine.start())
            assertTrue(engine.startRecording(partial, 1))
            Thread.sleep(delay)
            val accepted = engine.discardRecording()
            assertTrue("The recorder must finish", done.await(15, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            assertEquals(0, errors.get())
            assertEquals(1, ready.get() + discarded.get())
            assertEquals(accepted, discarded.get() == 1)
            assertFalse(engine.isRunning())
            assertEquals(ready.get() == 1, partial.exists())
            if (partial.exists()) assertTrue(partial.delete())
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun closeFailureEndsLiveAndClearedViewModelsWithoutImport() {
        for ((phase, cleared) in listOf(SessionPhase.CAPTURING to false, SessionPhase.SAVING to false, SessionPhase.SAVING to true)) {
            val store = RecordingStore(app)
            val existingAudio = WavCodec.encode(ShortArray(44100) { 1234 }, 44100, 44100)
            val existing = store.save(existingAudio, createdAt = 123L, instrument = InstrumentProfile.NATIVE_AMERICAN_FLUTE)
            store.rename(existing.id, "Existing synthetic session")
            val existingSessions = store.list()
            val owner = ViewModelStore()
            lateinit var vm: TallerViewModel
            instrumentation.runOnMainSync { vm = TallerViewModel(app); owner.put("capture", vm) }
            try {
                val partial = store.newCapture()
                val writer = ProgressiveWavWriter(partial)
                writer.append(shortArrayOf(-1234, 0, 1234), 3)
                val original = partial.readBytes()
                val id = partial.nameWithoutExtension
                val tools = SessionToolsStore(app)
                val design = UUID.randomUUID().toString()
                tools.associate(id, design)
                tools.addMarker(id, 0, "manual", "Synthetic capture")
                val prefs = app.getSharedPreferences("local_data", 0)
                val index = prefs.getString("recordings", null)
                val extras = app.getSharedPreferences("session_tools", 0).getString("extras", null)
                val engine = field(vm, "mic").get(vm) as MicrophoneEngine
                attach(engine, writer)
                instrumentation.runOnMainSync {
                    (field(vm, "sessionPhase\$delegate").get(vm) as MutableState<SessionPhase>).value = phase
                }
                breakOwnedHandle(writer)
                if (cleared) instrumentation.runOnMainSync { owner.clear() }
                repeat(2) { finish(engine) }
                instrumentation.waitForIdleSync()
                // Also drain any erroneously queued persistence before checking observable state.
                val executor = field(vm, "sessionExecutor").get(vm) as ExecutorService
                if (cleared) assertTrue(executor.isShutdown) else executor.submit {}.get(10, TimeUnit.SECONDS)
                instrumentation.waitForIdleSync()

                assertEquals(SessionPhase.IDLE, vm.sessionPhase)
                assertFalse(vm.listening)
                assertTrue("Failed finalization must retain the partial", partial.exists())
                assertArrayEquals(original, partial.readBytes())
                assertEquals(index, prefs.getString("recordings", null))
                assertEquals(existingSessions, store.list())
                assertEquals(existingSessions, vm.sessions)
                assertArrayEquals(existingAudio, store.file(existing.id).readBytes())
                assertEquals(vm.microphoneError, vm.message)
                assertFalse(store.file(id).exists())
                assertEquals(design, tools.designId(id))
                assertEquals(extras, app.getSharedPreferences("session_tools", 0).getString("extras", null))
            } finally {
                instrumentation.runOnMainSync { owner.clear() }
            }
            // Recover only this iteration's owned partial before creating the next ViewModel.
            assertEquals(1, store.recoverPending())
            assertEquals(0, store.recoverPending())
            // Next iteration needs an empty isolated index, not the recovered orphan fixture.
            store.list().forEach { store.delete(it.id) }
        }
    }

    private class IsolatedApplication(context: Context) : Application() {
        private val token = "handoff-${UUID.randomUUID()}"
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
