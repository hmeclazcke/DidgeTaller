package ar.didgetaller.core

/** A sparse overview distinguishes measured raw stock from the calculated air passage. */
internal object WorkshopOverview {
    private fun n(v:Double)=WoodPresentation.number(v)
    fun page(p:ConstructionPlan,l:WorkshopLanguage,targetHz:Double?):WorkshopSheet {
        if(p.options.method!=ConstructionMethod.SANDWICH)return WorkshopSpecifications.page(p,l,targetHz)
        val c=WorkshopPage(842.0,595.0)
        WoodDrawing.background(c)
        c.text(34.0,37.0,WorkshopDocument.humanName(p.name),23.0,bold=true)
        c.text(34.0,60.0,l.wood(WoodTerm.OVERVIEW),12.0)
        c.text(34.0,97.0,l.word(WorkshopTerm.LENGTH)+": "+n(p.lengthMm)+" mm",13.0,bold=true)
        c.text(310.0,97.0,l.word(WorkshopTerm.MOUTH)+": "+n(p.pieces.first().inletMm)+" mm",11.0)
        c.text(568.0,97.0,l.word(WorkshopTerm.BELL)+": "+n(p.pieces.last().outletMm)+" mm",11.0)
        targetHz?.let {c.text(34.0,120.0,l.wood(WoodTerm.TARGET)+": "+WorkshopDocument.note(it,l)+" · "+n(it)+" Hz",10.0)}
        c.text(34.0,145.0,l.wood(WoodTerm.RAW),12.0,bold=true)
        outline(c,p,l)
        stock(c,p,l)
        c.paragraph(34.0,516.0,l.wood(WoodTerm.NO_SAFETY)+" "+l.wood(WoodTerm.NOTE_LIMIT),765.0,9.0)
        c.ruler(l);c.text(770.0,573.0,"1 / 2",8.0)
        return c.sheet()
    }
    private fun outline(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage) {
        val stock=p.options.woodStock
        val length=stock?.lengthMm ?: p.lengthMm
        val raw=rawBounds(p)
        val maxRadius=raw.maxOfOrNull {it.second} ?: 1.0
        val denominator=kotlin.math.ceil(maxOf(length*ConstructionDrawing.POINTS_PER_MM/710,maxRadius*ConstructionDrawing.POINTS_PER_MM/65))
        val s=ConstructionDrawing.POINTS_PER_MM/denominator
        val x0=62.0;val cy=225.0
        rawEnvelope(c,raw,x0,cy,s,stock!=null)
        val trim=stock?.preparation?.endTrimMm ?: 0.0
        val bore=ConstructionDrawing.outline(p,1).map {(x,r)->x0+(trim+x)*s to cy-r*s}+
            ConstructionDrawing.outline(p,-1).asReversed().map {(x,r)->x0+(trim+x)*s to cy-r*s}
        WoodDrawing.polygon(c,bore,WoodInk.AIR,WoodInk.TEAL,true)
        c.line(x0-8,cy,x0+length*s+8,cy,dashed=true)
        WoodDrawing.measure(c,"overall-length",p.lengthMm,s,x0+trim*s to 307.0,x0+(trim+p.lengthMm)*s to 307.0,
            n(p.lengthMm)+" mm",x0+trim*s+p.lengthMm*s/2-25,300.0)
        c.text(34.0,337.0,l.wood(WoodTerm.CAVITY)+" · "+l.wood(WoodTerm.SCALE)+" 1:"+n(denominator),9.0)
        val label=when(stock) {null->WoodTerm.UNMEASURED;is WoodStock.Log->if(stock.sections.isEmpty())WoodTerm.BOUND else WoodTerm.PROFILE_BOUND;is WoodStock.Timber->WoodPresentation.kind(stock)}
        c.text(34.0,355.0,l.wood(label),9.0)
    }
    private fun rawEnvelope(c:WorkshopPage,raw:List<Pair<Double,Double>>,x0:Double,cy:Double,s:Double,available:Boolean) {
        if(raw.size<2)return
        val outside=raw.map {(x,r)->x0+x*s to cy-r*s}+raw.asReversed().map {(x,r)->x0+x*s to cy+r*s}
        WoodDrawing.polygon(c,outside,if(available)WoodInk.WOOD else 0xffefede9.toInt())
        for(i in -3..3)c.marks+=WorkshopMark.Path(raw.map {(x,r)->x0+x*s to cy+r*s*i/4},strokeColor=WoodInk.GRAIN,strokeWidth=.4)
    }
    private fun rawBounds(p:ConstructionPlan):List<Pair<Double,Double>> = when(val stock=p.options.woodStock) {
        null->{val r=p.pieces.maxOf {maxOf(it.inletMm,it.outletMm)}/2+p.options.wallMm+p.options.radialToleranceMm;listOf(0.0 to r,p.lengthMm to r)}
        is WoodStock.Log->if(stock.sections.isEmpty())listOf(0.0 to stock.minimumDiameterMm/2,stock.lengthMm to stock.minimumDiameterMm/2) else stock.sections.map {it.positionMm to it.outerDiameterMm/2}
        is WoodStock.Timber->{val height=if(stock.twoBoards)stock.thicknessMm*2 else stock.thicknessMm;listOf(0.0 to height/2,stock.lengthMm to height/2)}
    }
    private fun stock(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage) {
        val stock=p.options.woodStock;val fit=WoodFit.assess(p.ideal,stock,p.options.wallMm,p.options.radialToleranceMm)
        c.text(34.0,386.0,l.wood(WoodPresentation.kind(stock)),13.0,bold=true)
        c.text(34.0,408.0,stockDimensions(stock,l),10.0)
        c.text(34.0,431.0,l.wood(WoodTerm.MINIMUM)+": "+l.wood(WoodTerm.SHORT_WIDTH)+" "+WoodPresentation.minimum(fit.requiredWidthMm)+" mm × "+l.wood(WoodTerm.SHORT_THICKNESS)+" "+WoodPresentation.minimum(fit.requiredHalfDepthMm)+" mm",10.0)
        c.text(455.0,431.0,l.wood(WoodTerm.CONSTRAINT)+": "+n(p.options.wallMm)+" mm + "+n(p.options.radialToleranceMm)+" mm",9.0)
        c.text(34.0,459.0,WoodPresentation.status(fit,l),13.0,color=if(fit.status==WoodFitStatus.DOES_NOT_FIT)WoodInk.RED else WoodInk.TEAL,bold=true)
        c.paragraph(34.0,480.0,WoodPresentation.detail(fit,l),765.0,9.0)
    }
    fun stockDimensions(stock:WoodStock?,l:WorkshopLanguage):String = when(stock) {
        null->l.wood(WoodTerm.UNMEASURED)
        is WoodStock.Log->n(stock.lengthMm)+" mm · "+l.wood(WoodTerm.DIAMETER)+": "+n(stock.minimumDiameterMm)
        is WoodStock.Timber->l.wood(WoodTerm.SHORT_LENGTH)+" "+n(stock.lengthMm)+" mm · "+l.wood(WoodTerm.SHORT_WIDTH)+" "+n(stock.widthMm)+" mm · "+l.wood(if(stock.twoBoards)WoodTerm.BOARD_T else WoodTerm.BLOCK_T)+": "+n(stock.thicknessMm)
    }
}
