package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import kotlinx.coroutines.*
import java.io.IOException

private val PitchAlgorithm.label get() = if (this == PitchAlgorithm.HARMONIC) R.string.detector_experimental else R.string.detector_standard

internal fun activeMeasurementTitle(vm: TallerViewModel) = if (vm.pitchAlgorithm == PitchAlgorithm.HARMONIC)
    R.string.detector_experimental else R.string.ui_session_in_progress

@Composable
internal fun DetectorSelector(vm: TallerViewModel) {
    var menu by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { menu = true }, enabled = vm.canConfigureMeasurement,
            modifier = Modifier.testTag("pitch-algorithm")) { Text(stringResource(vm.pitchAlgorithm.label)) }
        DropdownMenu(menu, { menu = false }, Modifier.widthIn(max = 300.dp)) {
            PitchAlgorithm.entries.forEach { algorithm ->
                DropdownMenuItem(text = { Text(stringResource(algorithm.label)) },
                    onClick = { vm.selectPitchAlgorithm(algorithm); menu = false })
            }
            Text(stringResource(R.string.detector_help), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal enum class DetectorView(val label: Int) {
    STANDARD(R.string.detector_standard), HARMONIC(R.string.detector_harmonic), BOTH(R.string.detector_both)
}

@Composable
internal fun DetectorComparisonAction(vm: TallerViewModel, initial: RecordingStore.Session? = null) {
    var open by rememberSaveable(initial?.id) { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = Modifier.testTag("compare-detectors")) {
        Text(stringResource(R.string.detector_compare))
    }
    if (open) Dialog({ open = false }, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            ComparisonScreen(vm, initial) { open = false }
        }
    }
}

@Composable
private fun ComparisonScreen(vm: TallerViewModel, initial: RecordingStore.Session?, close: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(initial?.id) }
    var mode by rememberSaveable { mutableStateOf(DetectorView.BOTH) }
    val session = vm.sessions.firstOrNull { it.id == selected }
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 12.dp)) {
        val controlsHeight = maxHeight * .45f
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.heightIn(max = controlsHeight).verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detector_compare), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = close) { Text(stringResource(R.string.ui_close)) }
                }
                RecordingChoice(vm.sessions, session) { selected = it.id }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetectorView.entries.forEach { view ->
                        FilterChip(selected = view == mode, onClick = { mode = view },
                            label = { Text(if (view == DetectorView.STANDARD) "YIN" else stringResource(view.label)) })
                    }
                }
                Text(stringResource(R.string.detector_help), style = MaterialTheme.typography.bodySmall)
            }
            if (session != null) key(session.id) { ComparisonResults(vm, session, mode, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RecordingChoice(sessions: List<RecordingStore.Session>, selected: RecordingStore.Session?,
                            onSelect: (RecordingStore.Session) -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { menu = true }, enabled = sessions.isNotEmpty()) {
            Text(selected?.let { context.sessionName(it) } ?: stringResource(
                if (sessions.isEmpty()) R.string.ui_no_sessions_yet else R.string.detector_choose))
        }
        DropdownMenu(menu, { menu = false }) {
            sessions.forEach { item ->
                DropdownMenuItem(text = { Text(context.sessionName(item)) }, onClick = { onSelect(item); menu = false })
            }
        }
    }
}

@Composable
private fun ComparisonResults(vm: TallerViewModel, session: RecordingStore.Session, mode: DetectorView, modifier: Modifier) {
    var comparison by remember { mutableStateOf<DetectorComparison?>(null) }
    var failed by remember { mutableStateOf(false) }
    val algorithm = if (mode == DetectorView.STANDARD) PitchAlgorithm.STANDARD else PitchAlgorithm.HARMONIC
    val result = comparison?.takeIf { it.supports(algorithm) }
    LaunchedEffect(algorithm) {
        if (result != null) return@LaunchedEffect
        failed = false
        try {
            comparison = replay(vm, session, algorithm)
        } catch (_: IOException) { failed = true }
          catch (_: IllegalArgumentException) { failed = true }
    }
    when {
        failed -> Text(stringResource(R.string.detector_error))
        result == null -> Text(stringResource(R.string.detector_loading))
        else -> ComparisonGraph(vm, session, result, mode, modifier)
    }
}

private suspend fun replay(vm: TallerViewModel, session: RecordingStore.Session, algorithm: PitchAlgorithm) =
    withContext(Dispatchers.Default) {
        val owner = currentCoroutineContext()
        val profile = session.instrument
        val upper = if (SavedYinReplay.usesExpandedRange(session.timeline)) profile.maxHz else profile.initialMaxHz
        Pcm16WavReader(vm.sessionFile(session)).use { source ->
            DetectorComparison.analyze(source, profile, upper, algorithm) { !owner.isActive }
        }
    }
