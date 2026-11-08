package ar.didgetaller.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import ar.didgetaller.R
import ar.didgetaller.core.*

internal class PlotAccessibility(val area: Rect, val viewport: PlotViewport, val temporal: Boolean,
    val cursor: Long, val duration: Long, val trace: TraceEvidence, val swipe: Int, val level: Int,
    val labels: List<String>, val labelBounds: List<Rect>)

/** Accessibility owns an Android semantic action, sharing the same timeline callback as touch. */
@Composable
internal fun Modifier.plotAccessibility(model: PlotAccessibility, description: String,
                              onScrub: (Long, Boolean) -> Unit): Modifier {
    val seek = stringResource(R.string.playback_seek_label)
    val scrub by rememberUpdatedState(onScrub)
    return semantics {
        contentDescription = description
        this[PlotDataArea] = model.area; this[PlotWindow] = model.viewport
        this[PlotSwipeCount] = model.swipe; this[PlotPlaybackMs] = model.cursor
        this[PlotAudioLevelCount] = model.level; this[PlotDroneComponentCount] = model.trace.components
        this[PlotYinCount] = model.trace.yin; this[PlotHighRegisterCount] = model.trace.highRegister
        this[PlotXLabels] = model.labels; this[PlotXLabelBounds] = model.labelBounds
        if (model.temporal) {
            progressBarRangeInfo = ProgressBarRangeInfo(model.cursor.coerceAtLeast(0).toFloat(), 0f..model.duration.coerceAtLeast(1).toFloat())
            setProgress(seek) { target -> scrub(target.toLong().coerceIn(0, model.duration.coerceAtLeast(1)), true); true }
        }
    }
}

@Composable
internal fun plotDescription(plot: AcousticPlot, instrument: InstrumentProfile, camera: PlotViewport,
                            ticks: List<PitchAxis.Tick>, spectral: Boolean, diagnostic: Boolean): String {
    val context = LocalContext.current
    val approximately = stringResource(R.string.ui_approximately)
    val vertical = if (spectral) stringResource(R.string.musical_drone_axis_range,
        axisLabel(camera.y.low, camera.y.span), axisLabel(camera.y.high, camera.y.span))
    else if (plot == AcousticPlot.PITCH) stringResource(R.string.ui_hz_axis_and_base_note) + ticks.joinToString {
        "${axisLabel(it.hz, camera.y.span)} Hz ${if (it.approximate) approximately else ""} ${it.note.orEmpty()}"
    } else ""
    val interaction = if (diagnostic) stringResource(R.string.diagnostic_chart_accessibility)
    else if (plot.temporal) stringResource(R.string.graph_timeline_hint)
    else stringResource(R.string.ui_drag_and_zoom_along_both_axes_hold_to_inspect)
    return stringResource(R.string.ui_to_4e5d93, context.plotLabel(plot, instrument),
        if (plot.temporal) stringResource(R.string.ui_time) else stringResource(R.string.ui_frequency),
        axisLabel(camera.x.low, camera.x.span), axisLabel(camera.x.high, camera.x.span), vertical, interaction)
}
