package ar.didgetaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.core.AcousticDiagnostics

/** The last observed frame is diagnostic evidence, including after capture stops. */
@Composable
internal fun AcousticDiagnosticsPanel(sample:AcousticDiagnostics.Sample?,retainedCount:Int) {
    if(sample==null)return
    var expanded by remember(sample.instrument){mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("acoustic-diagnostics-toggle")) {
            Text(stringResource(if(expanded)R.string.acoustic_diagnostics_hide else R.string.acoustic_diagnostics_title))
        }
        if(expanded) {
            Text(stringResource(R.string.acoustic_diagnostics_latest),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(when(sample.signal) {
                AcousticDiagnostics.Signal.NO_RELEVANT_SIGNAL -> R.string.acoustic_state_low
                AcousticDiagnostics.Signal.ACCEPTED_PITCH -> R.string.acoustic_state_accepted
                AcousticDiagnostics.Signal.UNCERTAIN_PITCH -> R.string.ui_signal_present_uncertain_frequency
            }),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.acoustic_diagnostics_values,sample.rms,sample.periodicity),
                modifier=Modifier.testTag("acoustic-diagnostic-values"),style=MaterialTheme.typography.bodySmall)
            sample.candidateHz?.let {candidate->
                Text(stringResource(R.string.acoustic_diagnostics_candidate,candidate),
                    modifier=Modifier.testTag("acoustic-diagnostic-candidate"),style=MaterialTheme.typography.bodySmall)
            }
            sample.rejectionReason?.let {reason->
                val label=stringResource(when(reason) {
                    "low_amplitude" -> R.string.acoustic_state_low
                    "low_periodicity" -> R.string.acoustic_reason_low_periodicity
                    "outside_range" -> R.string.acoustic_reason_outside_range
                    "no_minimum" -> R.string.acoustic_reason_no_minimum
                    "short_window","short_comparison" -> R.string.acoustic_reason_insufficient_data
                    "profile_gate" -> R.string.acoustic_reason_profile_gate
                    else -> R.string.acoustic_reason_unknown
                })
                Text(stringResource(R.string.acoustic_diagnostics_reason,label),
                    modifier=Modifier.testTag("acoustic-diagnostic-reason"),style=MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.acoustic_diagnostics_retained,retainedCount,AcousticDiagnostics.MAX_SAMPLES),
                style=MaterialTheme.typography.bodySmall)
        }
    }
}
