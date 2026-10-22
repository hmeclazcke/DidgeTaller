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
        val target = (context as ContextWrapper).baseContext
        target.deleteSharedPreferences("$preferences-local_data")
        target.deleteSharedPreferences("$preferences-session_tools")
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
        incoming[40] = 0 // A stale size must not be rewritten when the UUID already has a WAV.
        partial.writeBytes(incoming)
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

    @Test fun importBeforeFirstCheckpointRecoversExactlyOnce() {
        val store = RecordingStore(context)
        val partial = store.newCapture()
        val source = File(directory, "snapshot-source.part")
        val pcm = shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE)
        ProgressiveWavWriter(source, 8000).use { writer ->
            writer.append(pcm, pcm.size)
            partial.writeBytes(source.readBytes()) // Exact pre-close on-disk header and PCM.
        }
        val expected = WavCodec.encode(pcm, pcm.size, 8000)
        val saved = store.importCapture(partial, emptyList(), 456L)
        assertFalse(partial.exists())
        assertArrayEquals(expected, store.file(saved.id).readBytes())
        Pcm16WavReader(store.file(saved.id)).use { reader ->
            val decoded = ShortArray(pcm.size)
            assertEquals(pcm.size, reader.read(0, decoded))
            assertArrayEquals(pcm, decoded)
        }
        assertThrows(IllegalStateException::class.java) { store.importCapture(partial, emptyList(), 456L) }
        val reopened = RecordingStore(context).list().single()
        assertEquals(saved.id, reopened.id)
        assertTrue(reopened.timeline.isEmpty())
        assertNull(reopened.summary)
        assertArrayEquals(expected, store.file(saved.id).readBytes())
    }

    @Test fun staleCheckpointImportRecoversEveryCompleteFrameAndPreservesMetadata() {
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
        val id = partial.nameWithoutExtension
        val tools = SessionToolsStore(context)
        val design = UUID.randomUUID().toString()
        tools.associate(id, design)
        tools.addMarker(id, 1250, "manual", "Synthetic tail")
        val extras = context.getSharedPreferences("session_tools", 0).getString("extras", null)
        val points = listOf(SessionAnalysis.Point(0, 440.0, .95), SessionAnalysis.Point(1250, 442.0, .96))
        val saved = store.importCapture(partial, points, 456L, InstrumentProfile.NATIVE_AMERICAN_FLUTE)
        assertEquals(id, saved.id)
        assertEquals(1500L, saved.durationMs)
        val expected = ShortArray(8000) { 1234 } + ShortArray(4000) { -4321 }
        assertArrayEquals(WavCodec.encode(expected, expected.size, 8000), store.file(saved.id).readBytes())
        assertArrayEquals(interrupted.copyOfRange(44, interrupted.size), store.file(saved.id).readBytes().copyOfRange(44, interrupted.size))
        Pcm16WavReader(store.file(saved.id)).use { reader ->
            assertEquals(12000L, reader.frameCount)
            val decoded = ShortArray(12000)
            assertEquals(12000, reader.read(0, decoded))
            assertArrayEquals(expected, decoded)
        }
        val reopened = RecordingStore(context).list().single()
        assertEquals(id, reopened.id)
        assertEquals(456L, reopened.createdAt)
        assertEquals(points, reopened.timeline)
        assertEquals(saved.summary, reopened.summary)
        assertEquals(InstrumentProfile.NATIVE_AMERICAN_FLUTE, reopened.instrument)
        assertEquals(design, SessionToolsStore(context).designId(id))
        assertEquals(1250L, SessionToolsStore(context).markers(id).single().timeMs)
        assertEquals(extras, context.getSharedPreferences("session_tools", 0).getString("extras", null))
        assertEquals(1500L, reopened.durationMs)
        assertEquals(44L + 12000L * 2, reopened.sizeBytes)
        assertEquals(0, RecordingStore(context).recoverPending()) // Already renamed to .wav.
    }

    @Test fun pendingRecoveryRepairsTornCheckpointOnceAndPreservesMalformedCapture() {
        val store = RecordingStore(context)
        val pcm = shortArrayOf(Short.MIN_VALUE, -1, 0, Short.MAX_VALUE)
        val expected = WavCodec.encode(pcm, pcm.size, 44100)
        val partial = store.newCapture()
        val torn = expected.copyOf(expected.size + 1)
        for (offset in 4..7) torn[offset] = 0xff.toByte()
        for (offset in 40..43) torn[offset] = 0
        torn[torn.lastIndex] = 99
        partial.writeBytes(torn)
        val malformed = store.newCapture()
        val invalid = expected.copyOf().apply { this[34] = 8 }
        malformed.writeBytes(invalid)
        val prefs = context.getSharedPreferences("local_data", 0)
        val index = prefs.getString("recordings", null)

        assertThrows(IllegalArgumentException::class.java) { store.importCapture(malformed, emptyList(), 456L) }
        assertEquals(index, prefs.getString("recordings", null))
        assertArrayEquals(invalid, malformed.readBytes())
        assertEquals(1, store.recoverPending())
        val recovered = store.file(partial.nameWithoutExtension)
        assertFalse(partial.exists())
        assertArrayEquals(expected, recovered.readBytes())
        repeat(2) {
            assertEquals(0, RecordingStore(context).recoverPending())
            assertArrayEquals(expected, recovered.readBytes())
            assertArrayEquals(invalid, malformed.readBytes())
        }
        val session = store.list().single()
        assertEquals(partial.nameWithoutExtension, session.id)
        assertTrue(session.recovered)
        assertTrue(session.timeline.isEmpty())
        assertNull(session.summary)
    }
}
