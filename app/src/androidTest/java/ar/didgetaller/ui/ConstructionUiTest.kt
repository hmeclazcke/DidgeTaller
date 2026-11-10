package ar.didgetaller.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import ar.didgetaller.TallerViewModel
import ar.didgetaller.R
import ar.didgetaller.core.*
import ar.didgetaller.data.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** Real Compose and PDF execution on clean emulators; every persistent document is test-owned. */
class ConstructionUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var app:IsolatedApp
    private val exports=mutableMapOf<String,ByteArray>()
    @Before fun setup() {
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish"))
        app=IsolatedApp(InstrumentationRegistry.getInstrumentation().targetContext)
    }
    @After fun cleanup() {if(::app.isInitialized)app.clean()}
    private fun open() {
        val design=LocalStore.Design("synthetic-construction","Synthetic bore",AcousticModel.Shape())
        compose.setContent {MaterialTheme(colorScheme=appColors) {ConstructionWorkspace(design,{name,_,bytes->exports[name.substringAfterLast('.')]=bytes},app){}}}
        compose.onNodeWithTag("construction-workspace").assertIsDisplayed()
    }
    private fun click(tag:String) {compose.onNodeWithTag(tag).performScrollTo().performClick()}
    private fun tab(index:Int) {compose.onNodeWithTag("construction-step-$index").performScrollTo().performClick()}
    private fun field(key:String,value:String) {
        compose.onNodeWithTag("construction-$key").performScrollTo().performTextReplacement(value)
    }
    private fun ready() {compose.waitUntil(60000){compose.onAllNodesWithTag("construction-plan-summary").fetchSemanticsNodes().isNotEmpty()}}
    private fun export(format:String) {
        val key=if(format=="tiles")"pdf" else format
        exports.remove(key);click("construction-export-$format")
        compose.waitUntil(60000){exports.containsKey(key)}
        assertTrue(exports.getValue(key).size>100)
    }
    @Test fun completeWoodWorkflowSearchSaveToleranceAndAllExports() {
        val legacy=app.getSharedPreferences("local_data",0)
        legacy.edit().putString("designs","synthetic-historical-format").commit()
        val before=legacy.all.toMap()
        open();screenshot("target")
        field("target","73.42");tab(1);click("construction-search")
        val first="construction-candidate-0"
        compose.waitUntil(60000){compose.onAllNodesWithTag(first).fetchSemanticsNodes().any {it.size.height>0}}
        val candidate=compose.onNodeWithTag(first).performScrollTo()
        compose.waitUntil(10000){candidate.isDisplayed()}
        candidate.assertIsDisplayed()
        screenshot("candidates");candidate.performClick()
        compose.onNodeWithTag("construction-method-SANDWICH").assertIsSelected()
        screenshot("wood-method");tab(3);click("construction-generate");ready()
        screenshot("wood-plan");click("construction-save")
        assertEquals("SANDWICH",ConstructionStore(app).project("synthetic-construction")["method"])
        export("csv");assertTrue(exports.getValue("csv").toString(Charsets.UTF_8).contains("station"))
        export("svg");assertTrue(exports.getValue("svg").toString(Charsets.UTF_8).contains("100 mm"))
        export("pdf");val overviewPages=verifyPdf(exports.getValue("pdf"))
        val selected=ConstructionForm.shape(ConstructionStore(app).project("synthetic-construction"))
        export("tiles")
        assertEquals(overviewPages+kotlin.math.ceil(selected.lengthMm/150.0).toInt(),verifyPdf(exports.getValue("pdf"),overviewPages))
        click("construction-tolerance")
        compose.waitUntil(60000){compose.onAllNodesWithTag("construction-sensitivity").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(before,legacy.all)
    }
    @Test fun measuredPvcCatalogPersistsAndProducesCorrectAssemblyWithoutChangingLegacyDesigns() {
        open();tab(2);click("construction-method-PVC")
        field("pipes","a,32,40,1500\nb,70,80,1500")
        field("fittings","c,40.2,80.2,30,25,90,32,0:32|35:70")
        field("assembly","a,800,c\nb,650,-")
        compose.onNodeWithTag("construction-heading").assertIsDisplayed()
        screenshot("pvc-editor")
        tab(2);compose.onNodeWithTag("construction-pipes").performScrollTo().assertIsDisplayed()
        screenshot("pvc-catalog");click("construction-save-catalog")
        assertEquals("a,32,40,1500\nb,70,80,1500",ConstructionStore(app).catalog()["pipes"])
        tab(3);click("construction-generate");ready()
        compose.onNodeWithTag("construction-plan-summary").assertTextContains("1485.000",substring=true)
        screenshot("pvc-plan");export("csv")
        val csv=exports.getValue("csv").toString(Charsets.UTF_8)
        assertTrue(csv.contains("1485.000"));assertTrue(csv.contains("770"))
        click("construction-save")
        assertEquals("PVC",ConstructionStore(app).project("synthetic-construction")["method"])
        tab(2);click("construction-method-LAMINATED");field("layers","100")
        tab(3);click("construction-generate");ready();screenshot("laminated-plan");export("pdf")
        tab(2);field("layers","0");tab(3);click("construction-generate")
        compose.waitUntil(30000){compose.onAllNodesWithTag("construction-error").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("construction-save").assertDoesNotExist()
        assertEquals("PVC",ConstructionStore(app).project("synthetic-construction")["method"])
    }
    @Test fun malformedAndFutureDocumentsArePreservedAndNeverAuthorizeOverwrite() {
        val prefs=app.getSharedPreferences("construction_v1",0)
        for(value in listOf("{broken", "{\"schema\":99,\"designs\":{}}")) {
            prefs.edit().clear().putString("projects",value).commit()
            val store=ConstructionStore(app)
            try {store.project("test");fail("Must reject unreadable project")}catch (_:Exception) { }
            try {store.saveProject("test",mapOf("method" to "PVC"));fail("Must preserve document")}catch (_:Exception) { }
            assertEquals(value,prefs.getString("projects",null))
        }
    }
    @Test fun laboratoryOpensConstructionWorkspaceAndCloseReturnsToSameDestination() {
        val owners=ViewModelStore()
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("construction",vm)}
        try {
            compose.setContent {TallerApp(vm,{},{},{_,_,_->})}
            compose.onNode(hasContentDescription(compose.activity.getString(R.string.ui_laboratory)) and hasAnyAncestor(hasTestTag("main-navigation"))).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.ui_search)).performScrollTo().performClick()
            click("construction-open")
            compose.onNodeWithTag("construction-workspace").assertIsDisplayed()
            compose.onNodeWithText(compose.activity.getString(R.string.ui_close)).performClick()
            compose.onNodeWithTag("construction-workspace").assertDoesNotExist()
            compose.onNodeWithTag("lab-screen").assertExists()
        } finally {compose.runOnUiThread {owners.clear()}}
    }
    private fun verifyPdf(bytes:ByteArray,firstTile:Int?=null):Int {
        val file=File(app.filesDir,"test.pdf").apply {writeBytes(bytes)}
        ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->PdfRenderer(fd).use {renderer->
            assertTrue(renderer.pageCount>=2)
            pdfPage(renderer,0,"pdf")
            if(firstTile!=null)pdfPage(renderer,firstTile,"pdf-tile")
            return renderer.pageCount
        }}
    }
    private fun pdfPage(renderer:PdfRenderer,index:Int,name:String) {
        renderer.openPage(index).use {page->
            assertEquals(595,page.width);assertEquals(842,page.height)
            val bitmap=Bitmap.createBitmap(1190,1684,Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            // Reference endpoints 40 and 40+100*72/25.4 points, rasterized at 2x.
            for(x in 82..644)assertTrue("Continuous 100 mm scale ruler",Color.red(bitmap.getPixel(x,1570))<180)
            if(index>0)assertTrue("Next tile registration at 150 mm",(929..931).any {Color.red(bitmap.getPixel(it,180))<180})
            evidence(name,bitmap);bitmap.recycle()
        }
    }
    private fun screenshot(name:String) {
        compose.waitForIdle()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(100,3000)
        automation.takeScreenshot().let {image->evidence(name,image);image.recycle()}
    }
    private fun evidence(name:String,image:Bitmap) {
        val label=InstrumentationRegistry.getArguments().getString("evidenceLabel") ?: "default"
        val file=File(compose.activity.getExternalFilesDir(null),"construction-evidence/$label-$name.png")
        file.parentFile!!.mkdirs();file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private class IsolatedApp(context:Context):Application() {
        private val token="construction-test-${UUID.randomUUID()}"
        private val root=File(context.cacheDir,token).apply {check(mkdirs())}
        init {attachBaseContext(context)}
        override fun getFilesDir()=root
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("$token-$name",mode)
        fun clean() {listOf("construction_v1","local_data").forEach {baseContext.deleteSharedPreferences("$token-$it")};root.deleteRecursively()}
    }
}
