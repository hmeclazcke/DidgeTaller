package ar.didgetaller.ui

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
import ar.didgetaller.audio.VisualRequest
import ar.didgetaller.core.Music
import ar.didgetaller.core.SpectralPalette
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import ar.didgetaller.data.RecordingStore
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
internal fun LabCard(title:String,content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium);content()
        }
    }
}

@Composable
internal fun SessionExplorer(vm:TallerViewModel,item:RecordingStore.Session,cursor:Long,onExpand:(AcousticPlot)->Unit) {
    val s=vm.preview(item.id)
    var tools by rememberSaveable(item.id){mutableStateOf(false)}
    var fragments by rememberSaveable(item.id){mutableStateOf(false)}
    var markers by rememberSaveable(item.id){mutableStateOf(false)}
    LaunchedEffect(item.id){vm.openTools(item)}
    LabCard("Explorar el audio") {
        ScrollableTabRow(selectedTabIndex=s.plot.ordinal,edgePadding=0.dp){
            AcousticPlot.entries.forEach {kind->Tab(selected=s.plot==kind,onClick={s.plot=kind;s.inspected=null},text={Text(kind.title,maxLines=1)})}
        }
        OutlinedButton(onClick={onExpand(s.plot)},modifier=Modifier.fillMaxWidth().testTag("expand-plot")){Text("Pantalla completa")}
        SessionPlot(vm,item,cursor,Modifier.fillMaxWidth().height(250.dp),onExpand={onExpand(s.plot)})
        if(s.plot==AcousticPlot.PITCH){
            val points=sessionPlotPoints(vm,item)
            Text("Verde: estable · Amarillo: variación · Naranja: revisar",style=MaterialTheme.typography.bodySmall)
            if(s.pitch(points).valid==0)Text("Sin lecturas confiables · el audio se conserva",style=MaterialTheme.typography.bodySmall)
        }
        if(s.plot==AcousticPlot.SPECTRUM && vm.recordingAnalysis.result==null)Text("Tocá «Analizar grabación» para obtener el espectro real.",style=MaterialTheme.typography.bodySmall)
        if(s.plot!=AcousticPlot.PITCH)Text("Tocá el gráfico para explorar con zoom y desplazamiento en dos ejes.",style=MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={tools=!tools}){Text(if(tools)"Ocultar escalas" else "Escalas y rangos")}
            TextButton(onClick={fragments=!fragments}){Text(if(fragments)"Ocultar fragmentos" else "Selección y fragmentos")}
            TextButton(onClick={markers=!markers}){Text("Marcadores")}
        }
        if(tools)PlotTools(vm,item,s)
        if(fragments)FragmentTools(vm,item,s)
        if(markers){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                TextButton(onClick={vm.addMarker(item,s.selectedMs,"Posible toot")}){Text("Marcar posible toot")}
                TextButton(onClick={vm.addMarker(item,s.selectedMs,"Vocalización")}){Text("Marcar vocalización")}
            }
            vm.markers.forEach {m->Row {
                TextButton(onClick={s.selectedMs=m.timeMs;vm.seekPlayback(item,m.timeMs)},modifier=Modifier.weight(1f)){Text("${m.type} · ${seconds(m.timeMs)}")}
                TextButton(onClick={vm.removeMarker(item,m.id)}){Text("Quitar")}
            }}
            Text("Marcadores manuales: un armónico intenso no demuestra un toot ni identifica una vocal.",style=MaterialTheme.typography.bodySmall)
        }
    }
    RecordingAnalysisPanel(vm,item)
    AcousticHelp()
}

@Composable
internal fun SessionRelations(vm:TallerViewModel,item:RecordingStore.Session) {
    if(item.instrument==ar.didgetaller.core.InstrumentProfile.NATIVE_AMERICAN_FLUTE){
        Text("Flauta nativa norteamericana · análisis de audio. Los diseños del Laboratorio simulan didgeridoos y no se asocian a esta sesión.",style=MaterialTheme.typography.bodySmall)
        return
    }
    var expanded by remember {mutableStateOf(false)}
    val design=vm.designs.find {it.id==vm.associatedDesignId}
    LabCard("Instrumento y diseño") {
        Box {
            OutlinedButton(onClick={expanded=true}){Text(design?.name ?: if(vm.associatedDesignId!=null)"Diseño en papelera" else "Asociar un diseño…")}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text("Sin asociación")},onClick={vm.associate(item,null);expanded=false})
                vm.designs.forEach {candidate ->DropdownMenuItem(text={Text(candidate.name)},onClick={vm.associate(item,candidate.id);expanded=false})}
            }
        }
        design?.let {Text("Geometría ingresada: ${it.shape.lengthMm} mm · boca ${it.shape.mouthMm} mm · campana ${it.shape.bellMm} mm")}
        vm.associatedSimulation?.let {sim ->
            val analysis=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
            val summary=if(analysis?.result!=null)analysis.result.summary else item.summary
            Text("SIMULACIÓN · resonancias del modelo: "+sim.peaks.take(6).joinToString(" · "){"${hzLabel(it.hz)} (${Music.note(it.hz)})"},color=MaterialTheme.colorScheme.secondary)
            Text("AUDIO ${if(analysis?.result!=null && analysis.endMs!=null)"· fragmento analizado" else "· sesión"} · fundamental: "+(summary?.let {"${Music.note(it.typicalHz)} · ${hzLabel(it.typicalHz)}"} ?: "sin estimación confiable"))
            summary?.let {Text("Diferencia exploratoria frente a la primera resonancia simulada: %+.2f Hz".format(it.typicalHz-sim.baseHz))}
            analysis?.result?.harmonics?.peaks?.filter {it.order!=null}?.take(8)?.let {peaks ->Text("Componentes medidas del WAV analizado: "+peaks.joinToString(" · "){"H${it.order}? ${hzLabel(it.hz)}"})}
        }
        Text("Las resonancias de impedancia del conducto y los armónicos emitidos son magnitudes distintas. No se emparejan por orden. El intérprete, la temperatura y la geometría real influyen; el modelo no equivale a CADSD.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun AcousticHelp() {
    var show by remember {mutableStateOf(false)}
    TextButton(onClick={show=true}){Text("¿Qué significa cada término?")}
    if(show)AlertDialog(onDismissRequest={show=false},title={Text("Pequeño glosario acústico")},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf(
                "Hz" to "Cantidad de ciclos por segundo. Una frecuencia mayor corresponde a un sonido más agudo.",
                "Nota" to "Nombre musical de una frecuencia cercana, usando La4 = 440 Hz como referencia.",
                "Fundamental" to "Frecuencia asociada a la repetición principal de la vibración. Puede ser más débil que sus armónicos.",
                "Armónico" to "Componente próxima a un múltiplo entero de la fundamental. H2 está cerca de dos veces su frecuencia. Un candidato no es una confirmación.",
                "Cents" to "Un semitono equivale a 100 cents. +10 indica un pequeño desvío hacia el agudo, sin calificar la calidad del instrumento.",
                "Toot" to "Registro agudo tocado por el intérprete. Un pico intenso del drone no implica un cambio de registro.",
                "Espectro" to "Distribución de energía por frecuencia en una ventana o fragmento.",
                "Espectrograma" to "Serie de espectros: tiempo horizontal, frecuencia vertical y color para energía relativa.",
                "dB relativo" to "Diferencia logarítmica respecto a una referencia del análisis. No es presión sonora absoluta ni volumen calibrado.",
                "Impedancia" to "Relación entre presión y caudal acústico. Sus resonancias son una predicción del conducto, no el espectro del sonido tocado.",
                "Centroide" to "Promedio de frecuencias ponderado por potencia. Ayuda a explorar la distribución de energía, sin indicar calidad.").forEach {(term,meaning)->Text("$term: $meaning")}
        }
    },confirmButton={TextButton(onClick={show=false}){Text("Entendido")}})
}
