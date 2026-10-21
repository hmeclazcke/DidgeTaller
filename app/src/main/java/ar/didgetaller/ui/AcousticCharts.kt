package ar.didgetaller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import ar.didgetaller.audio.VisualState
import ar.didgetaller.core.*
import kotlin.math.*

internal fun seconds(ms:Long)="%.2f s".format(spanish,ms/1000.0)
internal val traceA=Color(0xFF84DDC1)
internal val traceB=Color(0xFFFFC582)
private val playColor=Color(0xFF9AD8FF)

/** Vertical drags remain available to the enclosing scroll view. Two fingers pinch; one pans horizontally. */
@Composable
internal fun Modifier.chartGestures(onTap:(Float,Float)->Unit,onTransform:(Float,Float,Float)->Unit):Modifier {
    val tap by rememberUpdatedState(onTap)
    val transform by rememberUpdatedState(onTransform)
    return pointerInput(Unit) {
    awaitEachGesture {
        val down=awaitFirstDown(requireUnconsumed=false)
        var moved=false;var horizontal=false;var previous=down.position
        while(true) {
            val event=awaitPointerEvent();val pressed=event.changes.filter {it.pressed}
            if(pressed.isEmpty()) {
                if(!moved && !down.isConsumed)tap((previous.x/size.width).coerceIn(0f,1f),(previous.y/size.height).coerceIn(0f,1f))
                break
            }
            val point=pressed.first().position;previous=point
            val delta=point-down.position
            if(!horizontal && pressed.size==1 && abs(delta.y)>viewConfiguration.touchSlop && abs(delta.y)>abs(delta.x))break
            if(pressed.size>1 || abs(delta.x)>viewConfiguration.touchSlop)horizontal=true
            if(horizontal) {
                moved=true
                val pan=event.calculatePan().x/size.width
                val zoom=event.calculateZoom().takeIf {it.isFinite() && it>0} ?: 1f
                transform((event.calculateCentroid(useCurrent=true).x/size.width).coerceIn(0f,1f),zoom,pan)
                event.changes.forEach {it.consume()}
            }
        }
    }
}
}

@Composable
internal fun TimeChart(kind:Int,visual:VisualState,points:List<SessionAnalysis.Point>,start:Long,end:Long,
                       lowHz:Double,highHz:Double,log:Boolean,cursor:Long,selected:Long,region:LongRange?,
                       onTap:(Long,Double)->Unit,onTransform:(Float,Float,Float)->Unit) {
    val colors=MaterialTheme.colorScheme
    val image=remember(visual.bitmap){visual.bitmap?.asImageBitmap()}
    val labelSize=with(LocalDensity.current){11.sp.toPx()}
    val labelPaint=remember(labelSize){android.graphics.Paint().apply {color=android.graphics.Color.LTGRAY;textSize=labelSize;isAntiAlias=true;setShadowLayer(3f,0f,1f,android.graphics.Color.BLACK)}}
    val span=(end-start).coerceAtLeast(1)
    val minFreq=if(log)max(lowHz,visual.rate/32768.0) else lowHz
    fun frequency(y:Float)=if(log)minFreq*(highHz/minFreq).pow(1-y.toDouble()) else lowHz+(highHz-lowHz)*(1-y)
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(if(kind==2)"Amplitud digital · −1 a +1" else "${minFreq.toInt()}–${highHz.toInt()} Hz · ${if(log)"logarítmica" else "lineal"}",style=MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF101B25)).testTag("time-chart")
            .semantics {contentDescription=when(kind){0->"Frecuencia fundamental en el tiempo";1->"Espectrograma real del WAV";else->"Forma de onda del WAV"}}
            .chartGestures({x,y ->onTap(start+(span*x).toLong(),frequency(y))},onTransform)) {
            val width=size.width;val height=size.height
            repeat(5){i ->drawLine(colors.outlineVariant,Offset(0f,i*height/4),Offset(width,i*height/4),0.6f)}
            fun x(ms:Long)=((ms-start).toDouble()/span*width).toFloat()
            fun y(hz:Double):Float {val f=if(log)ln(max(hz,minFreq)/minFreq)/ln(highHz/minFreq) else (hz-lowHz)/(highHz-lowHz);return ((1-f)*height).toFloat()}
            if(kind==1 && image!=null)drawImage(image,dstSize=IntSize(width.toInt(),height.toInt()),filterQuality=FilterQuality.None)
            if(kind==2)visual.wave?.let {wave ->
                val n=wave.min.size
                if(wave.samples){val path=Path();for(i in 0 until n){val px=i.toFloat()/max(1,n-1)*width;val py=height/2-wave.min[i]*height*0.5f;if(i==0)path.moveTo(px,py)else path.lineTo(px,py)};drawPath(path,traceA,style=Stroke(1.8f))}
                else for(i in 0 until n){val px=(i+0.5f)/n*width;drawLine(traceA,Offset(px,height/2-wave.max[i]*height*0.5f),Offset(px,height/2-wave.min[i]*height*0.5f),max(1f,width/n))}
            }
            if(kind==0){var previous:SessionAnalysis.Point?=null
                for(point in points){val hz=point.hz
                    if(point.timeMs !in start..end || hz==null || hz !in lowHz..highHz){previous=null;continue}
                    val last=previous
                    val cents=last?.hz?.let {abs(1200*ln(hz/it)/ln(2.0))} ?: 0.0
                    val color=when {point.confidence<0.8 || cents>60->Color(0xFFEFA171);point.confidence<0.9 || cents>25->Color(0xFFE2CE7D);else->traceA}
                    if(last?.hz!=null && point.timeMs-last.timeMs<1500)drawLine(color,Offset(x(last.timeMs),y(last.hz)),Offset(x(point.timeMs),y(hz)),2.5f)
                    else drawCircle(color,2.5f,Offset(x(point.timeMs),y(hz)))
                    previous=point
                }
            }
            region?.let {val left=x(it.first).coerceIn(0f,width);val right=x(it.last).coerceIn(0f,width)
                drawRect(playColor.copy(alpha=.13f),Offset(left,0f),Size(max(0f,right-left),height))}
            if(selected in start..end)drawLine(Color.White.copy(alpha=.65f),Offset(x(selected),0f),Offset(x(selected),height),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(4f,4f)))
            if(cursor in start..end)drawLine(playColor,Offset(x(cursor),0f),Offset(x(cursor),height),2.5f)
            repeat(5){i ->val label=if(kind==2)"%.1f".format(1-i*.5) else "%.0f Hz".format(frequency(i/4f))
                drawContext.canvas.nativeCanvas.drawText(label,6f,(i*height/4+labelSize).coerceAtMost(height-3),labelPaint)}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(seconds(start),style=MaterialTheme.typography.labelSmall);Text(seconds(end),style=MaterialTheme.typography.labelSmall)}
        Text(when(kind){0->"Verde: periodicidad alta y poca variación · amarillo: variación media · naranja: mayor incertidumbre · hueco: sin dato";1->"Violeta → azul → verde → amarillo: energía creciente. Azul claro vertical: reproducción.";else->"Envolvente mínimo/máximo; al ampliar se muestran las muestras reales. Azul claro: reproducción."},style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SpectrumChart(a:WavAnalysis.Result,b:WavAnalysis.Result?=null,showA:Boolean=true,showB:Boolean=true,normalize:Boolean=true) {
    var from by remember(a,b){mutableDoubleStateOf(0.0)}
    var to by remember(a,b){mutableDoubleStateOf(min(2000.0,a.sampleRate/2.0))}
    var selected by remember(a,b){mutableStateOf<HarmonicAnalysis.Peak?>(null)}
    LaunchedEffect(normalize,showA,showB){selected=null}
    val referenceA=a.spectrum.density.maxOrNull() ?: 0.0
    val referenceB=b?.spectrum?.density?.maxOrNull() ?: 0.0
    val common=max(referenceA,referenceB)
    val maxHz=min(a.sampleRate,b?.sampleRate ?: a.sampleRate)/2.0
    val labelSize=with(LocalDensity.current){11.sp.toPx()}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("${from.toInt()}–${to.toInt()} Hz · 0 a −80 dB rel",style=MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF101B25)).testTag("spectrum-chart")
            .semantics {contentDescription="Espectro de potencia; candidatos armónicos y picos medidos"}
            .chartGestures({x,_ ->val hz=from+x*(to-from)
                fun adjusted(r:WavAnalysis.Result,reference:Double)=r.harmonics.peaks.map {if(normalize || reference<=0 || common<=0)it else it.copy(relativeDb=it.relativeDb+10*log10(reference/common))}
                selected=(if(showA)adjusted(a,referenceA) else emptyList()).plus(if(showB && b!=null)adjusted(b,referenceB) else emptyList()).filter {it.hz in from..to}.minByOrNull {abs(it.hz-hz)}?.takeIf {abs(it.hz-hz)<(to-from)*.06}},
                {center,zoom,pan ->val old=to-from;val span=(old/zoom).coerceIn(10.0,maxHz);val left=(from+old*center-span*center-pan*old).coerceIn(0.0,maxHz-span);from=left;to=left+span})) {
            val paint=android.graphics.Paint().apply {color=android.graphics.Color.LTGRAY;textSize=labelSize;isAntiAlias=true;setShadowLayer(3f,0f,1f,android.graphics.Color.BLACK)}
            repeat(5){i ->val y=size.height*i/4;drawLine(Color.Gray.copy(alpha=.25f),Offset(0f,y),Offset(size.width,y),.6f)}
            fun x(hz:Double)=((hz-from)/(to-from)*size.width).toFloat()
            fun trace(result:WavAnalysis.Result,color:Color,reference:Double,dashed:Boolean) {
                if(reference<=1e-16)return
                val s=result.spectrum;val bin=s.sampleRate.toDouble()/s.fftSize
                // One maximum per display column preserves narrow peaks when zoomed out.
                val columns=size.width.toInt().coerceAtLeast(1);val maxima=DoubleArray(columns){-80.0}
                val first=ceil(from/bin).toInt().coerceAtLeast(0);val last=floor(to/bin).toInt().coerceAtMost(s.density.lastIndex)
                for(i in first..last){val col=x(i*bin).toInt().coerceIn(0,columns-1);maxima[col]=max(maxima[col],10*log10(max(1e-30,s.density[i])/reference))}
                val path=Path()
                if(last-first+1<columns){for(i in first..last){val y=(-((10*log10(max(1e-30,s.density[i])/reference)).coerceIn(-80.0,0.0))/80*size.height).toFloat();if(i==first)path.moveTo(x(i*bin),y)else path.lineTo(x(i*bin),y)}}
                else for(i in maxima.indices){val y=(-maxima[i].coerceIn(-80.0,0.0)/80*size.height).toFloat();if(i==0)path.moveTo(i.toFloat(),y)else path.lineTo(i.toFloat(),y)}
                drawPath(path,color,style=Stroke(2f,pathEffect=if(dashed)PathEffect.dashPathEffect(floatArrayOf(9f,5f)) else null))
                result.harmonics.peaks.filter {it.hz in from..to}.forEach {peak ->
                    val own=if(result===a)referenceA else referenceB
                    val db=peak.relativeDb+10*log10(own/reference)
                    val y=(-db.coerceIn(-80.0,0.0)/80*size.height).toFloat();drawCircle(color,3.5f,Offset(x(peak.hz),y))
                    peak.order?.let {drawContext.canvas.nativeCanvas.drawText("H$it?",x(peak.hz)+3,y.coerceAtLeast(25f)-3,paint)}
                }
            }
            if(showA){a.harmonics.fundamentalHz?.let {f0 ->for(h in 1..min(64,(to/f0).toInt())){val hz=f0*h;if(hz>=from)drawLine(traceA.copy(alpha=.2f),Offset(x(hz),0f),Offset(x(hz),size.height),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(3f,5f)))}};trace(a,traceA,if(normalize)referenceA else common,false)}
            if(showB && b!=null)trace(b,traceB,if(normalize)referenceB else common,true)
            repeat(5){i ->drawContext.canvas.nativeCanvas.drawText("−${i*20} dB",5f,(i*size.height/4+labelSize).coerceAtMost(size.height-3),paint)}
            repeat(4){i ->val hz=from+(to-from)*i/3;val px=i*size.width/3
                drawContext.canvas.nativeCanvas.drawText("${hz.toInt()} Hz",px.coerceAtMost(size.width-labelSize*4),size.height-5,paint)}
        }
        Text("Pellizcá para ampliar · arrastrá horizontalmente · tocá un pico",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick={val span=max(10.0,(to-from)/2);val center=selected?.hz ?: (to+from)/2;from=(center-span/2).coerceIn(0.0,maxHz-span);to=from+span}){Text("Ampliar espectro")}
            androidx.compose.material3.TextButton(onClick={from=0.0;to=maxHz}){Text("Ver todo")}
        }
        selected?.let {p ->Text("${hzLabel(p.hz)} · %.1f dB rel · ".format(p.relativeDb)+(p.order?.let {"H$it candidato"} ?: "pico sin asignar")+(p.deviationCents?.let {" · %+.1f cents".format(it)} ?: ""),style=MaterialTheme.typography.bodyMedium,modifier=Modifier.testTag("selected-peak"))}
        Text(if(b==null)"Línea: energía medida · guías tenues: múltiplos teóricos · H?: candidatos, no confirmaciones." else "A: verde continua · B: naranja discontinua. ${if(normalize)"Cada espectro se refiere a su propio máximo." else "Referencia digital común al mayor pico de A/B; sin calibración SPL."}",style=MaterialTheme.typography.bodySmall)
    }
}
