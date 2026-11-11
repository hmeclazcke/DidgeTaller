package ar.didgetaller.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import ar.didgetaller.core.*
import ar.didgetaller.data.ConstructionPdf
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Synthetic document examples only. Both PDF pages are executed and rasterized by Android itself. */
class WorkshopDocumentsUiTest {
    private fun plans():List<ConstructionPlan> {
        val shape=AcousticModel.Shape(mouthMm=30,bellMm=83)
        val pvc=ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,30,40,1500\nb,70,80,1500",
            fittings="c,40.2,80.2,30,25,90,30,0:30|35:70",assembly="a,800,c\nb,650,-")
        return listOf(ConstructionOptions(),ConstructionOptions(method=ConstructionMethod.LAMINATED),pvc).map {
            ConstructionProjects.create("Synthetic workshop 1500",shape,it)
        }
    }
    @Test fun allThreeMethodsGenerateExactlyTwoLocalizedDimensionedSheets() {
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val directory=File(context.getExternalFilesDir(null),"construction-evidence/documents").apply {mkdirs()}
        for(plan in plans())for(language in WorkshopLanguage.entries) {
            val prefix="${language.name.lowercase()}-${plan.options.method.name.lowercase()}"
            val document=WorkshopDocument.create(plan,language,69.3)
            assertEquals(2,document.pages.size)
            assertTrue(document.text().contains(WorkshopDocument.number(plan.lengthMm)))
            val bytes=ConstructionPdf.create(plan,ConstructionPdf.Labels(language,69.3),false)
            val pdf=File(directory,"$prefix.pdf").apply {writeBytes(bytes)}
            renderAll(pdf,directory,prefix,2)
            File(directory,"$prefix.svg").writeText(WorkshopDocument.svg(document))
            File(directory,"$prefix.csv").writeText(WorkshopDocument.csv(plan,language))
        }
    }
    @Test fun tiledTemplateIsSeparateAndEveryPageHasMetricCalibration() {
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val directory=File(context.getExternalFilesDir(null),"construction-evidence/documents").apply {mkdirs()}
        val file=File(directory,"spanish-sandwich-tiles.pdf")
        file.writeBytes(ConstructionPdf.create(plans().first(),ConstructionPdf.Labels(WorkshopLanguage.SPANISH),true))
        renderAll(file,directory,"spanish-sandwich-tiles",10)
    }
    private fun renderAll(file:File,directory:File,prefix:String,count:Int) {
        ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->PdfRenderer(fd).use {renderer->
            assertEquals(count,renderer.pageCount)
            for(i in 0 until renderer.pageCount)renderer.openPage(i).use {page->
                val bitmap=Bitmap.createBitmap(page.width*2,page.height*2,Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                if(i==1 || count!=2) {
                    val y=(page.height-45)*2
                    for(x in 70..632)assertTrue("100 mm ruler must remain unbroken",Color.red(bitmap.getPixel(x,y))<190)
                }
                File(directory,"$prefix-page-${i+1}.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
        }}
    }
}
