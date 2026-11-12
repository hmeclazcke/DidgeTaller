package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class WorkshopPlansTest {
    private val shape = AcousticModel.Shape(1000, 30, 40, 70, listOf(
        AcousticModel.BorePoint(0, 30.0), AcousticModel.BorePoint(125, 50.0),
        AcousticModel.BorePoint(1000, 70.0)))

    @Test fun stationsIncludeTransitionsAndEndpointsWithoutDuplicates() {
        val rows = WorkshopPlans.stations(shape, 100)
        assertEquals(listOf(0,100,125,200,300,400,500,600,700,800,900,1000), rows.map { it.positionMm })
        assertEquals(46.0, rows[1].diameterMm, 1e-9)
        assertEquals(70.0, rows.last().diameterMm, 0.0)
    }
    @Test fun splitHalvesUseRadiusAndReserveDiameterToleranceInStock() {
        val plan = WorkshopPlans.sandwich(shape, 100, 8.0, 1.0)
        assertEquals(35.0, plan.stations.last().halfDepthMm, 0.0)
        assertEquals(88.0, plan.minimumBlankWidthMm, 0.0)
        assertEquals(44.0, plan.minimumHalfThicknessMm, 0.0)
    }
    @Test fun layersEncloseEveryInternalTransitionRatherThanOnlyTheirEnds() {
        val hump = shape.copy(points=listOf(AcousticModel.BorePoint(0,30.0),
            AcousticModel.BorePoint(125,90.0),AcousticModel.BorePoint(1000,30.0)))
        val layers = WorkshopPlans.laminated(hump, listOf(200,800), 8.0, 1.0)
        assertEquals(90.0, layers[0].openingMm, 0.0)
        assertEquals(60.0, layers[0].maximumExcessMm, 0.0)
        assertEquals(200, layers[1].startMm)
        assertEquals(1000, layers.last().endMm)
        assertEquals(108.0, layers.first().minimumStockWidthMm, 0.0)
    }
    @Test fun pvcCountsTubeBoresAndOnlyUnoccupiedFittingPassageInAssembledLength() {
        val tube = WorkshopPlans.Pipe("measured pipe", 32.0, 40.0, 1500)
        val fitting = WorkshopPlans.Fitting("measured coupling", 40.2,40.2,30,25,75,35.0)
        val plan = WorkshopPlans.pvc(listOf(WorkshopPlans.Cut(tube,500),WorkshopPlans.Cut(tube,600)),listOf(fitting),.5)
        assertEquals(1120,plan.lengthMm)
        assertEquals(listOf(500,20,600),plan.bore.map { it.lengthMm })
        assertEquals(listOf(32.0,35.0,32.0),plan.bore.map { it.diameterMm })
        assertEquals(listOf(470),plan.fittingStartsMm)
    }
    @Test fun threeTubesAccountForBothInsertionsOnMiddlePiece() {
        val p=WorkshopPlans.Pipe("p",32.0,40.0,1000)
        val f=WorkshopPlans.Fitting("f",40.0,40.0,30,30,60,32.0)
        assertEquals(900,WorkshopPlans.pvc(List(3){WorkshopPlans.Cut(p,300)},List(2){f},0.0).lengthMm)
        reject { WorkshopPlans.pvc(listOf(WorkshopPlans.Cut(p,300),WorkshopPlans.Cut(p,50),WorkshopPlans.Cut(p,300)),List(2){f},0.0) }
    }
    @Test fun rejectsInvalidDimensionsAndMissingMaterial() {
        reject { WorkshopPlans.stations(shape,0) }
        reject { WorkshopPlans.sandwich(shape,100,-1.0,1.0) }
        reject { WorkshopPlans.sandwich(shape,100,8.0,Double.NaN) }
        reject { WorkshopPlans.laminated(shape,listOf(500),8.0,0.0) }
        reject { WorkshopPlans.laminated(shape,listOf(-1,1001),8.0,0.0) }
        reject { WorkshopPlans.Pipe("",32.0,40.0,1000) }
        reject { WorkshopPlans.Pipe("p",40.0,32.0,1000) }
        reject { WorkshopPlans.Fitting("f",40.0,40.0,30,30,50,32.0) }
    }
    @Test fun rejectsBadSocketsExcessiveClearanceAndStockOverruns() {
        val p=WorkshopPlans.Pipe("p",32.0,40.0,500)
        val f=WorkshopPlans.Fitting("f",40.0,40.0,20,20,60,32.0)
        reject { WorkshopPlans.pvc(listOf(WorkshopPlans.Cut(p,501)),emptyList(),0.0) }
        reject { WorkshopPlans.pvc(emptyList(),emptyList(),0.0) }
        reject { WorkshopPlans.pvc(List(2){WorkshopPlans.Cut(p,300)},emptyList(),0.0) }
        reject { WorkshopPlans.pvc(List(2){WorkshopPlans.Cut(p,300)},listOf(f.copy(leftSocketMm=39.0)),.5) }
        reject { WorkshopPlans.pvc(List(2){WorkshopPlans.Cut(p,300)},listOf(f.copy(rightSocketMm=41.0)),.5) }
    }
    private fun reject(block:()->Unit) {
        try { block();fail("Invalid workshop geometry must be rejected") } catch (_:IllegalArgumentException) { }
    }

    @Test fun inclusiveBoundariesAndZeroToleranceRemainManufacturable() {
        assertEquals(1001,WorkshopPlans.stations(shape,1).size)
        assertEquals(listOf(0,125,1000),WorkshopPlans.stations(shape,1000).map {it.positionMm})
        assertEquals(1,WorkshopPlans.laminated(shape,listOf(1000),8.0,0.0).size)
        assertEquals(1,WorkshopPlans.pvc(listOf(WorkshopPlans.Cut(WorkshopPlans.Pipe("p",1.0,2.0,1),1)),emptyList(),0.0).lengthMm)
        reject {WorkshopPlans.stations(shape,1001)}
        reject {WorkshopPlans.sandwich(shape,100,0.0,0.0)}
        reject {WorkshopPlans.sandwich(shape,100,8.0,-1.0)}
        reject {WorkshopPlans.pvc(listOf(WorkshopPlans.Cut(WorkshopPlans.Pipe("p",1.0,2.0,1),0)),emptyList(),0.0)}
        reject {WorkshopPlans.pvc(listOf(WorkshopPlans.Cut(WorkshopPlans.Pipe("p",1.0,2.0,1),1)),emptyList(),Double.NaN)}
    }

    @Test fun overflowingStockCannotProduceNonfiniteDimensions() {
        reject {WorkshopPlans.sandwich(shape,100,Double.MAX_VALUE,0.0)}
    }

    @Test fun partsListRetainsMeasuredCatalogIdentityAndOwnsItsSequence() {
        val left=WorkshopPlans.Pipe("tube A",32.0,40.0,500)
        val right=WorkshopPlans.Pipe("tube B",45.0,50.0,1000)
        val connector=WorkshopPlans.Fitting("coupling C",40.2,50.2,20,25,80,30.0)
        val cuts=mutableListOf(WorkshopPlans.Cut(left,300),WorkshopPlans.Cut(right,400))
        val fittings=mutableListOf(connector)
        val plan=WorkshopPlans.pvc(cuts,fittings,.5)
        cuts.clear();fittings.clear()
        assertEquals(listOf("tube A","tube B"),plan.cuts.map {it.pipe.id})
        assertEquals(listOf(300,400),plan.cuts.map {it.lengthMm})
        assertEquals(listOf("coupling C"),plan.fittings.map {it.id})
        assertEquals(20,plan.fittings.single().leftInsertionMm)
        assertEquals(25,plan.fittings.single().rightInsertionMm)
        assertEquals(735,plan.lengthMm)
        assertEquals(listOf(280),plan.fittingStartsMm)
        assertEquals(listOf(32.0,30.0,45.0),plan.bore.map {it.diameterMm})
    }
}
