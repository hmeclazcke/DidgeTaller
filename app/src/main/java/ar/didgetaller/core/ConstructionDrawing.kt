package ar.didgetaller.core

import java.util.Locale

object ConstructionDrawing {
    const val POINTS_PER_MM=72.0/25.4
    data class Row(val xMm:Double,val idealMm:Double?,val beforeMm:Double,val afterMm:Double)
    fun rows(plan:ConstructionPlan):List<Row> {
        require(plan.lengthMm in 500.0..3000.0)
        val positions=sortedSetOf(0.0,plan.lengthMm)
        var x=0.0
        plan.pieces.forEach {x+=it.lengthMm;positions+=x}
        (0..plan.lengthMm.toInt() step plan.options.intervalMm).forEach {positions+=it.toDouble()}
        plan.ideal.controlPoints().filter {it.positionMm<=plan.lengthMm}.forEach {positions+=it.positionMm.toDouble()}
        return positions.map {p->Row(p,ideal(plan,p),diameter(plan,p,true),diameter(plan,p,false))}
    }
    private fun ideal(plan:ConstructionPlan,x:Double):Double? {
        if(x>plan.ideal.lengthMm)return null
        val nodes=plan.ideal.controlPoints()
        val right=nodes.indexOfFirst {it.positionMm>=x}.coerceAtLeast(1)
        val a=nodes[right-1];val b=nodes[right]
        return a.diameterMm+(b.diameterMm-a.diameterMm)*(x-a.positionMm)/(b.positionMm-a.positionMm)
    }
    fun diameter(plan:ConstructionPlan,x:Double,before:Boolean):Double {
        require(x in 0.0..plan.lengthMm)
        var start=0.0
        plan.pieces.forEach {piece->
            val end=start+piece.lengthMm
            if(x<end || before && x==end)return piece.inletMm+(piece.outletMm-piece.inletMm)*(x-start)/piece.lengthMm
            start=end
        }
        return plan.pieces.last().outletMm
    }
    fun tileStarts(lengthMm:Double):List<Double> {
        require(lengthMm in 500.0..3000.0)
        return (0 until kotlin.math.ceil(lengthMm/150).toInt()).map {it*150.0}
    }
    fun csv(plan:ConstructionPlan):String=WorkshopDocument.csv(plan,WorkshopLanguage.ENGLISH)
    fun table(plan:ConstructionPlan):List<String> = WorkshopDocument.csv(plan,WorkshopLanguage.ENGLISH).lines()
    fun number(x:Double)=String.format(Locale.ROOT,"%.3f",x)
    fun outline(plan:ConstructionPlan,sign:Int):List<Pair<Double,Double>> {
        var x=0.0
        return plan.pieces.flatMap {p->listOf(x to sign*p.inletMm/2,(x+p.lengthMm) to sign*p.outletMm/2).also {x+=p.lengthMm}}
    }
    fun svg(plan:ConstructionPlan):String=WorkshopDocument.svg(plan,WorkshopLanguage.ENGLISH)
}
