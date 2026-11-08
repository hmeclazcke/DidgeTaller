package ar.didgetaller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.nativeCanvas
import ar.didgetaller.R
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun plotUnitLabel(plot: AcousticPlot): String? = when (plot) {
    AcousticPlot.WAVE -> LocalContext.current.getString(R.string.ui_amplitude)
    AcousticPlot.SPECTRUM -> "dB rel"
    else -> null
}

/** Canvas owns clipping and native font resources; all preparation and interaction are upstream. */
@Composable
internal fun PlotCanvas(frame: PlotFrame, content: PlotContent, effects: PlotEffects, level: FloatArray,
    unit: String?, modifier: Modifier) {
    Canvas(modifier) {
        if (size.width <= frame.left + frame.right + frame.textSize || size.height <= frame.top + frame.bottom + frame.textSize * 2) return@Canvas
        val width = (size.width - frame.left - frame.right).coerceAtLeast(1f)
        val height = (size.height - frame.top - frame.bottom).coerceAtLeast(1f)
        paintAudioLevel(level, frame.left, frame.top, frame.levelHeight)
        translate(frame.left, frame.top) {
            clipRect(0f, 0f, width, height) {
                    paintPlotContent(content, frame.projection, effects, frame.xTicks, frame.yTicks, frame.textSize, frame.paint::measureText) { text, x, y ->
                        drawContext.canvas.nativeCanvas.drawText(text, x, y, frame.paint)
                    }
            }
            frame.axisText.forEach { label ->
                frame.paint.textAlign = if (label.rightAligned) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.CENTER
                frame.paint.textSize = frame.textSize * label.scale; frame.paint.alpha = label.alpha
                drawContext.canvas.nativeCanvas.drawText(label.value, label.x, label.y, frame.paint)
            }
            frame.paint.textSize = frame.textSize; frame.paint.alpha = 255
        }
        frame.paint.textAlign = android.graphics.Paint.Align.LEFT
        unit?.let { drawContext.canvas.nativeCanvas.drawText(it, 6f, frame.textSize, frame.paint) }
    }
}
