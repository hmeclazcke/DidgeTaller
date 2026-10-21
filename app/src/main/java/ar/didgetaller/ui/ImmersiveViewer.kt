package ar.didgetaller.ui

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
    val s=if(immersive)vm.explorer(item.id) else vm.preview(item.id);val visual=vm.workspace.state
    val duration=visual.durationMs.takeIf {it>0} ?: item.durationMs.coerceAtLeast(2)
    val analysis=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    val result=analysis?.result
    val points=sessionPlotPoints(vm,item)
    val trace=remember(points,item.instrument){PitchTrace.build(points,item.instrument)}
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
    PlotSurface(s,visual,camera,points,result,if(vm.playingId==item.id)cursor else -1,modifier,immersive,onPreview=onExpand,
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
                        if(sample.uncertain) return@let "${hzLabel(hz)} · estimación incierta: ${sample.reason?.label} · confianza %.3f (YIN), sin nota confirmada".format(spanish,sample.point.confidence)
                        val musical=Music.pitch(hz)!!
                        "${hzLabel(hz)} · ${musical.note} · %+.1f cents (A4=440)".format(spanish,musical.cents)
                    } ?: "Sin lectura confiable cercana")
                    AcousticPlot.WAVE->{val wave=visual.wave;val frame=time*visual.rate/1000
                        if(wave!=null && frame in wave.start until wave.end){val i=((frame-wave.start)*wave.min.size/(wave.end-wave.start)).toInt().coerceIn(wave.min.indices)
                            "${seconds(time)} · amplitud PCM ${axisLabel(wave.min[i].toDouble(),.1)} a ${axisLabel(wave.max[i].toDouble(),.1)}"}else "${seconds(time)} · esperando muestras de esta región"}
                    else->{val tile=visual.spectrogram
                        if(tile!=null && time*visual.rate/1000 in tile.start until tile.end && y in tile.lowHz..tile.highHz){
                            val col=((time*visual.rate/1000.0-tile.start)/(tile.end-tile.start)*tile.columns).toInt().coerceIn(0,tile.columns-1)
                            val row=(AxisWindow(tile.lowHz,tile.highHz).fraction(y,tile.logarithmic)*tile.rows).toInt().coerceIn(0,tile.rows-1)
                            val power=tile.power[col*tile.rows+row].toDouble();val ref=s.spectrumReference
                            "${seconds(time)} · ${hzLabel(y)} · "+if(ref>1e-16)"%.1f dB rel (celda STFT)".format(spanish,10*log10(max(1e-30,power)/ref)) else "sin energía significativa"
                        }else "${seconds(time)} · ${hzLabel(y)} · sin celda calculada"}
                }
            }else{
                val peak=result?.harmonics?.peaks?.minByOrNull {abs(it.hz-x)}
                s.inspected=if(peak!=null && abs(peak.hz-x)<camera.x.span*.06)"${peak.order?.let {"H$it candidato"} ?: "Pico sin asignar"} · ${hzLabel(peak.hz)} · %.1f dB rel".format(spanish,peak.relativeDb)+(peak.deviationCents?.let {" · %+.1f cents".format(spanish,it)} ?: "")
                    else "${hzLabel(x)} · sin pico cercano asignado"
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImmersiveViewer(vm:TallerViewModel,item:RecordingStore.Session,initialPlot:String?,onBack:()->Unit) {
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
        IconButton(onClick={vm.togglePlayback(item)},modifier=Modifier.testTag("viewer-play")){
            Icon(if(playing)ViewerIcons.Pause else Icons.Default.PlayArrow,if(playing)"Pausar" else "Reproducir")
        }
        Text("${seconds(cursor)} / ${seconds(duration)}",Modifier.widthIn(max=if(landscape)180.dp else if(config.fontScale>=1.3f)200.dp else 120.dp).testTag("viewer-time"),style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
    @Composable fun follow(){
        if(state.plot.temporal)IconToggleButton(checked=state.follow,onCheckedChange={state.follow=it}){
            Icon(Icons.Default.ArrowForward,if(state.follow)"Seguir activado" else "Seguir")
        }
    }
    // This is the sole owner of safe insets. Outer Scaffold supplies zero in viewer.
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("acoustic-viewer")){
        Row(Modifier.fillMaxWidth().height(48.dp).testTag("viewer-toolbar"),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver a la sesión")}
            Box(Modifier.weight(1f)){
                TextButton(onClick={plots=true},modifier=Modifier.fillMaxWidth().semantics {contentDescription="Cambiar gráfico"}){
                    Text(state.plot.title+" ▾",
                        maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded=plots,onDismissRequest={plots=false}){AcousticPlot.entries.forEach {kind->
                    DropdownMenuItem(text={Text(kind.title)},onClick={state.plot=kind;state.inspected=null;plots=false})
                }}
            }
            if(singleBar){transport();follow()}
            IconButton(onClick={state.reset(duration,visual.rate,points)},modifier=Modifier.testTag("reset-viewport")){Icon(Icons.Default.Refresh,"Restablecer vista")}
            IconButton(onClick={
                if(vm.viewerOriginalOrientation==null)vm.viewerOriginalOrientation=activity?.requestedOrientation
                activity?.requestedOrientation=if(landscape)ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            },modifier=Modifier.testTag("rotate-viewer")){Icon(ViewerIcons.Rotate,"Cambiar orientación")}
            IconButton(onClick={tools=true},modifier=Modifier.testTag("viewer-tools")){
                BadgedBox(badge={if(state.inspected!=null)Badge{Text("i")}}){Icon(Icons.Default.MoreVert,"Herramientas")}
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()){
            SessionPlot(vm,item,cursor,Modifier.fillMaxSize(),immersive=true)
            if(state.plot==AcousticPlot.PITCH && range.valid==0)Text(
                "Sin lecturas confiables · el audio se conserva",Modifier.align(Alignment.Center).padding(16.dp),
                style=MaterialTheme.typography.bodySmall)
            if(state.plot==AcousticPlot.SPECTRUM && recording?.result==null && recording?.running!=true)
                TextButton(onClick={vm.analyzeRecording(item)},modifier=Modifier.align(Alignment.Center).testTag("viewer-analyze")){Text("Analizar grabación")}
            if(visual.error!=null)TextButton(onClick={vm.workspace.open(item)},modifier=Modifier.align(Alignment.Center)){
                Text("${visual.error} · Reintentar",color=MaterialTheme.colorScheme.error)
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
        if(!singleBar)Row(Modifier.fillMaxWidth().height(48.dp).testTag("viewer-transport"),verticalAlignment=Alignment.CenterVertically){
            transport()
            if(landscape)TextButton(onClick={tools=true},modifier=Modifier.weight(1f).testTag("open-reading")){
                Text(if(state.inspected!=null)"Lectura completa ›" else reading,style=MaterialTheme.typography.labelSmall,maxLines=1)
            }
            else Spacer(Modifier.weight(1f))
            if(config.fontScale<1.3f){
                IconButton(onClick={state.transform(duration,visual.rate,points,.5,.5,2.0,2.0,0.0,0.0)}){Icon(Icons.Default.Add,"Acercar")}
                IconButton(onClick={state.transform(duration,visual.rate,points,.5,.5,.5,.5,0.0,0.0)}){Text("−",Modifier.semantics {contentDescription="Alejar"})}
            }
            follow()
        }
        if(!landscape)TextButton(onClick={tools=true},modifier=Modifier.fillMaxWidth()){
            Text(reading,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
    if(tools)ModalBottomSheet(onDismissRequest={tools=false}){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Herramientas de exploración",style=MaterialTheme.typography.titleLarge)
            Text(reading,modifier=Modifier.fillMaxWidth().testTag("complete-reading"),style=MaterialTheme.typography.bodySmall)
            if(state.plot==AcousticPlot.PITCH)Text(if(item.timeline.isEmpty())"Fuente: reanálisis WAV" else "Fuente: lecturas originales de grabación · misma serie que Biblioteca",style=MaterialTheme.typography.bodySmall)
            Text("Arrastrá en dos ejes · pellizcá para ampliar · mantené para inspeccionar.",style=MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick={state.transform(duration,visual.rate,points,.5,.5,2.0,2.0,0.0,0.0)}){Text("Acercar")}
                TextButton(onClick={state.transform(duration,visual.rate,points,.5,.5,.5,.5,0.0,0.0)}){Text("Alejar")}
            }
            if(state.plot==AcousticPlot.SPECTRUM){
                Text(if(recording?.endMs!=null)"Espectro del fragmento" else "Espectro de toda la grabación · H?: candidato",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={if(recording?.running==true)vm.cancelRecordingAnalysis() else vm.analyzeRecording(item)},modifier=Modifier.testTag("viewer-analyze-tools")){Text(if(recording?.running==true)"Cancelar análisis" else "Analizar grabación")}
            }
            (visual.error ?: recording?.error)?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            PlotTools(vm,item)
            FragmentTools(vm,item)
            TextButton(onClick={tools=false}){Text("Volver al gráfico")}
        }
    }
}

@Composable
internal fun PlotTools(vm:TallerViewModel,item:RecordingStore.Session,s:ExplorerState=vm.explorer(item.id)){
    val visual=vm.workspace.state
    val points=sessionPlotPoints(vm,item)
    val duration=visual.durationMs.takeIf {it>0} ?: item.durationMs;val camera=s.camera(duration,visual.rate,points)
    var exact by remember(s.plot){mutableStateOf(false)}
    var from by remember(s.plot){mutableStateOf("")};var to by remember(s.plot){mutableStateOf("")}
    val axis=if(s.plot==AcousticPlot.SPECTRUM)camera.x else camera.y
    TextButton(onClick={from="%.4f".format(java.util.Locale.US,axis.low);to="%.4f".format(java.util.Locale.US,axis.high);exact=!exact}){Text("Ingresar rango exacto…")}
    if(exact){
        val unit=when(s.plot){AcousticPlot.WAVE->"PCM";else->"Hz"}
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(value=from,onValueChange={from=it},label={Text("Desde $unit")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(s.plot==AcousticPlot.WAVE)KeyboardType.Text else KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=to,onValueChange={to=it},label={Text("Hasta $unit")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(s.plot==AcousticPlot.WAVE)KeyboardType.Text else KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        }
        val a=from.replace(',','.').toDoubleOrNull();val b=to.replace(',','.').toDoubleOrNull()
        val domain=s.domains(duration,visual.rate,points,fullPitchDomain=true).let {if(s.plot==AcousticPlot.SPECTRUM)it.first else it.second}
        val valid=a!=null && b!=null && a.isFinite() && b.isFinite() && a>=domain.low && b<=domain.high && b>a
        TextButton(onClick={val window=domain.constrain(AxisWindow(a!!,b!!));if(s.plot==AcousticPlot.PITCH)s.extendedPitchRange=true;s.cameras[s.plot]=if(s.plot==AcousticPlot.SPECTRUM)camera.copy(x=window)else camera.copy(y=window);s.follow=false;exact=false},enabled=valid){Text("Aplicar rango")}
        if(!valid)Text("Ingresá un intervalo entre ${axisLabel(domain.low,domain.high-domain.low)} y ${axisLabel(domain.high,domain.high-domain.low)} $unit.",style=MaterialTheme.typography.bodySmall)
    }
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("← Anterior" to (-.3 to 0.0),"Siguiente →" to (.3 to 0.0),"↑ Subir" to (0.0 to .3),"↓ Bajar" to (0.0 to -.3)).forEach {(label,pan)->
            OutlinedButton(onClick={s.transform(duration,visual.rate,points,.5,.5,1.0,1.0,-pan.first,pan.second)}){Text(label,maxLines=1)}
        }
    }
    if(s.plot==AcousticPlot.SPECTROGRAM || s.plot==AcousticPlot.SPECTRUM){
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            (if(item.instrument==InstrumentProfile.NATIVE_AMERICAN_FLUTE)listOf(2000.0,4000.0,visual.rate/2.0) else listOf(100.0,500.0,2000.0,visual.rate/2.0)).distinct().forEach {limit->
                FilterChip(selected=(if(s.plot.temporal)camera.y.high else camera.x.high)==limit,onClick={
                    val window=AxisWindow(if(s.plot==AcousticPlot.SPECTROGRAM && s.logarithmic)visual.rate/32768.0 else 0.0,limit)
                    s.cameras[s.plot]=if(s.plot.temporal)camera.copy(y=window) else camera.copy(x=window);s.follow=false
                },label={Text(if(limit==visual.rate/2.0)"Completo" else "${limit.toInt()} Hz",maxLines=1)})
            }
        }
    }
    if(s.plot==AcousticPlot.PITCH){
        val range=s.pitch(points)
        Text("${range.valid} lecturas en la tendencia. General muestra su rango; Ajustada amplía las variaciones.",style=MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick={s.extendedPitchRange=false;s.cameras[s.plot]=camera.copy(y=s.overview(points).window);s.follow=false}){Text("Vista general")}
            TextButton(onClick={s.extendedPitchRange=false;s.cameras[s.plot]=camera.copy(y=range.window);s.follow=false}){Text("Vista ajustada")}
        }
        val trace=remember(points,s.instrument){PitchTrace.build(points,s.instrument)}
        val uncertain=trace.count {it.uncertain}
        Text("${range.clipped} fuera de Ajustada · $uncertain originales excluidos de la tendencia. Se omiten excursiones aisladas sin vaciar la curva; intervalos breves de incertidumbre unen solo extremos medidos próximos. Ausencia clara o prolongada deja un hueco. Los valores originales no se corrigen ni se rellenan. Confianza YIN no es probabilidad de acierto.",style=MaterialTheme.typography.bodySmall)
        Row(verticalAlignment=Alignment.CenterVertically){Text("Mostrar lecturas originales / inciertas",Modifier.weight(1f));Switch(checked=s.showUncertain,onCheckedChange={s.showUncertain=it},modifier=Modifier.testTag("show-uncertain"))}
        if(s.showUncertain)Text("Círculos naranjas: estimaciones originales, sin nota confirmada. Mantené pulsado sobre su tiempo para consultar Hz y confianza; usá Mostrar todos para incluir las que están fuera del rango.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={s.extendedPitchRange=true;s.showUncertain=true;s.cameras[s.plot]=camera.copy(y=s.pitch(points,true).window);s.follow=false}){Text("Mostrar todos los valores")}
    }
    if(s.plot==AcousticPlot.SPECTROGRAM){
        Row(verticalAlignment=Alignment.CenterVertically){Text("Frecuencia logarítmica",Modifier.weight(1f));Switch(checked=s.logarithmic,onCheckedChange={
            s.logarithmic=it
            val domain=s.domains(duration,visual.rate,points).second
            val low=max(camera.y.low,domain.low)
            val high=if(camera.y.high>low)camera.y.high else low+visual.rate/32768.0
            s.cameras[s.plot]=camera.copy(y=domain.constrain(AxisWindow(low,high)))
        },modifier=Modifier.testTag("log-frequency"))}
        Text("Rango dinámico: ${s.rangeDb.toInt()} dB")
        Slider(value=s.rangeDb.toFloat(),onValueChange={s.rangeDb=it.toDouble()},valueRange=30f..100f)
        Canvas(Modifier.fillMaxWidth().height(16.dp)){for(x in 0 until size.width.toInt())drawLine(Color(SpectralPalette.color(-s.rangeDb+s.rangeDb*x/size.width,s.rangeDb)),Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height),1f)}
        Text("−${s.rangeDb.toInt()} a 0 dB relativos. Referencia fijada en la primera vista calculada de la sesión, conservada al explorar. Colores saturados: energía mayor que esa referencia. No mide presión sonora.",style=MaterialTheme.typography.bodySmall)
        Text("STFT Hann 32768 muestras · ${"%.2f".format(spanish,visual.rate/32768.0)} Hz por bin · ventana ${32768000/visual.rate} ms. Cada celda resume una ventana, no un instante.",style=MaterialTheme.typography.bodySmall)
    }
    if(visual.loading)TextButton(onClick={vm.workspace.cancel()}){Text("Cancelar visualización")}
    if(visual.error!=null)TextButton(onClick={vm.workspace.open(item)}){Text("Reintentar visualización")}
}

@Composable
internal fun FragmentTools(vm:TallerViewModel,item:RecordingStore.Session,s:ExplorerState=vm.explorer(item.id)){
    val duration=(vm.workspace.state.durationMs.takeIf {it>0} ?: item.durationMs).coerceAtLeast(1)
    Text("Seleccionar fragmento",style=MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("Buscar","Marcar inicio","Marcar fin").forEachIndexed {i,label->FilterChip(selected=s.mode==i,onClick={s.mode=i},label={Text(label,maxLines=1)},modifier=Modifier.heightIn(min=48.dp).testTag("selection-mode-$i"))}
    }
    Text("Inicio: ${seconds(s.regionStart)} · fin: ${seconds(s.regionEnd)}",Modifier.testTag("selected-region"))
    RangeSlider(value=s.regionStart.toFloat().coerceIn(0f,duration.toFloat())..s.regionEnd.toFloat().coerceIn(s.regionStart.toFloat(),duration.toFloat()),onValueChange={
        s.regionStart=it.start.toLong();s.regionEnd=it.endInclusive.toLong();s.looping=false;vm.setPlaybackLoopOff()
    },valueRange=0f..duration.toFloat())
    s.region?.let {region->
        Button(onClick={vm.analyzeRecording(item,region.first,region.last)}){Text("Analizar fragmento")}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Repetir fragmento",Modifier.weight(1f));Switch(checked=s.looping,onCheckedChange={s.looping=it;vm.setPlaybackLoop(item,if(it)region else null)},modifier=Modifier.testTag("loop-fragment"))}
        Text("Bucle controlado por posición real; no garantiza continuidad entre muestras.",style=MaterialTheme.typography.bodySmall)
    }
    Text("Posición: ${seconds(s.selectedMs)} · análisis por ventanas cercanas",style=MaterialTheme.typography.bodySmall)
    Slider(value=s.selectedMs.toFloat().coerceIn(0f,duration.toFloat()),onValueChange={s.selectedMs=it.toLong()},onValueChangeFinished={vm.seekPlayback(item,s.selectedMs)},valueRange=0f..duration.toFloat(),modifier=Modifier.testTag("audio-seek"))
    TextButton(onClick={vm.analyzeRecording(item,max(0,s.selectedMs-750),min(duration,s.selectedMs+750))}){Text("Analizar entorno de la posición")}
}

@Composable
internal fun LabHome(onOpen:(Int)->Unit){
    Text("Laboratorio de didgeridoo",style=MaterialTheme.typography.headlineSmall)
    listOf("Diseñar" to "Perfil interior, puntos de control y resonancias simuladas.","Espectro" to "Observá el sonido del micrófono en tiempo real.","Buscar" to "Explorá geometrías para objetivos de resonancia.","Guía" to "Conceptos musicales y acústicos, explicados en español.").forEachIndexed {i,pair->
        OutlinedCard(onClick={onOpen(i)},modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(pair.first,style=MaterialTheme.typography.titleLarge);Text(pair.second,style=MaterialTheme.typography.bodyMedium)}
        }
    }
}
