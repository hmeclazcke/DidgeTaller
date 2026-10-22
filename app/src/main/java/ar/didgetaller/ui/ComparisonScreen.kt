package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.Music
import ar.didgetaller.core.WavAnalysis
import ar.didgetaller.data.RecordingStore

@Composable
private fun SessionPicker(label:String,items:List<RecordingStore.Session>,selected:String?,onSelect:(String)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember {mutableStateOf(false)}
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text("$label: "+(items.find {it.id==selected}?.let {context.sessionName(it)} ?: stringResource(R.string.ui_choose_session)),maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=350.dp)) {
            items.forEach {item ->DropdownMenuItem(text={Text(context.sessionName(item)+" · "+(item.summary?.let {hzLabel(it.typicalHz)} ?: stringResource(R.string.ui_no_analysis)))},onClick={onSelect(item.id);expanded=false})}
        }
    }
}

@Composable
internal fun ComparisonScreen(vm:TallerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var a by rememberSaveable {mutableStateOf(vm.sessions.firstOrNull()?.id)}
    var b by rememberSaveable {mutableStateOf(vm.sessions.drop(1).firstOrNull()?.id)}
    var aStart by rememberSaveable {mutableStateOf("")};var aEnd by rememberSaveable {mutableStateOf("")}
    var bStart by rememberSaveable {mutableStateOf("")};var bEnd by rememberSaveable {mutableStateOf("")}
    var show by rememberSaveable {mutableIntStateOf(2)}
    var normalize by rememberSaveable {mutableStateOf(true)}
    val first=vm.sessions.find {it.id==a};val second=vm.sessions.find {it.id==b}
    fun ms(value:String)=value.replace(',','.').toDoubleOrNull()?.takeIf {it.isFinite()}?.let {(it*1000).toLong()}
    fun valid(start:String,end:String,duration:Long)=
        (start.isBlank() || ms(start)?.let {it>=0 && it<duration}==true) &&
        (end.isBlank() || ms(end)?.let {it> (ms(start) ?: 0) && it<=duration}==true)
    val valid=first!=null && second!=null && valid(aStart,aEnd,first.durationMs) && valid(bStart,bEnd,second.durationMs)
    val state=vm.comparison
    LabCard(context.getString(R.string.ui_compare_sessions_or_excerpts)) {
        Text(stringResource(R.string.ui_choose_two_recordings_optionally_enter_start_and_end_times_in_seconds_leave_th))
        SessionPicker(context.getString(R.string.ui_session_a),vm.sessions,a){a=it;vm.cancelComparison(clear=true)}
        IntervalFields("A",aStart,aEnd,{aStart=it;vm.cancelComparison(clear=true)},{aEnd=it;vm.cancelComparison(clear=true)})
        SessionPicker(context.getString(R.string.ui_session_b),vm.sessions,b){b=it;vm.cancelComparison(clear=true)}
        IntervalFields("B",bStart,bEnd,{bStart=it;vm.cancelComparison(clear=true)},{bEnd=it;vm.cancelComparison(clear=true)})
        if(!valid)Text(stringResource(R.string.ui_choose_two_sessions_and_a_valid_interval_within_each_recording),color=MaterialTheme.colorScheme.secondary)
        if(state.running){LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth());Text(stringResource(R.string.ui_comparing, state.progress));TextButton(onClick={vm.cancelComparison()}){Text(stringResource(R.string.ui_cancel_comparison))}}
        else Button(onClick={vm.compare(first!!,second!!,ms(aStart) ?: 0,ms(aEnd),ms(bStart) ?: 0,ms(bEnd))},enabled=valid){Text(stringResource(R.string.ui_compare))}
        state.error?.let {Text(context.localizedDiagnostic(it),color=MaterialTheme.colorScheme.error)}
        if(state.a!=null && state.b!=null){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(context.getString(R.string.ui_a_only),context.getString(R.string.ui_b_only),context.getString(R.string.ui_both)).forEachIndexed {i,label ->FilterChip(selected=show==i,onClick={show=i},label={Text(label)})}
            }
            Row {Text(stringResource(R.string.ui_normalize_each_spectrum),Modifier.weight(1f));Switch(checked=normalize,onCheckedChange={normalize=it})}
            SpectrumChart(state.a,state.b,showA=show!=1,showB=show!=0,normalize=normalize)
            Row(Modifier.fillMaxWidth()) {Text(stringResource(R.string.ui_a_solid_line),Modifier.weight(1f),color=traceA);Text(stringResource(R.string.ui_b_dashed_line),Modifier.weight(1f),color=traceB)}
            ComparisonMetrics(state.a,state.b)
        }
        Text(stringResource(R.string.ui_normalization_helps_compare_distributions_but_removes_the_overall_level_differ),style=MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.ui_to_explore_vocalizations_compare_excerpts_from_the_same_instrument_under_simil),style=MaterialTheme.typography.bodySmall)
        AcousticHelp()
    }
}

@Composable
private fun IntervalFields(label:String,start:String,end:String,onStart:(String)->Unit,onEnd:(String)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value=start,onValueChange=onStart,label={Text(stringResource(R.string.ui_start_s, label))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f).testTag("compare-$label-start"))
        OutlinedTextField(value=end,onValueChange=onEnd,label={Text(stringResource(R.string.ui_end_s, label))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f).testTag("compare-$label-end"))
    }
}

@Composable
private fun ComparisonMetrics(a:WavAnalysis.Result,b:WavAnalysis.Result) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun hz(value:Double?)=value?.let {hzLabel(it)} ?: context.getString(R.string.ui_uncertain)
    val rows=listOf(
        context.getString(R.string.ui_analyzed_interval) to ("${seconds(a.startFrame*1000/a.sampleRate)}–${seconds(a.endFrame*1000/a.sampleRate)}" to "${seconds(b.startFrame*1000/b.sampleRate)}–${seconds(b.endFrame*1000/b.sampleRate)}"),
        context.getString(R.string.ui_typical_fundamental) to (hz(a.summary?.typicalHz) to hz(b.summary?.typicalHz)),
        context.getString(R.string.ui_note) to ((a.summary?.let {context.noteLabel(it.typicalHz)} ?: context.getString(R.string.ui_uncertain)) to (b.summary?.let {context.noteLabel(it.typicalHz)} ?: context.getString(R.string.ui_uncertain))),
        context.getString(R.string.ui_stability) to ((a.summary?.let {"${it.stabilityPercent}%"} ?: context.getString(R.string.ui_no_data)) to (b.summary?.let {"${it.stabilityPercent}%"} ?: context.getString(R.string.ui_no_data))),
        context.getString(R.string.ui_typical_range) to ((a.summary?.let {"${hz(it.lowHz)}–${hz(it.highHz)}"} ?: context.getString(R.string.ui_no_data)) to (b.summary?.let {"${hz(it.lowHz)}–${hz(it.highHz)}"} ?: context.getString(R.string.ui_no_data))),
        context.getString(R.string.ui_power_centroid) to (hz(a.spectralCentroidHz) to hz(b.spectralCentroidHz)),
        context.getString(R.string.ui_duration) to (seconds((a.durationSeconds*1000).toLong()) to seconds((b.durationSeconds*1000).toLong())),
        context.getString(R.string.ui_valid_windows) to ("${(a.validWindowFraction*100).toInt()}%" to "${(b.validWindowFraction*100).toInt()}%"))
    rows.forEach {(label,values)->Text(label,style=MaterialTheme.typography.labelMedium);Row(Modifier.fillMaxWidth()){Text(values.first,Modifier.weight(1f));Text(values.second,Modifier.weight(1f))};HorizontalDivider()}
    Text(stringResource(R.string.ui_energy_distribution_by_band),style=MaterialTheme.typography.titleSmall)
    a.bandEnergies.zip(b.bandEnergies).forEach {(x,y)->Text("${x.fromHz.toInt()}–${x.toHz.toInt()} Hz: A ${(x.fraction*100).toInt()}% · B ${(y.fraction*100).toInt()}%")}
    listOf("A" to a,"B" to b).forEach {(label,r)->
        Text(stringResource(R.string.ui_harmonic_candidates, label),style=MaterialTheme.typography.titleSmall)
        Text(r.harmonics.peaks.filter {it.order!=null}.take(12).joinToString(" · "){"H${it.order}? ${hzLabel(it.hz)} (%.1f dB)".format(it.relativeDb)}.ifEmpty {stringResource(R.string.ui_no_confidently_assignable_candidates)})
    }
}
