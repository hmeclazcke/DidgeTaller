package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class WorkshopAcousticsTest {
    @Test fun exactCylinderChainAgreesWithLegacyUniformBore() {
        val shape=AcousticModel.Shape(1500,40,40,40)
        val expected=AcousticModel.evaluate(shape)
        val actual=AcousticModel.evaluateCylinders(List(72){AcousticModel.Cylinder(1500.0/72,40.0)})
        assertEquals(expected.baseHz,actual.baseHz,1e-8)
        expected.impedance.zip(actual.impedance).forEach {(a,b)->assertEquals(a.toDouble(),b.toDouble(),1e-6)}
    }
    @Test fun subdivisionPreservesAbruptTubeTransition() {
        val coarse=AcousticModel.evaluateCylinders(listOf(AcousticModel.Cylinder(900.0,32.0),AcousticModel.Cylinder(600.0,70.0)))
        val fine=AcousticModel.evaluateCylinders(List(9){AcousticModel.Cylinder(100.0,32.0)}+List(6){AcousticModel.Cylinder(100.0,70.0)})
        assertEquals(coarse.baseHz,fine.baseHz,1e-8)
    }
}
