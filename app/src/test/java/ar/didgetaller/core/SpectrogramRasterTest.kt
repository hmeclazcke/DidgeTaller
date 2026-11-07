package ar.didgetaller.core

import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class SpectrogramRasterTest {
    private fun tile(power: FloatArray = floatArrayOf(10000f, 100f, 1f, 100f, 10000f, 0f), reference: Double = 10000.0) =
        AudioVisuals.Spectrogram(0, 44100, 44100, 0.0, 500.0, false, 2, 3, power, reference, 32768, 16384)

    private val purple = 0xFF440154.toInt()
    private val teal = 0xFF21918C.toInt()
    private val yellow = 0xFFFDE725.toInt()
    private val unavailable = 0xFF101C25.toInt()

    @Test fun knownPowersKeepFrequencyUpwardAndTimeLeftToRight() {
        assertArrayEquals(intArrayOf(purple, purple, teal, yellow, yellow, teal), SpectrogramRaster.pixels(tile(), 40.0))
    }

    @Test fun aSharedReferenceChangesBrightnessWithoutChangingTheMeasurement() {
        val tile = tile(); val original = tile.power.copyOf()
        assertArrayEquals(intArrayOf(teal, purple, yellow, yellow, yellow, yellow), SpectrogramRaster.pixels(tile, 40.0, 100.0))
        assertArrayEquals(original, tile.power, 0f)
        assertEquals(10000.0, tile.reference, 0.0)
    }

    @Test fun changingDynamicRangeChangesOnlyThePaletteMapping() {
        val tile = tile()
        assertArrayEquals(intArrayOf(purple, purple, purple, yellow, yellow, purple), SpectrogramRaster.pixels(tile, 20.0))
    }

    @Test fun missingReferenceProducesUnavailablePixelsAndAnExplicitReferenceCanRestoreThem() {
        val tile = tile(reference = 0.0)
        assertArrayEquals(IntArray(6) { unavailable }, SpectrogramRaster.pixels(tile, 40.0))
        assertArrayEquals(IntArray(6) { unavailable }, SpectrogramRaster.pixels(tile(reference = 1e-16), 40.0))
        assertArrayEquals(intArrayOf(purple, purple, teal, yellow, yellow, teal), SpectrogramRaster.pixels(tile, 40.0, 10000.0))
    }

    @Test fun invalidOverridesFallBackToTheTileAndNonfinitePowerStaysUnavailable() {
        val tile = tile()
        val expected = SpectrogramRaster.pixels(tile, 40.0)
        for (value in listOf(-1.0, 0.0, 1e-16, Double.NaN)) assertArrayEquals(expected, SpectrogramRaster.pixels(tile, 40.0, value))
        assertArrayEquals(IntArray(6) { unavailable }, SpectrogramRaster.pixels(tile(FloatArray(6) { Float.NaN }), 40.0))
    }

    @Test fun cancellationStopsObsoleteRasterWorkBeforeItCanBePublished() {
        var checks = 0
        assertThrows(CancellationException::class.java) { SpectrogramRaster.pixels(tile(), 40.0) { ++checks == 2 } }
        assertEquals(2, checks)
        assertThrows(CancellationException::class.java) { SpectrogramRaster.pixels(tile(), 40.0) { true } }
    }

    @Test fun invalidMatricesAndRangesAreRejectedAtTheRenderingBoundary() {
        for (invalid in listOf(tile().copy(columns = 0), tile().copy(rows = -1), tile().copy(columns = Int.MAX_VALUE),
            tile(FloatArray(5)), tile(FloatArray(0)).copy(columns = 0), tile(FloatArray(0)).copy(rows = 0),
            tile().copy(columns = -2, rows = -3))) {
            assertThrows(IllegalArgumentException::class.java) { SpectrogramRaster.pixels(invalid, 40.0) }
        }
        for (range in listOf(0.0, -1.0, Double.NaN)) {
            assertThrows(IllegalArgumentException::class.java) {
                SpectrogramRaster.pixels(tile(), range) { error("Validate range before starting cancellable work") }
            }
        }
    }
}
