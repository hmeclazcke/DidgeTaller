package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import kotlin.math.roundToInt

private val standardColor = Color(0xFF8FE0BD)

@Composable
internal fun ComparisonGraph(vm: TallerViewModel, session: RecordingStore.Session, result: DetectorComparison,
                            mode: DetectorView, modifier: Modifier) {
    val state = remember(session.id) { ExplorerState(session.instrument, vm.explorer(session.id).selection) }
    val playback = playbackCursor(vm, session)
    val position = if (vm.playingId == session.id) playback else state.selectedMs
    val select: (Long, Boolean) -> Unit = { ms, commit ->
        state.selectedMs = ms.coerceIn(0, result.durationMs)
        state.selection.scrubMs = state.selectedMs.takeUnless { commit }
        if (commit && vm.playingId == session.id) vm.seekPlayback(session, state.selectedMs)
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
      val legendHeight = maxHeight * .48f
      Column(Modifier.fillMaxSize()) {
        ComparisonCanvas(result, state, mode, position, Modifier.fillMaxWidth().weight(1f), select)
        Column(Modifier.heightIn(max = legendHeight).verticalScroll(rememberScrollState())) {
            val frame = result.at(position)
            if (mode != DetectorView.HARMONIC) DetectorReadout("━ YIN", frame?.standard, standardColor)
            if (mode != DetectorView.STANDARD) DetectorReadout("┄ " + stringResource(R.string.detector_harmonic), frame?.harmonic, swipeColor)
            val label = stringResource(R.string.detector_cursor)
            Text("$label · ${seconds(position)}", style = MaterialTheme.typography.bodySmall)
            Slider(position.toFloat(), { select(it.toLong(), false) }, Modifier.semantics { contentDescription = label },
                valueRange = 0f..result.durationMs.coerceAtLeast(1).toFloat(), onValueChangeFinished = { select(state.selectedMs, true) })
        }
      }
    }
}

@Composable
private fun DetectorReadout(label: String, hz: Double?, color: Color) {
    val context = LocalContext.current
    val pitch = hz?.let { Music.pitch(it) }
    val value = if (pitch == null) stringResource(R.string.detector_absent)
        else "${hzLabel(hz)} · ${context.noteLabel(hz)} · ${"%+d".format(displayLocale, pitch.cents.roundToInt())} ¢"
    Text("$label  $value", color = color, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ComparisonCanvas(result: DetectorComparison, state: ExplorerState, mode: DetectorView, cursor: Long,
                             modifier: Modifier, select: (Long, Boolean) -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val standard = remember(result) { result.marks { it.standard } }
    val harmonic = remember(result) { result.marks { it.harmonic } }
    val upper = remember(result) { result.frames.maxOfOrNull { maxOf(it.standard ?: 0.0, it.harmonic ?: 0.0) } ?: 0.0 }
    val rate = result.sampleRate
    LaunchedEffect(result) { state.configureComparisonRange(AxisWindow(state.instrument.validMinHz * .95, maxOf(state.instrument.initialMaxHz.toDouble(), upper * 1.05))) }
    val camera = state.camera(result.durationMs, rate, emptyList())
    val frame = rememberPlotFrame(AcousticPlot.PITCH, camera, size, state.gestureActive, false, false, false)
    Box(modifier.onSizeChanged { size = it }.testTag("detector-graph")) {
        if (frame == null) return@Box
        val coords = PlotGestureCoordinates(camera, frame.projection.width, frame.projection.height, frame.left.toDouble(), frame.top.toDouble(), false)
        val interaction = Modifier.fillMaxSize().plotInteraction(state, coords, select, { x, _, _ -> select(x.toLong(), true) },
            { state.reset(result.durationMs, rate, emptyList()) },
            { x,y,zx,zy,dx,dy -> state.transform(result.durationMs, rate, emptyList(),x,y,zx,zy,dx,dy) })
        val pitch = remember(standard, mode) { if (mode == DetectorView.HARMONIC) null else PitchInk(standard, standardColor, null, emptyList()) }
        val overlay = remember(harmonic, mode, frame.projection) {
            if (mode == DetectorView.STANDARD) null else comparisonInk(harmonic, frame.projection)
        }
        val effects = remember { PlotEffects(null, null, PathEffect.dashPathEffect(floatArrayOf(8f,5f)), null, null) }
        PlotCanvas(frame, PlotContent(null, pitch, overlay, null, null, TimelineInk(null, cursor, -1, 0f)),
            effects, remember { FloatArray(0) }, null, interaction)
    }
}
