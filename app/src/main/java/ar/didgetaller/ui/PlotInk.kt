package ar.didgetaller.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import ar.didgetaller.core.*
import kotlin.math.max

/** Immutable drawing inputs; painters cannot mutate explorer, playback or analysis state. */
internal class PitchInk(val marks: List<TraceDrawing.Mark>, val canonical: Color?,
    val referenceHz: Double?, val highMarkers: List<Path>)

internal fun PlotProjection.offset(x: Double, y: Double): Offset =
    pixels(x, y).let { Offset(it.x.toFloat(), it.y.toFloat()) }

internal fun DrawScope.paintPitch(ink: PitchInk, projection: PlotProjection, bridge: PathEffect?, reference: PathEffect?) {
    ink.referenceHz?.takeIf { it in projection.viewport.y.low..projection.viewport.y.high }?.let { hz ->
        val y = projection.offset(projection.viewport.x.low, hz).y
        drawLine(Color(0xFF93CDE7).copy(alpha = .5f), Offset(0f, y), Offset(projection.width.toFloat(), y), 1f, pathEffect = reference)
    }
    ink.marks.forEach { paintTraceMark(it, ink.canonical, projection, bridge) }
    ink.highMarkers.forEach { drawPath(it, Color(0xFFE9BD87)) }
}

private fun DrawScope.paintTraceMark(mark: TraceDrawing.Mark, canonical: Color?, projection: PlotProjection, bridge: PathEffect?) {
    when (mark) {
        is TraceDrawing.Segment -> drawLine(canonical ?: pitchColor(mark.tone),
            projection.offset(mark.from.timeMs, mark.from.hz), projection.offset(mark.to.timeMs, mark.to.hz),
            if (canonical != null) 2.5f else 2.3f, pathEffect = if (mark.bridged) bridge else null)
        is TraceDrawing.Dot -> paintTraceDot(mark, canonical, projection)
    }
}

private fun DrawScope.paintTraceDot(mark: TraceDrawing.Dot, canonical: Color?, projection: PlotProjection) {
    val position = projection.offset(mark.at.timeMs, mark.at.hz)
    if (mark.uncertain) drawCircle(Color(0xFFEBA16E), 3f, position, style = Stroke(1.5f))
    else drawCircle(canonical ?: pitchColor(mark.tone), if (canonical != null) 3f else 2.5f, position)
}

internal fun DrawScope.paintWave(drawing: WaveDrawing?) {
    when (drawing) {
        is WaveDrawing.Samples -> drawPath(drawing.path, traceA, style = Stroke(1.8f))
        is WaveDrawing.Envelope -> drawing.geometry.columns.forEach { column ->
            drawLine(traceA, Offset(column.x.toFloat(), column.top.toFloat()),
                Offset(column.x.toFloat(), column.bottom.toFloat()), drawing.geometry.strokeWidth.toFloat())
        }
        null -> Unit
    }
}

internal class SpectrumInk(val geometry: SpectrumGeometry, val path: Path)

internal class ComparisonInk(val path: Path, val dots: List<Offset>)

internal fun DrawScope.paintComparison(ink: ComparisonInk, effect: PathEffect?) {
    drawPath(ink.path, swipeColor, style = Stroke(2.3f, pathEffect = effect))
    ink.dots.forEach { drawCircle(swipeColor, 2.8f, it) }
}

/** Font metrics and glyph drawing stay at the native boundary, independently controllable in tests. */
internal fun DrawScope.paintSpectrum(ink: SpectrumInk, height: Float, textSize: Float, guides: PathEffect?,
    measure: (String) -> Float, write: (String, Float, Float) -> Unit) {
    ink.geometry.guides.forEach { x ->
        drawLine(traceA.copy(alpha = .2f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), height), 1f, pathEffect = guides)
    }
    drawPath(ink.path, traceA, style = Stroke(2f))
    var labelRight = -1000f
    ink.geometry.peaks.forEach { peak ->
        val x = peak.position.x.toFloat(); val y = peak.position.y.toFloat()
        drawCircle(if (peak.order != null) traceA else traceB, 3.5f, Offset(x, y))
        if (peak.order != null && x > labelRight && y in 0f..height) {
            val label = "H${peak.order}?"
            write(label, x + 4, y.coerceAtLeast(textSize + 2) - 3)
            labelRight = x + measure(label) + 12
        }
    }
}

internal class TimelineInk(val region: LongRange?, val selectedMs: Long, val cursorMs: Long, val radius: Float)

internal fun DrawScope.paintTimeline(ink: TimelineInk, projection: PlotProjection, selection: PathEffect?) {
    val height = projection.height.toFloat()
    ink.region?.let { range ->
        val a = projection.offset(range.first.toDouble(), 0.0).x
        val b = projection.offset(range.last.toDouble(), 0.0).x
        drawRect(Color(0xFF8CCFF3).copy(alpha = .13f), Offset(a, 0f), Size(max(0f, b - a), height))
    }
    val time = if (ink.cursorMs < 0) ink.selectedMs else ink.cursorMs
    if (time.toDouble() !in projection.viewport.x.low..projection.viewport.x.high) return
    val x = projection.offset(time.toDouble(), projection.viewport.y.low).x
    if (ink.cursorMs < 0) drawLine(Color.White.copy(alpha = .7f), Offset(x, 0f), Offset(x, height), 1f, pathEffect = selection)
    else {
        drawLine(Color(0xFF9AD9FF), Offset(x, 0f), Offset(x, height), 2.5f)
        drawCircle(Color(0xFF9AD9FF), ink.radius, Offset(x, height - 10f))
    }
}
