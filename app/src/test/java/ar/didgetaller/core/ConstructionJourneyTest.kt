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
    @Test fun materialMustBeMeasuredBeforeValidationCanStart() {
        assertFalse(ConstructionJourney.available(fields(),2))
        assertFalse(ConstructionJourney.available(boards()+("stockConfirmed" to "false"),2))
        assertFalse(ConstructionJourney.available(boards()+("stockThickness" to ""),2))
        assertTrue(ConstructionJourney.available(boards(),2))
        assertTrue(ConstructionJourney.available(boards()+("stockWidth" to "30"),2))
        assertFalse(ConstructionJourney.available(fields()+("method" to "PVC"),2))
    }

    @Test fun savedIncompleteDraftsResumeWhereTheUserCanCorrectThem() {
        val draft = boards()+mapOf("stockWidth" to "", "_wizardStep" to "3")
        assertEquals(1, ConstructionJourney.restore(draft).step)
        assertEquals(0, ConstructionJourney.restore(draft+("geometry" to "")).step)
        val configured = ConstructionJourney(boards(), 1)
        assertEquals("false", configured.edit("stockKind","LOG").values["stockConfirmed"])
        assertEquals("true", configured.edit("stockKind","BOARDS").values["stockConfirmed"])
    }

    @Test fun materialErrorsPointToMeasuredPvcInputs() {
        val pvc = fields()+mapOf("method" to "PVC", "pipes" to "tube,30,40,1800", "assembly" to "tube,1500,-")
        assertNull(ConstructionMaterial.issue(pvc))
        assertTrue(ConstructionJourney.available(pvc,2))
        assertEquals("pipes",ConstructionMaterial.issue(pvc+("pipes" to "tube,14,40,1800")))
        assertEquals("fittings",ConstructionMaterial.issue(pvc+("fittings" to "invalid")))
        assertEquals("clearance",ConstructionMaterial.issue(pvc+("clearance" to "NaN")))
        assertEquals("assembly",ConstructionMaterial.issue(pvc+("assembly" to "other,1500,-")))
        assertEquals("interval",ConstructionMaterial.issue(pvc+("interval" to "5")))
        assertEquals("options",ConstructionMaterial.issue(pvc+("interval" to "")))
        assertNull(ConstructionMaterial.issue(fields()+("method" to "LAMINATED")))
    }

    @Test fun logAndBlockUseTheirOwnDimensionsAndKeepLegacyValues() {
        val values = boards()
        assertEquals("stockDiameter", ConstructionMaterial.issue(values+mapOf("stockKind" to "LOG","stockDiameter" to "0")))
        assertNull(ConstructionMaterial.issue(values+mapOf("stockKind" to "LOG","stockDiameter" to "120,5")))
        assertEquals("stockBlockThickness", ConstructionMaterial.issue(values+mapOf("stockKind" to "BLOCK","stockBlockThickness" to "NaN")))
        assertNull(ConstructionMaterial.issue(values+("stockKind" to "BLOCK")))
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
