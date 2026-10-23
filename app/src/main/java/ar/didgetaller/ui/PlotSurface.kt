package ar.didgetaller.ui

import ar.didgetaller.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    swipe:SwipeCurve?=null,
    onPreview:()->Unit={},onPoint:(Double,Double,Boolean)->Unit={_,_,_->},onReset:()->Unit={},
    onTransform:(Double,Double,Double,Double,Double,Double)->Unit={_,_,_,_,_,_->}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density=LocalDensity.current
    val textSize=with(density){11.sp.toPx()}
    val right=with(density){12.dp.toPx()}
    val top=max(with(density){(if(state.plot==AcousticPlot.SPECTROGRAM)16.dp else 10.dp).toPx()},textSize*(if(state.plot==AcousticPlot.SPECTROGRAM)1.6f else 1f))
    val bottom=textSize*1.8f
    var dimensions by remember {mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    val bitmap=remember(visual.bitmap){visual.bitmap?.asImageBitmap()}
    val log=state.plot==AcousticPlot.SPECTROGRAM && state.logarithmic
    val camera=viewport
    val point by rememberUpdatedState(onPoint);val transform by rememberUpdatedState(onTransform)
    val palette=Color(0xFF101D25);val grid=Color(0xFF29414B)
    val paint=remember(textSize){android.graphics.Paint().apply {isAntiAlias=true;color=android.graphics.Color.rgb(185,208,211);this.textSize=textSize}}
    val pitchTicks=remember(camera.y,dimensions.height,textSize,state.plot){
        if(state.plot==AcousticPlot.PITCH)PitchAxis.ticks(camera.y,((dimensions.height-top-bottom)/(textSize*2.8f)).toInt()) else emptyList()
    }
    val labelGap=with(density){4.dp.toPx()}
    val yTicks=if(state.plot==AcousticPlot.PITCH)pitchTicks.map {it.hz} else
        PlotScales.ticks(camera.y,if((dimensions.height-top-bottom)/textSize<12)3 else 5,log)
    // Measure the exact strings with the same native Paint that renders them, including notes.
    val labelLocale=androidx.compose.ui.platform.LocalConfiguration.current.locales.toLanguageTags()
    val requiredLeft=if(state.plot in listOf(AcousticPlot.PITCH,AcousticPlot.SPECTROGRAM))remember(pitchTicks,yTicks,camera.y.span,textSize,labelGap,labelLocale){
        val hzWidth=yTicks.maxOfOrNull {paint.measureText(axisLabel(it,camera.y.span))} ?: 0f
        val noteWidth=pitchTicks.maxOfOrNull {paint.measureText((if(it.approximate)"≈ " else "")+it.note.orEmpty())*.85f} ?: 0f
        max(hzWidth,noteWidth)+labelGap*2
    } else max(with(density){(if(state.plot==AcousticPlot.SPECTRUM)58.dp else 56.dp).toPx()},textSize*3.2f)
    // Keep the projection's origin fixed for the entire gesture; measure again when released.
    var settledLeft by remember(state.plot,textSize){mutableFloatStateOf(requiredLeft)}
    SideEffect {if(!state.gestureActive)settledLeft=requiredLeft}
    val left=if(state.gestureActive)settledLeft else requiredLeft
    val pitchTrace=remember(points,state.instrument){PitchTrace.build(points,state.instrument)}
    val plotWidth=(dimensions.width-left-right).coerceAtLeast(0f)
    val swipePaths=remember(swipe,camera,dimensions,left,right,top,bottom,state.plot){
        val path=Path();val dots=mutableListOf<Offset>()
        if(swipe!=null && state.plot==AcousticPlot.PITCH){
            val h=(dimensions.height-top-bottom).coerceAtLeast(1f)
            fun position(sample:SwipeCurve.Sample)=Offset((camera.x.fraction(sample.timeMs)*plotWidth).toFloat(),((1-camera.y.fraction(sample.hz!!))*h).toFloat())
            for(i in swipe.samples.indices){
                val sample=swipe.samples[i]
                if(sample.hz==null || sample.timeMs<camera.x.low-swipe.hopMs || sample.timeMs>camera.x.high+swipe.hopMs)continue
                val p=position(sample)
                if(swipe.linked(i)){
                    val previous=position(swipe.samples[i-1]);path.moveTo(previous.x,previous.y);path.lineTo(p.x,p.y)
                }else if(!swipe.linked(i+1))dots+=p
            }
        }
        path to dots
    }
    val xTicks=PlotScales.ticks(camera.x,if(plotWidth/textSize<30)3 else 5)
    val xLabels=placeAxisLabels(xTicks,camera.x,plotWidth,labelGap*2,
        {if(state.plot.temporal)seconds(it.toLong()) else axisLabel(it,camera.x.span)+" Hz"},paint::measureText)

    fun data(position:Offset):Pair<Double,Double>{
        val w=(dimensions.width-left-right).coerceAtLeast(1f);val h=(dimensions.height-top-bottom).coerceAtLeast(1f)
        val mapped=PlotProjection(camera,w.toDouble(),h.toDouble(),left.toDouble(),top.toDouble(),log).data(position.x.toDouble(),position.y.toDouble())
        return mapped.x to mapped.y
    }
    val gesture=if(immersive)Modifier.immersiveGestures(
        onTap={p->val d=data(p);point(d.first,d.second,false)},onInspect={p->val d=data(p);point(d.first,d.second,true)},
        onReset=onReset,onActive={state.gestureActive=it},onTransform={center,pan,zx,zy ->
            val w=(dimensions.width-left-right).coerceAtLeast(1f);val h=(dimensions.height-top-bottom).coerceAtLeast(1f)
            transform(((center.x-left)/w).toDouble().coerceIn(0.0,1.0),((center.y-top)/h).toDouble().coerceIn(0.0,1.0),zx.toDouble(),zy.toDouble(),(pan.x/w).toDouble(),(pan.y/h).toDouble())
        }) else Modifier.pointerInput(onPreview){detectTapGestures(onTap={onPreview()})}
    // Reduction is retained while the native playback cursor advances.
    val spectrum=analysis?.spectrum
    val spectrumReference=remember(spectrum){spectrum?.density?.maxOrNull() ?: 0.0}
    val curve=remember(spectrum,camera.x,dimensions.width){
        if(spectrum==null || dimensions.width==0)emptyList() else {
            val bin=spectrum.sampleRate.toDouble()/spectrum.fftSize
            val first=ceil(camera.x.low/bin).toInt().coerceIn(0,spectrum.density.lastIndex)
            val last=floor(camera.x.high/bin).toInt().coerceIn(first,spectrum.density.lastIndex)
            val count=(dimensions.width-left-right).toInt().coerceAtLeast(2)
            if(last-first<count)(first..last).map {it*bin to spectrum.density[it]}
            else {
                val peaks=DoubleArray(count);for(i in first..last){val cell=((i*bin-camera.x.low)/camera.x.span*(count-1)).toInt().coerceIn(0,count-1);peaks[cell]=max(peaks[cell],spectrum.density[i])}
                peaks.mapIndexed {i,value->camera.x.value(i.toDouble()/(count-1)) to value}
            }
        }
    }
    Canvas(modifier.background(palette).then(gesture).testTag(if(immersive)"immersive-plot" else if(state.plot==AcousticPlot.SPECTRUM)"spectrum-chart" else "time-chart")
        .semantics {contentDescription=context.getString(R.string.ui_to_4e5d93, context.plotLabel(state.plot), if(state.plot.temporal)context.getString(R.string.ui_time) else context.getString(R.string.ui_frequency), axisLabel(camera.x.low,camera.x.span), axisLabel(camera.x.high,camera.x.span), if(state.plot==AcousticPlot.PITCH)context.getString(R.string.ui_hz_axis_and_base_note)+pitchTicks.joinToString {"${axisLabel(it.hz,camera.y.span)} Hz ${if(it.approximate)context.getString(R.string.ui_approximately) else ""} ${it.note.orEmpty()}"} else "", if(immersive)context.getString(R.string.ui_drag_and_zoom_along_both_axes_hold_to_inspect) else context.getString(R.string.ui_tap_to_open_viewer))}
        .semantics {this[PlotDataArea]=androidx.compose.ui.geometry.Rect(left,top,dimensions.width-right,dimensions.height-bottom);this[PlotWindow]=camera
            this[PlotSwipeCount]=if(state.plot==AcousticPlot.PITCH)swipe?.samples?.count {it.hz!=null} ?: 0 else 0
            this[PlotPlaybackMs]=cursor
            this[PlotXLabels]=xLabels.map {it.text};this[PlotXLabelBounds]=xLabels.map {androidx.compose.ui.geometry.Rect(left+it.center-it.width/2,dimensions.height-bottom,left+it.center+it.width/2,dimensions.height.toFloat())}}
        .onSizeChanged {dimensions=it}) {
        // Navigation/configuration transitions can measure a temporarily collapsed surface.
        if(size.width<=left+right+textSize || size.height<=top+bottom+textSize*2)return@Canvas
        val w=(size.width-left-right).coerceAtLeast(1f);val h=(size.height-top-bottom).coerceAtLeast(1f)
        fun x(value:Double)=(camera.x.fraction(value)*w).toFloat()
        fun y(value:Double)=((1-camera.y.fraction(value,log))*h).toFloat()
        val xt=xTicks
        val yt=yTicks
        translate(left,top){
            clipRect(0f,0f,w,h){
                if(state.plot==AcousticPlot.SPECTROGRAM && bitmap!=null)visual.spectrogram?.let {tile ->
                    // Crop using the coordinates of the completed tile, never stretch old data to a new request.
                    val a=tile.start*1000.0/tile.rate;val b=tile.end*1000.0/tile.rate
                    if(tile.logarithmic==log){val x0=x(a);val x1=x(b);val y0=y(tile.highHz);val y1=y(tile.lowHz)
                        if(x1>x0 && y1>y0)withTransform({translate(x0,y0);scale((x1-x0)/bitmap.width,(y1-y0)/bitmap.height,pivot=Offset.Zero)}){
                            drawImage(bitmap,dstSize=IntSize(bitmap.width,bitmap.height),filterQuality=FilterQuality.None)
                        }}
                }
                xt.forEach {drawLine(grid,Offset(x(it),0f),Offset(x(it),h),.7f)}
                yt.forEach {drawLine(grid,Offset(0f,y(it)),Offset(w,y(it)),.7f)}
                if(state.plot==AcousticPlot.PITCH){
                    analysis?.summary?.typicalHz?.let {hz->if(hz in camera.y.low..camera.y.high)drawLine(Color(0xFF93CDE7).copy(alpha=.5f),Offset(0f,y(hz)),Offset(w,y(hz)),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,6f)))}
                    for(sample in pitchTrace){val p=sample.point;val hz=p.hz!!
                        if(sample.uncertain){if(state.showUncertain)drawCircle(Color(0xFFEBA16E),3f,Offset(x(p.timeMs.toDouble()),y(hz)),style=Stroke(1.5f));continue}
                        val prior=sample.previous?.let {pitchTrace[it].point}
                        val color=pitchColor(sample.tone)
                        if(prior?.hz!=null && p.timeMs-prior.timeMs<=1200)drawLine(color,Offset(x(prior.timeMs.toDouble()),y(prior.hz)),Offset(x(p.timeMs.toDouble()),y(hz)),2.3f,
                            pathEffect=if(sample.bridged)PathEffect.dashPathEffect(floatArrayOf(5f,5f)) else null)
                        else drawCircle(color,2.5f,Offset(x(p.timeMs.toDouble()),y(hz)))
                    }
                }
                if(state.plot==AcousticPlot.PITCH && swipe!=null){
                    drawPath(swipePaths.first,swipeColor,style=Stroke(2.3f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,5f))))
                    swipePaths.second.forEach {drawCircle(swipeColor,2.8f,it)}
                }
                if(state.plot==AcousticPlot.WAVE)visual.wave?.let {wave ->
                    val a=wave.start*1000.0/wave.rate;val b=wave.end*1000.0/wave.rate
                    if(wave.samples){val path=Path();wave.min.forEachIndexed {i,value ->val px=x(a+i*1000.0/wave.rate);val py=y(value.toDouble());if(i==0)path.moveTo(px,py)else path.lineTo(px,py)};drawPath(path,traceA,style=Stroke(1.8f))}
                    else wave.min.indices.forEach {i ->val px=x(a+(i+.5)/wave.min.size*(b-a));drawLine(traceA,Offset(px,y(wave.max[i].toDouble())),Offset(px,y(wave.min[i].toDouble())),max(1f,(w*(b-a)/camera.x.span/wave.min.size).toFloat()))}
                }
                if(state.plot==AcousticPlot.SPECTRUM && analysis!=null && spectrumReference>1e-16){
                    analysis.harmonics.fundamentalHz?.let {f0->for(order in 1..min(128,(camera.x.high/f0).toInt())){val hz=f0*order;if(hz>=camera.x.low)drawLine(traceA.copy(alpha=.2f),Offset(x(hz),0f),Offset(x(hz),h),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(3f,5f)))}}
                    val path=Path();curve.forEachIndexed {i,p ->val py=y(10*log10(max(1e-30,p.second)/spectrumReference));if(i==0)path.moveTo(x(p.first),py)else path.lineTo(x(p.first),py)};drawPath(path,traceA,style=Stroke(2f))
                    var labelRight=-1000f
                    analysis.harmonics.peaks.filter {it.hz in camera.x.low..camera.x.high}.forEach {peak ->val px=x(peak.hz);val py=y(peak.relativeDb);drawCircle(if(peak.order!=null)traceA else traceB,3.5f,Offset(px,py))
                        if(peak.order!=null && px>labelRight && py in 0f..h){val label="H${peak.order}?";drawContext.canvas.nativeCanvas.drawText(label,px+4,py.coerceAtLeast(textSize+2)-3,paint);labelRight=px+paint.measureText(label)+12}
                    }
                }
                if(state.plot.temporal){
                    state.region?.let {region ->val a=x(region.first.toDouble());val b=x(region.last.toDouble());drawRect(Color(0xFF8CCFF3).copy(alpha=.13f),Offset(a,0f),Size(max(0f,b-a),h))}
                    if(state.selectedMs.toDouble() in camera.x.low..camera.x.high)drawLine(Color.White.copy(alpha=.7f),Offset(x(state.selectedMs.toDouble()),0f),Offset(x(state.selectedMs.toDouble()),h),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(3f,5f)))
                    if(cursor>=0 && cursor.toDouble() in camera.x.low..camera.x.high)drawLine(Color(0xFF9AD9FF),Offset(x(cursor.toDouble()),0f),Offset(x(cursor.toDouble()),h),2.5f)
                }
            }
            yt.forEach {tick ->
                paint.textAlign=android.graphics.Paint.Align.RIGHT
                val label=axisLabel(tick,camera.y.span)
                val musical=pitchTicks.firstOrNull {it.hz==tick}
                val baseline=y(tick).coerceIn(textSize,if(musical?.note!=null)max(textSize,h-textSize*1.15f) else h-textSize/4)
                drawContext.canvas.nativeCanvas.drawText(label,-labelGap,baseline,paint)
                musical?.note?.let {note->
                    paint.textSize=textSize*.85f;paint.alpha=195
                    drawContext.canvas.nativeCanvas.drawText((if(musical.approximate)"≈ " else "")+note,-labelGap,baseline+textSize,paint)
                    paint.textSize=textSize;paint.alpha=255
                }
            }
            xLabels.forEach {label ->paint.textAlign=android.graphics.Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(label.text,label.center,h+textSize*1.5f,paint)}
        }
        paint.textAlign=android.graphics.Paint.Align.LEFT
        // Hz is already identified by the detail legend / fullscreen range readout.
        if(state.plot==AcousticPlot.WAVE || state.plot==AcousticPlot.SPECTRUM)
            drawContext.canvas.nativeCanvas.drawText(if(state.plot==AcousticPlot.WAVE)context.getString(R.string.ui_amplitude) else "dB rel",6f,textSize,paint)
    }
}

