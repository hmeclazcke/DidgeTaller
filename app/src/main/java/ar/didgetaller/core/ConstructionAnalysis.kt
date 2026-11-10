package ar.didgetaller.core

data class ConstructionRequest(val name:String,val values:Map<String,String>,val action:String)
data class ConstructionResult(val plan:ConstructionPlan,val prediction:AcousticModel.Simulation,
    val candidates:List<ConstructionCandidate> = emptyList(),val sensitivity:List<ConstructionSensitivity> = emptyList())

internal fun calculateConstruction(r:ConstructionRequest,cancelled:()->Boolean):ConstructionResult {
    val shape=ConstructionForm.shape(r.values);val options=ConstructionForm.options(r.values)
    if(r.action=="search") {
        val candidates=ConstructionSearch.search(shape,options,ConstructionForm.target(r.values),cancelled)
        require(candidates.isNotEmpty())
        val p=candidates.first().plan
        return ConstructionResult(p,ConstructionProjects.predict(p),candidates)
    }
    val plan=ConstructionProjects.create(r.name,shape,options)
    val prediction=ConstructionProjects.predict(plan)
    val sensitivity=if(r.action=="tolerance")constructionTolerance(plan,cancelled) else emptyList()
    return ConstructionResult(plan,prediction,sensitivity=sensitivity)
}

internal fun constructionTolerance(p:ConstructionPlan,cancelled:()->Boolean):List<ConstructionSensitivity> {
    val base=ConstructionProjects.predict(p).baseHz
    require(base>0)
    val variants=listOf("ID +1 mm" to {ConstructionProjects.perturb(p,1.0)},"ID -1 mm" to {ConstructionProjects.perturb(p,-1.0)},
        "L +5 mm" to {ConstructionProjects.perturb(p,lengthDeltaMm=5.0)},"L -5 mm" to {ConstructionProjects.perturb(p,lengthDeltaMm=-5.0)})
    val result=variants.map {(name,changed)->
        if(cancelled())throw java.util.concurrent.CancellationException()
        ConstructionSensitivity(name,ConstructionSearch.shift(base,changed))
    }.toMutableList()
    result+=ConstructionSensitivity("ID bell +1 mm",ConstructionSearch.shift(base) {
        p.copy(pieces=p.pieces.dropLast(1)+p.pieces.last().let {it.copy(outletMm=it.outletMm+1)})})
    if(p.options.method==ConstructionMethod.PVC && p.options.fittings.isNotBlank())
        result+=ConstructionSensitivity("I -1 mm",ConstructionSearch.shift(base) {ConstructionSearch.shortInsertion(p,1.0)})
    return result+ConstructionSearch.sensitivities(p,1.0,cancelled)
}
