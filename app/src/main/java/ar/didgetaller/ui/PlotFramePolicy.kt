package ar.didgetaller.ui

import ar.didgetaller.core.*
import kotlin.math.max

internal class VerticalPlotTicks(val musical: List<PitchAxis.Tick>, val numeric: List<Double>)

/** Pure tick policy distinguishes supported components from musical-pitch annotations. */
internal fun verticalPlotTicks(musical: Boolean, window: AxisWindow, height: Float,
                              textSize: Float, logarithmic: Boolean): VerticalPlotTicks {
    if (musical) {
        val pitch = PitchAxis.ticks(window, (height / (textSize * 2.8f)).toInt())
        return VerticalPlotTicks(pitch, pitch.map { it.hz })
    }
    return VerticalPlotTicks(emptyList(), PlotScales.ticks(window, if (height / textSize < 12) 3 else 5, logarithmic))
}

internal fun plotLeftMargin(measured: Boolean, musical: List<PitchAxis.Tick>, ticks: List<Double>,
    span: Double, textSize: Float, gap: Float, fallback: Float, measure: (String) -> Float): Float {
    if (!measured) return max(fallback, textSize * 3.2f)
    val hz = ticks.maxOfOrNull { measure(axisLabel(it, span)) } ?: 0f
    val notes = musical.maxOfOrNull { measure((if (it.approximate) "≈ " else "") + it.note.orEmpty()) * .85f } ?: 0f
    return max(hz, notes) + gap * 2
}


/** Stable public semantic identifiers are shared by unmeasured and ready surfaces. */
internal fun plotSurfaceTag(plot: AcousticPlot, immersive: Boolean, diagnostic: Boolean): String = when {
    diagnostic -> "diagnostic-time-chart"
    immersive -> "immersive-plot"
    plot == AcousticPlot.SPECTRUM -> "spectrum-chart"
    else -> "time-chart"
}
