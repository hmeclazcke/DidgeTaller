package ar.didgetaller.core

/** Additive string fields preserve construction schema1 and leave historical projects unmeasured. */
object WoodStockForm {
    fun defaults(lengthMm:Int):Map<String,String> = mapOf(
        "stockKind" to "UNSPECIFIED","stockLength" to (lengthMm+20).toString(),"stockDiameter" to "120",
        "stockWidth" to "120","stockThickness" to "65","stockBlockThickness" to "130","stockSections" to "","stockBark" to "2",
        "stockExterior" to "1","stockKerf" to "3","stockFace" to "1","stockTrim" to "10",
        "stockUncertainty" to "1","stockConfirmed" to "false")
    private fun amount(v:Map<String,String>,key:String)=v.getValue(key).trim().replace(',','.').toDouble()
    fun read(v:Map<String,String>):WoodStock? {
        val kind=v["stockKind"] ?: "UNSPECIFIED"
        if(kind=="UNSPECIFIED")return null
        val prep=WoodPreparation(amount(v,"stockKerf"),amount(v,"stockFace"),
            amount(v,"stockTrim"),amount(v,"stockUncertainty"))
        val length=amount(v,"stockLength");val confirmed=v.getValue("stockConfirmed").toBooleanStrict()
        return when(kind) {
            "LOG"->WoodStock.Log(length,amount(v,"stockDiameter"),sections(v.getValue("stockSections")),
                amount(v,"stockBark"),amount(v,"stockExterior"),prep,confirmed)
            "BOARDS","BLOCK"->WoodStock.Timber(length,amount(v,"stockWidth"),amount(v,if(kind=="BOARDS")"stockThickness" else "stockBlockThickness"),kind=="BOARDS",prep,confirmed)
            else->throw IllegalArgumentException("Unsupported wood stock type")
        }
    }
    private fun sections(text:String):List<WoodSection> {
        require(text.length<=8192)
        return text.lineSequence().filter {it.isNotBlank()}.map {row->
            val fields=row.split(',');require(fields.size==2)
            WoodSection(fields[0].trim().toDouble(),fields[1].trim().toDouble())
        }.toList()
    }
    fun accepts(plan:ConstructionPlan):Boolean {
        if(plan.options.method!=ConstructionMethod.SANDWICH)return true
        return WoodFit.assess(plan.ideal,plan.options.woodStock,plan.options.wallMm,plan.options.radialToleranceMm).status!=WoodFitStatus.DOES_NOT_FIT
    }
}
