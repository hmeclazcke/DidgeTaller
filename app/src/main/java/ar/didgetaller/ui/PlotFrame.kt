package ar.didgetaller.ui

import android.graphics.Paint
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.didgetaller.core.*
import kotlin.math.max

/** One immutable layout frame is shared by painting, accessibility and gesture mapping. */
internal class PlotFrame(val camera: PlotViewport, val log: Boolean, val left: Float, val right: Float,
    val top: Float, val bottom: Float, val levelHeight: Float, val textSize: Float, val paint: Paint,
    val pitchTicks: List<PitchAxis.Tick>, val yTicks: List<Double>, val xTicks: List<Double>,
    val xLabels: List<PlacedAxisLabel>, val axisText: List<AxisDrawing.Text>,
    val plotWidth: Float, val projection: PlotProjection)

private class VerticalInsets(val top: Float, val bottom: Float, val levelHeight: Float)

@Composable
private fun verticalInsets(plot: AcousticPlot, level: Boolean, textSize: Float): VerticalInsets {
    val density = LocalDensity.current
    val levelHeight = if (level) with(density) { 16.dp.toPx() } else 0f
    val top = max(with(density) { (if (plot == AcousticPlot.SPECTROGRAM) 16.dp else 10.dp).toPx() },
        textSize * (if (plot == AcousticPlot.SPECTROGRAM) 1.6f else 1f)) + levelHeight
    return VerticalInsets(top, textSize * 1.8f, levelHeight)
}

@Composable
private fun settledPlotLeft(required: Float, plot: AcousticPlot, textSize: Float, active: Boolean): Float {
    var settled by remember(plot, textSize) { mutableFloatStateOf(required) }
    // Width-changing labels must not move the origin under the user's active finger.
    SideEffect { if (!active) settled = required }
    return if (active) settled else required
}

@Composable
internal fun rememberPlotFrame(plot: AcousticPlot, camera: PlotViewport, dimensions: IntSize,
    active: Boolean, level: Boolean, spectral: Boolean, logarithmic: Boolean): PlotFrame? {
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales.toLanguageTags()
    val textSize = with(density) { 11.sp.toPx() }
    val right = with(density) { 12.dp.toPx() }; val gap = with(density) { 4.dp.toPx() }
    val paint = remember(textSize) { Paint().apply { isAntiAlias = true; color = android.graphics.Color.rgb(185,208,211); this.textSize = textSize } }
    val insets = verticalInsets(plot, level, textSize)
    val log = plot == AcousticPlot.SPECTROGRAM && logarithmic
    val ticks = remember(camera.y, dimensions.height, insets.top, insets.bottom, textSize, plot, spectral, log) {
        verticalPlotTicks(plot == AcousticPlot.PITCH && !spectral, camera.y,
            dimensions.height - insets.top - insets.bottom, textSize, log)
    }
    val fallback = with(density) { (if (plot == AcousticPlot.SPECTRUM) 58.dp else 56.dp).toPx() }
    val required = remember(plot, ticks, camera.y.span, textSize, gap, fallback, locale) {
        plotLeftMargin(plot in listOf(AcousticPlot.PITCH, AcousticPlot.SPECTROGRAM), ticks.musical, ticks.numeric,
            camera.y.span, textSize, gap, fallback, paint::measureText)
    }
    val left = settledPlotLeft(required, plot, textSize, active)
    return rememberFrameLabels(plot, camera, dimensions, log, left, right, insets, textSize, gap, paint, ticks, locale)
}

/** Label fitting and projection use exactly the same measured rectangle. */
@Composable
private fun rememberFrameLabels(plot: AcousticPlot, camera: PlotViewport, dimensions: IntSize, log: Boolean,
    left: Float, right: Float, insets: VerticalInsets, textSize: Float, gap: Float, paint: Paint,
    ticks: VerticalPlotTicks, locale: String): PlotFrame? {
    val width = (dimensions.width - left - right).coerceAtLeast(0f)
    val height = (dimensions.height - insets.top - insets.bottom).coerceAtLeast(0f)
    if (width <= textSize || height <= textSize * 2) return null
    val projection = remember(camera, width, height, log) {
        PlotProjection(camera, width.toDouble(), height.toDouble(), logY = log)
    }
    val xTicks = PlotScales.ticks(camera.x, if (width / textSize < 30) 3 else 5)
    val xLabels = placeAxisLabels(xTicks, camera.x, width, gap * 2,
        { if (plot.temporal) seconds(it.toLong()) else axisLabel(it, camera.x.span) + " Hz" }, paint::measureText)
    val axisText = remember(ticks, projection, textSize, gap, locale, xLabels) {
        AxisDrawing.vertical(ticks.numeric, ticks.musical, projection, textSize, gap) { axisLabel(it, camera.y.span) } +
                AxisDrawing.horizontal(xLabels.map { it.text to it.center }, height, textSize)
    }
    return PlotFrame(camera, log, left, right, insets.top, insets.bottom, insets.levelHeight, textSize, paint,
        ticks.musical, ticks.numeric, xTicks, xLabels, axisText, width, projection)
}
