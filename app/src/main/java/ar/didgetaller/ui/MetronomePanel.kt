package ar.didgetaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ar.didgetaller.R
import ar.didgetaller.audio.PracticeMetronome
import ar.didgetaller.core.MetronomeMode
import ar.didgetaller.core.MetronomeSettings
import ar.didgetaller.core.MetronomeSubdivision

/** A compact practice tool; expanded settings remain in the existing scrollable measurement screen. */
@Composable
fun MetronomePanel(controller: PracticeMetronome) {
    val state = controller.state
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("metronome")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag("metronome-expand")) {
                Text(stringResource(R.string.metronome_title, state.settings.bpm))
            }
            MetronomeBeats(state.settings, state.pulse?.beat, state.pulse?.subdivision)
            MetronomeTransport(controller)
            if (state.captureActive) Text(stringResource(R.string.metronome_visual_only), Modifier.testTag("metronome-interlock"))
            if (state.audioFailed) Text(stringResource(R.string.metronome_audio_failed))
            if (expanded) MetronomeSettingsPanel(controller)
        }
    }
}

@Composable
private fun MetronomeTransport(controller: PracticeMetronome) {
    val mode = controller.state.mode
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = controller::start, enabled = mode != MetronomeMode.RUNNING, modifier = Modifier.testTag("metronome-start")) {
            Text(stringResource(if (mode == MetronomeMode.PAUSED) R.string.metronome_resume else R.string.metronome_start))
        }
        OutlinedButton(onClick = controller::pause, enabled = mode == MetronomeMode.RUNNING, modifier = Modifier.testTag("metronome-pause")) {
            Text(stringResource(R.string.metronome_pause))
        }
        TextButton(onClick = controller::stop, enabled = mode != MetronomeMode.STOPPED, modifier = Modifier.testTag("metronome-stop")) {
            Text(stringResource(R.string.metronome_stop))
        }
    }
}

@Composable
private fun MetronomeBeats(settings: MetronomeSettings, beat: Int?, subdivision: Int?) {
    val description = stringResource(R.string.metronome_beat, beat ?: 0, settings.beatsPerBar,
        subdivision ?: 0, settings.subdivision.pulses)
    Row(Modifier.fillMaxWidth().testTag("metronome-beats").semantics { stateDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        repeat(settings.beatsPerBar) { index ->
            val active = beat == index + 1
            Box(Modifier.size(36.dp).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center) {
                Text((index + 1).toString(), color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Text(description, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun MetronomeSettingsPanel(controller: PracticeMetronome) {
    val settings = controller.state.settings
    MetronomeTempo(settings.bpm) { controller.update(settings.copy(bpm = it)) }
    Text(stringResource(R.string.metronome_meter))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 3, 4).forEach { meter ->
            FilterChip(selected = settings.beatsPerBar == meter, onClick = { controller.update(settings.copy(beatsPerBar = meter)) },
                label = { Text("$meter/4") }, modifier = Modifier.testTag("metronome-meter-$meter"))
        }
    }
    MetronomeSubdivisions(settings.subdivision) { controller.update(settings.copy(subdivision = it)) }
    MetronomeToggle(stringResource(R.string.metronome_accent), settings.accentFirst, true, "metronome-accent") {
        controller.update(settings.copy(accentFirst = it))
    }
    MetronomeToggle(stringResource(R.string.metronome_sound), settings.audible, !controller.state.captureActive,
        "metronome-sound", controller::requestSound)
    Text(stringResource(R.string.metronome_volume, (settings.volume * 100).toInt()))
    val volumeLabel = stringResource(R.string.metronome_volume_label)
    Slider(value = settings.volume, onValueChange = { controller.update(settings.copy(volume = it)) },
        modifier = Modifier.testTag("metronome-volume").semantics { contentDescription = volumeLabel })
}

@Composable
private fun MetronomeTempo(bpm: Int, changed: (Int) -> Unit) {
    var draft by remember(bpm) { mutableStateOf(bpm.toString()) }
    val invalid = draft.toIntOrNull()?.let { it !in 30..240 } ?: true
    OutlinedTextField(value = draft, onValueChange = { value ->
        draft = value
        value.toIntOrNull()?.takeIf { it in 30..240 }?.let(changed)
    }, label = { Text(stringResource(R.string.metronome_bpm)) }, isError = invalid,
        supportingText = { Text(stringResource(R.string.metronome_bpm_range)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("metronome-bpm"))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { changed(bpm - 1) }, enabled = bpm > 30, modifier = Modifier.testTag("metronome-slower")) { Text("−1 BPM") }
        OutlinedButton(onClick = { changed(bpm + 1) }, enabled = bpm < 240, modifier = Modifier.testTag("metronome-faster")) { Text("+1 BPM") }
    }
}

@Composable
private fun MetronomeSubdivisions(selected: MetronomeSubdivision, changed: (MetronomeSubdivision) -> Unit) {
    Text(stringResource(R.string.metronome_subdivision))
    val labels = listOf(R.string.metronome_quarter, R.string.metronome_eighth, R.string.metronome_triplet, R.string.metronome_sixteenth)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetronomeSubdivision.entries.forEachIndexed { index, value ->
            FilterChip(selected = selected == value, onClick = { changed(value) }, label = { Text(stringResource(labels[index])) },
                modifier = Modifier.testTag("metronome-subdivision-${value.name}"))
        }
    }
}

@Composable
private fun MetronomeToggle(label: String, checked: Boolean, enabled: Boolean, tag: String, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = changed, enabled = enabled,
            modifier = Modifier.testTag(tag).semantics { contentDescription = label })
    }
}
