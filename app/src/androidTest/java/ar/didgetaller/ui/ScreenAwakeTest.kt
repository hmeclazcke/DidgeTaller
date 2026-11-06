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
        val lifecycle=androidx.lifecycle.LifecycleRegistry.createUnsafe(object:androidx.lifecycle.LifecycleOwner {
            override val lifecycle:androidx.lifecycle.Lifecycle get()=error("Registry owns test lifecycle")
        })
        lifecycle.currentState=androidx.lifecycle.Lifecycle.State.RESUMED
        compose.setContent {if(present.value)ScreenAwake(compose.activity.window.decorView,lifecycle,active.value)}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn);active.value=true}
        compose.runOnIdle {assertTrue(compose.activity.window.decorView.keepScreenOn);lifecycle.currentState=androidx.lifecycle.Lifecycle.State.STARTED}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn);lifecycle.currentState=androidx.lifecycle.Lifecycle.State.RESUMED}
        compose.runOnIdle {assertTrue(compose.activity.window.decorView.keepScreenOn);active.value=false}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn);active.value=true}
        compose.runOnIdle {assertTrue(compose.activity.window.decorView.keepScreenOn);present.value=false}
        compose.runOnIdle {assertFalse(compose.activity.window.decorView.keepScreenOn)}
    }

}
