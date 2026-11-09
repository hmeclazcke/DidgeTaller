package ar.didgetaller.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import ar.didgetaller.core.AudioVisuals
import ar.didgetaller.core.PlotProjection
import ar.didgetaller.core.WaveGeometry

/** Native resources belong to the drawing adapter, never to PCM/domain state. */
internal sealed interface WaveDrawing {
    class Samples(val path: Path) : WaveDrawing
    class Envelope(val geometry: WaveGeometry.Envelope) : WaveDrawing
}

@Composable
internal fun rememberWaveDrawing(wave: AudioVisuals.Wave?, projection: PlotProjection): WaveDrawing? =
    remember(wave, projection) {
        if (wave == null) return@remember null
        when (val geometry = WaveGeometry.prepare(wave, projection)) {
            is WaveGeometry.Samples -> WaveDrawing.Samples(Path().apply {
                geometry.points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x.toFloat(), point.y.toFloat())
                    else lineTo(point.x.toFloat(), point.y.toFloat())
                }
            })
            is WaveGeometry.Envelope -> WaveDrawing.Envelope(geometry)
        }
    }
