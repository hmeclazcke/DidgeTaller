package ar.didgetaller.core

import kotlin.math.sqrt

/** Measured losses, in mm. End trim is per end; face loss is per prepared face. */
data class WoodPreparation(val kerfMm:Double=0.0,val faceMm:Double=0.0,val endTrimMm:Double=0.0,val uncertaintyMm:Double=0.0) {
    init {require(listOf(kerfMm,faceMm,endTrimMm,uncertaintyMm).all {it.isFinite() && it in 0.0..500.0})}
}
data class WoodSection(val positionMm:Double,val outerDiameterMm:Double) {
    init {require(positionMm.isFinite() && positionMm>=0 && outerDiameterMm.isFinite() && outerDiameterMm in 1.0..2000.0)}
}

sealed interface WoodStock {
    val lengthMm:Double
    val preparation:WoodPreparation
    val confirmed:Boolean
    data class Log(override val lengthMm:Double,val minimumDiameterMm:Double,val sections:List<WoodSection>,
        val barkMm:Double,val exteriorLossMm:Double,override val preparation:WoodPreparation,override val confirmed:Boolean):WoodStock {
        init {
            require(lengthMm.isFinite() && lengthMm in 1.0..10000.0 && minimumDiameterMm in 1.0..2000.0)
            require(barkMm.isFinite() && barkMm in 0.0..500.0 && exteriorLossMm.isFinite() && exteriorLossMm in 0.0..500.0)
            require(sections.size<=64 && sections.zipWithNext().all {(a,b)->b.positionMm>a.positionMm})
            require(sections.all {it.positionMm<=lengthMm})
        }
    }
    /** For two boards enter the smaller available dimensions, not the sum of their thicknesses. */
    data class Timber(override val lengthMm:Double,val widthMm:Double,val thicknessMm:Double,val twoBoards:Boolean,
        override val preparation:WoodPreparation,override val confirmed:Boolean):WoodStock {
        init {require(listOf(lengthMm,widthMm,thicknessMm).all {it.isFinite() && it>0} && lengthMm<=10000 && widthMm<=2000 && thicknessMm<=2000)}
    }
}

enum class WoodFitStatus { FITS, DOES_NOT_FIT, UNCONFIRMED }
enum class WoodLimit { LENGTH, WIDTH, HALF_THICKNESS, LOG_ENVELOPE, MEASUREMENTS }
data class WoodAssessment(val status:WoodFitStatus,val reason:WoodLimit,val positionMm:Double,val marginMm:Double?,
    val requiredWidthMm:Double,val requiredHalfDepthMm:Double)
data class WoodCapacity(val halfDepthMm:Double,val halfWidthMm:Double,val logRadiusMm:Double?=null,val joinOffsetMm:Double=0.0)

/** Piecewise-linear stock and bore bounds attain their minimum clearance at their combined breakpoints. */
object WoodFit {
    fun assess(shape:AcousticModel.Shape,stock:WoodStock?,wallMm:Double,reserveMm:Double):WoodAssessment {
        require(wallMm.isFinite() && wallMm>0 && reserveMm.isFinite() && reserveMm>=0)
        val required=shape.controlPoints().maxOf {it.diameterMm}/2+wallMm+reserveMm
        if(stock==null)return WoodAssessment(WoodFitStatus.UNCONFIRMED,WoodLimit.MEASUREMENTS,0.0,null,2*required,required)
        return measured(shape,stock,wallMm+reserveMm).copy(requiredWidthMm=2*required,requiredHalfDepthMm=required)
    }
    private fun measured(shape:AcousticModel.Shape,stock:WoodStock,wall:Double):WoodAssessment {
        val length=stock.lengthMm-2*stock.preparation.endTrimMm
        if(length<shape.lengthMm)return WoodAssessment(WoodFitStatus.DOES_NOT_FIT,WoodLimit.LENGTH,shape.lengthMm.toDouble(),length-shape.lengthMm,0.0,0.0)
        val positions=positions(shape,stock)
        val limits=positions.mapNotNull {x->clearance(shape,stock,x,wall)}
        val limiting=limits.minByOrNull {it.marginMm!!}
            ?: return WoodAssessment(WoodFitStatus.UNCONFIRMED,WoodLimit.MEASUREMENTS,0.0,null,0.0,0.0)
        if(limiting.marginMm!! < -1e-9)return limiting.copy(status=WoodFitStatus.DOES_NOT_FIT)
        if(!stock.confirmed || limits.size!=positions.size)return limiting.copy(status=WoodFitStatus.UNCONFIRMED,reason=WoodLimit.MEASUREMENTS)
        return limiting.copy(status=WoodFitStatus.FITS)
    }
    private fun positions(shape:AcousticModel.Shape,stock:WoodStock):List<Double> {
        val result=shape.controlPoints().map {it.positionMm.toDouble()}.toMutableSet()
        if(stock is WoodStock.Log)result+=stock.sections.map {it.positionMm-stock.preparation.endTrimMm}.filter {it in 0.0..shape.lengthMm.toDouble()}
        return result.sorted()
    }
    private fun clearance(shape:AcousticModel.Shape,stock:WoodStock,x:Double,wall:Double):WoodAssessment? {
        val capacity=capacity(stock,x) ?: return null
        val needed=diameter(shape,x)/2+wall
        val depth=capacity.halfDepthMm-needed
        val width=capacity.halfWidthMm-needed
        val reason=when {stock is WoodStock.Log->WoodLimit.LOG_ENVELOPE;width<depth->WoodLimit.WIDTH;else->WoodLimit.HALF_THICKNESS}
        return WoodAssessment(WoodFitStatus.UNCONFIRMED,reason,x,minOf(depth,width),0.0,0.0)
    }
    fun diameter(shape:AcousticModel.Shape,x:Double):Double {
        require(x in 0.0..shape.lengthMm.toDouble())
        val points=shape.controlPoints();val i=points.indexOfFirst {it.positionMm>=x}.coerceAtLeast(1)
        val a=points[i-1];val b=points[i]
        return a.diameterMm+(b.diameterMm-a.diameterMm)*(x-a.positionMm)/(b.positionMm-a.positionMm)
    }
    fun outerDiameter(log:WoodStock.Log,positionMm:Double):Double? {
        if(log.sections.isEmpty())return log.minimumDiameterMm
        if(positionMm<log.sections.first().positionMm || positionMm>log.sections.last().positionMm)return null
        if(log.sections.size==1)return log.sections.single().outerDiameterMm
        val i=log.sections.indexOfFirst {it.positionMm>=positionMm}.coerceAtLeast(1)
        val a=log.sections[i-1];val b=log.sections[i]
        return a.outerDiameterMm+(b.outerDiameterMm-a.outerDiameterMm)*(positionMm-a.positionMm)/(b.positionMm-a.positionMm)
    }
    fun capacity(stock:WoodStock,x:Double):WoodCapacity? = when(stock) {
        is WoodStock.Log->logCapacity(stock,x)
        is WoodStock.Timber->timberCapacity(stock)
    }
    private fun logCapacity(stock:WoodStock.Log,x:Double):WoodCapacity? {
        val d=outerDiameter(stock,x+stock.preparation.endTrimMm) ?: return null
        val radius=d/2-stock.barkMm-stock.exteriorLossMm-stock.preparation.uncertaintyMm
        val offset=stock.preparation.kerfMm/2+stock.preparation.faceMm
        val halfWidth=sqrt(maxOf(0.0,radius*radius-offset*offset))
        return WoodCapacity(radius-offset,halfWidth,radius,offset)
    }
    private fun timberCapacity(stock:WoodStock.Timber):WoodCapacity {
        val loss=stock.preparation.faceMm+stock.preparation.uncertaintyMm
        val thickness=if(stock.twoBoards)stock.thicknessMm else (stock.thicknessMm-stock.preparation.kerfMm)/2
        return WoodCapacity(thickness-2*loss,stock.widthMm/2-loss)
    }
}
