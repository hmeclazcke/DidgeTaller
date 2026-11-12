package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class ConstructionJourneyTest {
    private val shape=AcousticModel.Shape(mouthMm=30,bellMm=83)
    private fun fields()=ConstructionForm.defaults(shape)
    private fun boards()=fields()+mapOf("stockKind" to "BOARDS","stockLength" to "1500","stockWidth" to "120","stockThickness" to "60","stockConfirmed" to "true","stockFace" to "0","stockTrim" to "0","stockUncertainty" to "0","wall" to "9","tolerance" to "0")
    @Test fun laterStepsNeedRealPrerequisitesAndUnknownWoodDoesNotAuthorizeAPlan() {
        assertTrue(ConstructionJourney.available(fields(),1))
        assertFalse(ConstructionJourney.available(fields()+("geometry" to "invalid"),1))
        assertFalse(ConstructionJourney.available(fields(),3))
        val p=ConstructionProjects.create("Test",shape,ConstructionForm.options(fields()))
        assertFalse(ConstructionJourney.canPlan(p,false))
    }
    @Test fun validatedBoardsPermitAPlanButInsufficientStockDoesNot() {
        val valid=ConstructionProjects.create("Test",shape,ConstructionForm.options(boards()))
        assertTrue(ConstructionJourney.canPlan(valid,false))
        val bad=ConstructionProjects.create("Test",shape,valid.options.copy(woodStock=(valid.options.woodStock as WoodStock.Timber).copy(widthMm=50.0)))
        assertFalse(ConstructionJourney.canPlan(bad,false))
    }
    @Test fun aStraightPvcTubeCannotSilentlyReplaceAWideningBore() {
        val p=ConstructionProjects.create("PVC",shape,ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,30,40,1800",assembly="a,1500,-"))
        assertTrue(ConstructionJourney.deviation(p)>50)
        assertFalse(ConstructionJourney.canPlan(p,false))
        assertTrue(ConstructionJourney.canPlan(p,true))
        assertEquals(30.0,p.pieces.last().outletMm,0.0)
        assertEquals(83.0,p.ideal.diameter(1500),0.0)
    }
    @Test fun acceptedPlanAndProgressAreInvalidatedByAnyInputChange() {
        val state=ConstructionJourney(boards(),2,boards())
        assertTrue(state.accepted)
        val changed=state.edit("bell","100")
        assertFalse(changed.accepted)
        assertEquals("100",changed.values["bell"])
        assertEquals(state.values["stockWidth"],changed.values["stockWidth"])
        assertNotEquals(changed.values,state.values)
    }
    @Test fun oldAndNewProjectMetadataRestoreWithoutChangingGeometry() {
        val old=ConstructionJourney.restore(boards())
        assertEquals(0,old.step);assertFalse(old.accepted)
        val saved=ConstructionJourney.restore(boards()+mapOf("_wizardStep" to "3","_wizardAccepted" to "true"))
        assertEquals(3,saved.step);assertTrue(saved.accepted);assertEquals(boards(),saved.values)
        assertEquals(0,ConstructionJourney.restore(boards()+("_wizardStep" to "bad")).step)
        assertEquals(3,ConstructionJourney.restore(boards()+("_wizardStep" to "99")).step)
    }
    @Test fun readinessSeparatesMarginMinimumUnknownIncompatibleAndLegacy() {
        val valid=ConstructionProjects.create("Test",shape,ConstructionForm.options(boards()))
        assertEquals(ConstructionReadiness.READY,ConstructionJourney.readiness(valid,false))
        val stock=valid.options.woodStock as WoodStock.Timber
        val minimum=valid.copy(options=valid.options.copy(woodStock=stock.copy(widthMm=101.0,thicknessMm=50.5)))
        assertEquals(ConstructionReadiness.MINIMUM,ConstructionJourney.readiness(minimum,false))
        assertEquals(ConstructionReadiness.UNCONFIRMED,ConstructionJourney.readiness(valid.copy(options=valid.options.copy(woodStock=null)),false))
        assertEquals(ConstructionReadiness.INCOMPATIBLE,ConstructionJourney.readiness(valid.copy(options=valid.options.copy(woodStock=stock.copy(widthMm=40.0))),false))
        assertEquals(ConstructionReadiness.LEGACY,ConstructionJourney.readiness(valid.copy(options=valid.options.copy(method=ConstructionMethod.LAMINATED)),false))
    }

    @Test fun lengthMismatchRequiresApprovalEvenWhenTheOverlappingBoreMatches() {
        val straight=AcousticModel.Shape(mouthMm=30,middleMm=30,bellMm=30)
        val options=ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,30,40,1800",assembly="a,1500,-")
        val exact=ConstructionProjects.create("Pipe",straight,options)
        assertEquals(ConstructionReadiness.READY,ConstructionJourney.readiness(exact,false))
        val longer=ConstructionProjects.create("Pipe",straight,options.copy(assembly="a,1650,-"))
        assertEquals(0.0,ConstructionJourney.deviation(longer),0.0)
        assertEquals(ConstructionReadiness.ALTERNATIVE,ConstructionJourney.readiness(longer,false))
        assertEquals(ConstructionReadiness.ACCEPTED_ALTERNATIVE,ConstructionJourney.readiness(longer,true))
        assertTrue(ConstructionJourney.canPlan(longer,true))
        assertFalse(ConstructionJourney.available(fields()+("method" to "invalid"),2))
    }

}
