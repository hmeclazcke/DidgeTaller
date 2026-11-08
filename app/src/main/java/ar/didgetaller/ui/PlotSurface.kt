package ar.didgetaller.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ar.didgetaller.audio.VisualState
import ar.didgetaller.core.*
import java.util.Locale
import kotlin.math.*

internal val displayLocale:Locale get()=Locale.getDefault()
internal val PlotDataArea=SemanticsPropertyKey<androidx.compose.ui.geometry.Rect>("PlotDataArea")
internal val PlotWindow=SemanticsPropertyKey<PlotViewport>("PlotWindow")
internal val PlotSwipeCount=SemanticsPropertyKey<Int>("PlotSwipeCount")
internal val PlotPlaybackMs=SemanticsPropertyKey<Long>("PlotPlaybackMs")
internal val PlotXLabels=SemanticsPropertyKey<List<String>>("PlotXLabels")
internal val PlotAudioLevelCount=SemanticsPropertyKey<Int>("PlotAudioLevelCount")
internal val PlotDroneComponentCount=SemanticsPropertyKey<Int>("PlotDroneComponentCount")
internal val PlotYinCount=SemanticsPropertyKey<Int>("PlotYinCount")
internal val PlotHighRegisterCount=SemanticsPropertyKey<Int>("PlotHighRegisterCount")
internal val PlotXLabelBounds=SemanticsPropertyKey<List<androidx.compose.ui.geometry.Rect>>("PlotXLabelBounds")
internal fun pitchColor(tone:PitchTrace.Tone)=when(tone){
    PitchTrace.Tone.STABLE->Color(0xFF8FE0BD)
    PitchTrace.Tone.MODERATE->Color(0xFFE9D285)
    PitchTrace.Tone.REVIEW->Color(0xFFEBA16E)
}
internal fun hzLabel(hz:Double)=if(hz.isFinite() && hz>0)"%.1f Hz".format(displayLocale,hz) else "— Hz"
internal fun axisLabel(value:Double,span:Double):String {
    val label=when {span<.1->"%.4f";span<10->"%.2f";span<100->"%.1f";else->"%.0f"}.format(displayLocale,if(abs(value)<1e-12)0.0 else value)
    val separator=java.text.DecimalFormatSymbols.getInstance(displayLocale).decimalSeparator
    return if(separator in label)label.trimEnd('0').trimEnd(separator) else label
}

internal data class PlacedAxisLabel(val text:String,val center:Float,val width:Float)
internal fun placeAxisLabels(ticks:List<Double>,window:AxisWindow,width:Float,gap:Float,
    format:(Double)->String,measure:(String)->Float):List<PlacedAxisLabel> {
    if(width<=0)return emptyList()
    val result=mutableListOf<PlacedAxisLabel>()
    for(tick in ticks){val text=format(tick);val measured=measure(text)
        if(measured>width)continue
        val center=(window.fraction(tick)*width).toFloat().coerceIn(measured/2,width-measured/2)
        if(result.lastOrNull()?.let {center-measured/2<it.center+it.width/2+gap}==true)continue
        result+=PlacedAxisLabel(text,center,measured)
    }
    return result
}

/** All four render adapters share axes, clipping, coordinates and cursor overlay. */
@Composable
internal fun PlotSurface(state:ExplorerState,visual:VisualState,viewport:PlotViewport,points:List<SessionAnalysis.Point>,
    analysis:WavAnalysis.Result?,cursor:Long,modifier:Modifier=Modifier,immersive:Boolean=false,
    swipe:SwipeCurve?=null,diagnostic:Boolean=false,
    musicianTrace:MusicalTrace?=null,
    durationMs:Long=visual.durationMs,
    onScrub:(Long,Boolean)->Unit={_,_->},
    onPreview:()->Unit={},onPoint:(Double,Double,Boolean)->Unit={_,_,_->},onReset:()->Unit={},
    onTransform:(Double,Double,Double,Double,Double,Double)->Unit={_,_,_,_,_,_->}) {
    val density=LocalDensity.current
    var dimensions by remember {mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    val displayedTrace=musicianTrace
    val spectralTrace=displayedTrace?.meaning==MusicalTrace.Meaning.SPECTRAL_COMPONENT
    val level=visual.level.takeIf {state.plot==AcousticPlot.PITCH}
    val frame=rememberPlotFrame(state.plot,viewport,dimensions,state.gestureActive,level!=null,spectralTrace,state.logarithmic)
    val palette=Color(0xFF101D25)
    val measuredModifier=modifier.background(palette).testTag(plotSurfaceTag(state.plot,immersive,diagnostic))
        .onSizeChanged {dimensions=it}
    // Unmount pointer ownership as soon as no drawable coordinate frame exists.
    if(frame==null){Canvas(measuredModifier) {};return}
    with(frame){
        val preparedTrace=remember(points,state.instrument,displayedTrace,state.showUncertain,state.plot){
            TraceDrawing.prepare(displayedTrace,points,state.instrument,state.plot==AcousticPlot.PITCH,state.showUncertain)
        }
        val traceMarks=preparedTrace.marks
        val waveDrawing=rememberWaveDrawing(visual.wave.takeIf {state.plot==AcousticPlot.WAVE},projection)
        val rasterInk=rememberRasterInk(state.plot==AcousticPlot.SPECTROGRAM,visual.spectrogram,visual.bitmap,projection)
        // Retain level reduction while only the playback cursor changes.
        val levelPixels=remember(level,camera.x,plotWidth){PlotReduction.levelPixels(level,camera.x,plotWidth)}
        val comparisonInk=rememberComparisonInk(swipe,projection,state.plot==AcousticPlot.PITCH)
        val coordinates=remember(camera,dimensions,left,right,top,bottom,log){
            PlotGestureCoordinates(camera,(dimensions.width-left-right).toDouble(),(dimensions.height-top-bottom).toDouble(),
                left.toDouble(),top.toDouble(),log)
        }
        val gesture=Modifier.plotInteraction(state,coordinates,onScrub,onPoint,onReset,onTransform)
        // Reduction is retained while the native playback cursor advances.
        val pitchInk=rememberPitchInk(state.plot==AcousticPlot.PITCH,traceMarks,displayedTrace,analysis?.summary?.typicalHz,projection)
        val spectrumInk=rememberSpectrumInk(state.plot==AcousticPlot.SPECTRUM,analysis,projection,plotWidth.toInt())
        val effects=remember {PlotEffects(PathEffect.dashPathEffect(floatArrayOf(5f,5f)),PathEffect.dashPathEffect(floatArrayOf(8f,6f)),
            PathEffect.dashPathEffect(floatArrayOf(8f,5f)),PathEffect.dashPathEffect(floatArrayOf(3f,5f)),PathEffect.dashPathEffect(floatArrayOf(3f,5f)))}
        val traceEvidence=preparedTrace.evidence
        val accessibility=PlotAccessibility(androidx.compose.ui.geometry.Rect(left,top,dimensions.width-right,dimensions.height-bottom),camera,
            state.plot.temporal,cursor,durationMs,traceEvidence,if(state.plot==AcousticPlot.PITCH)swipe?.samples?.count {it.hz!=null} ?: 0 else 0,
            level?.rms?.size ?: 0,xLabels.map {it.text},xLabels.map {androidx.compose.ui.geometry.Rect(left+it.center-it.width/2,
                dimensions.height-bottom,left+it.center+it.width/2,dimensions.height.toFloat())})
        val timeline=if(state.plot.temporal)TimelineInk(state.region,state.selectedMs,cursor,with(density){8.dp.toPx()}) else null
        val content=PlotContent(rasterInk,pitchInk,comparisonInk,waveDrawing,spectrumInk,timeline)
        PlotCanvas(frame,content,effects,levelPixels,plotUnitLabel(state.plot),
            measuredModifier.then(gesture)
            .plotAccessibility(accessibility,plotDescription(state.plot,state.instrument,camera,pitchTicks,spectralTrace,diagnostic),onScrub))
    }
}
