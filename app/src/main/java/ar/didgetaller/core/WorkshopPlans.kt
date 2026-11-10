package ar.didgetaller.core

/** Millimetres throughout. Manufacturing interpretations never replace the stored acoustic shape. */
object WorkshopPlans {
    data class Station(val positionMm:Int,val diameterMm:Double) {
        val halfDepthMm:Double get()=diameterMm/2.0
    }
    data class SplitPlan(val stations:List<Station>,val minimumBlankWidthMm:Double,
                         val minimumHalfThicknessMm:Double)
    data class Layer(val startMm:Int,val endMm:Int,val openingMm:Double,
                     val maximumExcessMm:Double,val minimumStockWidthMm:Double)
    data class Pipe(val id:String,val insideMm:Double,val outsideMm:Double,val stockLengthMm:Int) {
        init {
            require(id.isNotBlank())
            require(insideMm.isFinite() && insideMm>0 && outsideMm.isFinite() && outsideMm>insideMm)
            require(stockLengthMm in 1..10000)
        }
    }
    /** Body length includes both sockets. Passage diameter describes the measured central web only. */
    data class Fitting(val id:String,val leftSocketMm:Double,val rightSocketMm:Double,
                       val leftInsertionMm:Int,val rightInsertionMm:Int,val bodyLengthMm:Int,
                       val passageMm:Double) {
        init {
            require(id.isNotBlank())
            require(leftSocketMm.isFinite() && leftSocketMm>0 && rightSocketMm.isFinite() && rightSocketMm>0)
            require(leftInsertionMm in 1..1000 && rightInsertionMm in 1..1000)
            require(bodyLengthMm in (leftInsertionMm+rightInsertionMm)..10000)
            require(passageMm.isFinite() && passageMm>0)
        }
    }
    data class Cut(val pipe:Pipe,val lengthMm:Int)
    data class BoreSection(val lengthMm:Int,val diameterMm:Double)
    data class PvcPlan(val lengthMm:Int,val cuts:List<Cut>,val fittings:List<Fitting>,
                       val fittingStartsMm:List<Int>,val bore:List<BoreSection>)

    fun stations(shape:AcousticModel.Shape,intervalMm:Int):List<Station> {
        require(intervalMm in 1..shape.lengthMm)
        val positions=(0..shape.lengthMm step intervalMm).toSet()+shape.controlPoints().map {it.positionMm}
        return positions.sorted().map { Station(it,shape.diameter(it)) }
    }

    /** Two mirrored semicircular hollows; tolerance is a radial overcut allowance for each half. */
    fun sandwich(shape:AcousticModel.Shape,intervalMm:Int,minimumWallMm:Double,radialToleranceMm:Double):SplitPlan {
        val allowance=stockAllowance(minimumWallMm,radialToleranceMm)
        val maximum=shape.controlPoints().maxOf {it.diameterMm}
        return SplitPlan(stations(shape,intervalMm),maximum+2*allowance,maximum/2+allowance)
    }

    /** Axially stacked circular layers, each drilled to enclose every ideal bore section it spans. */
    fun laminated(shape:AcousticModel.Shape,thicknessesMm:List<Int>,minimumWallMm:Double,
                  radialToleranceMm:Double):List<Layer> {
        val allowance=stockAllowance(minimumWallMm,radialToleranceMm)
        require(thicknessesMm.size in 1..256 && thicknessesMm.all {it>0})
        require(thicknessesMm.sumOf {it.toLong()}==shape.lengthMm.toLong())
        var start=0
        return thicknessesMm.map { thickness ->
            val end=start+thickness
            val positions=listOf(start,end)+shape.controlPoints().filter {it.positionMm in start..end}.map {it.positionMm}
            val diameters=positions.map {shape.diameter(it)}
            val opening=diameters.max()
            Layer(start,end,opening,opening-diameters.min(),opening+2*allowance).also {start=end}
        }
    }

    fun pvc(cuts:List<Cut>,fittings:List<Fitting>,maximumSocketClearanceMm:Double):PvcPlan {
        require(cuts.size in 1..100 && fittings.size==cuts.size-1)
        require(maximumSocketClearanceMm.isFinite() && maximumSocketClearanceMm>=0)
        cuts.forEachIndexed {index,cut->validateCut(cut,fittings.getOrNull(index-1),fittings.getOrNull(index))}
        fittings.forEachIndexed {index,fitting->
            validateSocket(fitting.leftSocketMm,cuts[index].pipe.outsideMm,maximumSocketClearanceMm)
            validateSocket(fitting.rightSocketMm,cuts[index+1].pipe.outsideMm,maximumSocketClearanceMm)
        }
        return assemble(cuts,fittings)
    }

    private fun validateCut(cut:Cut,left:Fitting?,right:Fitting?) {
        require(cut.lengthMm in 1..cut.pipe.stockLengthMm)
        require(cut.lengthMm>=(left?.rightInsertionMm ?: 0)+(right?.leftInsertionMm ?: 0))
    }

    private fun validateSocket(socket:Double,outside:Double,maximumClearance:Double) {
        require(socket>=outside && socket-outside<=maximumClearance) {"Measured socket/pipe diameters do not fit"}
    }

    private fun assemble(cuts:List<Cut>,fittings:List<Fitting>):PvcPlan {
        var cursor=0
        val starts=mutableListOf<Int>()
        val bore=mutableListOf<BoreSection>()
        cuts.forEachIndexed {index,cut->
            bore+=BoreSection(cut.lengthMm,cut.pipe.insideMm)
            cursor+=cut.lengthMm
            fittings.getOrNull(index)?.let {fitting->
                starts+=cursor-fitting.leftInsertionMm
                val web=fitting.bodyLengthMm-fitting.leftInsertionMm-fitting.rightInsertionMm
                if(web>0)bore+=BoreSection(web,fitting.passageMm)
                cursor+=web
            }
        }
        return PvcPlan(cursor,cuts.toList(),fittings.toList(),starts.toList(),bore.toList())
    }

    private fun stockAllowance(wall:Double,tolerance:Double):Double {
        require(wall.isFinite() && wall>0 && tolerance.isFinite() && tolerance>=0)
        require(wall+tolerance<=Double.MAX_VALUE/4) {"Stock dimensions overflow"}
        return wall+tolerance
    }
}
