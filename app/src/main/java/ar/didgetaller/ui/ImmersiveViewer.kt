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
    return if(analysis?.result!=null && (analysis.startMs==0L && analysis.endMs==null || item.timeline.isEmpty()))analysis.result.timeline else item.timeline
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
    val s=vm.explorer(item.id);val visual=vm.workspace.state
    val duration=visual.durationMs.takeIf {it>0} ?: item.durationMs.coerceAtLeast(2)
    val analysis=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    val result=analysis?.result
    val points=sessionPlotPoints(vm,item)
    val camera=s.camera(duration,visual.rate,points)
    LaunchedEffect(item.id){vm.openTools(item)}
    LaunchedEffect(camera,s.plot,s.logarithmic,s.rangeDb,s.gestureActive,visual.frames){
        if(visual.frames>1 && !s.gestureActive && s.plot in listOf(AcousticPlot.SPECTROGRAM,AcousticPlot.WAVE)){
            delay(180)
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
                    AcousticPlot.PITCH->"${seconds(time)} · "+(point?.hz?.let {"${Music.note(it)} · ${hzLabel(it)} · periodicidad %.2f".format(spanish,point.confidence)} ?: "Sin lectura confiable cercana")
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
    val activity=LocalContext.current.activity()
    val landscape=LocalConfiguration.current.screenWidthDp>LocalConfiguration.current.screenHeightDp
    var plots by remember {mutableStateOf(false)}
    Scaffold(modifier=Modifier.fillMaxSize().testTag("acoustic-viewer"),
        topBar={TopAppBar(title={if(landscape)Box {
            TextButton(onClick={plots=true}){Text(state.plot.title,maxLines=1)}
            DropdownMenu(expanded=plots,onDismissRequest={plots=false}){AcousticPlot.entries.forEach {kind->DropdownMenuItem(text={Text(kind.title)},onClick={state.plot=kind;plots=false})}}
        }else Text(state.plot.title,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver a la sesión")}},
            actions={
                if(landscape){
                    IconButton(onClick={state.reset(visual.durationMs.takeIf {it>0} ?: item.durationMs,visual.rate,points)},modifier=Modifier.testTag("reset-viewport")){Icon(Icons.Default.Refresh,"Restablecer vista")}
                    IconButton(onClick={tools=true}){Icon(Icons.Default.MoreVert,"Herramientas")}
                }
                TextButton(onClick={
                if(vm.viewerOriginalOrientation==null)vm.viewerOriginalOrientation=activity?.requestedOrientation
                activity?.requestedOrientation=if(landscape)ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            },modifier=Modifier.testTag("rotate-viewer")){Text("Girar")}})},
        bottomBar={Column(Modifier.navigationBarsPadding().padding(horizontal=12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={vm.togglePlayback(item)},modifier=Modifier.heightIn(min=48.dp)) {Text(if(vm.playingId==item.id && !vm.playbackPaused)"Pausar" else "Reproducir")}
                Text(seconds(cursor),Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
                if(state.plot.temporal)FilterChip(selected=state.follow,onClick={state.follow=!state.follow},label={Text("Seguir")})
            }
        }}){padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            if(!landscape)ScrollableTabRow(selectedTabIndex=state.plot.ordinal,edgePadding=0.dp){AcousticPlot.entries.forEach {kind->Tab(selected=state.plot==kind,onClick={state.plot=kind;state.inspected=null},text={Text(kind.title,maxLines=1)})}}
            if(!landscape)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={state.reset(visual.durationMs.takeIf {it>0} ?: item.durationMs,visual.rate,points)},modifier=Modifier.weight(1f).testTag("reset-viewport")){Text("Restablecer vista",maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
                IconButton(onClick={state.transform(visual.durationMs,visual.rate,points,.5,.5,2.0,2.0,0.0,0.0)}){Icon(Icons.Default.Add,"Acercar")}
                IconButton(onClick={state.transform(visual.durationMs,visual.rate,points,.5,.5,.5,.5,0.0,0.0)}){Text("−",Modifier.semantics {contentDescription="Alejar"},style=MaterialTheme.typography.headlineSmall)}
                IconButton(onClick={tools=true}){Icon(Icons.Default.MoreVert,"Herramientas")}
            }
            SessionPlot(vm,item,cursor,Modifier.fillMaxWidth().weight(1f),immersive=true)
            if(visual.loading)LinearProgressIndicator(progress={visual.progress/100f},modifier=Modifier.fillMaxWidth())
            val camera=state.camera(visual.durationMs.takeIf {it>0} ?: item.durationMs,visual.rate,points)
            Text(state.inspected ?: "${if(state.plot.temporal)"Tiempo" else "Hz"}: ${axisLabel(camera.x.low,camera.x.span)}–${axisLabel(camera.x.high,camera.x.span)} ${if(state.plot.temporal)"ms" else "Hz"} · ${axisLabel(camera.y.low,camera.y.span)}–${axisLabel(camera.y.high,camera.y.span)} ${if(state.plot==AcousticPlot.WAVE)"PCM" else if(state.plot==AcousticPlot.SPECTRUM)"dB rel" else "Hz"}",
                style=MaterialTheme.typography.labelSmall,maxLines=if(landscape)1 else 2)
            if(state.plot==AcousticPlot.SPECTROGRAM)Row(verticalAlignment=Alignment.CenterVertically){
                Canvas(Modifier.weight(1f).height(8.dp)){for(x in 0 until size.width.toInt())drawLine(Color(SpectralPalette.color(-state.rangeDb+state.rangeDb*x/size.width,state.rangeDb)),Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height),1f)}
                Text(" −${state.rangeDb.toInt()} → 0 dB rel",style=MaterialTheme.typography.labelSmall)
            }
            if(!landscape)Text("Arrastrá ↔ ↕ · zoom con dos dedos · mantené para inspeccionar",style=MaterialTheme.typography.labelSmall)
            visual.error?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
            if(state.plot==AcousticPlot.SPECTRUM){
                Text(if(recording?.endMs!=null)"Espectro del fragmento ${seconds(recording.startMs)}–${seconds(recording.endMs)}" else "Espectro de toda la grabación · H?: candidato",style=MaterialTheme.typography.labelSmall)
                if(recording?.running==true){
                    LinearProgressIndicator(progress={recording.progress/100f},modifier=Modifier.fillMaxWidth())
                    TextButton(onClick={vm.cancelRecordingAnalysis()}){Text("Cancelar análisis · ${recording.progress}%")}
                }else if(recording?.result==null)TextButton(onClick={vm.analyzeRecording(item)}){Text("Analizar grabación")}
                recording?.error?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
            }
        }
    }
    if(tools)ModalBottomSheet(onDismissRequest={tools=false}){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Herramientas de exploración",style=MaterialTheme.typography.titleLarge)
            PlotTools(vm,item)
            FragmentTools(vm,item)
            TextButton(onClick={tools=false}){Text("Volver al gráfico")}
        }
    }
}

@Composable
internal fun PlotTools(vm:TallerViewModel,item:RecordingStore.Session){
    val s=vm.explorer(item.id);val visual=vm.workspace.state
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
        val domain=s.domains(duration,visual.rate,points).let {if(s.plot==AcousticPlot.SPECTRUM)it.first else it.second}
        val valid=a!=null && b!=null && a.isFinite() && b.isFinite() && a>=domain.low && b<=domain.high && b>a
        TextButton(onClick={val window=domain.constrain(AxisWindow(a!!,b!!));s.cameras[s.plot]=if(s.plot==AcousticPlot.SPECTRUM)camera.copy(x=window)else camera.copy(y=window);s.follow=false;exact=false},enabled=valid){Text("Aplicar rango")}
        if(!valid)Text("Ingresá un intervalo entre ${axisLabel(domain.low,domain.high-domain.low)} y ${axisLabel(domain.high,domain.high-domain.low)} $unit.",style=MaterialTheme.typography.bodySmall)
    }
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("← Anterior" to (-.3 to 0.0),"Siguiente →" to (.3 to 0.0),"↑ Subir" to (0.0 to .3),"↓ Bajar" to (0.0 to -.3)).forEach {(label,pan)->
            OutlinedButton(onClick={s.transform(duration,visual.rate,points,.5,.5,1.0,1.0,-pan.first,pan.second)}){Text(label,maxLines=1)}
        }
    }
    if(s.plot==AcousticPlot.SPECTROGRAM || s.plot==AcousticPlot.SPECTRUM){
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf(100.0,500.0,2000.0,visual.rate/2.0).distinct().forEach {limit->
                FilterChip(selected=(if(s.plot.temporal)camera.y.high else camera.x.high)==limit,onClick={
                    val window=AxisWindow(if(s.plot==AcousticPlot.SPECTROGRAM && s.logarithmic)visual.rate/32768.0 else 0.0,limit)
                    s.cameras[s.plot]=if(s.plot.temporal)camera.copy(y=window) else camera.copy(x=window);s.follow=false
                },label={Text(if(limit==visual.rate/2.0)"Completo" else "${limit.toInt()} Hz",maxLines=1)})
            }
        }
    }
    if(s.plot==AcousticPlot.PITCH){
        val range=s.pitch(points)
        Text("Escala automática: ${range.valid} lecturas válidas. ${range.clipped} fuera del encuadre robusto; los datos originales se conservan.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={s.cameras[s.plot]=camera.copy(y=s.pitch(points,true).window)}){Text("Mostrar todos los valores")}
    }
    if(s.plot==AcousticPlot.SPECTROGRAM){
        Row(verticalAlignment=Alignment.CenterVertically){Text("Frecuencia logarítmica",Modifier.weight(1f));Switch(checked=s.logarithmic,onCheckedChange={s.logarithmic=it;s.cameras[s.plot]=camera.copy(y=AxisWindow(if(it)max(camera.y.low,visual.rate/32768.0) else 0.0,camera.y.high))},modifier=Modifier.testTag("log-frequency"))}
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
internal fun FragmentTools(vm:TallerViewModel,item:RecordingStore.Session){
    val s=vm.explorer(item.id);val duration=(vm.workspace.state.durationMs.takeIf {it>0} ?: item.durationMs).coerceAtLeast(1)
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
    Text("Explorá el instrumento y su sonido",style=MaterialTheme.typography.headlineSmall)
    listOf("Diseñar" to "Perfil interior, puntos de control y resonancias simuladas.","Espectro" to "Observá el sonido del micrófono en tiempo real.","Buscar" to "Explorá geometrías para objetivos de resonancia.","Guía" to "Conceptos musicales y acústicos, explicados en español.").forEachIndexed {i,pair->
        OutlinedCard(onClick={onOpen(i)},modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(pair.first,style=MaterialTheme.typography.titleLarge);Text(pair.second,style=MaterialTheme.typography.bodyMedium)}
        }
    }
}
