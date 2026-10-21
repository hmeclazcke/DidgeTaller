package ar.didgetaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import ar.didgetaller.TallerViewModel
import kotlin.math.abs

/** Commit only after the dismiss animation settles following pointer release. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DismissCard(onDelete:()->Unit,tag:String,content:@Composable ()->Unit) {
    val currentDelete by rememberUpdatedState(onDelete)
    var held by remember {mutableStateOf(false)}
    var width by remember {mutableIntStateOf(0)}
    var releasedPastThreshold by remember {mutableStateOf(false)}
    // Material's default fling can dismiss before the distance threshold. Require distance at release.
    val state=rememberSwipeToDismissBoxState(positionalThreshold={it*0.38f},confirmValueChange={
        it!=SwipeToDismissBoxValue.EndToStart || releasedPastThreshold
    })
    LaunchedEffect(state.currentValue,state.targetValue,state.progress,held) {
        if(!held && state.currentValue==SwipeToDismissBoxValue.EndToStart && state.targetValue==state.currentValue && state.progress==1f)currentDelete()
    }
    SwipeToDismissBox(state=state,enableDismissFromStartToEnd=false,modifier=Modifier.testTag(tag).onSizeChanged {width=it.width}.pointerInput(Unit){
        awaitPointerEventScope {while(true){
            val event=awaitPointerEvent(PointerEventPass.Initial);val pressed=event.changes.any {it.pressed}
            if(pressed && !held)releasedPastThreshold=false
            if(!pressed && held)releasedPastThreshold=width>0 && state.requireOffset()<=-width*.38f
            held=pressed
        }}
    },
        backgroundContent={
            Row(Modifier.fillMaxSize().background(if(state.dismissDirection==SwipeToDismissBoxValue.Settled)androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.errorContainer,RoundedCornerShape(18.dp)).padding(18.dp),
                horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                Text(if(state.targetValue==SwipeToDismissBoxValue.EndToStart)"Soltá para eliminar" else "Papelera",color=MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(10.dp));Icon(Icons.Default.Delete,contentDescription="Eliminar al soltar")
            }
        }) {content()}
}

@Composable
internal fun LibraryTools(vm:TallerViewModel,onCompare:()->Unit) {
    var showTrash by remember {mutableStateOf(false)}
    var empty by remember {mutableStateOf(false)}
    Column {
        OutlinedButton(onClick=onCompare,enabled=vm.sessions.size>=2,modifier=Modifier.fillMaxWidth()) {Text("Comparar dos sesiones")}
        TextButton(onClick={showTrash=!showTrash}) {Text("Papelera (${vm.trashCount})")}
        if(showTrash) {
            Text("Los archivos se conservan hasta que vacíes la papelera. Podés restaurarlos incluso después de cerrar la app.",style=MaterialTheme.typography.bodySmall)
            vm.trashedSessions.forEach {item ->key(item.id) {
                Text(item.name);TextButton(onClick={vm.restore("session",item.id)}) {Text("Restaurar sesión")}
            }}
            vm.trashedDesigns.forEach {item ->key(item.id) {
                Text(item.name);TextButton(onClick={vm.restore("design",item.id)}) {Text("Restaurar diseño")}
            }}
            if(vm.trashCount>vm.trashedSessions.size+vm.trashedDesigns.size)Text("Hay una limpieza interrumpida. Vaciar la papelera permite completarla.",style=MaterialTheme.typography.bodySmall)
            if(vm.trashCount>0)TextButton(onClick={empty=true}){Text("Vaciar papelera…")}
        }
    }
    if(empty)AlertDialog(onDismissRequest={empty=false},title={Text("¿Vaciar la papelera?")},
        text={Text("Se eliminarán definitivamente los WAV, análisis y relaciones de las sesiones de la papelera. Esta acción no se puede deshacer.")},
        confirmButton={TextButton(onClick={empty=false;vm.emptyTrash()}){Text("Eliminar definitivamente")}},
        dismissButton={TextButton(onClick={empty=false}){Text("Cancelar")}})
}

@Composable
internal fun RecordingSettings(vm:TallerViewModel) {
    var expanded by remember {mutableStateOf(false)}
    if(!vm.fullScreen)Column {
        TextButton(onClick={expanded=!expanded}){Text("Límite: ${vm.recordingLimitSeconds/60} min · ajustes")}
        if(expanded) {
            Text("Al salir de la app se detiene y guarda la sesión. No se graba en segundo plano.",style=MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(1,5,15,30).forEach {minutes ->
                    FilterChip(selected=vm.recordingLimitSeconds==minutes*60,onClick={vm.setRecordingLimit(minutes*60)},label={Text("$minutes m")})
                }
            }
            Text("Espacio disponible: ${vm.availableMegabytes()} MB · 15 min ≈ 76 MB de audio",style=MaterialTheme.typography.bodySmall)
        }
    }
}
