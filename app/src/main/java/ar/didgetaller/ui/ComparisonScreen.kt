package ar.didgetaller.ui

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
    var expanded by remember {mutableStateOf(false)}
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text("$label: "+(items.find {it.id==selected}?.name ?: "elegir sesión"),maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=350.dp)) {
            items.forEach {item ->DropdownMenuItem(text={Text(item.name+" · "+(item.summary?.let {hzLabel(it.typicalHz)} ?: "sin análisis"))},onClick={onSelect(item.id);expanded=false})}
        }
    }
}

@Composable
internal fun ComparisonScreen(vm:TallerViewModel) {
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
    LabCard("Comparar sesiones o fragmentos") {
        Text("Elegí dos grabaciones. Opcionalmente ingresá inicio y fin en segundos; dejalos vacíos para usar todo el audio.")
        SessionPicker("Sesión A",vm.sessions,a){a=it;vm.cancelComparison(clear=true)}
        IntervalFields("A",aStart,aEnd,{aStart=it;vm.cancelComparison(clear=true)},{aEnd=it;vm.cancelComparison(clear=true)})
        SessionPicker("Sesión B",vm.sessions,b){b=it;vm.cancelComparison(clear=true)}
        IntervalFields("B",bStart,bEnd,{bStart=it;vm.cancelComparison(clear=true)},{bEnd=it;vm.cancelComparison(clear=true)})
        if(!valid)Text("Elegí dos sesiones y un intervalo válido dentro de cada grabación.",color=MaterialTheme.colorScheme.secondary)
        if(state.running){LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth());Text("Comparando · ${state.progress}%");TextButton(onClick={vm.cancelComparison()}){Text("Cancelar comparación")}}
        else Button(onClick={vm.compare(first!!,second!!,ms(aStart) ?: 0,ms(aEnd),ms(bStart) ?: 0,ms(bEnd))},enabled=valid){Text("Comparar")}
        state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        if(state.a!=null && state.b!=null){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Solo A","Solo B","Ambas").forEachIndexed {i,label ->FilterChip(selected=show==i,onClick={show=i},label={Text(label)})}
            }
            Row {Text("Normalizar cada espectro",Modifier.weight(1f));Switch(checked=normalize,onCheckedChange={normalize=it})}
            SpectrumChart(state.a,state.b,showA=show!=1,showB=show!=0,normalize=normalize)
            Row(Modifier.fillMaxWidth()) {Text("A · línea continua",Modifier.weight(1f),color=traceA);Text("B · línea discontinua",Modifier.weight(1f),color=traceB)}
            ComparisonMetrics(state.a,state.b)
        }
        Text("La normalización permite comparar distribuciones, pero elimina la diferencia de nivel global. Sin normalizar se usa una referencia digital común, no presión sonora absoluta. La distancia, el volumen tocado y el procesamiento del teléfono pueden cambiar los resultados.",style=MaterialTheme.typography.bodySmall)
        Text("Para explorar vocalizaciones, compará fragmentos del mismo instrumento en condiciones similares. Los cambios del centroide y de las bandas no identifican una vocal ni prueban diferencias intrínsecas entre materiales.",style=MaterialTheme.typography.bodySmall)
        AcousticHelp()
    }
}

@Composable
private fun IntervalFields(label:String,start:String,end:String,onStart:(String)->Unit,onEnd:(String)->Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value=start,onValueChange=onStart,label={Text("Inicio $label (s)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f).testTag("compare-$label-start"))
        OutlinedTextField(value=end,onValueChange=onEnd,label={Text("Fin $label (s)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f).testTag("compare-$label-end"))
    }
}

@Composable
private fun ComparisonMetrics(a:WavAnalysis.Result,b:WavAnalysis.Result) {
    fun hz(value:Double?)=value?.let {hzLabel(it)} ?: "incierta"
    val rows=listOf(
        "Intervalo analizado" to ("${seconds(a.startFrame*1000/a.sampleRate)}–${seconds(a.endFrame*1000/a.sampleRate)}" to "${seconds(b.startFrame*1000/b.sampleRate)}–${seconds(b.endFrame*1000/b.sampleRate)}"),
        "Fundamental típica" to (hz(a.summary?.typicalHz) to hz(b.summary?.typicalHz)),
        "Nota" to ((a.summary?.let {Music.note(it.typicalHz)} ?: "incierta") to (b.summary?.let {Music.note(it.typicalHz)} ?: "incierta")),
        "Estabilidad" to ((a.summary?.let {"${it.stabilityPercent}%"} ?: "sin datos") to (b.summary?.let {"${it.stabilityPercent}%"} ?: "sin datos")),
        "Rango habitual" to ((a.summary?.let {"${hz(it.lowHz)}–${hz(it.highHz)}"} ?: "sin datos") to (b.summary?.let {"${hz(it.lowHz)}–${hz(it.highHz)}"} ?: "sin datos")),
        "Centroide de potencia" to (hz(a.spectralCentroidHz) to hz(b.spectralCentroidHz)),
        "Duración" to (seconds((a.durationSeconds*1000).toLong()) to seconds((b.durationSeconds*1000).toLong())),
        "Ventanas válidas" to ("${(a.validWindowFraction*100).toInt()}%" to "${(b.validWindowFraction*100).toInt()}%"))
    rows.forEach {(label,values)->Text(label,style=MaterialTheme.typography.labelMedium);Row(Modifier.fillMaxWidth()){Text(values.first,Modifier.weight(1f));Text(values.second,Modifier.weight(1f))};HorizontalDivider()}
    Text("Distribución de energía por bandas",style=MaterialTheme.typography.titleSmall)
    a.bandEnergies.zip(b.bandEnergies).forEach {(x,y)->Text("${x.fromHz.toInt()}–${x.toHz.toInt()} Hz: A ${(x.fraction*100).toInt()}% · B ${(y.fraction*100).toInt()}%")}
    listOf("A" to a,"B" to b).forEach {(label,r)->
        Text("$label · candidatos armónicos",style=MaterialTheme.typography.titleSmall)
        Text(r.harmonics.peaks.filter {it.order!=null}.take(12).joinToString(" · "){"H${it.order}? ${hzLabel(it.hz)} (%.1f dB)".format(it.relativeDb)}.ifEmpty {"Sin candidatos asignables con confianza."})
    }
}
