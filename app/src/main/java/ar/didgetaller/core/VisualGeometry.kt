package ar.didgetaller.core

import kotlin.math.max

/** Prepared in plot-local pixels. Clipping remains the renderer's responsibility. */
sealed interface WaveGeometry {
    data class Samples(val points: List<PlotPoint>) : WaveGeometry
    data class Column(val x: Double, val top: Double, val bottom: Double)
    data class Envelope(val columns: List<Column>, val strokeWidth: Double) : WaveGeometry

    companion object {
        fun prepare(wave: AudioVisuals.Wave, projection: PlotProjection): WaveGeometry {
            require(wave.rate > 0 && wave.min.size == wave.max.size)
            val start = wave.start * 1000.0 / wave.rate
            val end = wave.end * 1000.0 / wave.rate
            if (wave.samples) return Samples(wave.min.mapIndexed { index, value ->
                projection.pixels(start + index * 1000.0 / wave.rate, value.toDouble())
            })
            if (wave.min.isEmpty()) return Envelope(emptyList(), 1.0)
            val columns = wave.min.indices.map { index ->
                val time = start + (index + .5) / wave.min.size * (end - start)
                val high = projection.pixels(time, wave.max[index].toDouble())
                val low = projection.pixels(time, wave.min[index].toDouble())
                Column(high.x, high.y, low.y)
            }
            return Envelope(columns, max(1.0, projection.width * (end - start) / projection.viewport.x.span / wave.min.size))
        }
    }
}

/** A completed tile keeps its source coordinates while a replacement is pending. */
data class RasterPlacement(val origin: PlotPoint, val width: Double, val height: Double) {
    companion object {
        fun of(tile: AudioVisuals.Spectrogram, projection: PlotProjection): RasterPlacement? {
            if (tile.logarithmic != projection.logY) return null
            val origin = projection.pixels(tile.start * 1000.0 / tile.rate, tile.highHz)
            val corner = projection.pixels(tile.end * 1000.0 / tile.rate, tile.lowHz)
            val width = corner.x - origin.x
            val height = corner.y - origin.y
            // Finite differences also imply finite endpoints; no temporary collection is needed.
            if (width <= 0 || height <= 0 || !width.isFinite() || !height.isFinite()) return null
            return RasterPlacement(origin, width, height)
        }
    }
}
