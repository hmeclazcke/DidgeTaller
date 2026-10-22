package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource

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
    val context = androidx.compose.ui.platform.LocalContext.current
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
                Text(if(state.targetValue==SwipeToDismissBoxValue.EndToStart)stringResource(R.string.ui_release_to_delete) else stringResource(R.string.ui_trash),color=MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(10.dp));Icon(Icons.Default.Delete,contentDescription=context.getString(R.string.ui_delete_on_release))
            }
        }) {content()}
}

@Composable
internal fun LibraryTools(vm:TallerViewModel,onCompare:()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showTrash by remember {mutableStateOf(false)}
    var empty by remember {mutableStateOf(false)}
    Column {
        OutlinedButton(onClick=onCompare,enabled=vm.sessions.size>=2,modifier=Modifier.fillMaxWidth()) {Text(stringResource(R.string.ui_compare_two_sessions))}
        TextButton(onClick={showTrash=!showTrash}) {Text(stringResource(R.string.ui_trash_888dd8, vm.trashCount))}
        if(showTrash) {
            Text(stringResource(R.string.ui_files_are_kept_until_you_empty_the_trash_you_can_restore_them_even_after_closi),style=MaterialTheme.typography.bodySmall)
            vm.trashedSessions.forEach {item ->key(item.id) {
                Text(item.name);TextButton(onClick={vm.restore("session",item.id)}) {Text(stringResource(R.string.ui_restore_session))}
            }}
            vm.trashedDesigns.forEach {item ->key(item.id) {
                Text(item.name);TextButton(onClick={vm.restore("design",item.id)}) {Text(stringResource(R.string.ui_restore_design))}
            }}
            if(vm.trashCount>vm.trashedSessions.size+vm.trashedDesigns.size)Text(stringResource(R.string.ui_a_cleanup_was_interrupted_empty_the_trash_to_complete_it),style=MaterialTheme.typography.bodySmall)
            if(vm.trashCount>0)TextButton(onClick={empty=true}){Text(stringResource(R.string.ui_empty_trash))}
        }
    }
    if(empty)AlertDialog(onDismissRequest={empty=false},title={Text(stringResource(R.string.ui_empty_trash_02347f))},
        text={Text(stringResource(R.string.ui_the_wav_files_analysis_and_associations_of_trashed_sessions_will_be_permanentl))},
        confirmButton={TextButton(onClick={empty=false;vm.emptyTrash()}){Text(stringResource(R.string.ui_delete_permanently))}},
        dismissButton={TextButton(onClick={empty=false}){Text(stringResource(R.string.ui_cancel))}})
}

@Composable
internal fun RecordingSettings(vm:TallerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember {mutableStateOf(false)}
    if(!vm.fullScreen)Column {
        TextButton(onClick={expanded=!expanded}){Text(stringResource(R.string.ui_limit_min_settings, vm.recordingLimitSeconds/60))}
        if(expanded) {
            Text(stringResource(R.string.ui_leaving_the_app_stops_and_saves_the_session_audio_is_not_recorded_in_the_backg),style=MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(1,5,15,30).forEach {minutes ->
                    FilterChip(selected=vm.recordingLimitSeconds==minutes*60,onClick={vm.setRecordingLimit(minutes*60)},label={Text("$minutes m")})
                }
            }
            Text(stringResource(R.string.ui_available_space_mb_15_min_76_mb_of_audio, vm.availableMegabytes()),style=MaterialTheme.typography.bodySmall)
        }
    }
}
