package ar.didgetaller.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import ar.didgetaller.core.PlotGestureCoordinates
import ar.didgetaller.core.PlotPoint

private fun Offset.point() = PlotPoint(x.toDouble(), y.toDouble())

/** Binds the existing finger-priority engine to one measured coordinate frame and selection owner. */
@Composable
internal fun Modifier.plotInteraction(state: ExplorerState, coordinates: PlotGestureCoordinates,
    onScrub: (Long, Boolean) -> Unit, onPoint: (Double, Double, Boolean) -> Unit, onReset: () -> Unit,
    onTransform: (Double, Double, Double, Double, Double, Double) -> Unit): Modifier = immersiveGestures(
    onScrubStart = { state.plot.temporal && coordinates.contains(it.point()) },
    onScrub = { position, commit -> onScrub(coordinates.data(position.point()).x.toLong(), commit) },
    onTap = { position -> coordinates.data(position.point()).let { onPoint(it.x, it.y, false) } },
    onInspect = { position -> coordinates.data(position.point()).let { onPoint(it.x, it.y, true) } },
    onReset = onReset,
    onActive = { state.gestureActive = it; if (!it) state.selection.scrubMs = null },
    onTransform = { center, pan, zoomX, zoomY ->
        coordinates.transform(center.point(), pan.point(), zoomX.toDouble(), zoomY.toDouble()).let {
            onTransform(it.anchorX, it.anchorY, it.zoomX, it.zoomY, it.panX, it.panY)
        }
    },
)
