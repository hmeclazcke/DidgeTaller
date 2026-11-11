package ar.didgetaller.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/** Manufacturing geometry in points, with explicit independent longitudinal and section scales. */
internal object WorkshopDiagram {
    private fun n(x:Double)=WorkshopDocument.number(x)
    fun page(p:ConstructionPlan,l:WorkshopLanguage):WorkshopSheet {
        val c=WorkshopPage(842.0,595.0)
        c.text(34.0,32.0,l.word(WorkshopTerm.DRAWING),16.0)
        c.text(34.0,53.0,WorkshopDocument.humanName(p.name)+" - "+l.word(WorkshopDocument.method(p)),12.0)
        val scale=scale(p)
        c.text(34.0,74.0,l.word(WorkshopTerm.SCALE)+" 1:"+n(ConstructionDrawing.POINTS_PER_MM/scale),9.0)
        if(p.options.method==ConstructionMethod.SANDWICH)wood(c,p,l,scale) else assembled(c,p,l,scale)
        c.paragraph(34.0,505.0,l.word(WorkshopTerm.TOLERANCE),760.0,9.0)
        c.ruler(l);c.text(770.0,573.0,"2 / 2",8.0)
        return c.sheet()
    }
    private fun scale(p:ConstructionPlan):Double {
        val longitudinal=p.lengthMm*ConstructionDrawing.POINTS_PER_MM/750
        val thickness=if(p.options.method==ConstructionMethod.SANDWICH)
            WorkshopPlans.sandwich(p.ideal,p.options.intervalMm,p.options.wallMm,p.options.radialToleranceMm).minimumHalfThicknessMm else p.pieces.maxOf {maxOf(it.inletMm,it.outletMm)}/2
        return ConstructionDrawing.POINTS_PER_MM/kotlin.math.ceil(maxOf(longitudinal,thickness*ConstructionDrawing.POINTS_PER_MM/40))
    }
    private fun wood(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,s:Double) {
        val stock=WorkshopPlans.sandwich(p.ideal,p.options.intervalMm,p.options.wallMm,p.options.radialToleranceMm)
        half(c,p,s,142.0,1.0,stock.minimumHalfThicknessMm,l.word(WorkshopTerm.HALF_ONE))
        half(c,p,s,242.0,-1.0,stock.minimumHalfThicknessMm,l.word(WorkshopTerm.HALF_TWO))
        c.text(34.0,278.0,l.word(WorkshopTerm.PLANE)+" / "+l.word(WorkshopTerm.CENTER),9.0)
        stations(c,p,s,294.0)
        c.dimension(34.0,333.0,34+p.lengthMm*s,333.0,l.word(WorkshopTerm.LENGTH)+" "+n(p.lengthMm)+" mm")
        c.text(34.0,363.0,l.word(WorkshopTerm.SECTIONS)+" - "+l.word(WorkshopTerm.REMOVE),10.0)
        val xs=listOf(0.0,p.lengthMm/2,p.lengthMm)
        xs.forEachIndexed {i,x->section(c,p,l,x,165.0+i*255,426.0,stock)}
    }
    private fun half(c:WorkshopPage,p:ConstructionPlan,s:Double,y:Double,sign:Double,thickness:Double,label:String) {
        c.text(34.0,y-40,label,11.0)
        c.path(listOf(34.0 to y,34+p.lengthMm*s to y,34+p.lengthMm*s to y+sign*thickness*s,34.0 to y+sign*thickness*s),closed=true)
        val outline=ConstructionDrawing.outline(p,1).map {(x,r)->34+x*s to y+sign*r*s}
        c.path(listOf(34.0 to y)+outline+listOf(34+p.lengthMm*s to y),closed=true,shaded=true)
        c.line(27.0,y,42+p.lengthMm*s,y,dashed=true)
    }
    private fun stations(c:WorkshopPage,p:ConstructionPlan,s:Double,y:Double) {
        var lower=-100.0;var upper=-100.0
        ConstructionDrawing.rows(p).forEach {r->
            val x=34+r.xMm*s
            c.line(x,y-10,x,y)
            when {
                x-lower>=40->{c.text(x-4,y+12,n(r.xMm),8.0);lower=x}
                x-upper>=40->{c.text(x-4,y+24,n(r.xMm),8.0);upper=x}
            }
        }
    }
    private fun section(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,x:Double,cx:Double,cy:Double,stock:WorkshopPlans.SplitPlan) {
        val diameter=ConstructionDrawing.diameter(p,x,false);val r=diameter/2
        val scale=ConstructionDrawing.POINTS_PER_MM/kotlin.math.ceil(maxOf(stock.minimumBlankWidthMm*ConstructionDrawing.POINTS_PER_MM/150,stock.minimumHalfThicknessMm*ConstructionDrawing.POINTS_PER_MM/45))
        val w=stock.minimumBlankWidthMm/2*scale;val h=stock.minimumHalfThicknessMm*scale
        c.path(listOf(cx-w to cy,cx+w to cy,cx+w to cy+h,cx-w to cy+h),closed=true)
        val groove=(0..40).map {i->val a=i*PI/40;cx+r*scale*cos(a) to cy+r*scale*sin(a)}
        c.path(groove,closed=true,shaded=true)
        c.line(cx-w-4,cy,cx+w+4,cy,dashed=true)
        c.dimension(cx-r*scale,cy-8,cx+r*scale,cy-8,n(diameter)+" mm")
        c.dimension(cx+w+8,cy,cx+w+8,cy+r*scale,n(r)+" mm")
        c.text(cx-w,cy-28,l.word(WorkshopTerm.STATION)+" "+n(x)+" mm",8.5)
        c.text(cx-w,cy+h+14,l.word(WorkshopTerm.SECTION_SCALE)+" 1:"+n(ConstructionDrawing.POINTS_PER_MM/scale),8.0)
    }
    private fun assembled(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,s:Double) {
        val center=220.0
        for(sign in listOf(-1,1))c.path(ConstructionDrawing.outline(p,sign).map {(x,y)->34+x*s to center+y*s})
        c.line(34.0,center,34+p.lengthMm*s,center,dashed=true)
        c.text(34.0,115.0,l.word(WorkshopTerm.PROFILE),11.0)
        c.dimension(34.0,335.0,34+p.lengthMm*s,335.0,l.word(WorkshopTerm.LENGTH)+" "+n(p.lengthMm)+" mm")
        c.dimension(26.0,center-p.pieces.first().inletMm*s/2,26.0,center+p.pieces.first().inletMm*s/2,n(p.pieces.first().inletMm)+" mm")
        c.dimension(40+p.lengthMm*s,center-p.pieces.last().outletMm*s/2,40+p.lengthMm*s,center+p.pieces.last().outletMm*s/2,n(p.pieces.last().outletMm)+" mm")
        stations(c,p,s,275.0)
        segments(c,p,s,center)
        if(p.options.method==ConstructionMethod.PVC)sockets(c,p,s,l)
        val instruction=if(p.options.method==ConstructionMethod.PVC)WorkshopTerm.PVC_INSTRUCTION else WorkshopTerm.LAYER_INSTRUCTION
        c.paragraph(34.0,375.0,l.word(instruction),760.0,11.0)
    }
    private fun sockets(c:WorkshopPage,p:ConstructionPlan,s:Double,l:WorkshopLanguage) {
        val fittings=ConstructionCatalog.connectors(p.options.fittings)
        c.text(34.0,135.0,l.word(WorkshopTerm.SOCKET_VIEW),9.0)
        var end=0.0
        p.options.assembly.lineSequence().filter {it.isNotBlank()}.forEach {line->
            val row=line.split(',').map {it.trim()};end+=row[1].toDouble()
            fittings[row[2]]?.fitting?.let {f->
                val x=34+(end-f.leftInsertionMm)*s;val right=x+f.bodyLengthMm*s
                socket(c,x,f.leftSocketMm,f.leftInsertionMm,s)
                socket(c,right-f.rightInsertionMm*s,f.rightSocketMm,f.rightInsertionMm,s)
                c.text(x,345.0,"${f.id}: ${l.word(WorkshopTerm.INSERTIONS)} ${f.leftInsertionMm} / ${f.rightInsertionMm} mm",9.0)
                end+=f.bodyLengthMm-f.leftInsertionMm-f.rightInsertionMm
            }
        }
    }
    private fun socket(c:WorkshopPage,start:Double,diameter:Double,insertion:Int,scale:Double) {
        val radius=diameter/2*scale;val end=start+insertion*scale
        c.path(listOf(start to 220-radius,end to 220-radius,end to 220+radius,start to 220+radius),closed=true,dashed=true)
    }
    private fun segments(c:WorkshopPage,p:ConstructionPlan,s:Double,center:Double) {
        var start=0.0
        p.pieces.forEachIndexed {i,piece->
            val x=34+start*s
            c.line(x,center-65,x,center+65,dashed=true)
            if(piece.lengthMm*s>27)c.text(x+3,center-72,piece.label,9.0)
            start+=piece.lengthMm
        }
    }
}
