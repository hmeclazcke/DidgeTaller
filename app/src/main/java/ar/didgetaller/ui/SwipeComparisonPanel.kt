package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.data.RecordingStore

internal val swipeColor = Color(0xFFC9A4FF)

@Composable
internal fun AdvancedPitchDiagnostics(vm: TallerViewModel, item: RecordingStore.Session, positionMs: Long) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded; if(!expanded) vm.cancelSavedEvidence() },
        modifier = Modifier.testTag("advanced-diagnostics-toggle")) {
        Text(stringResource(if(expanded) R.string.advanced_diagnostics_hide else R.string.advanced_diagnostics))
    }
    if(expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.advanced_diagnostics_help), style = MaterialTheme.typography.bodySmall)
        if(item.instrument == ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            SwipeComparisonPanel(vm, item, showLegend = false)
        SavedPitchEvidencePanel(vm, item, positionMs)
    }
}

@Composable
internal fun SwipeComparisonPanel(vm: TallerViewModel, item: RecordingStore.Session, showLegend: Boolean = true) {
    if (item.instrument != ar.didgetaller.core.InstrumentProfile.DIDGERIDOO) {
        Text(stringResource(R.string.swipe_didgeridoo_only), style = MaterialTheme.typography.bodySmall)
        return
    }
    val state = vm.swipeComparison.takeIf { it.sessionId == item.id }
    val label = stringResource(R.string.swipe_compare)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = state?.enabled == true, onCheckedChange = { vm.setSwipeComparison(item,it) },
            modifier = Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)
                .testTag("swipe-compare-toggle").semantics { contentDescription = label })
    }
    if (state?.enabled == true) {
        if(showLegend) SwipeLegend()
        Text(stringResource(R.string.swipe_caution), style = MaterialTheme.typography.bodySmall)
        if (state.running) {
            LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth().testTag("swipe-progress"))
            Text(stringResource(R.string.swipe_progress,state.progress), style = MaterialTheme.typography.labelSmall)
        }
        if (state.failed) {
            Text(stringResource(R.string.swipe_failed), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { vm.setSwipeComparison(item,true) }) { Text(stringResource(R.string.swipe_retry)) }
        }
    }
}

@Composable
internal fun SwipeLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("swipe-legend")) {
        Text(stringResource(R.string.swipe_yin), style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.swipe_experimental),
            color = if(MaterialTheme.colorScheme.surface.luminance()>.5f)Color(0xFF684098) else swipeColor,
            style = MaterialTheme.typography.labelSmall)
    }
}
