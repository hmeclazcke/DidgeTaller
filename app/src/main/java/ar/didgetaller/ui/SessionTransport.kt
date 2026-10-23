package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.TallerViewModel
import ar.didgetaller.data.RecordingStore

/** A seek preview never replaces the chart clock: both read the actual player position. */
@Composable
internal fun SessionTransport(vm: TallerViewModel, item: RecordingStore.Session, cursor: Long) {
    var seekPreview by remember(item.id) { mutableStateOf<Float?>(null) }
    val duration = item.durationMs.coerceAtLeast(1)
    val position = cursor.coerceIn(0, duration)
    val seekLabel = stringResource(R.string.playback_seek_label)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = { vm.togglePlayback(item) }, modifier = Modifier.heightIn(min = 50.dp)) {
            Text(stringResource(if(vm.playingId == item.id && !vm.playbackPaused) R.string.ui_pause else R.string.ui_play))
        }
        OutlinedButton(onClick = { vm.restartPlayback(item) }, modifier = Modifier.heightIn(min = 50.dp)) {
            Text(stringResource(R.string.ui_restart))
        }
    }
    Text(stringResource(R.string.session_playback_time, transportTime(position), transportTime(item.durationMs)),
        style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("session-playback-time"))
    Slider(value = seekPreview ?: position.toFloat(),
        onValueChange = { seekPreview = it },
        onValueChangeFinished = {
            seekPreview?.let { vm.seekPlayback(item, it.toLong()) }
            seekPreview = null
        },
        valueRange = 0f..duration.toFloat(), enabled = item.durationMs > 0,
        modifier = Modifier.fillMaxWidth().testTag("session-playback-seek")
            .semantics { contentDescription = seekLabel })
}

private fun transportTime(timeMs: Long) = "%02d:%02d".format(timeMs / 60000, (timeMs / 1000) % 60)
