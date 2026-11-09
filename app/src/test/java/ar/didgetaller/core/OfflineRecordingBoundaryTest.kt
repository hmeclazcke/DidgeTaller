package ar.didgetaller.core

import java.math.BigInteger
import org.junit.Assert.*
import org.junit.Test

class OfflineRecordingBoundaryTest {
    private class SparseSource : MonoPcmSource {
        override val sampleRate = 44100
        override val frameCount = Long.MAX_VALUE
        var reads = 0
        override fun read(startFrame: Long, destination: ShortArray): Int {
            reads++; destination.fill(0)
            return minOf(destination.size.toLong(), frameCount - startFrame).toInt()
        }
    }
    private val settings = WavAnalysis.Settings(4096, 2048)
    @Test fun overflowedWindowBoundsAreRejectedBeforeReadingAnything() {
        val source = SparseSource()
        assertThrows(IllegalArgumentException::class.java) { WavAnalysis.analyze(source, Long.MAX_VALUE, 0, settings) }
        assertEquals(0, source.reads)
    }
    @Test fun absoluteFrameTimesDoNotOverflowBeforeMillisecondDivision() {
        val source = SparseSource(); val start = Long.MAX_VALUE - 10000
        val result = WavAnalysis.analyze(source, start, Long.MAX_VALUE - 211, settings)
        val expected = BigInteger.valueOf(start + settings.fftSize / 2).multiply(BigInteger.valueOf(1000))
            .divide(BigInteger.valueOf(source.sampleRate.toLong())).longValueExact()
        assertEquals(expected, result.timeline.first().timeMs)
        assertTrue(result.timeline.zipWithNext().all { (a, b) -> a.timeMs < b.timeMs })
    }
    @Test fun weightedSpectrumPreservesPartialWindowsAndCannotBeRenormalizedOrMutatedAfterPublication() {
        val first = ShortArray(4096) { (it % 41 * 100).toShort() }
        val second = ShortArray(4096) { (it % 37 * 200).toShort() }
        val source = SpectralAnalysis(4096)
        val a = source.frame(first, 44100, 4096).density
        val b = source.frame(second, 44100, 2048).density
        val average = OfflineSpectrum(44100, 4096)
        assertThrows(IllegalStateException::class.java) { average.finish() }
        average.append(first, 4096); average.append(second, 2048)
        val result = average.finish()
        assertEquals(2, average.windows)
        assertArrayEquals(DoubleArray(a.size) { (a[it] + b[it] * .5) / 1.5 }, result.density, 0.0)
        assertSame(result, average.finish())
        assertThrows(IllegalStateException::class.java) { average.append(first, 4096) }
    }
}
