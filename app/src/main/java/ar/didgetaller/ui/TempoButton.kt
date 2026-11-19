package ar.didgetaller.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The two buttons share one repeat job; release can only stop the press that owns it. */
internal class TempoHold(private val scope: CoroutineScope, private val wait: suspend (Long) -> Unit = { delay(it) }) {
    private var owner: Any? = null
    private var repeating: Job? = null

    fun press(token: Any) {
        cancel()
        owner = token
    }

    fun start(token: Any, step: () -> Boolean) {
        if (owner !== token || repeating != null) return
        repeating = scope.launch {
            var interval = 150L
            while (step()) {
                wait(interval)
                interval = (interval - 10).coerceAtLeast(55)
            }
        }
    }

    fun release(token: Any) {
        if (owner === token) cancel()
    }

    fun owns(token: Any) = owner === token

    fun cancel() {
        owner = null
        repeating?.cancel()
        repeating = null
    }
}

@Composable
internal fun rememberTempoHold(): TempoHold {
    val scope = rememberCoroutineScope()
    val hold = remember(scope) { TempoHold(scope) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, hold) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) hold.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            hold.cancel()
        }
    }
    return hold
}

@Composable
internal fun TempoButton(glyph: String, label: String, tag: String, enabled: Boolean, hold: TempoHold, step: () -> Boolean) {
    val press = remember { TempoPress() }
    val update by rememberUpdatedState(step)
    val colors = MaterialTheme.colorScheme
    Surface(shape = CircleShape,
        color = if (enabled) colors.secondaryContainer else colors.onSurface.copy(alpha = .12f),
        contentColor = if (enabled) colors.onSecondaryContainer else colors.onSurface.copy(alpha = .38f)) {
        Box(Modifier.size(48.dp).testTag(tag).semantics { contentDescription = label }
            .then(if (enabled) Modifier.watchTempoPointer(hold, press) else Modifier)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onLongClick = { hold.start(press) { update() } },
                onClick = { if (!press.touch || hold.owns(press)) { hold.cancel(); update() } },
            ),
            contentAlignment = Alignment.Center) {
            Text(glyph, fontSize = 26.sp)
        }
    }
}

private class TempoPress(var touch: Boolean = false)

private fun Modifier.watchTempoPointer(hold: TempoHold, press: TempoPress) =
    pointerInput(hold, press) { awaitEachGesture { trackTempoPress(hold, press) } }

/** Observe boundaries alongside Android's click recognition without consuming pointer changes. */
private suspend fun AwaitPointerEventScope.trackTempoPress(hold: TempoHold, press: TempoPress) {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val bounds = Rect(Offset.Zero, size.toSize())
    press.touch = true
    hold.press(press)
    try {
        do {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val pointer = event.changes.firstOrNull { it.id == down.id }
            if (pointer != null && !bounds.contains(pointer.position)) hold.release(press)
        } while (pointer?.pressed == true)
    } finally {
        hold.release(press)
        press.touch = false
    }
}
