package ar.didgetaller.core

/** Stable string fields are persisted separately from legacy geometry; parsing occurs once per request. */
object ConstructionForm {
    fun activeMethods()=listOf(ConstructionMethod.SANDWICH,ConstructionMethod.PVC)
    fun defaults(shape:AcousticModel.Shape):Map<String,String> = mapOf(
        "geometry" to shape.controlPoints().joinToString("\n"){"${it.positionMm},${it.diameterMm}"},
        "method" to "SANDWICH","target" to "73.42","higher" to "","mouth" to shape.controlPoints().first().diameterMm.toString(),
        "preferred" to shape.lengthMm.toString(),"maximumLength" to maxOf(shape.lengthMm,2200).toString(),
        "minimumBore" to "23","maximumBore" to "170","bell" to "120","wall" to "8","tolerance" to "1",
        "interval" to "100","layers" to "50","pipes" to "","fittings" to "","assembly" to "","clearance" to "0.5")+WoodStockForm.defaults(shape.lengthMm)
    fun name(v:Map<String,String>,fallback:String)=WorkshopDocument.humanName(v["planName"]?.ifBlank {fallback} ?: fallback)
    fun shape(values:Map<String,String>):AcousticModel.Shape {
        require(values.getValue("geometry").length<=8192)
        val points=values.getValue("geometry").lineSequence().filter {it.isNotBlank()}.map {line->
            val r=line.split(',');require(r.size==2)
            AcousticModel.BorePoint(r[0].trim().toInt(),r[1].trim().toDouble())
        }.toList()
        require(points.size in 2..32)
        return AcousticModel.Shape(points.last().positionMm).withControlPoints(points)
    }
    fun options(v:Map<String,String>)=ConstructionOptions(ConstructionMethod.valueOf(v.getValue("method")),
        v.getValue("interval").toInt(),v.getValue("wall").toDouble(),v.getValue("tolerance").toDouble(),
        v.getValue("layers"),v.getValue("pipes"),v.getValue("fittings"),v.getValue("assembly"),v.getValue("clearance").toDouble(),if(v.getValue("method")=="SANDWICH")WoodStockForm.read(v) else null)
    fun target(v:Map<String,String>)=ConstructionTarget(v.getValue("target").toDouble(),
        v.getValue("higher").split(',').filter {it.isNotBlank()}.map {it.trim().toDouble()},
        v.getValue("mouth").toDouble(),v.getValue("preferred").toInt(),v.getValue("maximumLength").toInt(),
        v.getValue("minimumBore").toDouble(),v.getValue("maximumBore").toDouble(),v.getValue("bell").toDouble())
}
