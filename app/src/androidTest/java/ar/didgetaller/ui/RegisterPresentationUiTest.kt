package ar.didgetaller.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.audio.VisualState
import ar.didgetaller.core.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class RegisterPresentationUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun bothRegistersRemainVisibleWithoutAnArtificialSweep() {
        assumeTrue("Emulator only",Build.HARDWARE in listOf("ranchu","goldfish"))
        val points=listOf(35.0,35.0,70.0,70.0,140.0,140.0).mapIndexed {i,hz->SessionAnalysis.Point(i*200L,hz,.98)}
        val state=ExplorerState()
        val viewport=PlotViewport(AxisWindow(0.0,1100.0),AxisWindow(20.0,160.0))
        compose.setContent {
            Column {PlotSurface(state,VisualState(),viewport,points,null,-1,Modifier.fillMaxWidth().height(400.dp))}
        }
        val node=compose.onNodeWithTag("time-chart")
        val area=node.fetchSemanticsNode().config[PlotDataArea]
        val bitmap=node.captureToImage().asAndroidBitmap()
        fun pixel(time:Double,hz:Double):Int {
            val x=(area.left+viewport.x.fraction(time)*area.width).roundToInt()
            val y=(area.top+(1-viewport.y.fraction(hz))*area.height).roundToInt()
            return bitmap.getPixel(x,y)
        }
        val background=android.graphics.Color.rgb(16,29,37)
        assertEquals("No 35→70 sweep",background,pixel(300.0,52.5))
        assertNotEquals("35 Hz remains",background,pixel(100.0,35.0))
        assertNotEquals("70 Hz remains",background,pixel(500.0,70.0))
        assertNotEquals("140 Hz remains",background,pixel(900.0,140.0))
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        File(target.getExternalFilesDir(null),"register-separation.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
}
