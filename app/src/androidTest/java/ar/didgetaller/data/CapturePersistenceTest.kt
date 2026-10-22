package ar.didgetaller.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.Pcm16WavReader
import ar.didgetaller.core.ProgressiveWavWriter
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.WavCodec
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Real Android preferences and files; only a unique test-owned namespace is changed. */
@RunWith(AndroidJUnit4::class)
class CapturePersistenceTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var preferences: String

    @Before fun setUpIsolatedEmulatorContext() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val token = UUID.randomUUID().toString()
        directory = File(target.cacheDir, "capture-persistence-$token").apply { check(mkdirs()) }
        preferences = "capture-persistence-$token"
        context = object : ContextWrapper(target) {
            override fun getFilesDir(): File = directory
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                target.getSharedPreferences("$preferences-$name", mode)
        }
    }

    @After fun removeOnlyOwnedFixtures() {
        if (!::context.isInitialized) return
        (context as ContextWrapper).baseContext.deleteSharedPreferences("$preferences-local_data")
        directory.deleteRecursively()
    }

    @Test fun destinationCollisionPreservesBothCapturesAndOriginalIndex() {
        val store = RecordingStore(context)
        val original = WavCodec.encode(shortArrayOf(10, -20, 30), 3, 8000)
        val points = listOf(SessionAnalysis.Point(0, 440.0, .95))
        val saved = store.save(original, points, 123L, InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        val partial = File(store.file(saved.id).parentFile, "${saved.id}.part")
        ProgressiveWavWriter(partial, 8000).use { it.append(shortArrayOf(90, -80), 2) }
        val incoming = partial.readBytes()
        val prefs = context.getSharedPreferences("local_data", 0)
        val index = prefs.getString("recordings", null)

        assertThrows(IllegalStateException::class.java) { store.importCapture(partial, emptyList(), 456L) }
        repeat(2) { assertEquals(0, RecordingStore(context).recoverPending()) }
        assertArrayEquals(original, store.file(saved.id).readBytes())
        assertArrayEquals(incoming, partial.readBytes())
        assertEquals(index, prefs.getString("recordings", null))
        val reopened = RecordingStore(context).list().single()
        assertEquals(saved.id, reopened.id)
        assertEquals(points, reopened.timeline)
        assertEquals(InstrumentProfile.NATIVE_AMERICAN_FLUTE, reopened.instrument)
    }

    @Test fun incompleteImportPreservesPartialAndCanBeRetriedExactlyOnceAfterRepair() {
        val store = RecordingStore(context)
        val partial = store.newCapture()
        val source = File(directory, "snapshot-source.part")
        val pcm = shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE)
        ProgressiveWavWriter(source, 8000).use { writer ->
            writer.append(pcm, pcm.size)
            partial.writeBytes(source.readBytes()) // Exact pre-close on-disk header and PCM.
        }
        val interrupted = partial.readBytes()
        val prefs = context.getSharedPreferences("local_data", 0)
        val index = prefs.getString("recordings", null)
        assertThrows(IllegalArgumentException::class.java) { store.importCapture(partial, emptyList(), 456L) }
        assertArrayEquals(interrupted, partial.readBytes())
        assertEquals(index, prefs.getString("recordings", null))
        assertFalse(store.file(partial.nameWithoutExtension).exists())

        assertTrue(ProgressiveWavWriter.repair(partial))
        val expected = partial.readBytes()
        val saved = store.importCapture(partial, emptyList(), 456L)
        assertFalse(partial.exists())
        assertArrayEquals(expected, store.file(saved.id).readBytes())
        Pcm16WavReader(store.file(saved.id)).use { reader ->
            val decoded = ShortArray(pcm.size)
            assertEquals(pcm.size, reader.read(0, decoded))
            assertArrayEquals(pcm, decoded)
        }
        assertThrows(java.io.FileNotFoundException::class.java) { store.importCapture(partial, emptyList(), 456L) }
        val reopened = RecordingStore(context).list().single()
        assertEquals(saved.id, reopened.id)
        assertTrue(reopened.timeline.isEmpty())
        assertNull(reopened.summary)
        assertArrayEquals(expected, store.file(saved.id).readBytes())
    }

    /** Characterizes a known import limitation, not the desired behavior of a future fix. */
    @Test fun staleCheckpointImportExposesOnlyHeaderFramesDespitePreservedTail() {
        val store = RecordingStore(context)
        val partial = store.newCapture()
        val source = File(directory, "checkpoint-source.part")
        ProgressiveWavWriter(source, 8000).use { writer ->
            writer.append(ShortArray(8000) { 1234 }, 8000) // Header checkpoint: one second.
            writer.append(ShortArray(4000) { -4321 }, 4000) // PCM tail, no second checkpoint.
            partial.writeBytes(source.readBytes())
        }
        val interrupted = partial.readBytes()
        assertEquals(44L + 12000L * 2, partial.length())
        val saved = store.importCapture(partial, emptyList(), 456L)
        assertEquals(1000L, saved.durationMs) // Only the checkpoint is exposed, not all 1500ms.
        assertArrayEquals(interrupted, store.file(saved.id).readBytes()) // Tail bytes are not deleted.
        Pcm16WavReader(store.file(saved.id)).use { assertEquals(8000L, it.frameCount) }
        val reopened = RecordingStore(context).list().single()
        assertEquals(1000L, reopened.durationMs)
        assertEquals(44L + 12000L * 2, reopened.sizeBytes)
        assertEquals(0, RecordingStore(context).recoverPending()) // Already renamed to .wav.
    }
}
