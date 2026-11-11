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
    fun capacity(p:ConstructionPlan,x:Double):WoodCapacity? {
        val stock=p.options.woodStock
        if(stock!=null)return WoodFit.capacity(stock,x)
        val depth=p.pieces.maxOf {maxOf(it.inletMm,it.outletMm)}/2+p.options.wallMm+p.options.radialToleranceMm
        return WoodCapacity(depth,depth)
    }
    fun half(c:WorkshopPage,p:ConstructionPlan,x0:Double,y0:Double,scale:Double,sign:Int) {
        val points=positions(p).mapNotNull {x->capacity(p,x)?.let {x to maxOf(0.0,it.halfDepthMm)}}
        if(points.size<2)return
        val outside=listOf(x0+points.first().first*scale to y0)+points.map {(x,d)->x0+x*scale to y0+sign*d*scale}+listOf(x0+points.last().first*scale to y0)
        polygon(c,outside,if(p.options.woodStock==null)0xfff1efec.toInt() else WoodInk.WOOD)
        grainHalf(c,points,x0,y0,scale,sign)
        val inner=ConstructionDrawing.outline(p,1).map {(x,r)->x0+x*scale to y0+sign*r*scale}
        polygon(c,listOf(x0 to y0)+inner+listOf(x0+p.lengthMm*scale to y0),WoodInk.AIR,WoodInk.TEAL,true)
        c.line(x0-6,y0,x0+p.lengthMm*scale+6,y0,dashed=true)
        p.ideal.controlPoints().forEach {point->
            val px=x0+point.positionMm*scale
            c.line(px,y0-3,px,y0+3)
        }
    }
    private fun grainHalf(c:WorkshopPage,points:List<Pair<Double,Double>>,x0:Double,y0:Double,scale:Double,sign:Int) {
        for(i in 1..6) {
            val f=i/7.0
            c.marks+=WorkshopMark.Path(points.map {(x,d)->x0+x*scale to y0+sign*d*scale*(f+.025*sin(x*.012+i))},strokeColor=WoodInk.GRAIN,strokeWidth=.35)
        }
    }
    fun section(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,x:Double,cx:Double,cy:Double) {
        val capacity=capacity(p,x)
        c.text(cx-70,cy-73,"${WoodPresentation.number(x)} mm",11.0,bold=true)
        if(capacity==null || capacity.halfDepthMm<=0 || capacity.halfWidthMm<=0) {
            c.paragraph(cx-90,cy,l.wood(WoodTerm.PREP_UNKNOWN),180.0,9.0);return
        }
        val radius=ConstructionDrawing.diameter(p,x,false)/2
        val scale=ConstructionDrawing.POINTS_PER_MM/maxOf(1.0,ceil(maxOf(capacity.halfWidthMm*2*ConstructionDrawing.POINTS_PER_MM/145,capacity.halfDepthMm*ConstructionDrawing.POINTS_PER_MM/55)))
        val w=capacity.halfWidthMm*scale;val h=capacity.halfDepthMm*scale
        polygon(c,sectionOutline(capacity,cx,cy,scale),if(p.options.woodStock==null)0xffefede9.toInt() else WoodInk.WOOD)
        val hollow=(0..48).map {i->val angle=i*PI/48;cx+radius*scale*cos(angle) to cy+radius*scale*sin(angle)}
        polygon(c,hollow,WoodInk.AIR,WoodInk.TEAL,true)
        c.line(cx-w-4,cy,cx+w+4,cy,dashed=true)
        c.text(cx-70,cy+h+49,l.wood(WoodTerm.HALF_T)+" "+WoodPresentation.number(capacity.halfDepthMm)+" mm",8.5)
        c.text(cx-70,cy+h+63,l.word(WorkshopTerm.SECTION_SCALE)+" 1:"+WoodPresentation.number(ConstructionDrawing.POINTS_PER_MM/scale),8.0)
        sectionMeasures(c,l,capacity,radius,scale,cx,cy)
        measure(c,"stock-width",capacity.halfWidthMm*2,scale,cx-w to cy+h+17,cx+w to cy+h+17,
            l.wood(WoodTerm.HALF_WIDTH)+" "+WoodPresentation.number(capacity.halfWidthMm*2)+" mm",cx-65,cy+h+31)
    }
    private fun sectionOutline(k:WoodCapacity,cx:Double,cy:Double,s:Double):List<Pair<Double,Double>> {
        val radius=k.logRadiusMm
        if(radius==null)return listOf(cx-k.halfWidthMm*s to cy,cx+k.halfWidthMm*s to cy,cx+k.halfWidthMm*s to cy+k.halfDepthMm*s,cx-k.halfWidthMm*s to cy+k.halfDepthMm*s)
        val angle=asin((k.joinOffsetMm/radius).coerceIn(0.0,1.0))
        return (0..64).map {i->val a=angle+(PI-2*angle)*i/64;cx+radius*cos(a)*s to cy+(radius*sin(a)-k.joinOffsetMm)*s}
    }
    private fun sectionMeasures(c:WorkshopPage,l:WorkshopLanguage,k:WoodCapacity,r:Double,s:Double,cx:Double,cy:Double) {
        val w=k.halfWidthMm*s
        c.line(cx-r*s,cy,cx-r*s,cy-29);c.line(cx+r*s,cy,cx+r*s,cy-29)
        measure(c,"bore-diameter",2*r,s,cx-r*s to cy-29,cx+r*s to cy-29,"Ø "+WoodPresentation.number(2*r)+" mm",cx-25,cy-39)
        c.line(cx,cy+r*s,cx+w+11,cy+r*s);c.line(cx+w,cy,cx+w+11,cy)
        measure(c,"groove-depth",r,s,cx+w+10 to cy,cx+w+10 to cy+r*s,WoodPresentation.number(r)+" mm",cx+w+16,cy+25)
        c.text(cx+w+16,cy+12,l.wood(WoodTerm.GROOVE),8.0)
        val remaining=k.halfDepthMm-r
        measure(c,"base-wall",remaining,s,cx-w-10 to cy+r*s,cx-w-10 to cy+k.halfDepthMm*s,
            WoodPresentation.number(remaining)+" mm",cx-w-47,cy+k.halfDepthMm*s+6)
        c.text(cx-w-47,cy+k.halfDepthMm*s-6,l.wood(WoodTerm.SHORT_WALL),8.0)
        c.text(cx-70,cy-56,l.wood(WoodTerm.BORE),8.5)
    }
}
