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
    fun csv(plan:ConstructionPlan):String = buildString {
        append("record,reference,x_mm,length_mm,ideal_inner_mm,inner_before_mm,inner_after_mm,details,half_depth_before_mm,half_depth_after_mm\n")
        append(listOf("design",plan.name,"",number(plan.lengthMm),"","","",plan.options.method.name,"","").joinToString(",",transform=::cell)+"\n")
        rows(plan).forEachIndexed {i,r->append(listOf("station",i.toString(),number(r.xMm),"",r.idealMm?.let(::number).orEmpty(),number(r.beforeMm),number(r.afterMm),"mm",number(r.beforeMm/2),number(r.afterMm/2)).joinToString(",",transform=::cell)+"\n")}
        plan.parts.forEachIndexed {i,part->append(listOf("part",(i+1).toString(),"","","","","",part,"","").joinToString(",",transform=::cell)+"\n")}
    }
    fun table(plan:ConstructionPlan):List<String> = rows(plan).map {r->
        "${number(r.xMm)} | ${r.idealMm?.let(::number) ?: "-"} | ${number(r.beforeMm)} / ${number(r.afterMm)} | ${number(r.beforeMm/2)} / ${number(r.afterMm/2)}"
    }
    private fun cell(raw:String):String {
        val text=if(raw.firstOrNull() in listOf('=','+','-','@') && raw.toDoubleOrNull()==null)"'"+raw else raw
        return "\""+text.replace("\"","\"\"")+"\""
    }
    fun number(x:Double)=String.format(Locale.ROOT,"%.3f",x)
    fun outline(plan:ConstructionPlan,sign:Int):List<Pair<Double,Double>> {
        var x=0.0
        return plan.pieces.flatMap {p->listOf(x to sign*p.inletMm/2,(x+p.lengthMm) to sign*p.outletMm/2).also {x+=p.lengthMm}}
    }
    private fun xml(text:String)=text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    fun svg(plan:ConstructionPlan):String {
        val width=plan.lengthMm+80
        val center=150.0
        val height=320+plan.parts.size*6
        fun path(sign:Int)=outline(plan,sign).joinToString(" "){(x,y)->"${number(x+30)},${number(center+y)}"}
        val title=xml(plan.name)
        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${number(width)}mm\" height=\"${height}mm\" viewBox=\"0 0 ${number(width)} $height\"><title>$title</title>")
            append("<g stroke=\"black\" fill=\"none\" stroke-width=\"0.3\"><polyline points=\"${path(-1)}\"/><polyline points=\"${path(1)}\"/><path d=\"M30 290 h100\"/></g>")
            append("<g font-family=\"sans-serif\" font-size=\"3\"><text x=\"30\" y=\"12\">$title - ${plan.options.method}; L=${number(plan.lengthMm)} mm</text>")
            rows(plan).forEach {r->append("<text x=\"${number(r.xMm+30)}\" y=\"275\">${number(r.xMm)}: ${number(r.beforeMm)}/${number(r.afterMm)}</text>")}
            plan.parts.forEachIndexed {i,part->append("<text x=\"30\" y=\"${320+i*6}\">${xml(part)}</text>")}
            append("<text x=\"30\" y=\"300\">100 mm; verify print scaling. ID before/after at junctions; not a validated played note.</text></g></svg>")
        }
    }
}
