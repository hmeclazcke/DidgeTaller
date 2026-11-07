package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class RecordingCaptureTest {
    @get:Rule val folder = TemporaryFolder()
    private fun partial() = File(folder.root, "${UUID.randomUUID()}.part")
    private fun begin(file: File = partial(), rate: Int = 44100, seconds: Int = 900) =
        RecordingCapture().also { assertTrue(it.start(file, rate, seconds)) }

    @Test fun retiringMonitorCannotCloseOrAppendToItsSuccessor() {
        val monitor = RecordingCapture()
        val file = partial()
        val next = begin(file)
        assertNull(monitor.finish(true))
        assertFalse(monitor.append(shortArrayOf(99), 1))
        assertFalse(next.append(shortArrayOf(10, 20), 2))
        assertEquals(RecordingCapture.Outcome.Ready(file), next.finish(true))
        assertArrayEquals(WavCodec.encode(shortArrayOf(10, 20), 2, 44100), file.readBytes())
    }

    @Test fun twoRetiringRecordingsFinalizeOnlyTheirOwnBytes() {
        val old = partial()
        val newer = partial()
        val first = begin(old)
        val second = begin(newer)
        first.append(shortArrayOf(1), 1)
        second.append(shortArrayOf(2), 1)
        assertEquals(RecordingCapture.Outcome.Ready(old), first.finish(true))
        second.append(shortArrayOf(3), 1)
        assertEquals(RecordingCapture.Outcome.Ready(newer), second.finish(true))
        assertArrayEquals(WavCodec.encode(shortArrayOf(1), 1, 44100), old.readBytes())
        assertArrayEquals(WavCodec.encode(shortArrayOf(2, 3), 2, 44100), newer.readBytes())
    }

    @Test fun durationLimitTruncatesTheLastChunkAtTheExactSample() {
        val file = partial()
        val capture = begin(file, 8000, 1)
        assertFalse(capture.append(ShortArray(7999) { 3 }, 7999))
        assertTrue(capture.append(shortArrayOf(4, 5), 2))
        assertTrue(capture.append(shortArrayOf(6), 1))
        capture.finish(true)
        val expected = ShortArray(8000) { if (it == 7999) 4 else 3 }
        assertArrayEquals(WavCodec.encode(expected, expected.size, 8000), file.readBytes())
    }

    @Test fun configuredDurationUsesSecondsRatherThanOneFixedSecond() {
        val file = partial()
        val capture = begin(file, 8000, 2)
        assertFalse(capture.append(ShortArray(8000) { 1 }, 8000))
        assertTrue(capture.append(ShortArray(8001) { 2 }, 8001))
        capture.finish(true)
        val expected = ShortArray(16000) { if (it < 8000) 1 else 2 }
        assertArrayEquals(WavCodec.encode(expected, expected.size, 8000), file.readBytes())
    }

    @Test fun emptyChunksAreValidAndCannotAdvanceTheRecording() {
        val capture = RecordingCapture()
        assertFalse(capture.append(shortArrayOf(), 0))
        val file = partial()
        assertTrue(capture.start(file, 44100, 1))
        assertFalse(capture.append(shortArrayOf(), 0))
        assertEquals(RecordingCapture.Outcome.Ready(file), capture.finish(true))
        assertArrayEquals(WavCodec.encode(shortArrayOf(), 0, 44100), file.readBytes())
    }

    @Test fun completionAndDiscardAreIdempotent() {
        val file = partial()
        val capture = begin(file)
        capture.append(shortArrayOf(1), 1)
        assertEquals(RecordingCapture.Outcome.Ready(file), capture.finish(true))
        assertNull(capture.finish(true))
        assertFalse(capture.discard())
        assertFalse(capture.start(partial(), 44100, 1))
        assertFalse(capture.append(shortArrayOf(9), 1))
        assertArrayEquals(WavCodec.encode(shortArrayOf(1), 1, 44100), file.readBytes())
    }

    @Test fun recordingCannotReplaceAnOwnedWriter() {
        val file = partial()
        val capture = begin(file)
        val other = partial()
        assertFalse(capture.start(other, 44100, 1))
        assertFalse(other.exists())
        assertEquals(RecordingCapture.Outcome.Ready(file), capture.finish(true))
    }

    @Test fun discardingClaimsOnlyTheOwnedPartialAndNeverAuthorizesSave() {
        val file = partial()
        val other = partial().apply { writeText("saved bytes") }
        val capture = begin(file)
        capture.append(shortArrayOf(1), 1)
        assertTrue(capture.discard())
        val claimed = File(file.parentFile, "${file.nameWithoutExtension}.discard").exists()
        // Windows can reject renaming an open RandomAccessFile. This is a failed discard,
        // never permission to save; Android's successful rename is exercised on the emulator.
        assertEquals(!claimed, file.exists())
        assertFalse(capture.discard())
        assertEquals(RecordingCapture.Outcome.Discarded(claimed), capture.finish(true))
        assertNull(capture.finish(true))
        assertFalse(File(file.parentFile, "${file.nameWithoutExtension}.discard").exists())
        assertEquals(!claimed, file.exists())
        if (!claimed) assertArrayEquals(WavCodec.encode(shortArrayOf(1), 1, 44100), file.readBytes())
        assertEquals("saved bytes", other.readText())
    }

    @Test fun discardCollisionPreservesTheOtherFileAndReportsFailure() {
        val file = partial()
        val capture = begin(file)
        capture.append(shortArrayOf(7), 1)
        val blocker = File(file.parentFile, "${file.nameWithoutExtension}.discard").apply { writeText("sentinel") }
        assertTrue(capture.discard())
        assertEquals(RecordingCapture.Outcome.Discarded(false), capture.finish(true))
        assertEquals("sentinel", blocker.readText())
        assertArrayEquals(WavCodec.encode(shortArrayOf(7), 1, 44100), file.readBytes())
    }

    @Test fun nonPartialOrNonUuidFileCannotBeDeletedByDiscard() {
        for (name in listOf("${UUID.randomUUID()}.wav", "not-a-uuid.part", "ABCDEFAB-CDEF-ABCD-EFAB-CDEFABCDEFAB.part")) {
            val file = File(folder.root, name)
            val capture = begin(file)
            assertTrue(capture.discard())
            assertEquals(RecordingCapture.Outcome.Discarded(false), capture.finish(true))
            assertTrue(file.exists())
        }
    }

    @Test fun failedMicrophoneReleaseCannotReportSuccessfulDiscard() {
        val file = partial()
        val capture = begin(file)
        assertTrue(capture.discard())
        val claimed = !file.exists()
        assertEquals(RecordingCapture.Outcome.Discarded(false), capture.finish(false))
        assertEquals(!claimed, file.exists())
    }

    @Test fun failedCloseNeverHandsOffThePartialForSaving() {
        val file = partial()
        val writer = ProgressiveWavWriter(file)
        writer.append(shortArrayOf(10), 1)
        val capture = RecordingCapture { _, _ -> writer }
        assertTrue(capture.start(file, 44100, 1))
        val original = file.readBytes()
        val output = ProgressiveWavWriter::class.java.getDeclaredField("output").apply { isAccessible = true }
        (output.get(writer) as RandomAccessFile).close()
        assertTrue(capture.finish(true) is RecordingCapture.Outcome.Failed)
        assertNull(capture.finish(true))
        assertArrayEquals(original, file.readBytes())
    }

    @Test fun failedOpenLeavesMonitoringUsableWithoutReplacingFiles() {
        val existing = partial().apply { writeText("owned sentinel") }
        val capture = RecordingCapture()
        assertThrows(IllegalStateException::class.java) { capture.start(existing, 44100, 1) }
        assertEquals("owned sentinel", existing.readText())
        val next = partial()
        assertTrue(capture.start(next, 44100, 1))
        assertEquals(RecordingCapture.Outcome.Ready(next), capture.finish(true))
    }

    @Test fun invalidCountsAndDurationAreRejectedBeforeCreatingAFile() {
        val capture = RecordingCapture()
        val file = partial()
        for ((rate, seconds) in listOf(0 to 1, -1 to 1, 44100 to 0, 44100 to -1)) {
            assertThrows(IllegalArgumentException::class.java) { capture.start(file, rate, seconds) }
            assertFalse(file.exists())
        }
        for (count in listOf(-1, 2)) {
            assertThrows(IllegalArgumentException::class.java) { capture.append(shortArrayOf(1), count) }
        }
        assertFalse(capture.discard())
        assertFalse(capture.append(shortArrayOf(1), 1))
        assertNull(capture.finish(true))
    }

    @Test fun simultaneousSaveAndDiscardHaveExactlyOneTerminalOutcome() {
        repeat(30) {
            val file = partial()
            val capture = begin(file)
            capture.append(shortArrayOf(1, 2, 3), 3)
            val start = CountDownLatch(1)
            val outcome = AtomicReference<RecordingCapture.Outcome?>()
            val saving = Thread { start.await(); outcome.set(capture.finish(true)) }
            val cancelling = Thread { start.await(); capture.discard() }
            saving.start(); cancelling.start(); start.countDown(); saving.join(); cancelling.join()
            assertNull(capture.finish(true))
            when (val result = outcome.get()) {
                is RecordingCapture.Outcome.Ready -> {
                    assertEquals(file, result.file)
                    assertArrayEquals(WavCodec.encode(shortArrayOf(1, 2, 3), 3, 44100), file.readBytes())
                }
                is RecordingCapture.Outcome.Discarded -> {
                    assertEquals(result.success, !file.exists())
                    if (!result.success) assertArrayEquals(WavCodec.encode(shortArrayOf(1, 2, 3), 3, 44100), file.readBytes())
                }
                else -> fail("Finalization must have exactly one save/discard outcome: $result")
            }
        }
    }
}
