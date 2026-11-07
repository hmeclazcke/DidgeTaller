package ar.didgetaller.ui
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*
class ScreenAwakeTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun screenAwakeReleasesOnPauseStopAndDisposal() {
        Assume.assumeTrue(android.os.Build.HARDWARE in listOf("ranchu","goldfish"))
        val active=androidx.compose.runtime.mutableStateOf(false)
        val present=androidx.compose.runtime.mutableStateOf(true)
        // LifecycleRegistry retains its owner weakly; retain the owner in every callback.
        val owner=object:androidx.lifecycle.LifecycleOwner {
            override val lifecycle=androidx.lifecycle.LifecycleRegistry.createUnsafe(this)
        }
        owner.lifecycle.currentState=androidx.lifecycle.Lifecycle.State.RESUMED
        compose.setContent {if(present.value)ScreenAwake(compose.activity.window.decorView,owner.lifecycle,active.value)}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn);active.value=true}
        compose.runOnIdle {assertTrue(compose.activity.window.decorView.keepScreenOn);owner.lifecycle.currentState=androidx.lifecycle.Lifecycle.State.STARTED}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn);owner.lifecycle.currentState=androidx.lifecycle.Lifecycle.State.RESUMED}
        compose.runOnIdle {assertTrue(compose.activity.window.decorView.keepScreenOn);active.value=false}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn);active.value=true}
        compose.runOnIdle {assertTrue(compose.activity.window.decorView.keepScreenOn);present.value=false}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn)}
    }

}
