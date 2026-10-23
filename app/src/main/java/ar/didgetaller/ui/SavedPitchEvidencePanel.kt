package ar.didgetaller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.SavedPitchEvidence
import ar.didgetaller.data.RecordingStore

/** An explicit snapshot request; cursor/playback changes never schedule analysis. */
@Composable
internal fun SavedPitchEvidencePanel(vm:TallerViewModel,item:RecordingStore.Session,positionMs:Long) {
    var expanded by rememberSaveable(item.id){mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded;if(!expanded)vm.cancelSavedEvidence()},
        modifier=Modifier.testTag("saved-evidence-toggle")) {
        Text(stringResource(if(expanded)R.string.saved_evidence_hide else R.string.saved_evidence_title))
    }
    if(!expanded)return
    val state=vm.savedEvidence.takeIf {it.sessionId==item.id}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.testTag("saved-evidence-panel")) {
        Text(stringResource(R.string.saved_evidence_help),style=MaterialTheme.typography.bodySmall)
        if(state?.running==true){
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            TextButton(onClick={vm.cancelSavedEvidence()}){Text(stringResource(R.string.ui_cancel_analysis))}
        }else OutlinedButton(onClick={vm.inspectSavedEvidence(item,positionMs)},modifier=Modifier.testTag("saved-evidence-inspect")){
            Text(stringResource(R.string.saved_evidence_inspect,seconds(positionMs)))
        }
        if(state?.failed==true)Text(stringResource(R.string.saved_evidence_failed),color=MaterialTheme.colorScheme.error)
        state?.result?.let {SavedEvidenceResult(it)}
    }
}

@Composable
internal fun SavedEvidenceResult(result:SavedPitchEvidence.Snapshot) {
    val colors=MaterialTheme.colorScheme
    Text(stringResource(R.string.saved_evidence_window,seconds(result.startFrame*1000/result.sampleRate),
        seconds(result.endFrame*1000/result.sampleRate)),style=MaterialTheme.typography.bodySmall)
    Text(stringResource(when(result.presence){
        SavedPitchEvidence.Presence.BELOW_THRESHOLD->R.string.acoustic_state_low
        SavedPitchEvidence.Presence.ENERGY_PRESENT->R.string.saved_evidence_energy
        SavedPitchEvidence.Presence.INSUFFICIENT_INPUT->R.string.acoustic_reason_insufficient_data
    }),modifier=Modifier.testTag("saved-evidence-presence"))
    if(result.presence!=SavedPitchEvidence.Presence.ENERGY_PRESENT)return
    Text(stringResource(R.string.saved_evidence_accepted,result.acceptedHz?.let(::hzLabel)?:stringResource(R.string.ui_no_data)),
        modifier=Modifier.testTag("saved-evidence-accepted"))
    if(result.acceptedHz==null)result.candidateHz?.let {
        Text(stringResource(R.string.saved_evidence_rejected,hzLabel(it)))
    }
    Text(stringResource(R.string.acoustic_diagnostics_values,result.rms,result.periodicity),style=MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.saved_evidence_alternative,result.alternative?.let{hzLabel(it.hz)}?:stringResource(R.string.ui_no_data)),
        modifier=Modifier.testTag("saved-evidence-alternative"))
    result.alternative?.let {Text(stringResource(R.string.saved_evidence_alternative_periodicity,it.confidence),style=MaterialTheme.typography.bodySmall)}
    Text(stringResource(R.string.saved_evidence_families,result.families.takeIf {it.isNotEmpty()}?.joinToString(" · "){hzLabel(it)}
        ?:stringResource(R.string.ui_no_data)),modifier=Modifier.testTag("saved-evidence-families"))
    val low=minOf(25,result.minHz).toDouble();val high=result.maxHz.toDouble()
    Canvas(Modifier.fillMaxWidth().height(76.dp).testTag("saved-evidence-markers")) {
        val margin=6.dp.toPx()
        fun x(hz:Double)=margin+((hz-low)/(high-low)).coerceIn(0.0,1.0).toFloat()*(size.width-2*margin)
        // Separate lanes, no connecting lines, inferred samples or temporal interpolation.
        result.acceptedHz?.let {drawCircle(colors.primary,5.dp.toPx(),Offset(x(it),12.dp.toPx()))}
        result.alternative?.let {drawCircle(colors.tertiary,5.dp.toPx(),Offset(x(it.hz),36.dp.toPx()))}
        result.families.forEach {drawCircle(colors.secondary,5.dp.toPx(),Offset(x(it),60.dp.toPx()))}
    }
    Text(stringResource(R.string.saved_evidence_lanes,low.toInt(),high.toInt()),style=MaterialTheme.typography.bodySmall)
}
