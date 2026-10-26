package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ar.didgetaller.TallerViewModel
import ar.didgetaller.audio.VisualRequest
import ar.didgetaller.core.Music
import ar.didgetaller.core.AxisWindow
import ar.didgetaller.core.SpectralPalette
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionExplorer(vm:TallerViewModel,item:RecordingStore.Session,cursor:Long,onExpand:(AcousticPlot)->Unit,
                            playbackControls:@Composable ()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val s=vm.preview(item.id)
    var tools by rememberSaveable(item.id){mutableStateOf(false)}
    var fragments by rememberSaveable(item.id){mutableStateOf(false)}
    var markers by rememberSaveable(item.id){mutableStateOf(false)}
    var showTools by rememberSaveable(item.id){mutableStateOf(false)}
    val chartRequester=remember {BringIntoViewRequester()}
    val scope=rememberCoroutineScope()
    LaunchedEffect(item.id){vm.openTools(item)}
    val musician=vm.musicalTrace(item)
    val landscape=androidx.compose.ui.platform.LocalConfiguration.current.let {
        it.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE && it.screenHeightDp<500
    }
    @Composable fun chartContent(height:androidx.compose.ui.unit.Dp) {
        if(s.plot==AcousticPlot.PITCH && item.instrument==ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            Text(stringResource(R.string.musical_drone_axis_title),style=MaterialTheme.typography.labelSmall)
        if(s.plot==AcousticPlot.SPECTRUM)
            Text(stringResource(R.string.spectrum_axis_title),style=MaterialTheme.typography.labelSmall)
        if(s.plot==AcousticPlot.PITCH && musician==null && !vm.musicalTraceFailed(item.id) &&
            item.instrument==ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(2.dp))
        if(s.plot==AcousticPlot.PITCH && vm.musicalTraceFailed(item.id))
            Text(stringResource(R.string.musical_trace_unavailable),style=MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().bringIntoViewRequester(chartRequester)) {
            SessionPlot(vm,item,cursor,Modifier.fillMaxWidth().height(height),onExpand={onExpand(s.plot)})
        }
    }
    LabCard(context.getString(R.string.ui_explore_audio), action={
        TextButton(onClick={showTools=true},modifier=Modifier.testTag("session-tools")) {Text(stringResource(R.string.ui_tools))}
        IconButton(onClick={onExpand(s.plot)},modifier=Modifier.testTag("expand-plot")){
            Icon(ViewerIcons.Expand,stringResource(R.string.ui_full_screen))
        }
    }) {
        ScrollableTabRow(selectedTabIndex=s.plot.ordinal,edgePadding=0.dp,
            modifier=Modifier.testTag("session-chart-tabs")) {
            AcousticPlot.entries.forEach {kind->Tab(selected=s.plot==kind,
                onClick={s.plot=kind;s.inspected=null;scope.launch {chartRequester.bringIntoView()}},
                modifier=Modifier.testTag("plot-tab-${kind.name.lowercase()}"),
                text={Text(if(kind==AcousticPlot.WAVE)stringResource(R.string.session_wave_label)
                    else context.plotLabel(kind,item.instrument),maxLines=1)})}
        }
        if(landscape) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(.42f),verticalArrangement=Arrangement.spacedBy(6.dp)) {playbackControls()}
            Column(Modifier.weight(.58f),verticalArrangement=Arrangement.spacedBy(6.dp)) {chartContent(160.dp)}
        } else {
            playbackControls()
            chartContent(250.dp)
        }
        if(s.plot==AcousticPlot.PITCH && item.instrument==ar.didgetaller.core.InstrumentProfile.DIDGERIDOO)
            Text(stringResource(R.string.musical_drone_description),style=MaterialTheme.typography.labelSmall)
        if(s.plot==AcousticPlot.PITCH && musician?.samples?.none {it.hz!=null}==true)
            Text(stringResource(R.string.musical_trace_unavailable),style=MaterialTheme.typography.bodySmall)
        if(s.plot==AcousticPlot.PITCH && musician?.highRegisterCandidates?.isNotEmpty()==true)
            Text(stringResource(R.string.musical_high_candidate),style=MaterialTheme.typography.labelSmall)
        if(s.plot==AcousticPlot.SPECTRUM && vm.recordingAnalysis.result==null)Text(stringResource(R.string.ui_tap_analyze_recording_to_obtain_the_actual_spectrum),style=MaterialTheme.typography.bodySmall)
        if(s.plot!=AcousticPlot.PITCH)Text((if(s.plot==AcousticPlot.SPECTROGRAM)stringResource(R.string.ui_frequency_in_hz) else "")+stringResource(R.string.ui_tap_the_chart_to_explore_with_zoom_and_pan_along_both_axes),style=MaterialTheme.typography.bodySmall)
    }
    if(showTools) ModalBottomSheet(onDismissRequest={showTools=false;vm.setSwipeComparison(item,false)},
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        modifier=Modifier.testTag("session-tools-sheet")) {
        Column(Modifier.fillMaxWidth().heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp-48).coerceAtLeast(200).dp)
            .verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ui_tools),style=MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={tools=!tools}){Text(if(tools)stringResource(R.string.ui_hide_scales) else stringResource(R.string.ui_scales_and_ranges))}
            TextButton(onClick={fragments=!fragments}){Text(if(fragments)stringResource(R.string.ui_hide_excerpts) else stringResource(R.string.ui_selection_and_excerpts))}
            TextButton(onClick={markers=!markers}){Text(stringResource(R.string.ui_markers))}
        }
        if(tools)PlotTools(vm,item,s)
        if(fragments)FragmentTools(vm,item,s)
        AdvancedPitchDiagnostics(vm,item,s.selectedMs) {
            val diagnosticState=remember(item.id){ExplorerState(item.instrument)}
            LaunchedEffect(vm.swipeComparison.enabled) {
                diagnosticState.configureComparisonRange(if(vm.swipeComparison.enabled)
                    AxisWindow(item.instrument.minHz*.95,item.instrument.initialMaxHz*1.05) else null)
            }
            Text(stringResource(R.string.pitch_yin_layer),style=MaterialTheme.typography.labelSmall)
            if(vm.swipeComparison.enabled) SwipeLegend()
            SessionPlot(vm,item,cursor,Modifier.fillMaxWidth().height(220.dp),
                stateOverride=diagnosticState,diagnosticMode=true)
            TextButton(onClick={onExpand(AcousticPlot.PITCH)},modifier=Modifier.testTag("expand-diagnostics")) {
                Text(stringResource(R.string.ui_full_screen))
            }
        }
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
        Spacer(Modifier.height(24.dp))
        }
    }
    RecordingAnalysisPanel(vm,item)
    AcousticHelp()
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
