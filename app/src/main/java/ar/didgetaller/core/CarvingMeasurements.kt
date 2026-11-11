package ar.didgetaller.core

import kotlin.math.abs

/** Workshop stations retain real profile turns and the smallest known remaining wall. */
internal object CarvingMeasurements {
    /** Remaining wood is below the groove; minimum wall also considers the sides of a board. */
    data class Row(val positionMm:Double,val diameterMm:Double,val depthMm:Double,val remainingMm:Double?,val minimumWallMm:Double?)
    fun row(p:ConstructionPlan,x:Double):Row {
        val diameter=ConstructionDrawing.diameter(p,x,false)
        val unknown=Row(x,diameter,diameter/2,null,null)
        val stock=p.options.woodStock ?: return unknown
        if(!stock.confirmed)return unknown
        val capacity=WoodFit.capacity(stock,x) ?: return unknown
        val bottom=when(stock) {
            is WoodStock.Timber->capacity.halfDepthMm-unknown.depthMm
            // A concentric cavity in a circular cap is nearest its outside at the cap's base.
            is WoodStock.Log->capacity.logRadiusMm!!-capacity.joinOffsetMm-unknown.depthMm
        }
        val minimum=if(stock is WoodStock.Timber)minOf(capacity.halfDepthMm,capacity.halfWidthMm)-unknown.depthMm else bottom
        return unknown.copy(remainingMm=bottom,minimumWallMm=minimum)
    }
    fun minimum(p:ConstructionPlan):Row? = WoodDrawing.positions(p).map {row(p,it)}
        .filter {it.minimumWallMm!=null}.minByOrNull {it.minimumWallMm!!}
    fun positions(p:ConstructionPlan,limit:Int):List<Double> {
        require(limit in 3..12)
        val chosen=linkedSetOf(0.0,p.lengthMm)
        minimum(p)?.let {chosen+=it.positionMm}
        val turns=p.ideal.controlPoints().windowed(3).sortedByDescending {(a,b,c)->
            abs((b.diameterMm-a.diameterMm)/(b.positionMm-a.positionMm)-(c.diameterMm-b.diameterMm)/(c.positionMm-b.positionMm))
        }.map {it[1].positionMm.toDouble()}
        turns.forEach {if(chosen.size<limit)chosen+=it}
        spread(p,chosen,limit)
        return chosen.sorted()
    }
    private fun spread(p:ConstructionPlan,chosen:MutableSet<Double>,limit:Int) {
        val available=WoodDrawing.positions(p).toMutableSet()
        available+=p.lengthMm/2
        available.removeAll(chosen)
        while(chosen.size<limit && available.isNotEmpty()) {
            val next=available.maxBy {x->chosen.minOf {abs(it-x)}}
            if(chosen.minOf {abs(it-next)}<p.lengthMm/12)break
            chosen+=next;available.remove(next)
        }
    }
    fun sections(p:ConstructionPlan):List<Double> = positions(p,3)
}
