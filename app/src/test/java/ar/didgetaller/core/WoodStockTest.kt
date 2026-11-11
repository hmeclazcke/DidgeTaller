package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class WoodStockTest {
    private val shape=AcousticModel.Shape(mouthMm=30,bellMm=83)
    @Test fun circularAndRectangularStocksAreDifferentPhysicalEnvelopes() {
        val boards=WoodStock.Timber(1500.0,101.0,50.5,true,WoodPreparation(),true)
        val log=WoodStock.Log(1500.0,101.0,emptyList(),0.0,0.0,WoodPreparation(),true)
        assertEquals(41.5,WoodFit.assess(shape,boards,9.0,0.0).requiredHalfDepthMm-9,1e-9)
        assertEquals(WoodFitStatus.FITS,WoodFit.assess(shape,boards,9.0,0.0).status)
        assertEquals(101.0,WoodFit.assess(shape,boards,9.0,0.0).requiredWidthMm,0.0)
        assertEquals(WoodFitStatus.FITS,WoodFit.assess(shape,log,9.0,0.0).status)
        val block=boards.copy(thicknessMm=101.0,twoBoards=false)
        assertEquals(WoodFitStatus.FITS,WoodFit.assess(shape,block,9.0,0.0).status)
        assertEquals(WoodFitStatus.DOES_NOT_FIT,WoodFit.assess(shape,block.copy(preparation=WoodPreparation(kerfMm=3.0)),9.0,0.0).status)
        assertEquals(WoodFitStatus.FITS,WoodFit.assess(shape,boards.copy(preparation=WoodPreparation(kerfMm=3.0)),9.0,0.0).status)
    }
    @Test fun localNecksAndIntermediateBorePeaksAreNeverMissedBySampling() {
        val log=WoodStock.Log(1500.0,120.0,listOf(WoodSection(0.0,120.0),WoodSection(733.0,40.0),WoodSection(1500.0,120.0)),0.0,0.0,WoodPreparation(),true)
        val result=WoodFit.assess(shape,log,9.0,0.0)
        assertEquals(WoodFitStatus.DOES_NOT_FIT,result.status)
        assertEquals(733.0,result.positionMm,0.0)
        val peak=shape.copy(points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(725,120.0),AcousticModel.BorePoint(1500,83.0)))
        assertEquals(725.0,WoodFit.assess(peak,WoodStock.Timber(1500.0,110.0,70.0,true,WoodPreparation(),true),9.0,0.0).positionMm,0.0)
    }
    @Test fun preparationAndIncompleteKnowledgeCannotProduceAFalseConfirmation() {
        val stock=WoodStock.Log(1520.0,115.0,emptyList(),2.0,1.0,WoodPreparation(kerfMm=3.0,faceMm=1.0,endTrimMm=10.0,uncertaintyMm=1.0),true)
        assertEquals(WoodFitStatus.FITS,WoodFit.assess(shape,stock,9.0,0.0).status)
        assertEquals(WoodFitStatus.DOES_NOT_FIT,WoodFit.assess(shape,stock.copy(lengthMm=1519.0),9.0,0.0).status)
        assertEquals(WoodFitStatus.UNCONFIRMED,WoodFit.assess(shape,stock.copy(confirmed=false),9.0,0.0).status)
        val partial=stock.copy(sections=listOf(WoodSection(50.0,115.0),WoodSection(1400.0,115.0)))
        assertEquals(WoodFitStatus.UNCONFIRMED,WoodFit.assess(shape,partial,9.0,0.0).status)
    }    @Test fun invalidMeasurementsAreRejectedAndEachPreparationAxisIsApplied() {
        for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0)) {
            try {WoodPreparation(faceMm=value);fail("Invalid loss")}catch(_:IllegalArgumentException){}
            try {WoodSection(value,100.0);fail("Invalid station")}catch(_:IllegalArgumentException){}
            try {WoodStock.Timber(1500.0,value,70.0,true,WoodPreparation(),true);fail("Invalid width")}catch(_:IllegalArgumentException){}
        }
        val boards=WoodStock.Timber(1520.0,110.0,60.0,true,WoodPreparation(kerfMm=5.0,faceMm=1.0,endTrimMm=10.0,uncertaintyMm=2.0),true)
        val capacity=WoodFit.capacity(boards,0.0)!!
        assertEquals(54.0,capacity.halfDepthMm,0.0)
        assertEquals(52.0,capacity.halfWidthMm,0.0)
        val block=WoodFit.capacity(boards.copy(twoBoards=false,thicknessMm=120.0),0.0)!!
        assertEquals(51.5,block.halfDepthMm,0.0)
        val log=WoodStock.Log(1520.0,120.0,emptyList(),2.0,1.0,boards.preparation,true)
        val cap=WoodFit.capacity(log,0.0)!!
        assertEquals(55.0,cap.logRadiusMm!!,0.0)
        assertEquals(3.5,cap.joinOffsetMm,0.0)
        assertEquals(51.5,cap.halfDepthMm,0.0)
        assertEquals(kotlin.math.sqrt(55.0*55.0-3.5*3.5),cap.halfWidthMm,1e-10)
        assertEquals(WoodFitStatus.DOES_NOT_FIT,WoodFit.assess(shape,boards.copy(widthMm=100.0),9.0,0.0).status)
        val short=WoodFit.assess(shape,boards.copy(lengthMm=1519.0),9.0,0.0)
        assertEquals(WoodLimit.LENGTH,short.reason);assertEquals(-1.0,short.marginMm!!,0.0)
        assertEquals(WoodFitStatus.DOES_NOT_FIT,WoodFit.assess(shape,boards.copy(preparation=WoodPreparation(faceMm=100.0)),9.0,0.0).status)
    }
    @Test fun reservesMeasurementOffsetsAndFractionalInterpolationHaveIndependentExpectations() {
        val boards=WoodStock.Timber(1500.0,101.0,50.5,true,WoodPreparation(),true)
        val unknown=WoodFit.assess(shape,null,9.0,2.0)
        assertEquals(105.0,unknown.requiredWidthMm,0.0);assertEquals(52.5,unknown.requiredHalfDepthMm,0.0)
        assertEquals(WoodFitStatus.DOES_NOT_FIT,WoodFit.assess(shape,boards,9.0,2.0).status)
        assertEquals(36.5,WoodFit.diameter(shape,510.0),1e-9)
        for(x in listOf(-.1,1500.1))try {WoodFit.diameter(shape,x);fail("Outside instrument")}catch(_:IllegalArgumentException){}
        for((wall,reserve) in listOf(0.0 to 0.0,Double.NaN to 0.0,Double.POSITIVE_INFINITY to 0.0,9.0 to -1.0,9.0 to Double.NaN,9.0 to Double.POSITIVE_INFINITY))try {
            WoodFit.assess(shape,boards,wall,reserve);fail("Invalid wall or reserve")
        }catch(_:IllegalArgumentException){}
        val log=WoodStock.Log(1520.0,120.0,listOf(WoodSection(0.0,120.0),WoodSection(743.25,40.0),WoodSection(1520.0,120.0)),0.0,0.0,WoodPreparation(endTrimMm=10.0),true)
        val limited=WoodFit.assess(shape,log,9.0,0.0)
        assertEquals(733.25,limited.positionMm,1e-9)
        assertEquals(20.0,WoodFit.capacity(log,733.25)!!.halfDepthMm,1e-9)
        assertEquals(WoodLimit.LOG_ENVELOPE,limited.reason)
        assertEquals(120.0,WoodFit.outerDiameter(log,0.0)!!,0.0)
        assertEquals(120.0,WoodFit.outerDiameter(log,1520.0)!!,0.0)
        assertEquals(80.0,WoodFit.outerDiameter(log,743.25/2)!!,1e-9)
        assertNull(WoodFit.outerDiameter(log,-.01));assertNull(WoodFit.outerDiameter(log,1520.01))
        assertEquals(110.0,WoodFit.outerDiameter(log.copy(sections=listOf(WoodSection(0.0,110.0))),0.0)!!,0.0)
        val missing=WoodFit.assess(shape,log.copy(lengthMm=2000.0,sections=listOf(WoodSection(1900.0,120.0))),9.0,0.0)
        assertEquals(WoodFitStatus.UNCONFIRMED,missing.status);assertNull(missing.marginMm)
        assertEquals(WoodLimit.WIDTH,WoodFit.assess(shape,boards.copy(widthMm=90.0,thicknessMm=100.0),9.0,0.0).reason)
    }

}
