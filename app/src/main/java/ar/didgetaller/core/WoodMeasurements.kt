package ar.didgetaller.core

/** Additional numeric CSV rows retain transition sides and six decimal places independently of print rounding. */
internal object WoodMeasurements {
    private fun exact(v:Double)=java.lang.String.format(java.util.Locale.ROOT,"%.6f",v)
    fun metadata(p:ConstructionPlan,l:WorkshopLanguage):List<List<String>> {
        val stock=p.options.woodStock ?: return listOf(listOf(l.wood(WoodTerm.STOCK),l.wood(WoodTerm.NONE)))
        val prep=stock.preparation
        val rows=mutableListOf(listOf(l.wood(WoodTerm.STOCK),l.wood(WoodPresentation.kind(stock))),
            listOf(l.wood(WoodTerm.LENGTH),exact(stock.lengthMm)),listOf(l.wood(WoodTerm.KERF),exact(prep.kerfMm)),
            listOf(l.wood(WoodTerm.FACE),exact(prep.faceMm)),listOf(l.wood(WoodTerm.TRIM),exact(prep.endTrimMm)),
            listOf(l.wood(WoodTerm.UNCERTAINTY),exact(prep.uncertaintyMm)))
        when(stock) {
            is WoodStock.Log->{rows+=listOf(l.wood(WoodTerm.BARK),exact(stock.barkMm));rows+=listOf(l.wood(WoodTerm.EXTERIOR),exact(stock.exteriorLossMm))}
            is WoodStock.Timber->{rows+=listOf(l.wood(WoodTerm.WIDTH),exact(stock.widthMm));rows+=listOf(l.wood(if(stock.twoBoards)WoodTerm.BOARD_T else WoodTerm.BLOCK_T),exact(stock.thicknessMm))}
        }
        return rows
    }
    fun table(p:ConstructionPlan,l:WorkshopLanguage):WorkshopTable {
        val stockLabels=if(p.options.woodStock is WoodStock.Log)listOf(WoodTerm.JOIN_WIDTH,WoodTerm.LOG_DEPTH) else listOf(WoodTerm.HALF_WIDTH,WoodTerm.HALF_T)
        val headings=listOf(l.word(WorkshopTerm.STATION),l.wood(WoodTerm.BEFORE),l.wood(WoodTerm.AFTER),l.wood(WoodTerm.DEPTH),
            l.wood(stockLabels[0]),l.wood(stockLabels[1]),l.wood(WoodTerm.MIN_REMAINING),l.wood(WoodTerm.TABLE_REMAINING))
        val positions=(WoodDrawing.positions(p)+CarvingMeasurements.positions(p,9)).distinct().sorted()
        val rows=positions.map {x->
            val before=ConstructionDrawing.diameter(p,x,true);val after=ConstructionDrawing.diameter(p,x,false)
            listOf(exact(x),exact(before),exact(after),exact(maxOf(before,after)/2))+
                stockCells(p.options.woodStock,x,maxOf(before,after)/2)+
                listOf(CarvingMeasurements.row(p,x).remainingMm?.let {exact(it)} ?: "")
        }
        return WorkshopTable(headings.map {it+" (mm)"},rows)
    }    private fun stockCells(stock:WoodStock?,x:Double,radius:Double):List<String> {
        val capacity=stock?.let {WoodFit.capacity(it,x)} ?: return listOf("","","")
        return listOf(exact(capacity.halfWidthMm*2),exact(capacity.halfDepthMm),exact(minOf(capacity.halfWidthMm,capacity.halfDepthMm)-radius))
    }

}
