package ar.didgetaller.core

import kotlin.math.*

/** Display coordinates only. Never modifies measured samples or acoustic calculations. */
data class AxisWindow(val low:Double,val high:Double) {
    init {require(low.isFinite() && high.isFinite() && high>low)}
    val span get()=high-low
    fun value(fraction:Double,log:Boolean=false)=if(log)exp(ln(low)+(ln(high)-ln(low))*fraction) else low+span*fraction
    fun fraction(value:Double,log:Boolean=false)=if(log)(ln(value)-ln(low))/(ln(high)-ln(low)) else (value-low)/span
}

data class AxisDomain(val low:Double,val high:Double,val minimumSpan:Double,val logarithmic:Boolean=false) {
    init {require(low.isFinite() && high>low && minimumSpan>0 && (!logarithmic || low>0))}
    private fun space(value:Double)=if(logarithmic)ln(value.coerceAtLeast(low)) else value
    private fun raw(value:Double)=if(logarithmic)exp(value) else value
    fun constrain(window:AxisWindow):AxisWindow {
        val a=space(low);val b=space(high)
        val span=(space(window.high)-space(window.low)).coerceIn(minimumSpan.coerceAtMost(b-a),b-a)
        val start=space(window.low).coerceIn(a,b-span)
        return AxisWindow(raw(start),raw(start+span))
    }
    fun transform(window:AxisWindow,anchor:Double,zoom:Double,pan:Double):AxisWindow {
        require(anchor.isFinite() && zoom.isFinite() && zoom>0 && pan.isFinite())
        val current=constrain(window);val a=space(current.low);val width=space(current.high)-a
        val span=(width/zoom).coerceIn(minimumSpan.coerceAtMost(space(high)-space(low)),space(high)-space(low))
        val start=(a+width*anchor-span*anchor-pan*span).coerceIn(space(low),max(space(low),space(high)-span))
        return AxisWindow(raw(start),raw(start+span))
    }
}

data class PlotViewport(val x:AxisWindow,val y:AxisWindow) {
    fun transform(xDomain:AxisDomain,yDomain:AxisDomain,anchorX:Double,anchorYFromTop:Double,
                  zoomX:Double,zoomY:Double,panX:Double,panY:Double)=PlotViewport(
        xDomain.transform(x,anchorX,zoomX,panX),yDomain.transform(y,1-anchorYFromTop,zoomY,-panY))
}

data class PlotPoint(val x:Double,val y:Double)
/** Coordinates relative to the measured plot rectangle, including its axis margins. */
data class PlotProjection(val viewport:PlotViewport,val width:Double,val height:Double,val left:Double=0.0,val top:Double=0.0,val logY:Boolean=false){
    init{require(width>0 && height>0)}
    fun data(px:Double,py:Double)=PlotPoint(viewport.x.value(((px-left)/width).coerceIn(0.0,1.0)),viewport.y.value(1-((py-top)/height).coerceIn(0.0,1.0),logY))
    fun pixels(x:Double,y:Double)=PlotPoint(left+width*viewport.x.fraction(x),top+height*(1-viewport.y.fraction(y,logY)))
}

data class PitchViewRange(val window:AxisWindow,val clipped:Int,val valid:Int)
object PlotScales {
    fun pitch(points:List<SessionAnalysis.Point>,includeAll:Boolean=false):PitchViewRange {
        val values=points.filter {it.usable}.mapNotNull {it.hz?.takeIf {hz->hz.isFinite() && hz>0}}.sorted()
        if(values.isEmpty())return PitchViewRange(AxisWindow(40.0,120.0),0,0)
        val low=if(!includeAll && values.size>=20)values[((values.size-1)*.025).toInt()] else values.first()
        val high=if(!includeAll && values.size>=20)values[ceil((values.size-1)*.975).toInt()] else values.last()
        val middle=values[values.size/2];val margin=max(max(.6,middle*.012),(high-low)*.18)
        val window=AxisWindow(max(0.0,low-margin),high+margin)
        return PitchViewRange(window,values.count {it !in window.low..window.high},values.size)
    }
    fun ticks(window:AxisWindow,maximum:Int=5,log:Boolean=false):List<Double> {
        if(log){val first=floor(log10(window.low)).toInt();val last=floor(log10(window.high)).toInt()
            val powers=(first..last).flatMap {e->listOf(1.0,2.0,5.0).map {it*10.0.pow(e)}}.filter {it in window.low..window.high}
            if(powers.size in 2..maximum)return powers
            return (0 until maximum).map {window.value(it.toDouble()/(maximum-1),true).coerceIn(window.low,window.high)}
        }
        val approximate=window.span/(maximum-1);val base=10.0.pow(floor(log10(approximate)))
        val factor=approximate/base;val step=base*when {factor<=1->1;factor<=2->2;factor<=5->5;else->10}
        val start=ceil(window.low/step)*step
        return (0..maximum).map {start+it*step}.filter {it<=window.high+step*1e-8}
    }
}
