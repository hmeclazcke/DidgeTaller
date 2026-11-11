package ar.didgetaller.ui

import ar.didgetaller.core.*
import ar.didgetaller.data.ConstructionPdf
import org.junit.Assert.*
import org.junit.Test

class ConstructionExportsTest {
    @Test fun textFormatsExportTheActualSelectedPlanWithoutAndroidOrPersistence() {
        val p=ConstructionProjects.create("named",AcousticModel.Shape(),ConstructionOptions())
        val labels=ConstructionPdf.Labels()
        assertTrue(constructionBytes(ConstructionExport(p,"csv",labels)).toString(Charsets.UTF_8).contains("named"))
        assertTrue(constructionBytes(ConstructionExport(p,"svg",labels)).toString(Charsets.UTF_8).contains("<svg"))
        assertEquals(1500.0,p.lengthMm,0.0)
    }
}
