package ar.didgetaller.core

internal data class WorkshopTable(val headings:List<String>,val rows:List<List<String>>)

/** Semantic rows are calculated from validated dimensions, never parsed back out of display strings. */
internal object WorkshopTables {
    private fun n(x:Double)=WorkshopDocument.number(x)
    fun measurements(p:ConstructionPlan,l:WorkshopLanguage):WorkshopTable {
        val allowance=p.options.wallMm+p.options.radialToleranceMm
        val headings=listOf(l.word(WorkshopTerm.STATION),l.word(WorkshopTerm.DIAMETER),l.word(WorkshopTerm.DEPTH),l.word(WorkshopTerm.WIDTH)+" (min.)",l.word(WorkshopTerm.THICKNESS)+" (min.)")
        val transitions=p.ideal.controlPoints().map {it.positionMm.toDouble()}.toSet()
        val rows=ConstructionDrawing.rows(p).map {r->
            val dia=maxOf(r.beforeMm,r.afterMm)
            listOf(n(r.xMm)+if(r.xMm in transitions)" *" else "",pair(r.beforeMm,r.afterMm),pair(r.beforeMm/2,r.afterMm/2),WorkshopDocument.minimum(dia+2*allowance),WorkshopDocument.minimum(dia/2+allowance))
        }
        return WorkshopTable(headings,rows)
    }
    private fun pair(a:Double,b:Double)=if(a==b)n(a) else "${n(a)} / ${n(b)}"
    fun layers(p:ConstructionPlan,l:WorkshopLanguage):WorkshopTable {
        var start=0.0
        val rows=p.pieces.mapIndexed {i,piece->
            val end=start+piece.lengthMm
            listOf("${i+1}","${n(start)}-${n(end)}",n(piece.lengthMm),n(piece.inletMm),WorkshopDocument.minimum(piece.inletMm+2*(p.options.wallMm+p.options.radialToleranceMm))).also {start=end}
        }
        return WorkshopTable(listOf(l.word(WorkshopTerm.ORDER),l.word(WorkshopTerm.RANGE),l.word(WorkshopTerm.LENGTH_SHORT),l.word(WorkshopTerm.OPENING),l.word(WorkshopTerm.SQUARE)),rows)
    }
    fun pvc(p:ConstructionPlan,l:WorkshopLanguage):WorkshopTable {
        val pipes=ConstructionCatalog.pipes(p.options.pipes);val fittings=ConstructionCatalog.connectors(p.options.fittings)
        var cursor=0.0
        val rows=p.options.assembly.lineSequence().filter {it.isNotBlank()}.flatMapIndexed {i,line->
            val fields=line.split(',').map {it.trim()};val pipe=pipes.getValue(fields[0])
            val cut=listOf("${i+1}. ${l.word(WorkshopTerm.TUBE)} ${pipe.id} @${n(cursor)}",fields[1],n(pipe.insideMm),n(pipe.outsideMm),"—")
            cursor+=fields[1].toDouble()
            val connector=fittings[fields[2]]?.fitting
            val start=cursor-(connector?.leftInsertionMm ?: 0)
            connector?.let {cursor+=it.bodyLengthMm-it.leftInsertionMm-it.rightInsertionMm}
            listOfNotNull(cut,connector?.let {listOf(l.word(WorkshopTerm.CONNECTOR)+" "+it.id+" @"+n(start),it.bodyLengthMm.toString(),"${n(it.passageMm)} / ${n(fittings.getValue(fields[2]).profile.maxOf {point->point.diameterMm})}","${n(it.leftSocketMm)} / ${n(it.rightSocketMm)}","${it.leftInsertionMm} / ${it.rightInsertionMm}")})
        }.toList()
        return WorkshopTable(listOf(l.word(WorkshopTerm.PART_POSITION),l.word(WorkshopTerm.LENGTH_SHORT),l.word(WorkshopTerm.DIAMETER),l.word(WorkshopTerm.OUTSIDE)+" / "+l.word(WorkshopTerm.SOCKETS),l.word(WorkshopTerm.INSERTIONS)),rows)
    }
    fun csv(p:ConstructionPlan,l:WorkshopLanguage):String {
        val station=WorkshopTable(listOf(l.word(WorkshopTerm.STATION)+" (mm)",l.word(WorkshopTerm.DIAMETER)+" (mm)",l.word(WorkshopTerm.DEPTH)+" (mm)",l.word(WorkshopTerm.WIDTH)+" (mm)",l.word(WorkshopTerm.THICKNESS)+" (mm)"),measurements(p,l).rows)
        val extras=when(p.options.method) {
            ConstructionMethod.SANDWICH->listOf(WoodMeasurements.table(p,l))
            ConstructionMethod.LAMINATED->listOf(layers(p,l))
            ConstructionMethod.PVC->listOf(pvc(p,l))
        }
        val selected=if(p.options.method==ConstructionMethod.SANDWICH)station else WorkshopTable(station.headings.take(2),station.rows.map {it.take(2)})
        val metadata=listOf(listOf(l.word(WorkshopTerm.TITLE),WorkshopDocument.humanName(p.name)),listOf(l.word(WorkshopTerm.LENGTH)+" (mm)",n(p.lengthMm)),listOf(l.word(WorkshopDocument.method(p))))+
            if(p.options.method==ConstructionMethod.SANDWICH)WoodMeasurements.metadata(p,l) else emptyList()
        return metadata.joinToString("\r\n"){it.joinToString(",",transform=::cell)}+"\r\n\r\n"+
            (listOf(selected)+extras).joinToString("\n"){table->(listOf(table.headings)+table.rows).joinToString("\r\n"){row->row.joinToString(",",transform=::cell)}}+"\r\n"
    }
    private fun cell(value:String):String {
        val safe=if(value.firstOrNull() in listOf('=','+','-','@') && value.toDoubleOrNull()==null)"'$value" else value
        return "\""+safe.replace("\"","\"\"")+"\""
    }
}

internal object WorkshopSpecifications {
    private fun n(v:Double)=WorkshopDocument.number(v)
    fun page(p:ConstructionPlan,l:WorkshopLanguage,targetHz:Double?):WorkshopSheet {
        val c=WorkshopPage(595.0,842.0)
        c.text(34.0,35.0,l.word(WorkshopTerm.TITLE),17.0)
        c.text(34.0,57.0,WorkshopDocument.humanName(p.name),13.0)
        c.text(34.0,77.0,l.word(WorkshopDocument.method(p)),11.0)
        dimensions(c,p,l)
        val target=targetHz?.let {"${WorkshopDocument.note(it,l)} / ${n(it)} Hz"} ?: l.word(WorkshopTerm.UNSPECIFIED)
        c.text(34.0,143.0,l.word(WorkshopTerm.TARGET)+": "+target)
        c.text(34.0,160.0,l.word(WorkshopTerm.PREDICTION)+": "+ConstructionProjects.predict(p).frequenciesHz.take(5).joinToString(" / "){n(it)}+" Hz",9.0)
        material(c,p,l)
        c.paragraph(34.0,240.0,l.wood(WoodTerm.DETAIL),520.0)
        table(c,p,l)
        c.paragraph(34.0,775.0,l.word(WorkshopTerm.WARNING),520.0,8.5)
        c.text(535.0,826.0,"1 / 2",8.0)
        return c.sheet()
    }
    private fun dimensions(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage) {
        c.text(34.0,101.0,l.word(WorkshopTerm.LENGTH)+": "+n(p.lengthMm)+" mm")
        c.text(34.0,120.0,l.word(WorkshopTerm.MOUTH)+": "+n(p.pieces.first().inletMm)+" mm")
        c.text(310.0,120.0,l.word(WorkshopTerm.BELL)+": "+n(p.pieces.last().outletMm)+" mm")
    }
    private fun material(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage) {
        if(p.options.method==ConstructionMethod.PVC) {
            c.text(34.0,184.0,l.word(WorkshopTerm.PARTS)+": "+WorkshopTables.pvc(p,l).rows.size,10.0)
            return
        }
        c.text(34.0,182.0,l.word(WorkshopTerm.WALL)+": "+n(p.options.wallMm)+" mm; "+l.word(WorkshopTerm.ALLOWANCE)+": "+n(p.options.radialToleranceMm)+" mm",9.0)
        if(p.options.method==ConstructionMethod.SANDWICH) {
            val s=WorkshopPlans.sandwich(p.ideal,p.options.intervalMm,p.options.wallMm,p.options.radialToleranceMm)
            c.text(34.0,198.0,l.word(WorkshopTerm.STOCK)+": 2",10.0)
            c.text(34.0,212.0,l.word(WorkshopTerm.STOCK_ORDER)+": "+n(p.lengthMm)+" x "+WorkshopDocument.minimum(s.minimumBlankWidthMm)+" x "+WorkshopDocument.minimum(s.minimumHalfThicknessMm)+" mm",9.0)
        } else c.text(34.0,199.0,l.word(WorkshopTerm.QUANTITY)+": "+p.pieces.size+"; "+l.word(WorkshopTerm.SQUARE),10.0)
    }
    private fun table(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage) {
        val t=when(p.options.method) {
            ConstructionMethod.SANDWICH->WorkshopTables.measurements(p,l)
            ConstructionMethod.LAMINATED->WorkshopTables.layers(p,l)
            ConstructionMethod.PVC->WorkshopTables.pvc(p,l)
        }
        require(t.rows.size<=36){l.word(WorkshopTerm.TOO_DENSE)}
        c.text(34.0,306.0,l.word(when(p.options.method) {ConstructionMethod.SANDWICH->WorkshopTerm.MEASUREMENTS;ConstructionMethod.LAMINATED->WorkshopTerm.LAYERS;ConstructionMethod.PVC->WorkshopTerm.PARTS}),11.0)
        val widths=if(p.options.method==ConstructionMethod.PVC)listOf(125.0,65.0,80.0,145.0,110.0) else listOf(100.0,110.0,105.0,105.0,105.0)
        require(t.rows.all {row->row.indices.all {j->row[j].length*4.8<widths[j]-5}}){l.word(WorkshopTerm.TOO_DENSE)}
        var x=34.0
        t.headings.forEachIndexed {i,h->c.paragraph(x,325.0,h,widths[i]-8,8.0);x+=widths[i]}
        t.rows.forEachIndexed {i,row->
            x=34.0;val y=361.0+i*11
            row.forEachIndexed {j,v->c.text(x,y,v,8.5);x+=widths[j]}
        }
        c.line(34.0,351.0,560.0,351.0)
        if(p.options.method==ConstructionMethod.SANDWICH)c.paragraph(34.0,754.0,l.word(WorkshopTerm.TRANSITIONS),520.0,8.0)
    }
}
