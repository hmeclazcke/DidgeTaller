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
import ar.didgetaller.core.DroneComponentCurve
import kotlin.math.abs

internal val swipeColor = Color(0xFFC9A4FF)

@Composable
internal fun AdvancedPitchDiagnostics(vm: TallerViewModel, item: RecordingStore.Session, positionMs: Long,
                                      diagnosticPlot: @Composable () -> Unit = {}) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded; if(!expanded) {vm.cancelSavedEvidence();vm.setSwipeComparison(item,false)} },
        modifier = Modifier.testTag("advanced-diagnostics-toggle")) {
        Text(stringResource(if(expanded) R.string.advanced_diagnostics_hide else R.string.advanced_diagnostics))
    }
    if(expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LaunchedEffect(item.id) { vm.analyzeSavedYin(item); vm.analyzeDroneComponent(item) }
        Text(stringResource(R.string.advanced_diagnostics_help), style = MaterialTheme.typography.bodySmall)
        diagnosticPlot()
        if(item.instrument == ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            SwipeComparisonPanel(vm, item, showLegend = false)
        if(item.instrument == ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            DroneComponentDiagnostic(vm,item,positionMs)
        if(item.instrument == ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            SavedYinProvenance(vm,item,positionMs)
        SavedPitchEvidencePanel(vm, item, positionMs)
    }
}

@Composable
private fun SavedYinProvenance(vm:TallerViewModel,item:RecordingStore.Session,positionMs:Long){
    item.timeline.minByOrNull {abs(it.timeMs-positionMs)}?.takeIf {abs(it.timeMs-positionMs)<=250}?.let {original->
        Text(stringResource(R.string.pitch_original_diagnostic,seconds(original.timeMs),
            original.hz?.let(::hzLabel) ?: stringResource(R.string.swipe_missing),original.confidence),
            style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("pitch-original-diagnostic"))
    }
    val replay=vm.savedYinReplay.takeIf {it.sessionId==item.id}?.result ?: return
    val frame=replay.frames.minByOrNull {abs(it.centerMs-positionMs)}
        ?.takeIf {abs(it.centerMs-positionMs)<=120} ?: return
    val reason=when(frame.reason){
        "accepted"->R.string.pitch_replay_accepted
        "low_amplitude"->R.string.pitch_replay_low_energy
        "low_comparison_amplitude"->R.string.pitch_replay_low_support
        "low_periodicity"->R.string.pitch_replay_low_periodicity
        else->R.string.pitch_replay_other_rejection
    }
    Text(stringResource(R.string.pitch_replay_diagnostic,seconds(frame.centerMs.toLong()),
        frame.acceptedHz?.let(::hzLabel) ?: stringResource(R.string.swipe_missing),
        frame.candidateHz?.let(::hzLabel) ?: stringResource(R.string.swipe_missing),
        stringResource(reason),frame.rms,frame.comparisonRms ?: 0.0),
        style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("pitch-replay-diagnostic"))
}

@Composable
private fun DroneComponentDiagnostic(vm:TallerViewModel,item:RecordingStore.Session,positionMs:Long){
    val curve=vm.droneComponent.takeIf {it.sessionId==item.id}?.curve ?: return
    val sample=curve.samples.minByOrNull {abs(it.timeMs-positionMs)}
        ?.takeIf {abs(it.timeMs-positionMs)<=curve.hopMs/2} ?: return
    val reason=when(sample.reason){
        DroneComponentCurve.Reason.COMPONENT->R.string.drone_reason_component
        DroneComponentCurve.Reason.COMPETING_PERIODICITY->R.string.drone_reason_competing
        DroneComponentCurve.Reason.QUIET->R.string.drone_reason_quiet
        DroneComponentCurve.Reason.NO_PEAK->R.string.drone_reason_no_peak
        DroneComponentCurve.Reason.NO_FAMILY->R.string.drone_reason_no_family
        DroneComponentCurve.Reason.TEMPORALLY_UNSUPPORTED->R.string.drone_reason_temporal
    }
    Text(stringResource(R.string.drone_component_diagnostic,
        sample.hz?.let(::hzLabel) ?: stringResource(R.string.swipe_missing),
        sample.candidateHz?.let(::hzLabel) ?: stringResource(R.string.swipe_missing),
        stringResource(reason),sample.rms),style=MaterialTheme.typography.bodySmall,
        modifier=Modifier.testTag("drone-component-diagnostic"))
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
