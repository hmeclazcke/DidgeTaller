package ar.didgetaller.data

import android.content.ContextWrapper
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Deterministic interleaving of two real derived-cache writers; no audio or historic data. */
@RunWith(AndroidJUnit4::class)
class AnalysisCacheConcurrencyTest {
    @Test fun concurrentWritersPublishCompleteValuesUsingIndependentTemporaryFiles() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val owned = File(targetContext.cacheDir, "cache-concurrency-${UUID.randomUUID()}").apply { check(mkdir()) }
        val context = object : ContextWrapper(targetContext) {
            override fun getFilesDir(): File = owned
        }
        val cache = AnalysisCache(context)
        val target = File(owned, "analysis-cache/result.bin").apply { writeBytes(value(7, 77)) }
        val write = cache.javaClass.getDeclaredMethod("write", File::class.java, kotlin.jvm.functions.Function1::class.java)
            .apply { isAccessible = true }
        val firstOpened = CountDownLatch(1)
        val secondOpened = CountDownLatch(1)
        val firstPublished = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<ByteArray> {
                try {
                    val block: (DataOutputStream) -> Unit = { out ->
                        out.writeInt(1); out.flush(); firstOpened.countDown()
                        check(secondOpened.await(10, TimeUnit.SECONDS))
                        out.writeInt(11)
                    }
                    write.invoke(cache, target, block)
                    target.readBytes()
                } finally { firstPublished.countDown() }
            }
            val second = executor.submit {
                check(firstOpened.await(10, TimeUnit.SECONDS))
                val block: (DataOutputStream) -> Unit = { out ->
                    out.writeInt(2); out.flush(); secondOpened.countDown()
                    check(firstPublished.await(10, TimeUnit.SECONDS))
                    out.writeInt(22)
                }
                write.invoke(cache, target, block)
            }
            assertArrayEquals("First publication must contain only its own complete value", value(1, 11), first.get(15, TimeUnit.SECONDS))
            second.get(15, TimeUnit.SECONDS)
            assertArrayEquals(value(2, 22), target.readBytes())
            assertEquals(listOf("result.bin"), target.parentFile!!.listFiles()!!.map { it.name })
        } finally {
            firstOpened.countDown(); secondOpened.countDown(); firstPublished.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
            owned.deleteRecursively()
        }
    }

    private fun value(first: Int, second: Int): ByteArray = ByteBuffer.allocate(8).putInt(first).putInt(second).array()
}
