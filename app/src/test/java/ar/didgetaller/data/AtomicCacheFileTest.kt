package ar.didgetaller.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AtomicCacheFileTest {
    @get:Rule val folder = TemporaryFolder()
    private fun target() = File(folder.root, "derived.bin")

    @Test fun publishesACompleteNewValueAndRemovesItsTemporaryFile() {
        val destination = target()
        AtomicCacheFile.write(destination) { it.writeInt(123); it.writeUTF("complete") }
        destination.inputStream().use { input ->
            java.io.DataInputStream(input).use { assertEquals(123, it.readInt()); assertEquals("complete", it.readUTF()); assertEquals(-1, it.read()) }
        }
        assertEquals(listOf(destination), folder.root.listFiles()!!.toList())
    }

    @Test fun previousValueRemainsVisibleUntilReplacementIsComplete() {
        val destination = target().apply { writeText("previous") }
        AtomicCacheFile.write(destination) {
            it.writeBytes("replacement"); it.flush()
            assertEquals("previous", destination.readText())
        }
        assertEquals("replacement", destination.readText())
        assertEquals(listOf(destination), folder.root.listFiles()!!.toList())
    }

    @Test fun failedWriterPreservesThePreviousValueAndCleansOnlyItsOwnTemporaryFile() {
        val destination = target().apply { writeText("previous") }
        val independent = File(folder.root, "other-writer.tmp").apply { writeText("unrelated") }
        val expected = IOException("controlled failure")
        val actual = assertThrows(IOException::class.java) {
            AtomicCacheFile.write(destination) { it.writeBytes("incomplete"); it.flush(); throw expected }
        }
        assertSame(expected, actual)
        assertEquals("previous", destination.readText())
        assertEquals("unrelated", independent.readText())
        assertEquals(setOf(destination, independent), folder.root.listFiles()!!.toSet())
    }

    @Test fun cancelledWriterCannotCreateAnApparentlyCompleteCacheValue() {
        val destination = target()
        assertThrows(CancellationException::class.java) {
            AtomicCacheFile.write(destination) { it.writeInt(1); throw CancellationException() }
        }
        assertFalse(destination.exists())
        assertTrue(folder.root.listFiles()!!.isEmpty())
    }

    @Test fun failedPublicationPreservesTheDestinationAndCleansItsTemporaryFile() {
        val destination = target().apply { check(mkdir()); resolve("sentinel").writeText("untouched") }
        assertThrows(IOException::class.java) { AtomicCacheFile.write(destination) { it.writeInt(1) } }
        assertEquals("untouched", destination.resolve("sentinel").readText())
        assertEquals(listOf(destination), folder.root.listFiles()!!.toList())
    }

    @Test fun concurrentWritersCannotTruncateOrDeleteEachOthersUnpublishedValues() {
        val destination = target().apply { writeText("previous") }
        val firstOpened = CountDownLatch(1)
        val secondOpened = CountDownLatch(1)
        val firstPublished = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<String> {
                try {
                    AtomicCacheFile.write(destination) { out ->
                        out.writeBytes("first-"); out.flush(); firstOpened.countDown()
                        check(secondOpened.await(10, TimeUnit.SECONDS))
                        out.writeBytes("complete")
                    }
                    destination.readText()
                } finally { firstPublished.countDown() }
            }
            val second = executor.submit {
                check(firstOpened.await(10, TimeUnit.SECONDS))
                AtomicCacheFile.write(destination) { out ->
                    out.writeBytes("second-"); out.flush(); secondOpened.countDown()
                    check(firstPublished.await(10, TimeUnit.SECONDS))
                    out.writeBytes("complete")
                }
            }
            assertEquals("first-complete", first.get(15, TimeUnit.SECONDS))
            second.get(15, TimeUnit.SECONDS)
            assertEquals("second-complete", destination.readText())
            assertEquals(listOf(destination), folder.root.listFiles()!!.toList())
        } finally {
            firstOpened.countDown(); secondOpened.countDown(); firstPublished.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }
}
