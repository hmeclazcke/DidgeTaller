package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WavFinalizationTest {
    @get:Rule val temporary = TemporaryFolder()

    private val pcm = shortArrayOf(Short.MIN_VALUE, -1234, 0, 1234, Short.MAX_VALUE)

    private fun assertPcm(file: java.io.File, expected: ShortArray) {
        Pcm16WavReader(file).use { reader ->
            assertEquals(8000, reader.sampleRate)
            assertEquals(expected.size.toLong(), reader.frameCount)
            val decoded = ShortArray(expected.size)
            assertEquals(expected.size, reader.read(0, decoded))
            assertArrayEquals(expected, decoded)
        }
    }

    @Test fun repeatedClosePreservesExactWavAndRejectsFurtherWrites() {
        val file = temporary.newFile("closed.part")
        val writer = ProgressiveWavWriter(file, 8000)
        try {
            writer.append(pcm, pcm.size)
            writer.close()
            val finalized = file.readBytes()
            writer.close()
            assertThrows(IllegalStateException::class.java) { writer.append(shortArrayOf(99), 1) }
            assertEquals(pcm.size.toLong(), writer.samplesWritten)
            assertArrayEquals(finalized, file.readBytes())
            assertArrayEquals(WavCodec.encode(pcm, pcm.size, 8000), finalized)
            assertPcm(file, pcm)
        } finally { writer.close() }
    }

    @Test fun initializationFailuresPreserveExistingAudioAndLeaveFreshFilesReusable() {
        val existing = temporary.newFile("existing.part")
        val original = WavCodec.encode(pcm, pcm.size, 8000)
        existing.writeBytes(original)
        assertThrows(IllegalStateException::class.java) { ProgressiveWavWriter(existing, 8000) }
        assertArrayEquals(original, existing.readBytes())

        val invalidRate = temporary.newFile("invalid-rate.part")
        assertThrows(IllegalArgumentException::class.java) { ProgressiveWavWriter(invalidRate, 0) }
        val insufficientSpace = temporary.newFile("no-space.part")
        assertThrows(IllegalStateException::class.java) {
            ProgressiveWavWriter(insufficientSpace, 8000, freeBytes = { 0L })
        }
        for (file in listOf(invalidRate, insufficientSpace)) {
            assertEquals(0L, file.length())
            ProgressiveWavWriter(file, 8000).use { it.append(pcm, pcm.size) }
            assertPcm(file, pcm)
        }
    }

    @Test fun rejectedAppendPreservesAcceptedSamplesAndCanResumeWithoutRejectedPcm() {
        val file = temporary.newFile("space-retry.part")
        var available = 10_000_000L
        ProgressiveWavWriter(file, 8000, freeBytes = { available }).use { writer ->
            writer.append(pcm, pcm.size)
            val beforeFailure = file.readBytes()
            available = 0L
            assertThrows(IllegalStateException::class.java) { writer.append(shortArrayOf(999), 1) }
            assertEquals(pcm.size.toLong(), writer.samplesWritten)
            assertArrayEquals(beforeFailure, file.readBytes())
            available = 10_000_000L
            writer.append(shortArrayOf(-77), 1)
        }
        assertPcm(file, pcm + shortArrayOf(-77))
        assertArrayEquals(WavCodec.encode(pcm + shortArrayOf(-77), pcm.size + 1, 8000), file.readBytes())
    }

    @Test fun unfinalizedSnapshotRecoversOnlyCompleteFramesAndRepairIsRepeatable() {
        val active = temporary.newFile("active.part")
        val snapshot = temporary.newFile("interrupted.part")
        ProgressiveWavWriter(active, 8000).use { writer ->
            writer.append(pcm, pcm.size) // Below the one-second header checkpoint.
            snapshot.writeBytes(active.readBytes() + byteArrayOf(7)) // Torn trailing PCM frame.
        }
        assertThrows(IllegalArgumentException::class.java) { Pcm16WavReader(snapshot).close() }
        assertTrue(ProgressiveWavWriter.repair(snapshot))
        assertPcm(snapshot, pcm)
        val repaired = snapshot.readBytes()
        assertArrayEquals(WavCodec.encode(pcm, pcm.size, 8000), repaired)
        assertTrue(ProgressiveWavWriter.repair(snapshot))
        assertArrayEquals(repaired, snapshot.readBytes())
        assertArrayEquals(WavCodec.encode(pcm, pcm.size, 8000), active.readBytes())
    }
}
