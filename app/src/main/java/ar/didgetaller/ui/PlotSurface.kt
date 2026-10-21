package ar.didgetaller.ui

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

internal val spanish=Locale.forLanguageTag("es-AR")
internal fun hzLabel(hz:Double)=if(hz.isFinite() && hz>0)"%.1f Hz".format(spanish,hz) else "— Hz"
internal fun axisLabel(value:Double,span:Double)=when {span<.1->"%.4f";span<10->"%.2f";span<100->"%.1f";else->"%.0f"}.format(spanish,if(abs(value)<1e-12)0.0 else value)

/** All four render adapters share axes, clipping, coordinates and cursor overlay. */
@Composable
internal fun PlotSurface(state:ExplorerState,visual:VisualState,viewport:PlotViewport,points:List<SessionAnalysis.Point>,
    analysis:WavAnalysis.Result?,cursor:Long,modifier:Modifier=Modifier,immersive:Boolean=false,
    onPreview:()->Unit={},onPoint:(Double,Double,Boolean)->Unit={_,_,_->},onReset:()->Unit={},
    onTransform:(Double,Double,Double,Double,Double,Double)->Unit={_,_,_,_,_,_->}) {
    val density=LocalDensity.current
    val textSize=with(density){11.sp.toPx()}
    val left=with(density){(if(state.plot==AcousticPlot.SPECTRUM)58.dp else 56.dp).toPx()}.coerceAtLeast(textSize*3.2f)
    val right=with(density){12.dp.toPx()};val top=with(density){14.dp.toPx()};val bottom=textSize*2.2f
    var dimensions by remember {mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    val bitmap=remember(visual.bitmap){visual.bitmap?.asImageBitmap()}
    val log=state.plot==AcousticPlot.SPECTROGRAM && state.logarithmic
    val camera=viewport
    fun data(position:Offset):Pair<Double,Double>{
        val w=(dimensions.width-left-right).coerceAtLeast(1f);val h=(dimensions.height-top-bottom).coerceAtLeast(1f)
        val mapped=PlotProjection(camera,w.toDouble(),h.toDouble(),left.toDouble(),top.toDouble(),log).data(position.x.toDouble(),position.y.toDouble())
        return mapped.x to mapped.y
    }
    val point by rememberUpdatedState(onPoint);val transform by rememberUpdatedState(onTransform)
    val palette=Color(0xFF101D25);val grid=Color(0xFF29414B)
    val paint=remember(textSize){android.graphics.Paint().apply {isAntiAlias=true;color=android.graphics.Color.rgb(185,208,211);this.textSize=textSize}}
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
        .semantics {contentDescription="${state.plot.title}. ${if(state.plot.temporal)"Tiempo" else "Frecuencia"} ${axisLabel(camera.x.low,camera.x.span)} a ${axisLabel(camera.x.high,camera.x.span)}. ${if(immersive)"Arrastrar y ampliar en dos ejes. Mantener para inspeccionar." else "Tocar para abrir visor."}"}
        .onSizeChanged {dimensions=it}) {
        // Navigation/configuration transitions can measure a temporarily collapsed surface.
        if(size.width<=left+right+textSize || size.height<=top+bottom+textSize*2)return@Canvas
        val w=(size.width-left-right).coerceAtLeast(1f);val h=(size.height-top-bottom).coerceAtLeast(1f)
        fun x(value:Double)=(camera.x.fraction(value)*w).toFloat()
        fun y(value:Double)=((1-camera.y.fraction(value,log))*h).toFloat()
        val xt=PlotScales.ticks(camera.x,if(w/textSize<30)3 else 5)
        val yt=PlotScales.ticks(camera.y,if(h/textSize<12)3 else 5,log)
        translate(left,top){
            clipRect(0f,0f,w,h){
                if(state.plot==AcousticPlot.SPECTROGRAM && bitmap!=null)visual.spectrogram?.let {tile ->
                    // Crop using the coordinates of the completed tile, never stretch old data to a new request.
                    val a=tile.start*1000.0/tile.rate;val b=tile.end*1000.0/tile.rate
                    if(tile.logarithmic==log){val x0=x(a);val x1=x(b);val y0=y(tile.highHz);val y1=y(tile.lowHz)
                        if(x1>x0 && y1>y0)drawImage(bitmap,dstOffset=androidx.compose.ui.unit.IntOffset(x0.toInt(),y0.toInt()),dstSize=IntSize((x1-x0).toInt().coerceAtLeast(1),(y1-y0).toInt().coerceAtLeast(1)),filterQuality=FilterQuality.None)}
                }
                xt.forEach {drawLine(grid,Offset(x(it),0f),Offset(x(it),h),.7f)}
                yt.forEach {drawLine(grid,Offset(0f,y(it)),Offset(w,y(it)),.7f)}
                if(state.plot==AcousticPlot.PITCH){
                    analysis?.summary?.typicalHz?.let {hz->if(hz in camera.y.low..camera.y.high)drawLine(Color(0xFF93CDE7).copy(alpha=.5f),Offset(0f,y(hz)),Offset(w,y(hz)),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,6f)))}
                    var last:SessionAnalysis.Point?=null
                    for(p in points){val hz=p.hz
                        if(!p.usable || hz==null || p.timeMs.toDouble() !in camera.x.low..camera.x.high){last=null;continue}
                        val prior=last;val cents=prior?.hz?.let {abs(1200*ln(hz/it)/ln(2.0))} ?: 0.0
                        val color=when {p.confidence<.8 || cents>60->Color(0xFFEBA16E);p.confidence<.9 || cents>25->Color(0xFFE9D285);else->traceA}
                        if(prior?.hz!=null && p.timeMs-prior.timeMs<=1200)drawLine(color,Offset(x(prior.timeMs.toDouble()),y(prior.hz)),Offset(x(p.timeMs.toDouble()),y(hz)),2.3f)
                        else drawCircle(color,2.5f,Offset(x(p.timeMs.toDouble()),y(hz)))
                        last=p
                    }
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
            yt.forEach {tick ->val label=axisLabel(tick,camera.y.span);paint.textAlign=android.graphics.Paint.Align.RIGHT;drawContext.canvas.nativeCanvas.drawText(label,-7f,y(tick).coerceIn(textSize/2,h-textSize/4)+textSize*.3f,paint)}
            xt.forEach {tick ->val label=if(state.plot.temporal)seconds(tick.toLong()) else axisLabel(tick,camera.x.span)+" Hz";paint.textAlign=android.graphics.Paint.Align.CENTER
                val half=(paint.measureText(label)/2).coerceAtMost(w/2);drawContext.canvas.nativeCanvas.drawText(label,x(tick).coerceIn(half,w-half),h+textSize*1.5f,paint)}
        }
        paint.textAlign=android.graphics.Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText(when(state.plot){AcousticPlot.WAVE->"amplitud";AcousticPlot.SPECTRUM->"dB rel";else->"Hz"},6f,textSize,paint)
    }
}

