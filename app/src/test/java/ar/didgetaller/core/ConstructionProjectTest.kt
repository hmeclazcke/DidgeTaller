package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class ConstructionProjectTest {
    @Test fun woodAndLayerPlansShareIdealButPreserveManufacturedSteps() {
        val shape=AcousticModel.Shape()
        val split=ConstructionProjects.create("wood",shape,ConstructionOptions())
        val layers=ConstructionProjects.create("layers",shape,ConstructionOptions(method=ConstructionMethod.LAMINATED,layers="100"))
        assertEquals(1500.0,split.lengthMm,0.0)
        assertEquals(1500.0,layers.lengthMm,0.0)
        assertEquals(15,layers.pieces.size)
        assertEquals(2,split.pieces.size)
        assertTrue(layers.pieces.all {it.inletMm==it.outletMm})
        assertEquals(shape,split.ideal)
        assertEquals(1500.0,ConstructionProjects.cylinders(split).sumOf {it.lengthMm},1e-8)
        assertTrue(ConstructionProjects.predict(split).baseHz>30)
        assertEquals(1505.0,ConstructionProjects.perturb(split,lengthDeltaMm=5.0).lengthMm,0.0)
        assertEquals(33.0,ConstructionProjects.perturb(split,1.0).pieces.first().inletMm,0.0)
    }
    @Test fun measuredReducerUsesItsVariablePassageAndFullSocketOverlaps() {
        val o=ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,32,40,1500\nb,70,80,1500",
            fittings="c,40.2,80.2,30,25,90,32,0:32|35:70",assembly="a,800,c\nb,650,-")
        val p=ConstructionProjects.create("pvc",AcousticModel.Shape(),o)
        assertEquals(1485.0,p.lengthMm,0.0)
        assertEquals(listOf(800.0,35.0,650.0),p.pieces.map {it.lengthMm})
        assertEquals(32.0,p.pieces[1].inletMm,0.0)
        assertEquals(70.0,p.pieces[1].outletMm,0.0)
        assertTrue(p.parts.last().contains("x=770"))
        assertTrue(ConstructionProjects.predict(p).baseHz>30)
        val short=ConstructionSearch.shortInsertion(p,1.0)
        assertEquals(1486.0,short.lengthMm,0.0)
        assertEquals(40.2,short.pieces[1].inletMm,0.0)
        assertEquals(35.0,short.pieces[2].lengthMm,0.0)
        assertNotNull(ConstructionSearch.shift(ConstructionProjects.predict(p).baseHz){short})
        assertNull(ConstructionSearch.shift(100.0){ConstructionProjects.perturb(p,200.0)})
    }
    @Test fun invalidCatalogDoesNotProduceAPlan() {
        reject {ConstructionCatalog.pipes("x,32,40,1000\nx,40,50,1000")}
        reject {ConstructionCatalog.connectors("c,40,40,20,20,60,32,0:32|19:32")}
        reject {ConstructionCatalog.connectors("c,40,40,20,20,60,31,0:32|20:32")}
        reject {ConstructionProjects.create("bad",AcousticModel.Shape(),ConstructionOptions(method=ConstructionMethod.PVC))}
        reject {ConstructionProjects.create("bad",AcousticModel.Shape(),ConstructionOptions(layers="0",method=ConstructionMethod.LAMINATED))}
    }
    private fun reject(block:()->Unit) {
        try {block();fail("Invalid construction input must fail")} catch (_:IllegalArgumentException) { }
    }
}
