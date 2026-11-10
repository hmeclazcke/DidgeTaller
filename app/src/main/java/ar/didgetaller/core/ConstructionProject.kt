package ar.didgetaller.core

/** Manufactured circular sections. Adjacent pieces may have discontinuous inner diameters. */
data class ConstructionPiece(val label:String,val lengthMm:Double,val inletMm:Double,val outletMm:Double) {
    init {
        require(lengthMm.isFinite() && lengthMm>0)
        require(inletMm in 15.0..220.0 && outletMm in 15.0..220.0)
    }
}

enum class ConstructionMethod { SANDWICH, LAMINATED, PVC }

data class ConstructionOptions(val method:ConstructionMethod=ConstructionMethod.SANDWICH,
    val intervalMm:Int=100,val wallMm:Double=8.0,val radialToleranceMm:Double=1.0,
    val layers:String="50",val pipes:String="",val fittings:String="",val assembly:String="",
    val socketClearanceMm:Double=0.5)

data class ConstructionPlan(val name:String,val ideal:AcousticModel.Shape,val options:ConstructionOptions,
    val pieces:List<ConstructionPiece>,val parts:List<String>) {
    val lengthMm:Double get()=pieces.sumOf {it.lengthMm}
}

object ConstructionProjects {
    fun create(name:String,shape:AcousticModel.Shape,options:ConstructionOptions):ConstructionPlan {
        require(name.isNotBlank() && name.none {it.code<32})
        WorkshopPlans.sandwich(shape,options.intervalMm,options.wallMm,options.radialToleranceMm)
        require(options.intervalMm in 50..500)
        val plan=when(options.method) {
            ConstructionMethod.SANDWICH -> split(name,shape,options)
            ConstructionMethod.LAMINATED -> laminated(name,shape,options)
            ConstructionMethod.PVC -> ConstructionCatalog.plan(name,shape,options)
        }
        require(plan.pieces.size<=256 && plan.lengthMm in 500.0..3000.0)
        return plan
    }

    private fun split(name:String,shape:AcousticModel.Shape,o:ConstructionOptions):ConstructionPlan {
        val stock=WorkshopPlans.sandwich(shape,o.intervalMm,o.wallMm,o.radialToleranceMm)
        val pieces=shape.controlPoints().zipWithNext().map {(a,b)->ConstructionPiece("${a.positionMm}-${b.positionMm}",(b.positionMm-a.positionMm).toDouble(),a.diameterMm,b.diameterMm)}
        val parts=listOf("A: L=${shape.lengthMm}; W=${stock.minimumBlankWidthMm}; H=${stock.minimumHalfThicknessMm} mm", "B: L=${shape.lengthMm}; W=${stock.minimumBlankWidthMm}; H=${stock.minimumHalfThicknessMm} mm")
        return ConstructionPlan(name,shape,o,pieces,parts)
    }

    private fun laminated(name:String,shape:AcousticModel.Shape,o:ConstructionOptions):ConstructionPlan {
        val requested=o.layers.split(',').map {it.trim().toInt()}
        val thicknesses=layerThicknesses(shape.lengthMm,requested)
        val layers=WorkshopPlans.laminated(shape,thicknesses,o.wallMm,o.radialToleranceMm)
        val pieces=layers.mapIndexed {i,l->ConstructionPiece("${i+1}",(l.endMm-l.startMm).toDouble(),l.openingMm,l.openingMm)}
        val parts=layers.mapIndexed {i,l->"${i+1}: x=${l.startMm}-${l.endMm} mm; L=${l.endMm-l.startMm}; ID=${l.openingMm}; Wmin=${l.minimumStockWidthMm}; Emax=${l.maximumExcessMm} mm"}
        return ConstructionPlan(name,shape,o,pieces,parts)
    }

    private fun layerThicknesses(length:Int,requested:List<Int>):List<Int> {
        if(requested.size!=1)return requested
        val size=requested.single()
        require(size in 20..1000)
        return (0 until length step size).map {minOf(size,length-it)}
    }

    fun cylinders(plan:ConstructionPlan,subdivisions:Int=8):List<AcousticModel.Cylinder> {
        require(subdivisions in 1..32)
        val result=plan.pieces.flatMap {p->
            val count=if(p.inletMm==p.outletMm)1 else subdivisions
            // The mesh has a fixed number of cells; allocate that exact bound before filling it.
            Array(count){i->AcousticModel.Cylinder(p.lengthMm/count,p.inletMm+(p.outletMm-p.inletMm)*(i+.5)/count)}.asList()
        }
        require(result.size<=2048)
        return result
    }

    fun predict(plan:ConstructionPlan):AcousticModel.Simulation = AcousticModel.evaluateCylinders(cylinders(plan))

    /** Independent diameter and last-piece length perturbations, never claimed as measured precision. */
    fun perturb(plan:ConstructionPlan,diameterDeltaMm:Double=0.0,lengthDeltaMm:Double=0.0):ConstructionPlan {
        require(diameterDeltaMm.isFinite() && lengthDeltaMm.isFinite())
        val pieces=plan.pieces.mapIndexed {i,p->p.copy(lengthMm=p.lengthMm+if(i==plan.pieces.lastIndex)lengthDeltaMm else 0.0,
            inletMm=p.inletMm+diameterDeltaMm,outletMm=p.outletMm+diameterDeltaMm)}
        return plan.copy(pieces=pieces)
    }
}
