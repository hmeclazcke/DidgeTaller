package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ar.didgetaller.TallerViewModel
import ar.didgetaller.PitchLayerMode
import ar.didgetaller.audio.VisualRequest
import ar.didgetaller.core.Music
import ar.didgetaller.core.SpectralPalette
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import ar.didgetaller.data.RecordingStore
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
internal fun LabCard(title:String,action:(@Composable ()->Unit)?=null,content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
internal fun SessionExplorer(vm:TallerViewModel,item:RecordingStore.Session,cursor:Long,onExpand:(AcousticPlot)->Unit,
                            playbackControls:@Composable ()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val s=vm.preview(item.id)
    var tools by rememberSaveable(item.id){mutableStateOf(false)}
    var fragments by rememberSaveable(item.id){mutableStateOf(false)}
    var markers by rememberSaveable(item.id){mutableStateOf(false)}
    LaunchedEffect(item.id){vm.openTools(item)}
    LabCard(context.getString(R.string.ui_explore_audio), action={
        IconButton(onClick={onExpand(s.plot)},modifier=Modifier.testTag("expand-plot")){
            Icon(ViewerIcons.Expand,stringResource(R.string.ui_full_screen))
        }
    }) {
        ScrollableTabRow(selectedTabIndex=s.plot.ordinal,edgePadding=0.dp){
            AcousticPlot.entries.forEach {kind->Tab(selected=s.plot==kind,onClick={s.plot=kind;s.inspected=null},text={Text(context.plotLabel(kind),maxLines=1)})}
        }
        if(s.plot==AcousticPlot.PITCH && item.instrument==ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            PitchLayerChoice(vm)
        if(s.plot==AcousticPlot.PITCH)Text(stringResource(R.string.pitch_audio_level_short),
            style=MaterialTheme.typography.labelSmall)
        if(s.plot==AcousticPlot.PITCH && vm.swipeComparison.sessionId==item.id && vm.swipeComparison.enabled)SwipeLegend()
        if(s.plot==AcousticPlot.PITCH && vm.pitchLayerMode!=PitchLayerMode.COMPONENT)
            Text(stringResource(R.string.pitch_yin_layer),style=MaterialTheme.typography.labelSmall)
        if(s.plot==AcousticPlot.PITCH && vm.pitchLayerMode!=PitchLayerMode.FUNDAMENTAL && vm.droneComponent.sessionId==item.id){
            val component=vm.droneComponent
            if(component.curve?.samples?.any {it.hz!=null}==true)DroneComponentLegend(showExplanation=false)
            if(vm.droneComponent.running)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(2.dp))
            if(component.failed)Text(stringResource(R.string.drone_component_failed),style=MaterialTheme.typography.bodySmall)
            else if(component.curve!=null && component.curve.samples.none {it.hz!=null})
                Text(stringResource(R.string.drone_component_unavailable),style=MaterialTheme.typography.bodySmall)
        }
        if(s.plot==AcousticPlot.PITCH && item.instrument==ar.didgetaller.core.InstrumentProfile.DIDGERIDOO){
            if(vm.savedYinReplay.running)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(2.dp))
        }
        SessionPlot(vm,item,cursor,Modifier.fillMaxWidth().height(250.dp),onExpand={onExpand(s.plot)})
        playbackControls()
        if(s.plot==AcousticPlot.PITCH){
            Text(stringResource(R.string.pitch_audio_level_help),style=MaterialTheme.typography.bodySmall,
                modifier=Modifier.testTag("pitch-audio-level-help"))
            if(vm.pitchLayerMode!=PitchLayerMode.FUNDAMENTAL && vm.droneComponent.sessionId==item.id &&
                vm.droneComponent.curve?.samples?.any {it.hz!=null}==true)
                Text(stringResource(R.string.drone_component_explanation),style=MaterialTheme.typography.labelSmall)
        }
        if(s.plot==AcousticPlot.PITCH && item.instrument==ar.didgetaller.core.InstrumentProfile.DIDGERIDOO){
            Text(stringResource(if(vm.savedYinReplay.sessionId==item.id && vm.savedYinReplay.result!=null)
                R.string.pitch_source_current_replay else R.string.pitch_source_original),
                style=MaterialTheme.typography.labelSmall)
        }
        if(s.plot==AcousticPlot.PITCH){
            val points=sessionPlotPoints(vm,item)
            Text(stringResource(R.string.ui_frequency_in_hz_green_stable_yellow_variation_orange_review),style=MaterialTheme.typography.bodySmall)
            if(s.pitch(points).valid==0)Text(stringResource(R.string.ui_no_reliable_readings_audio_is_preserved),style=MaterialTheme.typography.bodySmall)
        }
        if(s.plot==AcousticPlot.SPECTRUM && vm.recordingAnalysis.result==null)Text(stringResource(R.string.ui_tap_analyze_recording_to_obtain_the_actual_spectrum),style=MaterialTheme.typography.bodySmall)
        if(s.plot!=AcousticPlot.PITCH)Text((if(s.plot==AcousticPlot.SPECTROGRAM)stringResource(R.string.ui_frequency_in_hz) else "")+stringResource(R.string.ui_tap_the_chart_to_explore_with_zoom_and_pan_along_both_axes),style=MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={tools=!tools}){Text(if(tools)stringResource(R.string.ui_hide_scales) else stringResource(R.string.ui_scales_and_ranges))}
            TextButton(onClick={fragments=!fragments}){Text(if(fragments)stringResource(R.string.ui_hide_excerpts) else stringResource(R.string.ui_selection_and_excerpts))}
            TextButton(onClick={markers=!markers}){Text(stringResource(R.string.ui_markers))}
        }
        if(tools)PlotTools(vm,item,s)
        if(fragments)FragmentTools(vm,item,s)
        AdvancedPitchDiagnostics(vm,item,s.selectedMs)
        if(markers){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                TextButton(onClick={vm.addMarker(item,s.selectedMs,"Posible toot")}){Text(stringResource(R.string.ui_mark_possible_toot))}
                TextButton(onClick={vm.addMarker(item,s.selectedMs,"Vocalización")}){Text(stringResource(R.string.ui_mark_vocalization))}
            }
            vm.markers.forEach {m->Row {
                TextButton(onClick={s.selectedMs=m.timeMs;vm.seekPlayback(item,m.timeMs)},modifier=Modifier.weight(1f)){Text("${context.markerLabel(m.type)} · ${seconds(m.timeMs)}")}
                TextButton(onClick={vm.removeMarker(item,m.id)}){Text(stringResource(R.string.ui_remove))}
            }}
            Text(stringResource(R.string.ui_manual_markers_a_strong_harmonic_does_not_establish_a_toot_or_identify_a_vowel),style=MaterialTheme.typography.bodySmall)
        }
    }
    RecordingAnalysisPanel(vm,item)
    AcousticHelp()
}

@Composable
internal fun PitchLayerChoice(vm:TallerViewModel){
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(2.dp),
        modifier=Modifier.testTag("pitch-layer-choice")){
        listOf(
            PitchLayerMode.FUNDAMENTAL to R.string.pitch_layer_fundamental,
            PitchLayerMode.COMPONENT to R.string.pitch_layer_component,
            PitchLayerMode.BOTH to R.string.pitch_layer_both,
        ).forEach {(mode,label)->
            FilterChip(selected=vm.pitchLayerMode==mode,onClick={vm.choosePitchLayer(mode)},
                label={Text(stringResource(label))},modifier=Modifier.heightIn(min=48.dp)
                    .testTag("pitch-layer-${mode.name.lowercase()}"))
        }
    }
}

@Composable
internal fun DroneComponentLegend(modifier:Modifier=Modifier,showExplanation:Boolean=true){
    Column(modifier){
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Canvas(Modifier.width(28.dp).height(8.dp)){
                drawLine(androidx.compose.ui.graphics.Color(0xFF65C9D1),Offset(0f,size.height/2),
                    Offset(size.width,size.height/2),strokeWidth=2.dp.toPx(),
                    pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f,5f)))
            }
            Text(stringResource(R.string.drone_component_estimated),style=MaterialTheme.typography.bodySmall)
        }
        if(showExplanation)Text(stringResource(R.string.drone_component_explanation),style=MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun SessionRelations(vm:TallerViewModel,item:RecordingStore.Session) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if(item.instrument==ar.didgetaller.core.InstrumentProfile.NATIVE_AMERICAN_FLUTE){
        Text(stringResource(R.string.ui_native_american_flute_audio_analysis_laboratory_designs_simulate_didgeridoos_a),style=MaterialTheme.typography.bodySmall)
        return
    }
    var expanded by remember {mutableStateOf(false)}
    val design=vm.designs.find {it.id==vm.associatedDesignId}
    LabCard(context.getString(R.string.ui_instrument_and_design)) {
        Box {
            OutlinedButton(onClick={expanded=true}){Text(design?.name ?: if(vm.associatedDesignId!=null)stringResource(R.string.ui_design_in_trash) else stringResource(R.string.ui_associate_a_design))}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text(stringResource(R.string.ui_no_association))},onClick={vm.associate(item,null);expanded=false})
                vm.designs.forEach {candidate ->DropdownMenuItem(text={Text(candidate.name)},onClick={vm.associate(item,candidate.id);expanded=false})}
            }
        }
        design?.let {Text(stringResource(R.string.ui_entered_geometry_mm_mouthpiece_mm_bell_mm, it.shape.lengthMm, it.shape.mouthMm, it.shape.bellMm))}
        vm.associatedSimulation?.let {sim ->
            val analysis=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
            val summary=if(analysis?.result!=null)analysis.result.summary else item.summary
            Text(stringResource(R.string.ui_simulation_model_resonances)+sim.peaks.take(6).joinToString(" · "){"${hzLabel(it.hz)} (${context.noteLabel(it.hz)})"},color=MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.ui_audio_fundamental, if(analysis?.result!=null && analysis.endMs!=null)stringResource(R.string.ui_analyzed_excerpt) else stringResource(R.string.ui_session))+(summary?.let {"${context.noteLabel(it.typicalHz)} · ${hzLabel(it.typicalHz)}"} ?: stringResource(R.string.ui_no_reliable_estimate)))
            summary?.let {Text(stringResource(R.string.ui_exploratory_difference_from_the_first_simulated_resonance_hz, it.typicalHz-sim.baseHz))}
            analysis?.result?.harmonics?.peaks?.filter {it.order!=null}?.take(8)?.let {peaks ->Text(stringResource(R.string.ui_measured_components_of_the_analyzed_wav)+peaks.joinToString(" · "){"H${it.order}? ${hzLabel(it.hz)}"})}
        }
        Text(stringResource(R.string.ui_bore_impedance_resonances_and_emitted_harmonics_are_different_quantities_they),style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun AcousticHelp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var show by remember {mutableStateOf(false)}
    TextButton(onClick={show=true}){Text(stringResource(R.string.ui_what_does_each_term_mean))}
    if(show)AlertDialog(onDismissRequest={show=false},title={Text(stringResource(R.string.ui_short_acoustic_glossary))},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf(
                "Hz" to context.getString(R.string.ui_number_of_cycles_per_second_a_higher_frequency_corresponds_to_a_higher_pitched),
                context.getString(R.string.ui_note) to context.getString(R.string.ui_musical_name_of_a_nearby_frequency_using_a4_440_hz_as_reference),
                context.getString(R.string.ui_fundamental) to context.getString(R.string.ui_frequency_associated_with_the_main_repetition_of_the_vibration_it_can_be_weake),
                context.getString(R.string.ui_harmonic) to context.getString(R.string.ui_component_near_an_integer_multiple_of_the_fundamental_h2_is_near_twice_its_fre),
                context.getString(R.string.ui_cents) to context.getString(R.string.ui_one_semitone_equals_100_cents_10_indicates_a_small_deviation_toward_a_higher_p),
                context.getString(R.string.ui_toot) to context.getString(R.string.ui_higher_register_played_by_the_performer_a_strong_drone_peak_does_not_imply_a_r),
                context.getString(R.string.ui_spectrum) to context.getString(R.string.ui_energy_distribution_by_frequency_in_a_window_or_excerpt),
                context.getString(R.string.ui_spectrogram) to context.getString(R.string.ui_a_series_of_spectra_horizontal_time_vertical_frequency_and_color_for_relative),
                context.getString(R.string.ui_relative_db) to context.getString(R.string.ui_logarithmic_difference_from_an_analysis_reference_this_is_not_absolute_sound_p),
                context.getString(R.string.ui_impedance) to context.getString(R.string.ui_relationship_between_acoustic_pressure_and_volume_flow_its_resonances_predict),
                context.getString(R.string.ui_centroid) to context.getString(R.string.ui_power_weighted_average_of_frequencies_helps_explore_energy_distribution_withou)).forEach {(term,meaning)->Text("$term: $meaning")}
        }
    },confirmButton={TextButton(onClick={show=false}){Text(stringResource(R.string.ui_got_it))}})
}
