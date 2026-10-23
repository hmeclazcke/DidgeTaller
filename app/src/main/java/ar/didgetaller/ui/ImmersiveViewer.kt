package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ar.didgetaller.TallerViewModel
import ar.didgetaller.audio.VisualRequest
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import kotlinx.coroutines.delay
import kotlin.math.*

internal fun Context.activity():Activity?=when(this){is Activity->this;is ContextWrapper->baseContext.activity();else->null}

internal fun sessionPlotPoints(vm:TallerViewModel,item:RecordingStore.Session):List<SessionAnalysis.Point>{
    val analysis=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    // Reanalysis is a different estimator/window cadence, not a replacement for captured readings.
    return item.timeline.ifEmpty {analysis?.result?.timeline.orEmpty()}
}

@Composable
internal fun playbackCursor(vm:TallerViewModel,item:RecordingStore.Session):Long {
    var cursor by remember(item.id){mutableLongStateOf(0)}
    LaunchedEffect(item.id,vm.playingId){
        while(vm.playingId==item.id){cursor=vm.playbackPositionMs().toLong();delay(80)}
        cursor=0
    }
    return cursor
}

/** Stable session owner supplies both preview and immersive adapters. No WAV in camera state. */
@Composable
internal fun SessionPlot(vm:TallerViewModel,item:RecordingStore.Session,cursor:Long,modifier:Modifier,
                         immersive:Boolean=false,onExpand:()->Unit={}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val s=if(immersive)vm.explorer(item.id) else vm.preview(item.id);val visual=vm.workspace.state
    val language=LocalConfiguration.current.locales.toLanguageTags()
    LaunchedEffect(s,language){
        if(s.inspectionLanguage!=null && s.inspectionLanguage!=language)s.inspected=null
        s.inspectionLanguage=language
    }
    val duration=visual.durationMs.takeIf {it>0} ?: item.durationMs.coerceAtLeast(2)
    val analysis=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    val result=analysis?.result
    val points=sessionPlotPoints(vm,item)
    val trace=remember(points,item.instrument){PitchTrace.build(points,item.instrument)}
    val swipe=vm.swipeComparison.takeIf {it.sessionId==item.id && it.enabled}?.curve
    val camera=s.camera(duration,visual.rate,points)
    LaunchedEffect(item.id){vm.openTools(item)}
    LaunchedEffect(camera,s.plot,s.logarithmic,s.rangeDb,s.gestureActive,visual.frames){
        if(visual.frames>1 && !s.gestureActive && s.plot in listOf(AcousticPlot.SPECTROGRAM,AcousticPlot.WAVE)){
            vm.workspace.render(item,VisualRequest(camera.x.low.toLong(),camera.x.high.toLong().coerceAtLeast(camera.x.low.toLong()+1),
                camera.y.low.coerceAtLeast(0.0),camera.y.high,s.logarithmic,s.rangeDb,s.spectrumReference),s.plot==AcousticPlot.SPECTROGRAM)
        }
    }
    LaunchedEffect(visual.spectrogram?.reference){if(s.spectrumReference<=1e-16)s.spectrumReference=visual.spectrogram?.reference ?: 0.0}
    LaunchedEffect(cursor,s.follow,s.plot){
        if(immersive && s.follow && s.plot.temporal && vm.playingId==item.id && camera.x.span<duration &&
            (cursor<camera.x.low || cursor>camera.x.high-camera.x.span*.08)){
            val domain=s.domains(duration,visual.rate,points).first
            val left=max(0.0,cursor-camera.x.span*.12)
            s.cameras[s.plot]=camera.copy(x=domain.constrain(AxisWindow(left,left+camera.x.span)))
        }
    }
    PlotSurface(s,visual,camera,points,result,if(vm.playingId==item.id)cursor else -1,modifier,immersive,swipe=swipe,onPreview=onExpand,
        onReset={s.reset(duration,visual.rate,points)},onTransform={x,y,zx,zy,dx,dy->s.transform(duration,visual.rate,points,x,y,zx,zy,dx,dy)},
        onPoint={x,y,inspect ->
            s.selectedY=y;s.follow=false
            if(s.plot.temporal){
                val time=x.roundToLong().coerceIn(0,duration);s.selectedMs=time
                if(!inspect)when(s.mode){
                    1->{s.regionStart=time;s.regionEnd=max(s.regionEnd,time)}
                    2->{s.regionEnd=time;s.regionStart=min(s.regionStart,time)}
                    else->vm.seekPlayback(item,time)
                }
                if(s.mode!=0 && !inspect){s.looping=false;vm.setPlaybackLoopOff()}
                val point=points.minByOrNull {abs(it.timeMs-time)}?.takeIf {abs(it.timeMs-time)<=750}
                s.inspected=when(s.plot){
                    AcousticPlot.PITCH->"${seconds(time)} · "+(point?.let {p->trace.firstOrNull {it.point===p}}?.let {sample->
                        val hz=sample.point.hz!!
                        if(sample.uncertain) return@let context.getString(R.string.ui_uncertain_estimate_confidence_yin_no_confirmed_note, hzLabel(hz), context.reasonLabel(sample.reason), sample.point.confidence)
                        val musical=Music.pitch(hz)!!
                        context.getString(R.string.ui_cents_a4_440_periodicity_variation, hzLabel(hz), musical.note, musical.cents, sample.point.confidence, sample.variationCents?.let {"%.0f cents".format(displayLocale,it)} ?: context.getString(R.string.ui_no_neighbor))+
                            if(sample.bridged)context.getString(R.string.ui_display_link) else ""
                    } ?: context.getString(R.string.ui_no_nearby_reliable_reading))
                    AcousticPlot.WAVE->{val wave=visual.wave;val frame=time*visual.rate/1000
                        if(wave!=null && frame in wave.start until wave.end){val i=((frame-wave.start)*wave.min.size/(wave.end-wave.start)).toInt().coerceIn(wave.min.indices)
                            context.getString(R.string.ui_pcm_amplitude_to, seconds(time), axisLabel(wave.min[i].toDouble(),.1), axisLabel(wave.max[i].toDouble(),.1))}else context.getString(R.string.ui_waiting_for_samples_from_this_region, seconds(time))}
                    else->{val tile=visual.spectrogram
                        if(tile!=null && time*visual.rate/1000 in tile.start until tile.end && y in tile.lowHz..tile.highHz){
                            val col=((time*visual.rate/1000.0-tile.start)/(tile.end-tile.start)*tile.columns).toInt().coerceIn(0,tile.columns-1)
                            val row=(AxisWindow(tile.lowHz,tile.highHz).fraction(y,tile.logarithmic)*tile.rows).toInt().coerceIn(0,tile.rows-1)
                            val power=tile.power[col*tile.rows+row].toDouble();val ref=s.spectrumReference
                            "${seconds(time)} · ${hzLabel(y)} · "+if(ref>1e-16)context.getString(R.string.ui_db_rel_stft_cell, 10*log10(max(1e-30,power)/ref)) else context.getString(R.string.ui_no_significant_energy)
                        }else context.getString(R.string.ui_no_calculated_cell, seconds(time), hzLabel(y))}
                }
                if(s.plot==AcousticPlot.PITCH && vm.swipeComparison.enabled){
                    val estimate=swipe?.nearest(time)?.hz
                    s.inspected=s.inspected.orEmpty()+"\n"+context.getString(R.string.swipe_cursor,estimate?.let(::hzLabel) ?: context.getString(R.string.swipe_missing))
                }
            }else{
                val peak=result?.harmonics?.peaks?.minByOrNull {abs(it.hz-x)}
                s.inspected=if(peak!=null && abs(peak.hz-x)<camera.x.span*.06)"${peak.order?.let {context.getString(R.string.ui_h_candidate, it)} ?: context.getString(R.string.ui_unassigned_peak_66f03c)} · ${hzLabel(peak.hz)} · %.1f dB rel".format(displayLocale,peak.relativeDb)+(peak.deviationCents?.let {" · %+.1f cents".format(displayLocale,it)} ?: "")
                    else context.getString(R.string.ui_no_nearby_assigned_peak, hzLabel(x))
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImmersiveViewer(vm:TallerViewModel,item:RecordingStore.Session,initialPlot:String?,onBack:()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state=vm.explorer(item.id);val visual=vm.workspace.state
    val points=sessionPlotPoints(vm,item)
    val recording=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    val cursor=playbackCursor(vm,item)
    var tools by rememberSaveable(item.id){mutableStateOf(false)}
    var plots by remember {mutableStateOf(false)}
    val activity=LocalContext.current.activity()
    val config=LocalConfiguration.current
    val landscape=config.screenWidthDp>config.screenHeightDp
    // A short landscape phone cannot fit two 48dp bars plus readable axes at 70%.
    // Merge transport into the toolbar there; keep two bars on taller viewports.
    val singleBar=landscape && (config.screenHeightDp<400 || config.fontScale>=1.3f)
    DisposableEffect(activity){
        val window=activity?.window
        val controller=window?.let {androidx.core.view.WindowCompat.getInsetsController(it,it.decorView)}
        val wasVisible=window?.decorView?.let {androidx.core.view.ViewCompat.getRootWindowInsets(it)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars())} ?: true
        val behavior=controller?.systemBarsBehavior
        controller?.systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        onDispose {
            if(wasVisible)controller?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            if(behavior!=null)controller?.systemBarsBehavior=behavior
        }
    }
    val duration=visual.durationMs.takeIf {it>0} ?: item.durationMs
    val camera=state.camera(duration,visual.rate,points)
    val range=state.pitch(points)
    val reading=state.inspected ?:
        "${axisLabel(camera.y.low,camera.y.span)}–${axisLabel(camera.y.high,camera.y.span)} ${if(state.plot==AcousticPlot.WAVE)"PCM" else if(state.plot==AcousticPlot.SPECTRUM)"dB rel" else "Hz"}"
    val playing=vm.playingId==item.id && !vm.playbackPaused
    @Composable fun transport(){
    val context = androidx.compose.ui.platform.LocalContext.current
        IconButton(onClick={vm.togglePlayback(item)},modifier=Modifier.testTag("viewer-play")){
            Icon(if(playing)ViewerIcons.Pause else Icons.Default.PlayArrow,if(playing)context.getString(R.string.ui_pause) else context.getString(R.string.ui_play))
        }
        Text("${seconds(cursor)} / ${seconds(duration)}",Modifier.widthIn(max=if(landscape)180.dp else if(config.fontScale>=1.3f)200.dp else 120.dp).testTag("viewer-time"),style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
    @Composable fun follow(){
    val context = androidx.compose.ui.platform.LocalContext.current
        if(state.plot.temporal)IconToggleButton(checked=state.follow,onCheckedChange={state.follow=it}){
            Icon(Icons.Default.ArrowForward,if(state.follow)context.getString(R.string.ui_following_on) else context.getString(R.string.ui_follow))
        }
    }
    // This is the sole owner of safe insets. Outer Scaffold supplies zero in viewer.
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("acoustic-viewer")){
        Row(Modifier.fillMaxWidth().height(48.dp).testTag("viewer-toolbar"),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,context.getString(R.string.ui_back_to_session))}
            Box(Modifier.weight(1f)){
                TextButton(onClick={plots=true},modifier=Modifier.fillMaxWidth().semantics {contentDescription=context.getString(R.string.ui_change_chart)}){
                    Text(context.plotLabel(state.plot)+" ▾",
                        maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded=plots,onDismissRequest={plots=false}){AcousticPlot.entries.forEach {kind->
                    DropdownMenuItem(text={Text(context.plotLabel(kind))},onClick={state.plot=kind;state.inspected=null;plots=false})
                }}
            }
            if(singleBar){transport();follow()}
            IconButton(onClick={state.reset(duration,visual.rate,points)},modifier=Modifier.testTag("reset-viewport")){Icon(Icons.Default.Refresh,context.getString(R.string.ui_reset_view))}
            IconButton(onClick={
                if(vm.viewerOriginalOrientation==null)vm.viewerOriginalOrientation=activity?.requestedOrientation
                activity?.requestedOrientation=if(landscape)ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            },modifier=Modifier.testTag("rotate-viewer")){Icon(ViewerIcons.Rotate,context.getString(R.string.ui_change_orientation))}
            IconButton(onClick={tools=true},modifier=Modifier.testTag("viewer-tools")){
                BadgedBox(badge={if(state.inspected!=null)Badge{Text("i")}}){Icon(Icons.Default.MoreVert,stringResource(R.string.ui_tools))}
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()){
            SessionPlot(vm,item,cursor,Modifier.fillMaxSize(),immersive=true)
            if(state.plot==AcousticPlot.PITCH && range.valid==0)Text(
                context.getString(R.string.ui_no_reliable_readings_audio_is_preserved),Modifier.align(Alignment.Center).padding(16.dp),
                style=MaterialTheme.typography.bodySmall)
            if(state.plot==AcousticPlot.SPECTRUM && recording?.result==null && recording?.running!=true)
                TextButton(onClick={vm.analyzeRecording(item)},modifier=Modifier.align(Alignment.Center).testTag("viewer-analyze")){Text(stringResource(R.string.ui_analyze_recording))}
            if(visual.error!=null)TextButton(onClick={vm.workspace.open(item)},modifier=Modifier.align(Alignment.Center)){
                Text(stringResource(R.string.ui_retry, context.localizedDiagnostic(visual.error.orEmpty())),color=MaterialTheme.colorScheme.error)
            }
            if(state.plot==AcousticPlot.SPECTROGRAM)Row(Modifier.align(Alignment.TopEnd).padding(end=12.dp).heightIn(min=16.dp),verticalAlignment=Alignment.CenterVertically){
                Text("−${state.rangeDb.toInt()}",style=MaterialTheme.typography.labelSmall)
                Canvas(Modifier.width(72.dp).height(6.dp).padding(horizontal=4.dp)){
                    for(x in 0 until size.width.toInt())drawLine(Color(SpectralPalette.color(-state.rangeDb+state.rangeDb*x/size.width,state.rangeDb)),Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height),1f)
                }
                Text("0 dB rel",style=MaterialTheme.typography.labelSmall)
            }
            if(visual.loading || recording?.running==true)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter))
        }
        if(state.plot==AcousticPlot.PITCH && vm.swipeComparison.sessionId==item.id && vm.swipeComparison.enabled){
            SwipeLegend()
            if(vm.swipeComparison.running)LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(2.dp))
        }
        if(!singleBar)Row(Modifier.fillMaxWidth().height(48.dp).testTag("viewer-transport"),verticalAlignment=Alignment.CenterVertically){
            transport()
            if(landscape)TextButton(onClick={tools=true},modifier=Modifier.weight(1f).testTag("open-reading")){
                Text(if(state.inspected!=null)stringResource(R.string.ui_full_reading) else reading,style=MaterialTheme.typography.labelSmall,maxLines=1)
            }
            else Spacer(Modifier.weight(1f))
            if(config.fontScale<1.3f){
                IconButton(onClick={state.transform(duration,visual.rate,points,.5,.5,2.0,2.0,0.0,0.0)}){Icon(Icons.Default.Add,context.getString(R.string.ui_zoom_in))}
                IconButton(onClick={state.transform(duration,visual.rate,points,.5,.5,.5,.5,0.0,0.0)}){Text("−",Modifier.semantics {contentDescription=context.getString(R.string.ui_zoom_out)})}
            }
            follow()
        }
        if(!landscape)TextButton(onClick={tools=true},modifier=Modifier.fillMaxWidth()){
            Text(reading,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
    if(tools)ModalBottomSheet(onDismissRequest={tools=false}){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(stringResource(R.string.ui_explorer_tools),style=MaterialTheme.typography.titleLarge)
            Text(reading,modifier=Modifier.fillMaxWidth().testTag("complete-reading"),style=MaterialTheme.typography.bodySmall)
            if(state.plot==AcousticPlot.PITCH)Text(if(item.timeline.isEmpty())stringResource(R.string.ui_source_wav_reanalysis) else stringResource(R.string.ui_source_original_recording_readings_same_series_as_library),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.ui_drag_along_both_axes_pinch_to_zoom_hold_to_inspect),style=MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick={state.transform(duration,visual.rate,points,.5,.5,2.0,2.0,0.0,0.0)}){Text(stringResource(R.string.ui_zoom_in))}
                TextButton(onClick={state.transform(duration,visual.rate,points,.5,.5,.5,.5,0.0,0.0)}){Text(stringResource(R.string.ui_zoom_out))}
            }
            if(state.plot==AcousticPlot.SPECTRUM){
                Text(if(recording?.endMs!=null)stringResource(R.string.ui_excerpt_spectrum) else stringResource(R.string.ui_full_recording_spectrum_h_candidate),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={if(recording?.running==true)vm.cancelRecordingAnalysis() else vm.analyzeRecording(item)},modifier=Modifier.testTag("viewer-analyze-tools")){Text(if(recording?.running==true)stringResource(R.string.ui_cancel_analysis) else stringResource(R.string.ui_analyze_recording))}
            }
            (visual.error ?: recording?.error)?.let {Text(context.localizedDiagnostic(it),color=MaterialTheme.colorScheme.error)}
            PlotTools(vm,item)
            FragmentTools(vm,item)
            if(state.plot==AcousticPlot.PITCH){
                Text(stringResource(R.string.advanced_diagnostics),style=MaterialTheme.typography.titleMedium)
                SwipeComparisonPanel(vm,item,showLegend=false)
            }
            TextButton(onClick={tools=false}){Text(stringResource(R.string.ui_back_to_chart))}
        }
    }
}

@Composable
internal fun PlotTools(vm:TallerViewModel,item:RecordingStore.Session,s:ExplorerState=vm.explorer(item.id)){
    val context = androidx.compose.ui.platform.LocalContext.current
    val visual=vm.workspace.state
    val points=sessionPlotPoints(vm,item)
    val duration=visual.durationMs.takeIf {it>0} ?: item.durationMs;val camera=s.camera(duration,visual.rate,points)
    var exact by remember(s.plot){mutableStateOf(false)}
    var from by remember(s.plot){mutableStateOf("")};var to by remember(s.plot){mutableStateOf("")}
    val axis=if(s.plot==AcousticPlot.SPECTRUM)camera.x else camera.y
    TextButton(onClick={from="%.4f".format(displayLocale,axis.low);to="%.4f".format(displayLocale,axis.high);exact=!exact}){Text(stringResource(R.string.ui_enter_exact_range))}
    if(exact){
        val unit=when(s.plot){AcousticPlot.WAVE->"PCM";else->"Hz"}
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(value=from,onValueChange={from=it},label={Text(stringResource(R.string.ui_from, unit))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(s.plot==AcousticPlot.WAVE)KeyboardType.Text else KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=to,onValueChange={to=it},label={Text(stringResource(R.string.ui_to, unit))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(s.plot==AcousticPlot.WAVE)KeyboardType.Text else KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        }
        val a=from.replace(',','.').toDoubleOrNull();val b=to.replace(',','.').toDoubleOrNull()
        val domain=s.domains(duration,visual.rate,points,fullPitchDomain=true).let {if(s.plot==AcousticPlot.SPECTRUM)it.first else it.second}
        val valid=a!=null && b!=null && a.isFinite() && b.isFinite() && a>=domain.low && b<=domain.high && b>a
        TextButton(onClick={val window=domain.constrain(AxisWindow(a!!,b!!));if(s.plot==AcousticPlot.PITCH)s.extendedPitchRange=true;s.cameras[s.plot]=if(s.plot==AcousticPlot.SPECTRUM)camera.copy(x=window)else camera.copy(y=window);s.follow=false;exact=false},enabled=valid){Text(stringResource(R.string.ui_apply_range))}
        if(!valid)Text(stringResource(R.string.ui_enter_an_interval_between_and, axisLabel(domain.low,domain.high-domain.low), axisLabel(domain.high,domain.high-domain.low), unit),style=MaterialTheme.typography.bodySmall)
    }
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf(context.getString(R.string.ui_previous) to (-.3 to 0.0),context.getString(R.string.ui_next) to (.3 to 0.0),context.getString(R.string.ui_up) to (0.0 to .3),context.getString(R.string.ui_down) to (0.0 to -.3)).forEach {(label,pan)->
            OutlinedButton(onClick={s.transform(duration,visual.rate,points,.5,.5,1.0,1.0,-pan.first,pan.second)}){Text(label,maxLines=1)}
        }
    }
    if(s.plot==AcousticPlot.SPECTROGRAM || s.plot==AcousticPlot.SPECTRUM){
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            (if(item.instrument==InstrumentProfile.NATIVE_AMERICAN_FLUTE)listOf(2000.0,4000.0,visual.rate/2.0) else listOf(100.0,500.0,2000.0,visual.rate/2.0)).distinct().forEach {limit->
                FilterChip(selected=(if(s.plot.temporal)camera.y.high else camera.x.high)==limit,onClick={
                    val window=AxisWindow(if(s.plot==AcousticPlot.SPECTROGRAM && s.logarithmic)visual.rate/32768.0 else 0.0,limit)
                    s.cameras[s.plot]=if(s.plot.temporal)camera.copy(y=window) else camera.copy(x=window);s.follow=false
                },label={Text(if(limit==visual.rate/2.0)stringResource(R.string.ui_full) else "${limit.toInt()} Hz",maxLines=1)})
            }
        }
    }
    if(s.plot==AcousticPlot.PITCH){
        val range=s.pitch(points)
        Text(pluralStringResource(R.plurals.trend_reading_count, range.valid, range.valid),style=MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick={s.extendedPitchRange=false;s.cameras[s.plot]=camera.copy(y=s.overview(points).window);s.follow=false}){Text(stringResource(R.string.ui_overview))}
            TextButton(onClick={s.extendedPitchRange=false;s.cameras[s.plot]=camera.copy(y=range.window);s.follow=false}){Text(stringResource(R.string.ui_fitted_view))}
        }
        val trace=remember(points,s.instrument){PitchTrace.build(points,s.instrument)}
        Text(stringResource(R.string.ui_green_variation_25_cents_and_periodicity_0_90_yellow_up_to_60_cents_periodicit),style=MaterialTheme.typography.bodySmall)
        val uncertain=trace.count {it.uncertain}
        Text(stringResource(R.string.ui_outside_fitted_original_readings_excluded_from_the_trend_isolated_excursions_a, range.clipped, uncertain),style=MaterialTheme.typography.bodySmall)
        Row(verticalAlignment=Alignment.CenterVertically){Text(stringResource(R.string.ui_show_original_uncertain_readings),Modifier.weight(1f));Switch(checked=s.showUncertain,onCheckedChange={s.showUncertain=it},modifier=Modifier.testTag("show-uncertain"))}
        if(s.showUncertain)Text(stringResource(R.string.ui_orange_circles_original_estimates_with_no_confirmed_note_hold_at_their_time_to),style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={s.extendedPitchRange=true;s.showUncertain=true;s.cameras[s.plot]=camera.copy(y=s.pitch(points,true).window);s.follow=false}){Text(stringResource(R.string.ui_show_all_values))}
    }
    if(s.plot==AcousticPlot.SPECTROGRAM){
        Row(verticalAlignment=Alignment.CenterVertically){Text(stringResource(R.string.ui_logarithmic_frequency),Modifier.weight(1f));Switch(checked=s.logarithmic,onCheckedChange={
            s.logarithmic=it
            val domain=s.domains(duration,visual.rate,points).second
            val low=max(camera.y.low,domain.low)
            val high=if(camera.y.high>low)camera.y.high else low+visual.rate/32768.0
            s.cameras[s.plot]=camera.copy(y=domain.constrain(AxisWindow(low,high)))
        },modifier=Modifier.testTag("log-frequency"))}
        Text(stringResource(R.string.ui_dynamic_range_db, s.rangeDb.toInt()))
        Slider(value=s.rangeDb.toFloat(),onValueChange={s.rangeDb=it.toDouble()},valueRange=30f..100f)
        Canvas(Modifier.fillMaxWidth().height(16.dp)){for(x in 0 until size.width.toInt())drawLine(Color(SpectralPalette.color(-s.rangeDb+s.rangeDb*x/size.width,s.rangeDb)),Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height),1f)}
        Text(stringResource(R.string.ui_to_0_relative_db_reference_fixed_in_the_first_calculated_session_view_and_reta, s.rangeDb.toInt()),style=MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.ui_stft_hann_32768_samples_hz_per_bin_ms_window_each_cell_summarizes_a_window_not, "%.2f".format(displayLocale,visual.rate/32768.0), 32768000/visual.rate),style=MaterialTheme.typography.bodySmall)
    }
    if(visual.loading)TextButton(onClick={vm.workspace.cancel()}){Text(stringResource(R.string.ui_cancel_visualization))}
    if(visual.error!=null)TextButton(onClick={vm.workspace.open(item)}){Text(stringResource(R.string.ui_retry_visualization))}
}

@Composable
internal fun FragmentTools(vm:TallerViewModel,item:RecordingStore.Session,s:ExplorerState=vm.explorer(item.id)){
    val context = androidx.compose.ui.platform.LocalContext.current
    val duration=(vm.workspace.state.durationMs.takeIf {it>0} ?: item.durationMs).coerceAtLeast(1)
    Text(stringResource(R.string.ui_select_excerpt),style=MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf(context.getString(R.string.ui_search),context.getString(R.string.ui_mark_start),context.getString(R.string.ui_mark_end)).forEachIndexed {i,label->FilterChip(selected=s.mode==i,onClick={s.mode=i},label={Text(label,maxLines=1)},modifier=Modifier.heightIn(min=48.dp).testTag("selection-mode-$i"))}
    }
    Text(stringResource(R.string.ui_start_end, seconds(s.regionStart), seconds(s.regionEnd)),Modifier.testTag("selected-region"))
    RangeSlider(value=s.regionStart.toFloat().coerceIn(0f,duration.toFloat())..s.regionEnd.toFloat().coerceIn(s.regionStart.toFloat(),duration.toFloat()),onValueChange={
        s.regionStart=it.start.toLong();s.regionEnd=it.endInclusive.toLong();s.looping=false;vm.setPlaybackLoopOff()
    },valueRange=0f..duration.toFloat())
    s.region?.let {region->
        Button(onClick={vm.analyzeRecording(item,region.first,region.last)}){Text(stringResource(R.string.ui_analyze_excerpt))}
        Row(verticalAlignment=Alignment.CenterVertically){Text(stringResource(R.string.ui_loop_excerpt),Modifier.weight(1f));Switch(checked=s.looping,onCheckedChange={s.looping=it;vm.setPlaybackLoop(item,if(it)region else null)},modifier=Modifier.testTag("loop-fragment"))}
        Text(stringResource(R.string.ui_loop_controlled_by_actual_playback_position_sample_continuity_is_not_guarantee),style=MaterialTheme.typography.bodySmall)
    }
    Text(stringResource(R.string.ui_position_analysis_of_nearby_windows, seconds(s.selectedMs)),style=MaterialTheme.typography.bodySmall)
    Slider(value=s.selectedMs.toFloat().coerceIn(0f,duration.toFloat()),onValueChange={s.selectedMs=it.toLong()},onValueChangeFinished={vm.seekPlayback(item,s.selectedMs)},valueRange=0f..duration.toFloat(),modifier=Modifier.testTag("audio-seek"))
    TextButton(onClick={vm.analyzeRecording(item,max(0,s.selectedMs-750),min(duration,s.selectedMs+750))}){Text(stringResource(R.string.ui_analyze_around_position))}
}

@Composable
internal fun LabHome(onOpen:(Int)->Unit){
    val context = androidx.compose.ui.platform.LocalContext.current
    Text(stringResource(R.string.ui_didgeridoo_laboratory),style=MaterialTheme.typography.headlineSmall)
    listOf(context.getString(R.string.ui_design) to context.getString(R.string.ui_internal_bore_profile_control_points_and_simulated_resonances),context.getString(R.string.ui_spectrum) to context.getString(R.string.ui_explore_microphone_sound_in_real_time),context.getString(R.string.ui_search) to context.getString(R.string.ui_explore_geometries_for_resonance_targets),context.getString(R.string.ui_guide) to context.getString(R.string.ui_musical_and_acoustic_concepts_clearly_explained)).forEachIndexed {i,pair->
        OutlinedCard(onClick={onOpen(i)},modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(pair.first,style=MaterialTheme.typography.titleLarge);Text(pair.second,style=MaterialTheme.typography.bodyMedium)}
        }
    }
}
