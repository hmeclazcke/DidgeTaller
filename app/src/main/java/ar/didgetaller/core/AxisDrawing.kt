package ar.didgetaller.core

import kotlin.math.max

/** Glyph placement is independent of Android Paint; ticks always retain their measured values. */
object AxisDrawing {
    data class Text(val value: String, val x: Float, val y: Float, val scale: Float = 1f,
                    val alpha: Int = 255, val rightAligned: Boolean = true)

    fun vertical(ticks: List<Double>, notes: List<PitchAxis.Tick>, projection: PlotProjection,
                 textSize: Float, gap: Float, format: (Double) -> String): List<Text> = buildList {
        val height = projection.height.toFloat()
        var previousTop = Float.POSITIVE_INFINITY
        for (tick in ticks) {
            val musical = notes.firstOrNull { it.hz == tick }
            val baseline = projection.pixels(projection.viewport.x.low, tick).y.toFloat().coerceIn(textSize,
                if (musical?.note != null) max(textSize, height - textSize * 1.15f) else height - textSize / 4)
            val labelBottom = baseline + if (musical?.note != null) textSize else 0f
            // Keep ticks/grid positions intact; omit only glyphs that cannot fit legibly.
            if (labelBottom + textSize * .2f > previousTop) continue
            previousTop = baseline - textSize
            add(Text(format(tick), -gap, baseline))
            musical?.note?.let { note ->
                add(Text((if (musical.approximate) "≈ " else "") + note, -gap, baseline + textSize, .85f, 195))
            }
        }
    }

    fun horizontal(labels: List<Pair<String, Float>>, height: Float, textSize: Float): List<Text> =
        labels.map { (text, center) -> Text(text, center, height + textSize * 1.5f, rightAligned = false) }
}
