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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.semantics.getOrNull
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
    private fun hideKeyboard() {
        // Target the focused dialog/window, not Espresso's background Activity root.
        compose.runOnUiThread {
            val manager=compose.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            val roots=if(Build.VERSION.SDK_INT>=29)android.view.inspector.WindowInspector.getGlobalWindowViews() else listOf(compose.activity.window.decorView)
            roots.filter {it.hasWindowFocus()}.forEach {
                manager.hideSoftInputFromWindow(it.windowToken,0)
            }
        }
        compose.waitForIdle()
    }
    private fun click(tag:String) {
        hideKeyboard()
        val node=compose.onNodeWithTag(tag)
        if(!node.isDisplayed())node.performScrollTo()
        node.assertIsDisplayed().performClick()
    }
    private fun tab(index:Int) {
        hideKeyboard()
        val node=compose.onNodeWithTag("construction-step-$index")
        node.performScrollTo()
        val vertical=compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange) and hasAnyDescendant(hasTestTag("construction-step-$index")))
        if(vertical.fetchSemanticsNodes().isNotEmpty())vertical.onFirst().performScrollToNode(hasTestTag("construction-step-$index"))
        node.assertIsDisplayed().performClick()
        compose.waitUntil(5000){node.fetchSemanticsNode().config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected)==true}
        node.assertIsSelected()
    }
    private fun field(key:String,value:String) {
        compose.onNodeWithTag("construction-$key").performScrollTo().performTextReplacement(value)
        hideKeyboard()
    }
    private fun ready() {compose.waitUntil(60000){compose.onAllNodesWithTag("construction-plan-summary").fetchSemanticsNodes().isNotEmpty()}}
    private fun export(format:String) {
        val key=if(format=="tiles")"pdf" else format
        exports.remove(key);click("construction-export-$format")
        compose.waitUntil(60000){exports.containsKey(key)}
        assertTrue(exports.getValue(key).size>100)
    }
    @Test fun validationPublishesForCurrentGeometry() {
        open();tab(1);click("stock-kind-BOARDS");click("stock-confirmed")
        try {
            tab(2);ready()
            compose.onNodeWithTag("construction-step-3").assertIsEnabled()
            screenshot("validation")
        } catch(error:Throwable) {
            android.util.Log.e("WizardRegression","Validation failure before teardown",error)
            throw error
        }
    }
    @Test fun completeWoodWorkflowSearchSaveToleranceAndAllExports() {
        LocalStore(app).saveDesign("Original",AcousticModel.Shape())
        val original=LocalStore(app).designs().single()
        open();screenshot("target")
        field("planName","Workshop draft");field("target","73.42")
        click("wizard-save-draft")
        compose.onNodeWithTag("construction-step-0").assertIsSelected()
        tab(1);click("stock-kind-BOARDS");click("stock-confirmed")
        screenshot("wood-method");tab(2);ready()
        click("construction-search")
        val first="construction-candidate-0"
        compose.waitUntil(60000){compose.onAllNodesWithTag(first).fetchSemanticsNodes().any {it.size.height>0}}
        screenshot("candidates");click(first);ready();tab(3)
        screenshot("wood-plan");click("wizard-save-draft")
        assertEquals("SANDWICH",ConstructionStore(app).project("synthetic-construction")["method"])
        export("csv");assertTrue(exports.getValue("csv").toString(Charsets.UTF_8).contains("mm"))
        compose.onNodeWithTag("construction-export-pdf").performScrollTo().assertTextEquals(compose.activity.getString(R.string.construction_pdf))
        screenshot("export-options")
        export("svg");assertTrue(exports.getValue("svg").toString(Charsets.UTF_8).contains("100 mm"))
        export("pdf");val overviewPages=verifyPdf(exports.getValue("pdf"))
        val selected=ConstructionForm.shape(ConstructionStore(app).project("synthetic-construction"))
        export("tiles")
        assertEquals(kotlin.math.ceil(selected.lengthMm/150.0).toInt(),verifyPdf(exports.getValue("pdf"),0))
        click("construction-tolerance")
        compose.waitUntil(60000){compose.onAllNodesWithTag("construction-sensitivity").fetchSemanticsNodes().isNotEmpty()}
        val previousSvg=exports.getValue("svg").toString(Charsets.UTF_8)
        tab(0);click("construction-advanced");field("geometry","0,32\n1020,43\n1500,95")
        compose.onNodeWithTag("construction-step-3").assertIsNotEnabled()
        tab(2);ready();tab(3);export("svg")
        val changedSvg=exports.getValue("svg").toString(Charsets.UTF_8)
        assertNotEquals(previousSvg,changedSvg);assertTrue(changedSvg.contains("95 mm"))
        click("wizard-save-draft")
        assertEquals("73.42",ConstructionStore(app).project("synthetic-construction")["target"])
        assertTrue(original in LocalStore(app).designs());assertEquals(2,LocalStore(app).designs().size)
    }
    @Test fun measuredPvcCatalogPersistsAndProducesCorrectAssemblyWithoutChangingLegacyDesigns() {
        open();tab(1);click("construction-method-PVC")
        field("pipes","a,32,40,1500\nb,70,80,1500")
        field("fittings","c,40.2,80.2,30,25,90,32,0:32|35:70")
        field("assembly","a,800,c\nb,650,-")
        val heading=compose.onNodeWithTag("construction-heading")
        if(!heading.isDisplayed())heading.performScrollTo()
        heading.assertIsDisplayed()
        screenshot("pvc-editor")
        tab(1);compose.onNodeWithTag("construction-pipes").performScrollTo().assertIsDisplayed()
        screenshot("pvc-catalog");click("construction-save-catalog")
        assertEquals("a,32,40,1500\nb,70,80,1500",ConstructionStore(app).catalog()["pipes"])
        tab(2);ready()
        compose.onNodeWithTag("construction-step-3").assertIsNotEnabled()
        compose.onNodeWithTag("construction-approximation").performScrollTo().assertIsDisplayed()
        click("construction-accept-alternative");tab(3)
        compose.onNodeWithTag("construction-plan-summary").assertTextContains("1485 mm",substring=true)
        screenshot("pvc-plan");export("csv")
        val csv=exports.getValue("csv").toString(Charsets.UTF_8)
        assertTrue(csv.contains("1485.0"));assertTrue(csv.contains("770"))
        click("wizard-save-draft")
        assertEquals("PVC",ConstructionStore(app).project("synthetic-construction")["method"])
        tab(1);compose.onNodeWithTag("construction-method-LAMINATED").assertDoesNotExist()
        field("pipes","invalid");tab(2);click("construction-validate")
        compose.waitUntil(30000){compose.onAllNodesWithTag("construction-error").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("construction-step-3").assertIsNotEnabled()
        assertEquals("PVC",ConstructionStore(app).project("synthetic-construction")["method"])
    }
    @Test fun woodStockInputsPersistAndConflictsDoNotRewriteTheSavedBore() {
        open()
        field("planName","Río claro")
        tab(1);click("stock-kind-LOG")
        field("stockDiameter","130")
        val language=if(compose.activity.resources.configuration.locales[0].language=="es")WorkshopLanguage.SPANISH else WorkshopLanguage.ENGLISH
        compose.onNodeWithTag("stock-confirmed").assertContentDescriptionEquals(language.wood(WoodTerm.CONFIRM))
        click("stock-confirmed")
        compose.onNodeWithTag("stock-confirmed").assertIsOn()
        compose.onNodeWithTag("stock-fit-FITS").performScrollTo().assertIsDisplayed()
        screenshot("log-stock")
        click("stock-kind-BOARDS")
        field("stockWidth","130");field("stockThickness","70")
        screenshot("board-stock")
        tab(2);ready();tab(3);click("wizard-save-draft")
        val stored=ConstructionStore(app).project("synthetic-construction")
        assertEquals("Río claro",stored["planName"])
        assertEquals("BOARDS",stored["stockKind"])
        assertEquals("130",stored["stockWidth"])
        assertEquals(ConstructionForm.defaults(AcousticModel.Shape())["geometry"],stored["geometry"])
        export("pdf");verifyPdf(exports.getValue("pdf"))
        tab(1);field("stockWidth","40")
        compose.onNodeWithTag("stock-fit-DOES_NOT_FIT").performScrollTo().assertIsDisplayed()
        screenshot("stock-conflict")
        assertEquals(stored,ConstructionStore(app).project("synthetic-construction"))
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
            compose.onNodeWithTag("construction-workspace").assertIsDisplayed()
            compose.onNodeWithText(compose.activity.getString(R.string.ui_close)).performClick()
            compose.onNodeWithTag("construction-workspace").assertDoesNotExist()
            compose.onNodeWithTag("lab-home").assertExists()
        } finally {compose.runOnUiThread {owners.clear()}}
    }
    @Test fun laboratoryInvalidDraftKeepsTheStoredCanonicalBoreOnSave() {
        val shape=AcousticModel.Shape(bellMm=90)
        LocalStore(app).saveDesign("inverse-workspace","Saved bore",shape)
        ConstructionStore(app).saveProject("inverse-workspace",ConstructionForm.defaults(shape)+("geometry" to "unfinished"))
        val owners=ViewModelStore();lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("unfinished-draft",vm)}
        try {
            compose.setContent {TallerApp(vm,{},{},{_,_,_->})}
            compose.onNode(hasContentDescription(compose.activity.getString(R.string.ui_laboratory)) and hasAnyAncestor(hasTestTag("main-navigation"))).performClick()
            compose.onNodeWithTag("construction-step-1").assertIsNotEnabled()
            click("wizard-save-draft")
            assertEquals(shape,LocalStore(app).designs().single().shape)
            assertEquals("unfinished",ConstructionStore(app).project("inverse-workspace")["geometry"])
        } finally {compose.runOnUiThread {owners.clear()}}
    }
    @Test fun legacyLaboratoryProjectRemainsAccessibleWithoutMigration() {
        val values=ConstructionForm.defaults(AcousticModel.Shape())+mapOf("planName" to "Earlier draft","stockKind" to "BLOCK","stockBlockThickness" to "160")
        ConstructionStore(app).saveProject("inverse-workspace",values)
        val owners=ViewModelStore()
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("legacy-draft",vm)}
        try {
            compose.setContent {TallerApp(vm,{},{},{_,_,_->})}
            compose.onNode(hasContentDescription(compose.activity.getString(R.string.ui_laboratory)) and hasAnyAncestor(hasTestTag("main-navigation"))).performClick()
            compose.onNodeWithTag("construction-planName").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("Earlier draft")))
            tab(1);compose.onNodeWithTag("stock-kind-BLOCK").assertIsSelected()
            assertEquals(values,ConstructionStore(app).project("inverse-workspace"))
            assertTrue(LocalStore(app).designs().isEmpty())
        } finally {compose.runOnUiThread {owners.clear()}}
    }
    @Test fun laboratoryDraftSurvivesTabChangesWithoutImplicitSaving() {
        val owners=ViewModelStore()
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("draft-navigation",vm)}
        fun section(id:Int)=compose.onNode(hasContentDescription(compose.activity.getString(id)) and hasAnyAncestor(hasTestTag("main-navigation")))
        try {
            compose.setContent {TallerApp(vm,{},{},{_,_,_->})}
            section(R.string.ui_laboratory).performClick()
            field("planName","My instrument");tab(1);click("stock-kind-BOARDS");field("stockWidth","139")
            section(R.string.ui_library).performClick()
            assertTrue(LocalStore(app).designs().isEmpty())
            section(R.string.ui_laboratory).performClick()
            compose.onNodeWithTag("construction-step-1").assertIsSelected()
            compose.onNodeWithTag("construction-stockWidth").performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("139")))
            click("wizard-save-draft");assertEquals(1,LocalStore(app).designs().size)
            tab(0);screenshot("laboratory-wizard")
            val id=LocalStore(app).designs().single().id
            section(R.string.ui_library).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.ui_designs)).performClick()
            click("construction-design-$id")
            compose.onNodeWithTag("construction-step-1").assertIsSelected()
            compose.onNodeWithTag("construction-stockWidth").performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("139")))
            compose.onNodeWithText(compose.activity.getString(R.string.ui_close)).performClick()
            section(R.string.ui_laboratory).performClick()
            field("planName","Unsaved edit")
            androidx.test.espresso.Espresso.pressBackUnconditionally()
            compose.onNodeWithText(compose.activity.getString(R.string.wizard_unsaved)).assertIsDisplayed()
            compose.onNodeWithText(compose.activity.getString(R.string.wizard_keep_editing)).performClick()
            compose.onNodeWithTag("construction-workspace").assertIsDisplayed()
            assertEquals("My instrument",LocalStore(app).designs().single().name)
        } finally {compose.runOnUiThread {owners.clear()}}
    }
    @Test fun unsavedMaterialAndStepSurviveSavedStateRestorationWithoutCreatingADesign() {
        val restoration=StateRestorationTester(compose)
        val design=LocalStore.Design("restore-draft","Draft",AcousticModel.Shape())
        restoration.setContent {MaterialTheme(colorScheme=appColors) {ConstructionWorkspace(design,{_,_,_->},app){}}}
        tab(1);click("stock-kind-BOARDS");field("stockWidth","137")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("construction-step-1").assertIsSelected()
        compose.onNodeWithTag("construction-stockWidth").performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("137")))
        assertTrue(LocalStore(app).designs().isEmpty())
        click("wizard-save-draft");click("wizard-save-draft")
        assertEquals(1,LocalStore(app).designs().size)
        assertEquals("137",ConstructionStore(app).project(design.id)["stockWidth"])
    }
    @Test fun libraryResumesIncompleteAndCompletedWizardWithoutDuplicateDesigns() {
        val shape=AcousticModel.Shape()
        LocalStore(app).saveDesign("resume-id","Before",shape)
        ConstructionStore(app).saveProject("resume-id",ConstructionForm.defaults(shape)+mapOf("_wizardStep" to "1","stockKind" to "BOARDS","stockConfirmed" to "true","target" to "80"))
        val owners=ViewModelStore()
        lateinit var vm:TallerViewModel
        compose.runOnUiThread {vm=TallerViewModel(app);owners.put("wizard",vm)}
        try {
            compose.setContent {TallerApp(vm,{},{},{_,_,_->})}
            compose.onNode(hasContentDescription(compose.activity.getString(R.string.ui_library)) and hasAnyAncestor(hasTestTag("main-navigation"))).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.ui_designs)).performClick()
            click("construction-design-resume-id")
            compose.onNodeWithTag("construction-step-1").assertIsSelected()
            tab(0);compose.onNodeWithTag("construction-target").performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("80")))
            field("planName","Revised");click("wizard-save-draft")
            tab(2);ready();tab(3);click("wizard-save-draft")
            compose.onNodeWithText(compose.activity.getString(R.string.ui_close)).performClick()
            click("construction-design-resume-id")
            compose.onNodeWithTag("construction-step-3").assertIsSelected();ready()
            androidx.test.espresso.Espresso.pressBackUnconditionally()
            compose.onNodeWithTag("construction-step-2").assertIsSelected()
            assertEquals(1,LocalStore(app).designs().size)
            assertEquals("Revised",LocalStore(app).designs().single().name)
            assertEquals("80",ConstructionStore(app).project("resume-id")["target"])
            screenshot("library-resume")
        } finally {compose.runOnUiThread {owners.clear()}}
    }
    @Test fun spanishLargeFontLogCanReachPlanAndExport()=accessiblePlan("es-AR","LOG")
    @Test fun englishLargeFontBoardsCanReachPlanAndExport()=accessiblePlan("en","BOARDS")
    private fun accessiblePlan(language:String,stock:String) {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val configuration=android.content.res.Configuration(target.resources.configuration).apply {
            setLocales(android.os.LocaleList(java.util.Locale.forLanguageTag(language)));fontScale=1.5f
        }
        val resources=target.createConfigurationContext(configuration).resources
        val context=object:android.content.ContextWrapper(compose.activity) {
            override fun getResources()=resources
        }
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides context,
                androidx.compose.ui.platform.LocalConfiguration provides configuration,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(target.resources.displayMetrics.density,1.5f)) {
                MaterialTheme(colorScheme=appColors) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        ConstructionWorkspace(LocalStore.Design("accessible-plan","",AcousticModel.Shape()),{name,_,bytes->exports[name.substringAfterLast('.')]=bytes},app,embedded=true){}
                    }
                }
            }
        }
        compose.onNodeWithTag("construction-step-3").assertIsNotEnabled()
        compose.onNodeWithTag("construction-step-0").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,resources.getString(R.string.wizard_current)))
        screenshot("$language-large-design")
        tab(1);click("stock-kind-$stock");click("stock-confirmed")
        if(stock=="LOG")field("stockDiameter","140")
        tab(2);ready();tab(3)
        compose.onNodeWithTag("construction-export-pdf").performScrollTo().assertIsDisplayed()
        screenshot("$language-large-plan");export("pdf");verifyPdf(exports.getValue("pdf"))
        export("svg");assertTrue(exports.getValue("svg").toString(Charsets.UTF_8).contains("100 mm"))
    }
    @Test fun replacementDesignWithIdenticalInputsValidatesItsOwnPlan() {
        val shape=AcousticModel.Shape()
        val values=ConstructionForm.defaults(shape)+mapOf("_wizardStep" to "3","stockKind" to "BOARDS","stockConfirmed" to "true")
        val store=ConstructionStore(app)
        store.saveProject("first",values);store.saveProject("second",values)
        val selected=androidx.compose.runtime.mutableStateOf("first")
        compose.setContent {MaterialTheme(colorScheme=appColors) {
            ConstructionWorkspace(LocalStore.Design(selected.value,selected.value,shape),{_,_,_->},app){}
        }}
        ready();compose.runOnIdle {selected.value="second"}
        compose.waitUntil(5000){compose.onAllNodesWithTag("construction-plan-summary").fetchSemanticsNodes().any {
            it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.any {text->text.text.contains("second")}==true
        }}
        compose.onNodeWithTag("construction-plan-summary").assertTextContains("second",substring=true)
    }
    @Test fun replacementLifecycleOwnerDoesNotDisposeTheSurvivingDraft() {
        lateinit var first:androidx.lifecycle.LifecycleOwner
        lateinit var second:androidx.lifecycle.LifecycleOwner
        compose.runOnUiThread {
            fun owner()=object:androidx.lifecycle.LifecycleOwner {
                override val lifecycle=androidx.lifecycle.LifecycleRegistry(this).apply {currentState=androidx.lifecycle.Lifecycle.State.RESUMED}
            }
            first=owner();second=owner()
        }
        val active=androidx.compose.runtime.mutableStateOf(first)
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.lifecycle.compose.LocalLifecycleOwner provides active.value) {
                MaterialTheme(colorScheme=appColors) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        ConstructionWorkspace(LocalStore.Design("lifecycle-draft","Draft",AcousticModel.Shape()),{_,_,_->},app,embedded=true){}
                    }
                }
            }
        }
        tab(1);click("stock-kind-BOARDS");click("stock-confirmed");tab(2);ready()
        compose.runOnIdle {active.value=second}
        compose.onNodeWithTag("construction-plan-summary").assertExists()
        tab(0);field("target","80");tab(2);ready()
    }
    @Test fun unreadableCatalogDoesNotAuthorizeReplacingAnExistingProject() {
        val store=ConstructionStore(app)
        store.saveProject("synthetic-construction",mapOf("planName" to "Preserve me"))
        val prefs=app.getSharedPreferences("construction_v1",0)
        val original=prefs.getString("projects",null)
        prefs.edit().putString("catalog","{broken").commit()
        open();click("wizard-save-draft")
        compose.onNodeWithTag("construction-error").assertExists()
        assertEquals(original,prefs.getString("projects",null))
        assertTrue(LocalStore(app).designs().isEmpty())
    }
    private fun verifyPdf(bytes:ByteArray,firstTile:Int?=null):Int {
        val file=File(app.filesDir,"test.pdf").apply {writeBytes(bytes)}
        ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->PdfRenderer(fd).use {renderer->
            if(firstTile==null)assertEquals(2,renderer.pageCount) else assertTrue(renderer.pageCount>2)
            pdfPage(renderer,if(firstTile==null)1 else 0,"pdf")
            if(firstTile!=null)pdfPage(renderer,firstTile,"pdf-tile")
            return renderer.pageCount
        }}
    }
    private fun pdfPage(renderer:PdfRenderer,index:Int,name:String) {
        renderer.openPage(index).use {page->
            assertTrue(page.width in listOf(595,842));assertTrue(page.height in listOf(595,842))
            val bitmap=Bitmap.createBitmap(page.width*2,page.height*2,Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            val rulerY=(page.height-45)*2
            for(x in 70..632)assertTrue("Continuous 100 mm scale ruler",Color.red(bitmap.getPixel(x,rulerY))<180)
            evidence(name,bitmap);bitmap.recycle()
        }
    }
    private fun screenshot(name:String) {
        hideKeyboard()
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
