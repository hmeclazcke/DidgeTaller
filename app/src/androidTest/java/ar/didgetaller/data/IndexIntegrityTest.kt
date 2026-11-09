package ar.didgetaller.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.core.WavCodec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Every byte and preference belongs to this test's synthetic namespace. */
@RunWith(AndroidJUnit4::class)
class IndexIntegrityTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var namespace: String
    private lateinit var prefs: SharedPreferences
    private val wav = WavCodec.encode(shortArrayOf(10, -20, 30), 3, 8000)

    @Before fun isolate() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        namespace = "index-integrity-${UUID.randomUUID()}"
        directory = File(target.cacheDir, namespace).apply { check(mkdirs()) }
        context = object : ContextWrapper(target) {
            override fun getFilesDir() = directory
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                target.getSharedPreferences("$namespace-$name", mode)
        }
        prefs = context.getSharedPreferences("local_data", 0)
    }

    @After fun removeOwnedData() {
        if (!::context.isInitialized) return
        val target = (context as ContextWrapper).baseContext
        listOf("local_data", "session_tools").forEach { target.deleteSharedPreferences("$namespace-$it") }
        directory.deleteRecursively()
    }

    @Test fun corruptIndexCannotAuthorizeDeletionOrOverwrite() {
        val store = RecordingStore(context)
        val saved = store.save(wav)
        val damaged = "[{\"id\":\"${saved.id}\",\"name\":\"Important synthetic metadata\""
        check(prefs.edit().putString("recordings", damaged).commit())
        assertThrows(IllegalStateException::class.java) { store.delete(saved.id) }
        assertEquals(damaged, prefs.getString("recordings", null))
        assertArrayEquals(wav, store.file(saved.id).readBytes())
        assertThrows(IllegalStateException::class.java) { store.rename(saved.id, "Replacement") }
        assertThrows(IllegalStateException::class.java) { store.save(wav) }
        assertEquals(damaged, prefs.getString("recordings", null))
        assertEquals(1, File(directory, "recordings").listFiles()!!.count { it.extension == "wav" })
    }

    @Test fun deletingOneSessionPreservesUnrecognizedRows() {
        val store = RecordingStore(context)
        val saved = store.save(wav)
        val rows = JSONArray(prefs.getString("recordings", null))
            .put("future-format-entry").put(JSONObject().put("future", true))
        check(prefs.edit().putString("recordings", rows.toString()).commit())
        store.delete(saved.id)
        assertEquals("[\"future-format-entry\",{\"future\":true}]", prefs.getString("recordings", null))
    }

    @Test fun duplicateIdentifiersHaveOneVisibleSessionWithoutRewritingMetadata() {
        val store = RecordingStore(context)
        val saved = store.save(wav)
        val rows = JSONArray(prefs.getString("recordings", null))
        rows.put(JSONObject(rows.getJSONObject(0).toString()).put("name", "Duplicate"))
        val raw = rows.toString()
        check(prefs.edit().putString("recordings", raw).commit())
        assertEquals(listOf(saved.id), RecordingStore(context).list().map { it.id })
        assertEquals(raw, prefs.getString("recordings", null))
    }

    @Test fun damagedAndUnsupportedRootsStayReadOnlyUntilExplicitRecovery() {
        val store = RecordingStore(context)
        val saved = store.save(wav)
        val original = prefs.getString("recordings", null)
        for (broken in listOf("", " ", "[{", "not-json", "[] trailing", "{\"version\":999,\"sessions\":[]}")) {
            check(prefs.edit().putString("recordings", broken).commit())
            val reopened = RecordingStore(context)
            assertTrue(reopened.indexDamaged)
            assertEquals(listOf(saved.id), reopened.list().map { it.id })
            assertEquals(broken, prefs.getString("recordings", null))
            assertThrows(IllegalStateException::class.java) { reopened.delete(saved.id) }
            assertTrue(reopened.recoverIndex())
            assertEquals(original, prefs.getString("recordings", null))
            assertTrue(prefs.all.any { (key, value) -> key.startsWith("recordings.quarantine.") && value == broken })
            assertFalse(reopened.recoverIndex())
            assertArrayEquals(wav, store.file(saved.id).readBytes())
        }
    }

    @Test fun missingIndexAndEmptyArrayAreValidButMissingPreviouslyPublishedIndexIsNot() {
        val store = RecordingStore(context)
        assertFalse(store.indexDamaged)
        assertTrue(store.list().isEmpty())
        check(prefs.edit().putString("recordings", "[]").commit())
        assertFalse(store.indexDamaged)
        val saved = store.save(wav)
        check(prefs.edit().remove("recordings").commit())
        assertTrue(RecordingStore(context).indexDamaged)
        assertThrows(IllegalStateException::class.java) { store.delete(saved.id) }
        assertTrue(store.recoverIndex())
        assertEquals(saved.id, store.list().single().id)
    }

    @Test fun noBackupNeverCreatesEmptyAuthoritativeIndexAndOrphansRemainRecoverable() {
        val store = RecordingStore(context)
        val id = UUID.randomUUID().toString()
        store.file(id).writeBytes(wav)
        val missing = JSONObject().put("id", UUID.randomUUID().toString()).put("name", "Missing WAV")
        check(prefs.edit().putString("recordings", JSONArray().put(missing).toString()).commit())
        assertEquals(id, store.list().single().id)
        assertTrue(store.list().single().recovered)
        check(prefs.edit().putString("recordings", "broken").commit())
        assertThrows(IllegalStateException::class.java) { store.recoverIndex() }
        assertEquals("broken", prefs.getString("recordings", null))
        assertArrayEquals(wav, store.file(id).readBytes())
    }

    @Test fun invalidIdentifiersAndPointsCannotPublishOrDamageAudio() {
        val store = RecordingStore(context)
        val saved = store.save(wav)
        val raw = prefs.getString("recordings", null)
        assertThrows(IllegalArgumentException::class.java) { store.delete("../escape") }
        assertThrows(IllegalArgumentException::class.java) { store.rename("../escape", "Name") }
        val bad = listOf(ar.didgetaller.core.SessionAnalysis.Point(0, 440.0, Double.NaN))
        assertThrows(IllegalArgumentException::class.java) { store.save(wav, bad) }
        assertEquals(raw, prefs.getString("recordings", null))
        assertEquals(1, File(directory, "recordings").listFiles()!!.count { it.extension == "wav" })
        assertArrayEquals(wav, store.file(saved.id).readBytes())
    }

    @Test fun twoInstancesSaveAndReloadWithoutStaleIndexPublication() {
        val stores = listOf(RecordingStore(context), RecordingStore(context))
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val jobs = stores.map { store -> pool.submit<List<String>> {
                check(start.await(5, TimeUnit.SECONDS))
                (1..10).map {
                    val session = store.save(wav)
                    assertTrue(RecordingStore(context).list().any { it.id == session.id })
                    session.id
                }
            } }
            start.countDown()
            val ids = jobs.flatMap { it.get(30, TimeUnit.SECONDS) }.toSet()
            assertEquals(20, ids.size)
            assertEquals(ids, RecordingStore(context).list().map { it.id }.toSet())
            ids.forEach { assertArrayEquals(wav, stores[0].file(it).readBytes()) }
        } finally { pool.shutdownNow() }
    }

    @Test fun saveRenameAndRepeatedDeleteCannotOverwriteEachOthersChanges() {
        val first = RecordingStore(context)
        val second = RecordingStore(context)
        val removed = first.save(wav)
        val kept = first.save(wav)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val delete = pool.submit { second.delete(removed.id); second.delete(removed.id) }
            val write = pool.submit<String> { first.rename(kept.id, "Kept name"); first.save(wav).id }
            delete.get(15, TimeUnit.SECONDS)
            val added = write.get(15, TimeUnit.SECONDS)
            val sessions = RecordingStore(context).list()
            assertEquals(setOf(kept.id, added), sessions.map { it.id }.toSet())
            assertEquals("Kept name", sessions.single { it.id == kept.id }.name)
            assertFalse(first.file(removed.id).exists())
        } finally { pool.shutdownNow() }
    }

    @Test fun failedCommitCannotDeleteWavAndRetryUsesOriginalMetadata() {
        val store = RecordingStore(context)
        val saved = store.save(wav)
        val original = prefs.getString("recordings", null)
        val failing = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val editor = prefs.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit(): Boolean { editor.commit(); return false }
                }
            }
        }
        val failingContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = failing
        }
        assertThrows(IllegalStateException::class.java) { RecordingStore(failingContext).delete(saved.id) }
        assertEquals(original, prefs.getString("recordings", null))
        assertArrayEquals(wav, store.file(saved.id).readBytes())
        assertThrows(IllegalStateException::class.java) { RecordingStore(failingContext).save(wav) }
        assertEquals(original, prefs.getString("recordings", null))
        assertEquals(2, RecordingStore(context).list().size)
        val recovered = store.list().single { it.id != saved.id }
        assertTrue(recovered.recovered)
        assertArrayEquals(wav, store.file(recovered.id).readBytes())
        store.rename(saved.id, "Retry")
        assertEquals("Retry", RecordingStore(context).list().single { it.id == saved.id }.name)
    }

    @Test fun additiveMetadataAndTrashUseSharedWriterAndPreserveMalformedUnrelatedRows() {
        val stores = listOf(SessionToolsStore(context), SessionToolsStore(context))
        val id = UUID.randomUUID().toString()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val jobs = stores.map { store -> pool.submit { repeat(15) { store.addMarker(id, it.toLong(), "manual", "Synthetic") } } }
            jobs.forEach { it.get(15, TimeUnit.SECONDS) }
            assertEquals(30, SessionToolsStore(context).markers(id).size)
        } finally { pool.shutdownNow() }
        val extras = context.getSharedPreferences("session_tools", 0)
        val root = JSONObject(extras.getString("extras", null))
        val markers = root.getJSONObject(id).getJSONArray("markers")
        val removed = markers.getJSONObject(0).getString("id")
        markers.put("unknown marker")
        check(extras.edit().putString("extras", root.toString()).commit())
        stores[0].deleteMarker(id, removed)
        assertEquals(29, stores[1].markers(id).size)
        assertTrue(extras.getString("extras", "")!!.contains("unknown marker"))
        val trash = TrashStore(context)
        trash.put("session", id)
        check(prefs.edit().putString("trash_session", "broken").commit())
        assertEquals(setOf(id), TrashStore(context).ids("session"))
        assertThrows(IllegalStateException::class.java) { trash.restore("session", id) }
    }
}
