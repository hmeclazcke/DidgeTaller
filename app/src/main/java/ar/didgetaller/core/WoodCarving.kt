package ar.didgetaller.core

/** Two mirrored orthographic half profiles; cross sections use their own explicitly stated scale. */
internal object WoodCarving {
    fun page(p:ConstructionPlan,l:WorkshopLanguage):WorkshopSheet {
        val c=WorkshopPage(842.0,595.0)
        WoodDrawing.background(c)
        c.text(34.0,33.0,WorkshopDocument.humanName(p.name),20.0,bold=true)
        c.text(34.0,55.0,l.wood(WoodTerm.CARVING),12.0)
        val largest=WoodDrawing.positions(p).mapNotNull {WoodDrawing.capacity(p,it)?.halfDepthMm}.maxOrNull() ?: 1.0
        val denominator=kotlin.math.ceil(maxOf(p.lengthMm*ConstructionDrawing.POINTS_PER_MM/710,largest*ConstructionDrawing.POINTS_PER_MM/42))
        val s=ConstructionDrawing.POINTS_PER_MM/denominator
        c.text(34.0,79.0,l.wood(WoodTerm.SCALE)+" 1:"+WoodPresentation.number(denominator),9.0)
        val assessment=WoodFit.assess(p.ideal,p.options.woodStock,p.options.wallMm,p.options.radialToleranceMm)
        c.text(400.0,79.0,WoodPresentation.status(assessment,l),9.0,color=if(assessment.status==WoodFitStatus.DOES_NOT_FIT)WoodInk.RED else WoodInk.TEAL)
        c.text(34.0,104.0,l.wood(WoodTerm.A),11.0,bold=true)
        WoodDrawing.half(c,p,64.0,123.0,s,1)
        c.text(34.0,199.0,l.wood(WoodTerm.B),11.0,bold=true)
        WoodDrawing.half(c,p,64.0,253.0,s,-1)
        c.text(64.0+p.lengthMm*s/2,178.0,l.wood(WoodTerm.PLANE),9.0)
        WoodDrawing.measure(c,"overall-length",p.lengthMm,s,64.0 to 279.0,64+p.lengthMm*s to 279.0,
            WoodPresentation.number(p.lengthMm)+" mm",350.0,274.0)
        c.text(34.0,310.0,l.word(WorkshopTerm.SECTIONS)+" · "+l.wood(WoodTerm.LEGEND),10.0,bold=true)
        val fit=WoodFit.assess(p.ideal,p.options.woodStock,p.options.wallMm,p.options.radialToleranceMm)
        val middle=if(fit.positionMm>0 && fit.positionMm<p.lengthMm)fit.positionMm else p.ideal.controlPoints().drop(1).dropLast(1).firstOrNull()?.positionMm?.toDouble() ?: p.lengthMm/2
        listOf(0.0,middle,p.lengthMm).forEachIndexed {i,x->WoodDrawing.section(c,p,l,x,158.0+i*263,400.0)}
        c.text(34.0,526.0,l.wood(WoodTerm.DEPTH)+" = "+l.wood(WoodTerm.BORE)+" / 2 · "+l.wood(WoodTerm.DETAIL),9.0)
        c.ruler(l);c.text(770.0,573.0,"2 / 2",8.0)
        return c.sheet()
    }
}
