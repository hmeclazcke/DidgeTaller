package ar.didgetaller.core

/** One assembled section explains both identical half-grooves; the table carries longitudinal depth. */
internal object WoodCarving {
    fun page(p:ConstructionPlan,l:WorkshopLanguage):WorkshopSheet {
        val c=WorkshopPage(842.0,595.0)
        WoodDrawing.background(c)
        c.text(34.0,33.0,WorkshopDocument.humanName(p.name),20.0,bold=true)
        c.text(34.0,56.0,l.wood(WoodTerm.CARVING),12.0)
        CarvingMeasurements.sections(p).forEachIndexed {i,x->WoodDrawing.section(c,p,l,x,158.0+i*263,171.0)}
        table(c,p,l)
        val note=if(p.options.woodStock is WoodStock.Log)WoodTerm.LOG_NOTE else WoodTerm.TABLE_NOTE
        c.paragraph(34.0,511.0,l.wood(note)+" "+l.wood(WoodTerm.PREPARATION_NOTE),765.0,9.0)
        c.ruler(l);c.text(770.0,573.0,"2 / 2",8.0)
        return c.sheet()
    }
    private fun table(c:WorkshopPage,p:ConstructionPlan,l:WorkshopLanguage) {
        c.text(34.0,302.0,l.wood(WoodTerm.CARVING_TABLE),12.0,bold=true)
        val columns=listOf(34.0,224.0,411.0,621.0)
        val headings=listOf(WoodTerm.TABLE_POSITION,WoodTerm.BORE,WoodTerm.TABLE_DEPTH,WoodTerm.TABLE_REMAINING)
        headings.forEachIndexed {i,term->c.text(columns[i],326.0,l.wood(term),10.0,bold=true)}
        c.line(34.0,335.0,808.0,335.0)
        val minimum=CarvingMeasurements.minimum(p)?.positionMm
        CarvingMeasurements.positions(p,9).forEachIndexed {i,x->
            val row=CarvingMeasurements.row(p,x);val y=354.0+i*16
            val values=listOf(l.dimension(x),l.dimension(row.diameterMm),l.dimension(row.depthMm),row.remainingMm?.let {l.dimension(it)} ?: l.wood(WoodTerm.UNCONFIRMED))
            values.forEachIndexed {j,value->c.text(columns[j],y,value,10.0,bold=x==minimum)}
        }
        c.text(34.0,498.0,l.wood(WoodTerm.SELECTED),8.5)
    }
}
