package ar.didgetaller.core

import kotlin.math.*

internal object WoodInk {
    val WOOD=0xffddb184.toInt();val GRAIN=0xffbb8759.toInt();val EDGE=0xff886142.toInt()
    val AIR=0xffffffff.toInt();val TEAL=0xff38675e.toInt();val RED=0xffa03d2b.toInt();val LIGHT=0xffddd7d0.toInt()
}

/** Accurate orthographic vectors; grain is bounded inside the wood and has no dimensional meaning. */
internal object WoodDrawing {
    fun polygon(c:WorkshopPage,points:List<Pair<Double,Double>>,fill:Int,edge:Int=WoodInk.EDGE,removed:Boolean=false) {
        c.marks+=WorkshopMark.Path(points,closed=true,shaded=removed,fillColor=fill,strokeColor=edge,strokeWidth=.7)
    }
    fun measure(c:WorkshopPage,kind:String,value:Double,scale:Double,a:Pair<Double,Double>,b:Pair<Double,Double>,label:String,tx:Double,ty:Double) {
        c.dimensions+=WorkshopMeasure(kind,value,scale,a,b)
        c.line(a.first,a.second,b.first,b.second)
        arrow(c,a,b);arrow(c,b,a)
        c.text(tx,ty,label,9.0,bold=true)
    }
    private fun arrow(c:WorkshopPage,a:Pair<Double,Double>,b:Pair<Double,Double>) {
        val angle=atan2(b.second-a.second,b.first-a.first);val length=minOf(3.5,hypot(b.first-a.first,b.second-a.second)/3)
        val points=listOf(a,a.first+length*cos(angle+.4) to a.second+length*sin(angle+.4),a.first+length*cos(angle-.4) to a.second+length*sin(angle-.4))
        polygon(c,points,0xff5f4b3d.toInt(),0xff5f4b3d.toInt())
    }
    fun background(c:WorkshopPage) {polygon(c,listOf(0.0 to 0.0,c.width to 0.0,c.width to c.height,0.0 to c.height),WoodInk.AIR,WoodInk.AIR)}
    fun positions(p:ConstructionPlan):List<Double> {
        val stock=p.options.woodStock
        val xs=ConstructionDrawing.rows(p).map {it.xMm}.toMutableSet()
        if(stock is WoodStock.Log)xs+=stock.sections.map {it.positionMm-stock.preparation.endTrimMm}.filter {it in 0.0..p.lengthMm}
        return xs.sorted()
    }
    fun section(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,x:Double,cx:Double,cy:Double) {
        val capacity=p.options.woodStock?.let {WoodFit.capacity(it,x)}
        val radius=ConstructionDrawing.diameter(p,x,false)/2
        c.text(cx-70,cy-94,l.dimension(x)+" mm",11.0,bold=true)
        val outlineKnown=capacity!=null && capacity.halfDepthMm>0 && capacity.halfWidthMm>0
        val width=if(outlineKnown)capacity!!.halfWidthMm else radius
        val depth=if(outlineKnown)capacity!!.halfDepthMm else radius
        val scale=ConstructionDrawing.POINTS_PER_MM/maxOf(1.0,ceil(maxOf(width*2*ConstructionDrawing.POINTS_PER_MM/145,depth*ConstructionDrawing.POINTS_PER_MM/55)))
        if(outlineKnown)stockSection(c,capacity!!,cx,cy,scale)
        val hollow=(0..96).map {i->val angle=i*2*PI/96;cx+radius*scale*cos(angle) to cy+radius*scale*sin(angle)}
        polygon(c,hollow,WoodInk.AIR,WoodInk.TEAL,true)
        c.line(cx-width*scale-5,cy,cx+width*scale+5,cy,dashed=true)
        boreMeasures(c,l,radius,width,depth,scale,cx,cy)
        if(outlineKnown)stockMeasures(c,p,l,x,capacity!!,scale,cx,cy)
        else c.text(cx-70,cy+depth*scale+31,l.wood(WoodTerm.NONE),9.0)
        c.text(cx-70,cy+depth*scale+64,l.word(WorkshopTerm.SECTION_SCALE)+" 1:"+l.dimension(ConstructionDrawing.POINTS_PER_MM/scale),8.0)
    }
    private fun stockSection(c:WorkshopPage,k:WoodCapacity,cx:Double,cy:Double,s:Double) {
        val outline=if(k.logRadiusMm==null)listOf(cx-k.halfWidthMm*s to cy-k.halfDepthMm*s,cx+k.halfWidthMm*s to cy-k.halfDepthMm*s,
            cx+k.halfWidthMm*s to cy+k.halfDepthMm*s,cx-k.halfWidthMm*s to cy+k.halfDepthMm*s)
        else sectionOutline(k,cx,cy,s).let {lower->lower+lower.asReversed().map {(x,y)->x to 2*cy-y}}
        polygon(c,outline,WoodInk.WOOD)
    }
    private fun sectionOutline(k:WoodCapacity,cx:Double,cy:Double,s:Double):List<Pair<Double,Double>> {
        val radius=k.logRadiusMm
        if(radius==null)return listOf(cx-k.halfWidthMm*s to cy,cx+k.halfWidthMm*s to cy,cx+k.halfWidthMm*s to cy+k.halfDepthMm*s,cx-k.halfWidthMm*s to cy+k.halfDepthMm*s)
        val angle=asin((k.joinOffsetMm/radius).coerceIn(0.0,1.0))
        return (0..64).map {i->val a=angle+(PI-2*angle)*i/64;cx+radius*cos(a)*s to cy+(radius*sin(a)-k.joinOffsetMm)*s}
    }
    private fun boreMeasures(c:WorkshopPage,l:WorkshopLanguage,r:Double,w:Double,h:Double,s:Double,cx:Double,cy:Double) {
        val y=cy-h*s-12
        c.line(cx-r*s,cy,cx-r*s,y);c.line(cx+r*s,cy,cx+r*s,y)
        measure(c,"bore-diameter",2*r,s,cx-r*s to y,cx+r*s to y,"Ø "+l.dimension(2*r)+" mm",cx-25,y-9)
        c.line(cx,cy+r*s,cx+w*s+11,cy+r*s);c.line(cx+w*s,cy,cx+w*s+11,cy)
        measure(c,"groove-depth",r,s,cx+w*s+10 to cy,cx+w*s+10 to cy+r*s,l.dimension(r)+" mm",cx+w*s+16,cy+25)
        c.text(cx+w*s+16,cy+12,l.wood(WoodTerm.DEPTH_SHORT),8.0)
    }
    private fun stockMeasures(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,x:Double,k:WoodCapacity,s:Double,cx:Double,cy:Double) {
        val w=k.halfWidthMm*s;val h=k.halfDepthMm*s
        val row=CarvingMeasurements.row(p,x)
        row.remainingMm?.takeIf {it>=0}?.let {remaining->
            val y=cy+row.depthMm*s
            c.line(cx,y,cx-w-10,y);c.line(cx-w,cy+h,cx-w-10,cy+h)
            measure(c,"base-wall",remaining,s,cx-w-10 to y,cx-w-10 to cy+h,l.dimension(remaining)+" mm",cx-w-48,cy+h+5)
            c.text(cx-w-48,cy+h-7,l.wood(WoodTerm.SHORT_WALL),8.0)
        }
        measure(c,"stock-width",k.halfWidthMm*2,s,cx-w to cy+h+18,cx+w to cy+h+18,
            l.wood(if(k.logRadiusMm==null)WoodTerm.WIDTH else WoodTerm.JOIN_WIDTH).substringBefore(" (")+" "+l.dimension(k.halfWidthMm*2)+" mm",cx-70,cy+h+32)
        c.text(cx-70,cy+h+48,l.wood(if(k.logRadiusMm==null)WoodTerm.HALF_T else WoodTerm.LOG_DEPTH)+" "+l.dimension(k.halfDepthMm)+" mm",8.5)
    }
}
