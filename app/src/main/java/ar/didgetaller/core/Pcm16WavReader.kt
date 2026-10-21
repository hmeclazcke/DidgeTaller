package ar.didgetaller.core

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

/** Read-only, bounded-memory access. Frame means one sample per channel, not one byte. */
interface MonoPcmSource {
    val sampleRate: Int
    val frameCount: Long
    fun read(startFrame: Long, destination: ShortArray): Int
}

/** RIFF/WAVE PCM16 mono/stereo. Unknown chunks and odd-byte padding are respected. */
class Pcm16WavReader(file: File) : MonoPcmSource, Closeable {
    private val input = RandomAccessFile(file, "r")
    override val sampleRate: Int
    override val frameCount: Long
    val channels: Int
    private val dataOffset: Long
    private var buffer = ByteArray(0)

    init {
        try {
            require(input.length() >= 12 && ascii() == "RIFF") { "No es un WAV RIFF válido." }
            val riffEnd = 8 + uint32()
            require(riffEnd in 12..input.length() && ascii() == "WAVE") { "WAV incompleto o dañado." }
            var rate = 0; var channelCount = 0; var offset = -1L; var dataBytes = 0L
            while (input.filePointer + 8 <= riffEnd) {
                val tag = ascii(); val size = uint32(); val start = input.filePointer
                require(size <= riffEnd - start) { "El WAV tiene un bloque truncado." }
                when (tag) {
                    "fmt " -> {
                        require(rate == 0 && size >= 16) { "Formato WAV ambiguo o incompleto." }
                        val codec = uint16(); channelCount = uint16(); val rawRate = uint32()
                        val byteRate = uint32(); val alignment = uint16(); val bits = uint16()
                        require(codec == 1 && bits == 16 && channelCount in 1..2) {
                            "Se admite WAV PCM de 16 bits, mono o estéreo."
                        }
                        require(rawRate in 8000L..192000L && alignment == channelCount * 2 && byteRate == rawRate * alignment) {
                            "Frecuencia de muestreo o alineación WAV inválida."
                        }
                        rate = rawRate.toInt()
                    }
                    "data" -> {
                        require(offset == -1L) { "WAV con varios bloques de audio no admitido." }
                        offset = start; dataBytes = size
                    }
                }
                val next = start + size + size % 2
                require(next <= riffEnd) { "Falta el relleno de un bloque WAV." }
                input.seek(next)
            }
            require(rate > 0 && offset >= 0 && dataBytes > 0 && dataBytes % (channelCount * 2) == 0L) {
                "El WAV no contiene audio PCM completo."
            }
            sampleRate = rate; channels = channelCount; dataOffset = offset
            frameCount = dataBytes / (channels * 2)
        } catch (error: Exception) { input.close(); throw error }
    }

    override fun read(startFrame: Long, destination: ShortArray): Int {
        require(startFrame in 0..frameCount)
        val count = min(destination.size.toLong(), frameCount - startFrame).toInt()
        val bytes = count * channels * 2
        if (buffer.size < bytes) buffer = ByteArray(bytes)
        input.seek(dataOffset + startFrame * channels * 2)
        input.readFully(buffer, 0, bytes)
        for (i in 0 until count) {
            var sum = 0
            repeat(channels) { ch ->
                val at = (i * channels + ch) * 2
                sum += ((buffer[at].toInt() and 255) or (buffer[at + 1].toInt() shl 8)).toShort().toInt()
            }
            destination[i] = (sum / channels).toShort()
        }
        destination.fill(0, count)
        return count
    }
    private fun ascii() = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
    private fun uint16() = input.readUnsignedByte() or (input.readUnsignedByte() shl 8)
    private fun uint32(): Long = uint16().toLong() or (uint16().toLong() shl 16)
    override fun close() = input.close()
}
