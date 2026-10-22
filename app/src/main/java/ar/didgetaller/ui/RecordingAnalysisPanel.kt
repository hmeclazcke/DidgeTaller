package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.Music
import ar.didgetaller.data.RecordingStore
import java.util.Locale

/** Reuses the validated Phase A pipeline; cache and interval belong to the session workspace. */
@Composable
internal fun RecordingAnalysisPanel(vm:TallerViewModel,item:RecordingStore.Session) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    val colors=MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().testTag("recording-analysis"),shape=RoundedCornerShape(20.dp),color=colors.surface) {
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.ui_wav_analysis),fontSize=19.sp,fontWeight=FontWeight.SemiBold)
            Text(if(state?.startMs==0L && state.endMs==null || state==null)stringResource(R.string.ui_full_recording_calculated_locally_from_the_original_audio)
                else context.getString(R.string.ui_excerpt_original_wav, seconds(state.startMs), state.endMs?.let {seconds(it)} ?: context.getString(R.string.ui_end)),color=colors.onSurfaceVariant,fontSize=12.sp)
            if(state?.running==true) {
                LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth())
                Text(stringResource(R.string.ui_analyzing_audio, state.progress),fontSize=13.sp)
                OutlinedButton(onClick={vm.cancelRecordingAnalysis()}) {Text(stringResource(R.string.ui_cancel_analysis))}
            } else Button(onClick={vm.analyzeRecording(item,force=state?.result!=null)}) {
                Text(if(state?.result!=null)stringResource(R.string.ui_analyze_again) else stringResource(R.string.ui_analyze_recording))
            }
            state?.error?.let {Text(it,color=colors.error,fontSize=13.sp)}
            state?.result?.let {result ->
                val summary=result.summary
                Text(summary?.let {"${context.noteLabel(it.typicalHz)} · ${hzLabel(it.typicalHz)}"} ?:
                    if(item.instrument==ar.didgetaller.core.InstrumentProfile.NATIVE_AMERICAN_FLUTE && result.validWindowFraction>=.5)context.getString(R.string.ui_changing_notes_no_predominant_note) else context.getString(R.string.ui_uncertain_fundamental),
                    color=colors.primary,fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text(stringResource(R.string.ui_hz_sample_rate_spectral_windows_up_to_s_per_window, result.sampleRate, result.windows, result.spectrum.fftSize.toDouble()/result.sampleRate),
                    color=colors.onSurfaceVariant,fontSize=12.sp)
                if(item.instrument==ar.didgetaller.core.InstrumentProfile.NATIVE_AMERICAN_FLUTE)
                    Text(stringResource(R.string.ui_fundamental_ms_windows_ms_hop, item.instrument.windowSamples*1000/result.sampleRate, item.instrument.windowSamples*500/result.sampleRate),color=colors.onSurfaceVariant,fontSize=12.sp)
                Text(stringResource(R.string.ui_valid_fundamental_readings, (result.validWindowFraction*100).toInt())+
                    (result.medianPeriodicityConfidence?.let {context.getString(R.string.ui_periodicity, it)} ?: ""),fontSize=13.sp)
                summary?.let {Text(stringResource(R.string.ui_stability_typical_range, it.stabilityPercent, hzLabel(it.lowHz), hzLabel(it.highHz)),fontSize=13.sp)}
                result.medianValidHz?.let {median ->
                    Text(stringResource(R.string.ui_median_of_valid_readings_mean, hzLabel(median), hzLabel(result.meanValidHz!!)),
                        color=colors.onSurfaceVariant,fontSize=12.sp)
                }
                Text(result.spectralCentroidHz?.let {stringResource(R.string.ui_power_centroid_8e8014, hzLabel(it))} ?: stringResource(R.string.ui_no_significant_spectral_energy),
                    fontSize=14.sp)
                Text(stringResource(R.string.ui_peaks_in_the_average_spectrum),fontWeight=FontWeight.SemiBold)
                if(result.harmonics.peaks.isEmpty())Text(stringResource(R.string.ui_not_enough_peaks_detected),color=colors.onSurfaceVariant)
                result.harmonics.peaks.sortedByDescending {it.relativeDb}.take(8).sortedBy {it.hz}.forEach {peak ->
                    val label=peak.order?.let {context.getString(R.string.ui_h_candidate, it)} ?: context.getString(R.string.ui_unassigned_peak_66f03c)
                    Text("$label · ${hzLabel(peak.hz)} · %.1f dB rel".format(displayLocale,peak.relativeDb)+
                        (peak.deviationCents?.let {" · %+.1f cents".format(displayLocale,it)} ?: ""),fontSize=13.sp)
                }
                if(result.harmonics.fundamentalHz==null)Text(stringResource(R.string.ui_harmonic_orders_are_not_assigned_without_a_reliable_predominant_fundamental),
                    color=colors.secondary,fontSize=12.sp)
                result.harmonics.candidateBandEnergyFraction?.let {
                    Text(stringResource(R.string.ui_energy_in_candidate_bands_2_bins, (it*100).toInt()),fontSize=12.sp)
                }
                Text(result.bandEnergies.joinToString(" · ") {band ->
                    "${band.fromHz.toInt()}–${band.toHz.toInt()} Hz: ${(band.fraction*100).toInt()}%"
                },color=colors.onSurfaceVariant,fontSize=12.sp)
                Text(stringResource(R.string.ui_db_relative_to_the_highest_spectral_peak_without_sound_pressure_calibration_pe),
                    color=colors.onSurfaceVariant,fontSize=12.sp)
                Text(stringResource(R.string.ui_derived_results_are_kept_in_a_regenerable_cache_the_wav_and_original_summary_r),
                    color=colors.onSurfaceVariant,fontSize=12.sp)
            }
        }
    }
}
