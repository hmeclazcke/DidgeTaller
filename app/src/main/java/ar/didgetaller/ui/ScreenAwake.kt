package ar.didgetaller.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** One foreground owner combines recording and playback; disposal releases its request. */
@Composable
internal fun ScreenAwake(view:View,lifecycle:Lifecycle,active:Boolean) {
    DisposableEffect(view,lifecycle,active) {
        val observer=LifecycleEventObserver { _, _ ->
            view.keepScreenOn=active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        view.keepScreenOn=active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose {lifecycle.removeObserver(observer);view.keepScreenOn=false}
    }
}
