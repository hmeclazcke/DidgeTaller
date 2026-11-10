package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class ConstructionAnalysisTest {
    @Test fun selectedInputOwnsTheFinalPlanAndNumericalSensitivity() {
        val fields=ConstructionForm.defaults(AcousticModel.Shape())
        val plan=calculateConstruction(ConstructionRequest("owned",fields,"plan")){false}
        assertEquals("owned",plan.plan.name)
        assertTrue(plan.prediction.baseHz>0)
        assertTrue(plan.sensitivity.isEmpty())
        val tolerance=calculateConstruction(ConstructionRequest("owned",fields,"tolerance")){false}
        assertEquals(5+plan.plan.pieces.size,tolerance.sensitivity.size)
        val longer=tolerance.sensitivity.single {it.dimension=="L +5 mm"}.deltaHz!!
        assertTrue("Longer bore lowers resonance",longer<0)
        assertEquals(ConstructionProjects.predict(ConstructionProjects.perturb(plan.plan,lengthDeltaMm=5.0)).baseHz-plan.prediction.baseHz,longer,1e-9)
        assertEquals(1500.0,plan.plan.lengthMm,0.0)
        assertTrue(ConstructionDrawing.table(plan.plan).first().contains("0.000"))
    }
    @Test fun boundedCandidatesRespectManufacturingConstraintsAndPvcStock() {
        val form=ConstructionForm.defaults(AcousticModel.Shape())
        val search=calculateConstruction(ConstructionRequest("search",form,"search")){false}
        assertEquals(5,search.candidates.size)
        val pvc=form+mapOf("method" to "PVC","pipes" to "a,32,40,1500\nb,70,80,1500",
            "fittings" to "c,40.2,80.2,30,25,90,32,0:32|35:70","assembly" to "a,800,c\nb,650,-")
        val candidates=calculateConstruction(ConstructionRequest("pvc",pvc,"search")){false}
        assertEquals(5,candidates.candidates.size)
        val tolerance=calculateConstruction(ConstructionRequest("pvc",pvc,"tolerance")){false}
        assertNotNull(tolerance.sensitivity.single {it.dimension=="I -1 mm"}.deltaHz)
        try {calculateConstruction(ConstructionRequest("cancelled",pvc,"tolerance")){true};fail("Must cancel")}
        catch (_:CancellationException) { }
    }
    @Test fun explicitLayerTotalsFilterUnbuildableCandidatesRatherThanFailingWholeSearch() {
        val shapes=ConstructionSearch.search(AcousticModel.Shape(),ConstructionOptions(method=ConstructionMethod.LAMINATED,layers="500,500,500"),ConstructionTarget())
        assertEquals(3,shapes.size)
        assertTrue(shapes.all {it.plan.lengthMm==1500.0})
    }
}
