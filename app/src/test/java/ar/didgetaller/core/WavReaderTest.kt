package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavReaderTest {
    private fun withWav(bytes: ByteArray, block: (File) -> Unit) {
        val file = File.createTempFile("didge-analysis-", ".wav")
        try { file.writeBytes(bytes); block(file) } finally { file.delete() }
    }
    @Test fun readsOriginalEncoderAndArbitraryFramesWithoutChangingTheFile() {
        val bytes = WavCodec.encode(shortArrayOf(-32768, -100, 0, 100, 32767), 5, 48000)
        withWav(bytes) { file ->
            Pcm16WavReader(file).use { reader ->
                assertEquals(48000, reader.sampleRate); assertEquals(5L, reader.frameCount)
                val buffer = ShortArray(4)
                assertEquals(3, reader.read(2, buffer)); assertArrayEquals(shortArrayOf(0, 100, 32767, 0), buffer)
                assertEquals(0, reader.read(5, buffer)); assertArrayEquals(ShortArray(4), buffer)
            }
            assertArrayEquals(bytes, file.readBytes())
        }
    }
    @Test fun oddMetadataChunksAndStereoAreDecodedWithActualSampleRate() {
        val encoded = WavCodec.encode(shortArrayOf(1000, 3000, -2000, 2000), 4, 48000)
        val header = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(22, 2); header.putInt(28, 48000 * 4); header.putShort(32, 4)
        val junk = byteArrayOf(74, 85, 78, 75, 3, 0, 0, 0, 1, 2, 3, 0)
        val bytes = encoded.copyOfRange(0, 12) + junk + encoded.copyOfRange(12, encoded.size)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, bytes.size - 8)
        withWav(bytes) { file ->
            Pcm16WavReader(file).use { reader ->
                assertEquals(2, reader.channels); assertEquals(2L, reader.frameCount)
                val output = ShortArray(2); reader.read(0, output)
                assertArrayEquals(shortArrayOf(2000, 0), output)
            }
        }
    }
    @Test fun truncatedUnsupportedAndEmptyWavsFailExplicitly() {
        val good = WavCodec.encode(ShortArray(100), 100, 44100)
        val wrongCodec = good.copyOf().also { it[20] = 3 }
        val badAlign = good.copyOf().also { it[32] = 4 }
        for (bytes in listOf(good.copyOf(good.size - 2), wrongCodec, badAlign, ByteArray(12), WavCodec.encode(ShortArray(0), 0, 44100))) {
            withWav(bytes) { file -> assertThrows(IllegalArgumentException::class.java) { Pcm16WavReader(file).close() } }
        }
    }
}
