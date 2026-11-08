package ar.didgetaller.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import ar.didgetaller.core.*

@Composable
internal fun rememberRasterInk(enabled: Boolean, tile: AudioVisuals.Spectrogram?, bitmap: android.graphics.Bitmap?,
    projection: PlotProjection): RasterInk? = remember(enabled, tile, bitmap, projection) {
    if (!enabled || tile == null || bitmap == null) return@remember null
    val placement = RasterPlacement.of(tile, projection) ?: return@remember null
    RasterInk(bitmap.asImageBitmap(), placement)
}

/** Native paths are cached by actual geometry, never invalidated by a playback-only tick. */
@Composable
internal fun rememberPitchInk(enabled: Boolean, marks: List<TraceDrawing.Mark>, trace: MusicalTrace?,
    typicalHz: Double?, projection: PlotProjection): PitchInk? = remember(enabled, marks, trace, typicalHz, projection) {
    if (!enabled) return@remember null
    val color = trace?.let {
        if (it.meaning == MusicalTrace.Meaning.SPECTRAL_COMPONENT) Color(0xFF65C9D1) else Color(0xFF8FE0BD)
    }
    val markers = trace?.highRegisterCandidates.orEmpty().map { candidate ->
        val x = projection.pixels(candidate.timeMs.toDouble(), 0.0).x.toFloat()
        Path().apply { moveTo(x, 4f); lineTo(x - 5f, 14f); lineTo(x + 5f, 14f); close() }
    }
    PitchInk(marks, color, typicalHz.takeIf { trace == null }, markers)
}

@Composable
internal fun rememberComparisonInk(curve: SwipeCurve?, projection: PlotProjection, enabled: Boolean): ComparisonInk? =
    remember(curve, projection, enabled) {
        if (curve == null || !enabled) return@remember null
        val path = Path(); val dots = mutableListOf<Offset>()
        TraceDrawing.comparison(curve, projection.viewport.x).forEach { mark ->
            when (mark) {
                is TraceDrawing.Segment -> {
                    val a = projection.offset(mark.from.timeMs, mark.from.hz)
                    val b = projection.offset(mark.to.timeMs, mark.to.hz)
                    path.moveTo(a.x, a.y); path.lineTo(b.x, b.y)
                }
                is TraceDrawing.Dot -> dots += projection.offset(mark.at.timeMs, mark.at.hz)
            }
        }
        ComparisonInk(path, dots)
    }

@Composable
internal fun rememberSpectrumInk(enabled: Boolean, analysis: WavAnalysis.Result?, projection: PlotProjection,
    columns: Int): SpectrumInk? = remember(enabled, analysis, projection, columns) {
    if (!enabled) return@remember null
    val geometry = SpectrumGeometry.prepare(analysis, projection, columns) ?: return@remember null
    val path = Path().apply {
        geometry.curve.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x.toFloat(), point.y.toFloat())
            else lineTo(point.x.toFloat(), point.y.toFloat())
        }
    }
    SpectrumInk(geometry, path)
}
