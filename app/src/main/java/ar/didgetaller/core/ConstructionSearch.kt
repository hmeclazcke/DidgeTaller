package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.*

data class ConstructionTarget(val hz:Double=73.42,val higher:List<Double> = emptyList(),
    val mouthMm:Double=32.0,val preferredLengthMm:Int=1500,val maximumLengthMm:Int=2200,
    val minimumBoreMm:Double=23.0,val maximumBoreMm:Double=170.0,val maximumBellMm:Double=120.0) {
    init {
        require(hz in 30.0..200.0 && higher.size<=2 && higher.all {it in hz..650.0})
        require(preferredLengthMm in 500..3000 && maximumLengthMm in preferredLengthMm..3000)
        require(minimumBoreMm in 15.0..180.0 && maximumBoreMm in minimumBoreMm..220.0)
        require(mouthMm in minimumBoreMm..minOf(180.0,maximumBoreMm))
        require(maximumBellMm in minimumBoreMm..minOf(200.0,maximumBoreMm))
    }
}
data class ConstructionCandidate(val plan:ConstructionPlan,val hz:Double,val higher:List<Double>,val cents:Double,val score:Double)
data class ConstructionSensitivity(val dimension:String,val deltaHz:Double?)

object ConstructionSearch {
    fun search(shape:AcousticModel.Shape,o:ConstructionOptions,t:ConstructionTarget,cancelled:()->Boolean={false}):List<ConstructionCandidate> {
        val plans=if(o.method==ConstructionMethod.PVC)pvcPlans(shape,o) else woodPlans(shape,o,t)
        val candidates=plans.distinctBy {it.pieces}.mapNotNull {p->
            if(cancelled())throw CancellationException()
            if(!feasible(p,t) || !WoodStockForm.accepts(p))return@mapNotNull null
            val prediction=AcousticModel.evaluateCylinders(ConstructionProjects.cylinders(p),stepHz=3)
            if(prediction.baseHz<=0)return@mapNotNull null
            val errorCents=cents(prediction.baseHz,t.hz)
            val higherError=t.higher.mapIndexed {i,hz->prediction.frequenciesHz.getOrNull(i+1)?.let {abs(cents(it,hz))} ?: 2400.0}.sum()
            ConstructionCandidate(p,prediction.baseHz,prediction.frequenciesHz.drop(1).take(4),errorCents,abs(errorCents)+higherError*.5+p.pieces.size*.05)
        }
        return candidates.sortedBy {it.score}.take(5)
    }
    private fun feasible(p:ConstructionPlan,t:ConstructionTarget):Boolean =
        p.lengthMm<=t.maximumLengthMm && abs(p.pieces.first().inletMm-t.mouthMm)<.01 &&
        p.pieces.last().outletMm<=t.maximumBellMm && p.pieces.all {it.inletMm in t.minimumBoreMm..t.maximumBoreMm && it.outletMm in t.minimumBoreMm..t.maximumBoreMm}

    private fun woodPlans(shape:AcousticModel.Shape,o:ConstructionOptions,t:ConstructionTarget):List<ConstructionPlan> {
        val lengths=listOf(.75,.9,1.0,1.1,1.2).map {(t.preferredLengthMm*it).roundToInt().coerceIn(500,t.maximumLengthMm)}.distinct()
        return lengths.flatMap {length->listOf(.85,1.0,1.15).mapNotNull {bellFactor->
            val points=listOf(0.0,.25,.5,.75,1.0).mapIndexed {i,f->
                val diameter=when(i){0->t.mouthMm;4->(shape.diameter(shape.lengthMm)*bellFactor).coerceIn(t.minimumBoreMm,t.maximumBellMm);else->shape.diameter((shape.lengthMm*f).roundToInt()).coerceIn(t.minimumBoreMm,t.maximumBoreMm)}
                AcousticModel.BorePoint((length*f).roundToInt(),diameter)
            }
            try {ConstructionProjects.create("${length} mm / ${points.last().diameterMm} mm",woodShape(length,points,o),o)}
            catch (_:IllegalArgumentException) {null}
        }}
    }
    /** A drilled first layer cannot reproduce a widening inlet while keeping the requested mouth ID. */
    private fun woodShape(length:Int,points:List<AcousticModel.BorePoint>,o:ConstructionOptions):AcousticModel.Shape {
        val shape=AcousticModel.Shape(length)
        if(o.method!=ConstructionMethod.LAMINATED)return shape.withControlPoints(points)
        val inletLength=o.layers.substringBefore(',').trim().toInt()
        require(inletLength in 1..length)
        val inlet=listOf(points.first(),AcousticModel.BorePoint(inletLength,points.first().diameterMm))
        return shape.withControlPoints(inlet+points.filter {it.positionMm>inletLength})
    }
    private fun pvcPlans(shape:AcousticModel.Shape,o:ConstructionOptions):List<ConstructionPlan> {
        // Stock/socket validation rejects unbuildable scaled cuts rather than clamping physical lengths.
        return listOf(.9,.95,1.0,1.05,1.1).mapNotNull {factor->
            val assembly=o.assembly.lineSequence().filter {it.isNotBlank()}.map {line->
                val r=line.split(',');require(r.size==3)
                "${r[0]},${(r[1].trim().toInt()*factor).roundToInt()},${r[2]}"
            }.joinToString("\n")
            try {ConstructionProjects.create("PVC x $factor",shape,o.copy(assembly=assembly))} catch (_:IllegalArgumentException){null}
        }
    }
    fun cents(actual:Double,target:Double):Double {
        require(actual.isFinite() && actual>0 && target.isFinite() && target>0)
        return 1200*ln(actual/target)/ln(2.0)
    }
    fun sensitivities(plan:ConstructionPlan,deltaMm:Double,cancelled:()->Boolean={false}):List<ConstructionSensitivity> {
        require(deltaMm in .1..3.0)
        val base=ConstructionProjects.predict(plan).baseHz
        require(base>0)
        return plan.pieces.take(12).mapIndexed {index,p->
            if(cancelled())throw CancellationException()
            ConstructionSensitivity(p.label,shift(base) {
                val changed=p.copy(inletMm=p.inletMm+deltaMm,outletMm=p.outletMm+deltaMm)
                plan.copy(pieces=plan.pieces.toMutableList().also {it[index]=changed})
            })
        }.sortedByDescending {abs(it.deltaHz ?: 0.0)}
    }
    /** Out-of-model perturbations are unavailable, never reported as a zero-frequency change. */
    fun shift(baseHz:Double,variant:()->ConstructionPlan):Double? = try {
        val result=ConstructionProjects.predict(variant()).baseHz
        if(result>0)result-baseHz else null
    } catch (_:IllegalArgumentException) {null}

    /** Less-than-nominal first insertion exposes an actual socket gap, not a compressed reducer. */
    fun shortInsertion(plan:ConstructionPlan,deltaMm:Double):ConstructionPlan {
        require(plan.options.method==ConstructionMethod.PVC)
        val id=plan.options.assembly.lineSequence().first().split(',')[2].trim()
        val f=requireNotNull(ConstructionCatalog.connectors(plan.options.fittings)[id]).fitting
        require(deltaMm>0 && deltaMm<f.leftInsertionMm)
        val pieces=plan.pieces.toMutableList()
        pieces.add(1,ConstructionPiece("socket $id",deltaMm,f.leftSocketMm,f.leftSocketMm))
        return plan.copy(pieces=pieces)
    }
}
