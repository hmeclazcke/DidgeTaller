package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.data.RecordingStore

/** Compact transport shares the graph clock; seeking belongs to the plot. */
@Composable
internal fun SessionTransport(vm: TallerViewModel, item: RecordingStore.Session, cursor: Long) {
    val position=cursor.coerceIn(0,item.durationMs.coerceAtLeast(1))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        Button(onClick = { vm.togglePlayback(item) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if(vm.playingId == item.id && !vm.playbackPaused) R.string.ui_pause else R.string.ui_play))
        }
        OutlinedButton(onClick = { vm.restartPlayback(item) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.ui_restart))
        }
    Text(stringResource(R.string.session_playback_time, transportTime(position), transportTime(item.durationMs)),
        style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("session-playback-time"))
    }
}

private fun transportTime(timeMs: Long) = "%02d:%02d".format(timeMs / 60000, (timeMs / 1000) % 60)
