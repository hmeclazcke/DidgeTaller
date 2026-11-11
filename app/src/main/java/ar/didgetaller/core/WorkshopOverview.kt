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
        c.text(34.0,97.0,l.wood(WoodTerm.INSTRUMENT_LENGTH)+": "+l.dimension(p.lengthMm)+" mm",13.0,bold=true)
        c.text(310.0,97.0,l.wood(WoodTerm.MOUTH)+": "+l.dimension(p.pieces.first().inletMm)+" mm",11.0)
        c.text(568.0,97.0,l.wood(WoodTerm.BELL)+": "+l.dimension(p.pieces.last().outletMm)+" mm",11.0)
        targetHz?.let {c.text(34.0,120.0,l.wood(WoodTerm.TARGET)+": "+WorkshopDocument.note(it,l)+" · "+l.dimension(it)+" Hz",10.0)}
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
        val denominator=kotlin.math.ceil(maxOf(length*ConstructionDrawing.POINTS_PER_MM/710,maxRadius*ConstructionDrawing.POINTS_PER_MM/45))
        val s=ConstructionDrawing.POINTS_PER_MM/denominator
        val x0=62.0;val cy=260.0
        rawEnvelope(c,raw,x0,cy,s,stock!=null)
        val trim=stock?.preparation?.endTrimMm ?: 0.0
        val bore=ConstructionDrawing.outline(p,1).map {(x,r)->x0+(trim+x)*s to cy-r*s}+
            ConstructionDrawing.outline(p,-1).asReversed().map {(x,r)->x0+(trim+x)*s to cy-r*s}
        WoodDrawing.polygon(c,bore,WoodInk.AIR,WoodInk.TEAL,true)
        c.line(x0-8,cy,x0+length*s+8,cy,dashed=true)
        markers(c,p,l,x0+trim*s,cy,s)
        WoodDrawing.measure(c,"overall-length",p.lengthMm,s,x0+trim*s to 307.0,x0+(trim+p.lengthMm)*s to 307.0,
            l.dimension(p.lengthMm)+" mm",x0+trim*s+p.lengthMm*s/2-25,300.0)
        c.text(34.0,337.0,l.wood(WoodTerm.CAVITY)+" · "+l.wood(WoodTerm.SCALE)+" 1:"+n(denominator),9.0)
        val label=when(stock) {null->WoodTerm.UNMEASURED;is WoodStock.Log->if(stock.sections.isEmpty())WoodTerm.BOUND else WoodTerm.PROFILE_BOUND;is WoodStock.Timber->WoodTerm.PARALLEL}
        c.text(34.0,355.0,l.wood(label),9.0)
    }
    private fun markers(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage,x0:Double,cy:Double,s:Double) {
        val rows=CarvingMeasurements.positions(p,4).map {CarvingMeasurements.row(p,it)}
        val labels=rows.map {x0+it.positionMm*s-25}.toMutableList()
        for(i in 1 until labels.size)labels[i]=maxOf(labels[i],labels[i-1]+100)
        labels[labels.lastIndex]=minOf(labels.last(),735.0)
        for(i in labels.lastIndex-1 downTo 0)labels[i]=minOf(labels[i],labels[i+1]-100)
        rows.forEachIndexed {i,row->
            val x=x0+row.positionMm*s;val radius=row.depthMm*s;val tx=labels[i].coerceAtLeast(34.0)
            c.text(tx,174.0,l.dimension(row.positionMm)+" mm",10.0,bold=true)
            c.text(tx,190.0,"Ø "+l.dimension(row.diameterMm)+" mm",10.0,bold=true)
            c.line(tx+24,197.0,x,cy-radius-3)
            measureBore(c,row,x,cy,s)
        }
    }
    private fun measureBore(c:WorkshopPage,row:CarvingMeasurements.Row,x:Double,cy:Double,s:Double) {
        // Label and full-diameter arrow are separated so neither hides the cavity contour.
        WoodDrawing.measure(c,"station-bore",row.diameterMm,s,x to cy-row.depthMm*s,x to cy+row.depthMm*s,"",x,cy)
    }
    private fun rawEnvelope(c:WorkshopPage,raw:List<Pair<Double,Double>>,x0:Double,cy:Double,s:Double,available:Boolean) {
        if(!available || raw.size<2)return
        val outside=raw.map {(x,r)->x0+x*s to cy-r*s}+raw.asReversed().map {(x,r)->x0+x*s to cy+r*s}
        WoodDrawing.polygon(c,outside,WoodInk.WOOD)
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
        val dimensions=if(stock==null)l.wood(WoodTerm.MINIMUM)+": "+l.wood(WoodTerm.SHORT_WIDTH)+" "+WoodPresentation.minimum(fit.requiredWidthMm).let {if(l==WorkshopLanguage.SPANISH)it.replace('.',',') else it}+" mm × "+l.wood(WoodTerm.SHORT_THICKNESS)+" "+WoodPresentation.minimum(fit.requiredHalfDepthMm).let {if(l==WorkshopLanguage.SPANISH)it.replace('.',',') else it}+" mm" else stockDimensions(stock,l)
        c.text(34.0,408.0,dimensions,10.0)
        c.text(34.0,431.0,l.wood(WoodTerm.CONSTRAINT)+": "+l.dimension(p.options.wallMm)+" mm",10.0)
        if(p.options.radialToleranceMm>0)c.text(455.0,431.0,l.wood(WoodTerm.RESERVE)+": "+l.dimension(p.options.radialToleranceMm)+" mm",9.0)
        c.text(34.0,459.0,WoodPresentation.status(fit,l),13.0,color=if(fit.status==WoodFitStatus.DOES_NOT_FIT)WoodInk.RED else WoodInk.TEAL,bold=true)
        c.paragraph(34.0,480.0,description(p,fit,l),765.0,9.0)
    }
    private fun description(p:ConstructionPlan,fit:WoodAssessment,l:WorkshopLanguage):String {
        if(fit.status==WoodFitStatus.UNCONFIRMED)return l.wood(WoodTerm.LIMIT_UNKNOWN)
        if(fit.status!=WoodFitStatus.FITS)return WoodPresentation.reason(fit,l)+" · "+l.location(fit.positionMm,p.lengthMm)
        val minimum=CarvingMeasurements.minimum(p) ?: return l.wood(WoodTerm.UNKNOWN)
        return l.wood(WoodTerm.MIN_REMAINING)+": "+l.dimension(minimum.minimumWallMm!!)+" mm · "+l.location(minimum.positionMm,p.lengthMm)
    }
    fun stockDimensions(stock:WoodStock?,l:WorkshopLanguage):String = when(stock) {
        null->l.wood(WoodTerm.UNMEASURED)
        is WoodStock.Log->logDimensions(stock,l)
        is WoodStock.Timber->l.wood(WoodTerm.SHORT_LENGTH)+" "+l.dimension(stock.lengthMm)+" mm · "+l.wood(WoodTerm.SHORT_WIDTH)+" "+l.dimension(stock.widthMm)+" mm · "+l.wood(if(stock.twoBoards)WoodTerm.STOCK_T else WoodTerm.BLOCK_SIZE)+" "+l.dimension(stock.thicknessMm)+" mm"
    }
    private fun logDimensions(stock:WoodStock.Log,l:WorkshopLanguage):String {
        val diameters=stock.sections.map {it.outerDiameterMm}
        val minimum=diameters.minOrNull() ?: stock.minimumDiameterMm
        val maximum=diameters.maxOrNull() ?: minimum
        val label=if(diameters.isEmpty())WoodTerm.MIN_D else WoodTerm.MEASURED_D
        val range=if(minimum==maximum)l.dimension(minimum) else l.dimension(minimum)+"–"+l.dimension(maximum)
        return l.wood(WoodTerm.SHORT_LENGTH)+" "+l.dimension(stock.lengthMm)+" mm · "+l.wood(label)+" "+range+" mm"
    }
}
