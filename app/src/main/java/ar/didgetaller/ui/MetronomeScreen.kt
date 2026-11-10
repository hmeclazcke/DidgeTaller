package ar.didgetaller.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ar.didgetaller.R
import ar.didgetaller.audio.PracticeMetronome
import ar.didgetaller.core.*

/** Volume keys address media on this screen; returning from Android's volume panel refreshes mute state. */
@Composable
internal fun MetronomeMediaControls(controller: PracticeMetronome, active: Boolean) {
    val activity = LocalContext.current.activity()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(activity, lifecycle, controller, active) {
        val previous = activity?.volumeControlStream
        activity?.volumeControlStream = if (active) android.media.AudioManager.STREAM_MUSIC else android.media.AudioManager.USE_DEFAULT_STREAM_TYPE
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) controller.refreshAudioEnvironment() }
        controller.refreshAudioEnvironment()
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (previous != null) activity?.volumeControlStream = previous
        }
    }
}

/** Practice controls share a viewport; only explicitly requested editing opens an IME. */
@Composable
fun MetronomeScreen(controller: PracticeMetronome) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("metronome-screen")) {
        val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (landscape) Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                MetronomeTempo(controller, true); MetronomeMeter(controller); MetronomeBeats(controller)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetronomeTransport(controller); MetronomeAdvanced(controller)
            }
        } else Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetronomeTempo(controller, false); MetronomeMeter(controller); MetronomeBeats(controller)
            MetronomeTransport(controller); MetronomeAdvanced(controller)
        }
    }
}

@Composable
private fun MetronomeTempo(controller: PracticeMetronome, compact: Boolean) {
    val bpm = controller.state.settings.bpm
    var editing by rememberSaveable { mutableStateOf(false) }
    val editLabel = stringResource(R.string.metronome_edit)
    val slower = stringResource(R.string.metronome_slower)
    val faster = stringResource(R.string.metronome_faster)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val tempoSize = minOf(if (compact) 60f else 76f, (maxWidth.value - 96f) / 1.8f / LocalDensity.current.fontScale).coerceAtLeast(24f).sp
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = { controller.update(controller.state.settings.copy(bpm = bpm - 1)) }, enabled = bpm > 30,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp).testTag("metronome-slower").semantics { contentDescription = slower }) { Text("−", fontSize = 26.sp) }
        Column(Modifier.weight(1f).clickable { editing = true }.testTag("metronome-edit").semantics { contentDescription = editLabel },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(bpm.toString(), fontSize = tempoSize, lineHeight = tempoSize * 1.1f, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("metronome-tempo-value"),
                textAlign = TextAlign.Center, maxLines = 1)
            Text(stringResource(R.string.metronome_bpm), style = MaterialTheme.typography.labelMedium)
        }
        FilledTonalButton(onClick = { controller.update(controller.state.settings.copy(bpm = bpm + 1)) }, enabled = bpm < 240,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp).testTag("metronome-faster").semantics { contentDescription = faster }) { Text("+", fontSize = 26.sp) }
      }
    }
    if (editing) MetronomeTempoDialog(bpm, onDismiss = { editing = false }) {
        controller.update(controller.state.settings.copy(bpm = it)); editing = false
    }
}

@Composable
private fun MetronomeTempoDialog(bpm: Int, onDismiss: () -> Unit, apply: (Int) -> Unit) {
    var draft by remember { mutableStateOf(bpm.toString()) }
    val number = draft.toIntOrNull()
    val valid = number != null && number in 30..240
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.metronome_edit)) },
        text = { OutlinedTextField(draft, { draft = it }, modifier = Modifier.testTag("metronome-bpm"), singleLine = true,
            isError = !valid, supportingText = { Text(stringResource(R.string.metronome_bpm_range)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) apply(number!!) })) },
        confirmButton = { TextButton(onClick = { apply(number!!) }, enabled = valid, modifier = Modifier.testTag("metronome-apply")) { Text(stringResource(R.string.metronome_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.metronome_cancel)) } })
}

@Composable
private fun MetronomeMeter(controller: PracticeMetronome) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        listOf(2, 3, 4).forEach { meter ->
            FilterChip(controller.state.settings.beatsPerBar == meter, { controller.update(controller.state.settings.copy(beatsPerBar = meter)) },
                label = { Text("$meter/4") }, modifier = Modifier.testTag("metronome-meter-$meter"))
        }
    }
}

@Composable
private fun MetronomeBeats(controller: PracticeMetronome) {
    val state = controller.state
    val pulse = state.pulse
    val description = if (pulse == null) stringResource(R.string.metronome_ready) else
        stringResource(R.string.metronome_beat, pulse.beat, state.settings.beatsPerBar, pulse.subdivision, state.settings.subdivision.pulses)
    Row(Modifier.fillMaxWidth().height(52.dp).testTag("metronome-beats").semantics { stateDescription = description },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        repeat(state.settings.beatsPerBar) { index ->
            val selected = state.mode == MetronomeMode.RUNNING && pulse?.beat == index + 1
            val base = if (index == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            val color by animateColorAsState(if (selected) base else MaterialTheme.colorScheme.surfaceVariant, tween(70), label = "beat-color")
            val size by animateDpAsState(if (selected && pulse?.subdivision == 1) 44.dp else 36.dp, tween(70), label = "beat-size")
            Box(Modifier.size(size).background(color, CircleShape).border(2.dp, base, CircleShape), contentAlignment = Alignment.Center) {
                Text((index + 1).toString(), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    Text(description, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun MetronomeTransport(controller: PracticeMetronome) {
    val state = controller.state
    val focus = LocalFocusManager.current
    val status = metronomeSoundStatus(state.settings, state.captureActive, state.audioFailed, controller.systemMuted)
    val soundLabel = stringResource(if (state.settings.audible) R.string.metronome_sound_on else R.string.metronome_sound_off)
    val mainLabel = when (state.mode) {
        MetronomeMode.RUNNING -> R.string.metronome_stop
        MetronomeMode.PAUSED -> R.string.metronome_resume
        MetronomeMode.STOPPED -> R.string.metronome_start
    }
    Button(onClick = { focus.clearFocus(); if (state.mode == MetronomeMode.RUNNING) controller.stop() else controller.start() },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("metronome-transport")) {
        Text(stringResource(mainLabel), style = MaterialTheme.typography.titleLarge)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = state.settings.audible, onClick = { controller.requestSound(!state.settings.audible) },
            enabled = !state.captureActive, label = { Text(soundLabel) }, modifier = Modifier.weight(1f).testTag("metronome-sound"))
        TextButton(onClick = { focus.clearFocus(); if (state.mode == MetronomeMode.RUNNING) controller.pause() else controller.stop() },
            enabled = state.mode != MetronomeMode.STOPPED, modifier = Modifier.testTag("metronome-pause")) {
            Text(stringResource(if (state.mode == MetronomeMode.PAUSED) R.string.metronome_reset else R.string.metronome_pause))
        }
    }
    MetronomeSoundNotice(status)
}

@Composable
private fun MetronomeSoundNotice(status: MetronomeSoundStatus) {
    val context = LocalContext.current
    val notices = mapOf(MetronomeSoundStatus.BLOCKED to R.string.metronome_visual_only,
        MetronomeSoundStatus.SYSTEM_MUTED to R.string.metronome_system_muted,
        MetronomeSoundStatus.GAIN_ZERO to R.string.metronome_gain_zero, MetronomeSoundStatus.FAILED to R.string.metronome_audio_failed)
    notices[status]?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("metronome-notice")) }
    if (status == MetronomeSoundStatus.SYSTEM_MUTED) TextButton(onClick = {
        context.startActivity(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS))
    }) { Text(stringResource(R.string.metronome_media_settings)) }
}

@Composable
private fun MetronomeAdvanced(controller: PracticeMetronome) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag("metronome-expand")) {
        Text(stringResource(R.string.metronome_advanced))
    }
    if (expanded) {
        val settings = controller.state.settings
        val labels = listOf(R.string.metronome_quarter, R.string.metronome_eighth, R.string.metronome_triplet, R.string.metronome_sixteenth)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetronomeSubdivision.entries.forEachIndexed { index, value ->
                FilterChip(settings.subdivision == value, { controller.update(controller.state.settings.copy(subdivision = value)) },
                    label = { Text(stringResource(labels[index])) }, modifier = Modifier.testTag("metronome-subdivision-${value.name}"))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metronome_accent), Modifier.weight(1f))
            Switch(settings.accentFirst, { controller.update(controller.state.settings.copy(accentFirst = it)) }, Modifier.testTag("metronome-accent"))
        }
        Text(stringResource(R.string.metronome_volume, (settings.volume * 100).toInt()))
        val label = stringResource(R.string.metronome_volume_label)
        Slider(settings.volume, { controller.update(controller.state.settings.copy(volume = it)) },
            modifier = Modifier.testTag("metronome-volume").semantics { contentDescription = label })
    }
}
