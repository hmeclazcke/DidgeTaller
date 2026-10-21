package ar.didgetaller.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.audio.VisualState
import ar.didgetaller.core.*
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.math.max

class AxisReliabilityUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test fun measuredFrequencyMarginsAndTimeMarksFitBothRenderers(){
        assumeTrue("Solo emulador",Build.HARDWARE in listOf("ranchu","goldfish"))
        val state=ExplorerState()
        val points=List(100){SessionAnalysis.Point(it*470L,70.0,.97)}
        compose.setContent {MaterialTheme {Column(Modifier.fillMaxSize().padding(16.dp)){
            Text("Prueba de ejes · frecuencia en Hz")
            PlotSurface(state,VisualState(),PlotViewport(AxisWindow(0.0,47900.0),
                if(state.plot==AcousticPlot.PITCH)AxisWindow(60.0,80.0) else AxisWindow(0.0,500.0)),
                points,null,-1,Modifier.fillMaxWidth().height(250.dp))
        }}}
        for(kind in listOf(AcousticPlot.PITCH,AcousticPlot.SPECTROGRAM)){
            compose.runOnIdle {state.plot=kind}
            val node=compose.onNodeWithTag("time-chart").fetchSemanticsNode()
            val area=node.config[PlotDataArea];val window=node.config[PlotWindow]
            val density=compose.density.density
            val textSize=with(compose.density){11.sp.toPx()}
            val paint=android.graphics.Paint().apply {this.textSize=textSize}
            val ticks=if(kind==AcousticPlot.PITCH)PitchAxis.ticks(window.y,(area.height/(textSize*2.8f)).toInt()).map {it.hz}
                else PlotScales.ticks(window.y,if(area.height/textSize<12)3 else 5)
            val numbers=ticks.maxOf {paint.measureText(axisLabel(it,window.y.span))}
            val notes=if(kind==AcousticPlot.PITCH)PitchAxis.ticks(window.y,(area.height/(textSize*2.8f)).toInt())
                .maxOf {paint.measureText((if(it.approximate)"≈ " else "")+it.note.orEmpty())*.85f} else 0f
            val baseline=InstrumentationRegistry.getArguments().getString("baseline")=="true"
            if(!baseline){
                assertEquals(max(numbers,notes)+8*density,area.left,1f)
                assertTrue(node.config[PlotXLabels].size>=2)
                val labels=node.config[PlotXLabelBounds]
                assertTrue(labels.all {it.left>=area.left-1 && it.right<=area.right+1})
                labels.zipWithNext().forEach {(a,b)->assertTrue(b.left>=a.right+8*density-1)}
            }
            android.util.Log.i("Axis326","$kind baseline=$baseline width=${node.size.width} left=${area.left} data=${area.width}")
            compose.onNodeWithTag("time-chart").captureToImage()
            val label=InstrumentationRegistry.getArguments().getString("studioLabel")?:"v326"
            val file=File(compose.activity.getExternalFilesDir(null),"axes/$label-$kind.png").apply {parentFile!!.mkdirs()}
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let {bitmap->
                file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
        }
    }
}
