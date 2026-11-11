package ar.didgetaller.core

/** Explicit full-size template geometry; independent from the two-sheet report. */
object WorkshopTemplates {
    fun create(p:ConstructionPlan,l:WorkshopLanguage):List<WorkshopSheet> {
        val starts=ConstructionDrawing.tileStarts(p.lengthMm)
        return starts.mapIndexed {i,start->template(p,l,start,i+1,starts.size)}
    }
    private fun template(p:ConstructionPlan,l:WorkshopLanguage,start:Double,index:Int,total:Int):WorkshopSheet {
        val c=WorkshopPage(595.0,842.0);val factor=ConstructionDrawing.POINTS_PER_MM
        c.text(34.0,32.0,l.word(WorkshopTerm.TEMPLATE),14.0)
        c.text(34.0,52.0,WorkshopDocument.humanName(p.name),11.0)
        c.text(34.0,72.0,"${l.word(WorkshopTerm.TILE)} $index / $total - 1:1 - ${WorkshopDocument.number(start)}–${WorkshopDocument.number(minOf(start+160,p.lengthMm))} mm",10.0)
        c.text(34.0,92.0,l.word(WorkshopTerm.OVERLAP),9.0)
        for(sign in listOf(-1,1)) {
            val points=tileOutline(p,start,sign).map {(x,r)->34+(x-start)*factor to 430+r*factor}
            c.path(points)
        }
        c.line(34.0,430.0,34+minOf(160.0,p.lengthMm-start)*factor,430.0,dashed=true)
        registration(c,start,34.0)
        if(start+150<p.lengthMm)registration(c,start+150,34+150*factor)
        ConstructionDrawing.rows(p).filter {it.xMm in start..minOf(start+160,p.lengthMm)}.forEach {r->
            val x=34+(r.xMm-start)*factor
            c.line(x,750.0,x,758.0);c.text(x,770.0,WorkshopDocument.number(r.xMm),8.0)
        }
        c.ruler(l)
        return c.sheet()
    }
    private fun tileOutline(p:ConstructionPlan,start:Double,sign:Int):List<Pair<Double,Double>> {
        val end=minOf(start+160,p.lengthMm)
        return listOf(start to sign*ConstructionDrawing.diameter(p,start,false)/2)+
            ConstructionDrawing.outline(p,sign).filter {it.first>start && it.first<end}+
            listOf(end to sign*ConstructionDrawing.diameter(p,end,true)/2)
    }
    private fun registration(c:WorkshopPage,station:Double,x:Double) {
        c.line(x,115.0,x,710.0,dashed=true)
        c.text(x,110.0,WorkshopDocument.number(station)+" mm",8.0)
        c.line(x-4,140.0,x+4,140.0);c.line(x,136.0,x,144.0)
    }
}
