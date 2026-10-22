package ar.didgetaller.ui

import ar.didgetaller.R

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.MainActivity
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.*
import ar.didgetaller.data.RecordingStore
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.*

/** Actual MainActivity recreation. Only one new UUID fixture in the emulator; no prefs clearing. */
class ViewerRotationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:TallerViewModel
    private lateinit var item:RecordingStore.Session
    private lateinit var store:RecordingStore
    @Before fun setup(){
        assumeTrue("Exclusivamente emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        store=RecordingStore(compose.activity.application)
        val pcm=ShortArray(44100*30){(6000*sin(2*PI*70*it/44100)).toInt().toShort()}
        item=store.save(WavCodec.encode(pcm,pcm.size,44100),(0..140).map {SessionAnalysis.Point(it*210L,70.0,.95)})
        compose.runOnUiThread {vm=ViewModelProvider(compose.activity)[TallerViewModel::class.java];vm.renameSession(item,"Prueba sintética de rotación")}
    }
    @After fun cleanup(){
        if(::vm.isInitialized)compose.runOnUiThread {vm.stopPlayback();vm.workspace.clear();compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED}
        if(::item.isInitialized)store.delete(item.id) // Only the UUID this test created, never existing sessions.
    }
    @Test fun actualRecreationAndLandscapeKeepPlayerCameraSelectionAndBackStack(){
        compose.onNodeWithText(uiText(R.string.ui_library),useUnmergedTree=true).performClick()
        compose.onNodeWithTag("session-${item.id}").performScrollTo().performClick()
        compose.onNodeWithText(uiText(R.string.ui_play)).performScrollTo().performClick()
        compose.waitUntil {vm.playingId==item.id && !vm.playbackPaused}
        compose.runOnIdle {vm.seekPlayback(item,4000)}
        compose.waitUntil {vm.playbackPositionMs() in 3900..6000}
        compose.onNodeWithText(uiText(R.string.ui_pause)).performClick()
        val position=vm.playbackPositionMs()
        compose.onNodeWithTag("expand-plot").performScrollTo().performClick()
        compose.onNodeWithContentDescription(uiText(R.string.ui_zoom_in)).performClick()
        val state=vm.explorer(item.id)
        compose.runOnIdle {state.regionStart=3000;state.regionEnd=7000}
        val camera=state.cameras[state.plot]
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("acoustic-viewer").assertIsDisplayed()
        compose.runOnIdle {
            assertSame(vm,ViewModelProvider(compose.activity)[TallerViewModel::class.java])
            assertEquals(item.id,vm.playingId);assertTrue(vm.playbackPaused)
            assertEquals(position.toDouble(),vm.playbackPositionMs().toDouble(),80.0)
            assertEquals(camera,state.cameras[state.plot]);assertEquals(3000L..7000L,state.region)
        }
        compose.onNodeWithTag("viewer-play").performClick()
        compose.onNodeWithTag("rotate-viewer").performClick()
        compose.waitUntil(10000){compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
        compose.onNodeWithTag("acoustic-viewer").assertIsDisplayed()
        compose.runOnIdle {assertEquals(item.id,vm.playingId);assertFalse(vm.playbackPaused);assertEquals(camera,state.cameras[state.plot]);assertEquals(3000L..7000L,state.region)}
        val bounds=compose.onNodeWithTag("immersive-plot").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width>bounds.height);assertTrue(bounds.height>120*compose.activity.resources.displayMetrics.density)
        val path=File(compose.activity.getExternalFilesDir(null),"ui-audit/v31-after-landscape.png").apply {parentFile!!.mkdirs()}
        path.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_session)).performClick()
        // Check the live player before pausing it. This avoids waiting for a continuously
        // changing playback cursor to become Espresso-idle across the orientation/inset transition.
        compose.runOnUiThread {assertEquals(item.id,vm.playingId);assertFalse(vm.playbackPaused);vm.togglePlayback(item)}
        compose.onNodeWithTag("session-detail").assertIsDisplayed()
        compose.runOnIdle {assertEquals(item.id,vm.playingId)}
        compose.onNodeWithContentDescription(uiText(R.string.ui_back_to_sessions)).performClick()
        compose.onNodeWithTag("library-list").assertIsDisplayed()
        compose.runOnIdle {assertNull(vm.playingId);assertEquals(0,vm.playbackPositionMs())}
    }
    @Test fun returningWithPermissionGrantedUpdatesFeedbackWithoutStartingCapture(){
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(compose.activity.packageName,android.Manifest.permission.RECORD_AUDIO)
        compose.runOnIdle {vm.onPermissionResult(false,true);assertTrue(vm.permissionDenied)}
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {assertFalse(vm.permissionDenied);assertFalse(vm.listening);assertEquals(ar.didgetaller.SessionPhase.IDLE,vm.sessionPhase)}
        compose.onNodeWithText(uiText(R.string.ui_start_measurement)).assertIsDisplayed()
    }
}
