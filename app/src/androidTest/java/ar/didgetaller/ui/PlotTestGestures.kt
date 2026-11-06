package ar.didgetaller.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch

/** Translate both fingers equally: pan without zoom or timeline scrubbing. */
internal fun SemanticsNodeInteraction.panViewport(delta:Offset) {
    val area=fetchSemanticsNode().config[PlotDataArea]
    val spacing=Offset(area.width*.15f,0f)
    performTouchInput {pinch(start0=area.center-spacing,start1=area.center+spacing,
        end0=area.center-spacing+delta,end1=area.center+spacing+delta,durationMillis=500)}
}
