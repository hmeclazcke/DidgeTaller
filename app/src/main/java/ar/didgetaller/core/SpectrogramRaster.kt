package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.log10
import kotlin.math.max

/** Converts column-major spectral power into top-down ARGB pixels, without an Android dependency. */
object SpectrogramRaster {
    fun pixels(tile: AudioVisuals.Spectrogram, rangeDb: Double, reference: Double = 0.0,
               cancelled: () -> Boolean = { false }): IntArray {
        require(tile.columns > 0 && tile.rows > 0)
        require(tile.columns.toLong() * tile.rows == tile.power.size.toLong())
        require(rangeDb > 0)
        val pixels = IntArray(tile.power.size)
        val scale = reference.takeIf { it > 1e-16 } ?: tile.reference
        for (x in 0 until tile.columns) {
            if (cancelled()) throw CancellationException()
            for (y in 0 until tile.rows) {
                val db = if (scale > 1e-16) 10 * log10(max(1e-30, tile.power[x * tile.rows + y].toDouble()) / scale) else null
                pixels[(tile.rows - 1 - y) * tile.columns + x] = SpectralPalette.color(db, rangeDb)
            }
        }
        return pixels
    }
}
