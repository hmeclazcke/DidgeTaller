package ar.didgetaller.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntSize
import ar.didgetaller.core.*
import kotlin.math.log10
import kotlin.math.max

internal class RasterInk(val bitmap: ImageBitmap, val placement: RasterPlacement)
internal class PlotContent(val raster: RasterInk?, val pitch: PitchInk?, val comparison: ComparisonInk?,
    val wave: WaveDrawing?, val spectrum: SpectrumInk?, val timeline: TimelineInk?)
internal class PlotEffects(val bridge: PathEffect?, val reference: PathEffect?, val comparison: PathEffect?,
    val harmonic: PathEffect?, val selection: PathEffect?)

/** Preserves the established layer order; the owner and sources are absent from this boundary. */
internal fun DrawScope.paintPlotContent(content: PlotContent, projection: PlotProjection, effects: PlotEffects,
    xTicks: List<Double>, yTicks: List<Double>, textSize: Float, measure: (String) -> Float,
    write: (String, Float, Float) -> Unit) {
    content.raster?.let { paintRaster(it) }
    paintGrid(projection, xTicks, yTicks)
    content.pitch?.let { paintPitch(it, projection, effects.bridge, effects.reference) }
    content.comparison?.let { paintComparison(it, effects.comparison) }
    paintWave(content.wave)
    content.spectrum?.let { paintSpectrum(it, projection.height.toFloat(), textSize, effects.harmonic, measure, write) }
    content.timeline?.let { paintTimeline(it, projection, effects.selection) }
}

private fun DrawScope.paintRaster(ink: RasterInk) {
    withTransform({
        translate(ink.placement.origin.x.toFloat(), ink.placement.origin.y.toFloat())
        scale((ink.placement.width / ink.bitmap.width).toFloat(), (ink.placement.height / ink.bitmap.height).toFloat(), pivot = Offset.Zero)
    }) {
        drawImage(ink.bitmap, dstSize = IntSize(ink.bitmap.width, ink.bitmap.height), filterQuality = FilterQuality.None)
    }
}

internal fun DrawScope.paintGrid(projection: PlotProjection, xTicks: List<Double>, yTicks: List<Double>) {
    val grid = Color(0xFF29414B)
    xTicks.forEach { tick ->
        val x = projection.offset(tick, projection.viewport.y.low).x
        drawLine(grid, Offset(x, 0f), Offset(x, projection.height.toFloat()), .7f)
    }
    yTicks.forEach { tick ->
        val y = projection.offset(projection.viewport.x.low, tick).y
        drawLine(grid, Offset(0f, y), Offset(projection.width.toFloat(), y), .7f)
    }
}

internal fun DrawScope.paintAudioLevel(pixels: FloatArray, left: Float, top: Float, stripHeight: Float) {
    for (index in pixels.indices) {
        val db = 20 * log10(max(1e-9f, pixels[index]))
        val height = ((db + 70) / 70).coerceIn(0f, 1f) * (stripHeight - 2)
        drawLine(Color(0xFF93CDE7), Offset(left + index, top - 2), Offset(left + index, top - 2 - height), 1f)
    }
}
