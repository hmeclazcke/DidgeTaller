package ar.didgetaller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ar.didgetaller.R
import ar.didgetaller.core.*
import ar.didgetaller.data.*

internal data class ConstructionExport(val plan:ConstructionPlan,val format:String,val labels:ConstructionPdf.Labels)

internal fun constructionBytes(r:ConstructionExport):ByteArray = when(r.format) {
    "csv"->WorkshopDocument.csv(r.plan,r.labels.language).toByteArray(Charsets.UTF_8)
    "svg"->WorkshopDocument.svg(WorkshopDocument.create(r.plan,r.labels.language,r.labels.targetHz)).toByteArray(Charsets.UTF_8)
    else->ConstructionPdf.create(r.plan,r.labels,r.format=="tiles")
}

private val journeySaver=mapSaver(
    save={j:ConstructionJourney->j.values.mapKeys {"value:"+it.key}+mapOf("step" to j.step,"accepted" to j.accepted)},
    restore={saved->
        val values=saved.filterKeys {it.startsWith("value:")}.mapKeys {it.key.removePrefix("value:")}.mapValues {it.value as String}
        ConstructionJourney(values,saved["step"] as Int,if(saved["accepted"]==true)values else null)
    })

/** Saveable input state outlives composition; rendering never owns a second draft. */
@Composable
internal fun ConstructionWorkspace(design:LocalStore.Design,export:(String,String,ByteArray)->Unit,storageContext:android.content.Context?=null,embedded:Boolean=false,dismiss:()->Unit) {
    val context=storageContext ?: LocalContext.current
    val store=remember(context){ConstructionStore(context)}
    val load=remember(design.id){runCatching {ConstructionForm.defaults(design.shape)+store.catalog()+store.project(design.id)}}
    val initial=remember(design.id){ConstructionJourney.restore(load.getOrElse {ConstructionForm.defaults(design.shape)})}
    val draft=rememberSaveable(design.id,stateSaver=journeySaver){mutableStateOf(initial)}
    val scope=rememberCoroutineScope()
    val editor=remember(design.id,context){ConstructionEditor(context,design,draft,initial,load,scope)}
    val language=exportLanguage()
    val journey=draft.value
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(editor){onDispose {editor.dispose()}}
    DisposableEffect(lifecycle,editor) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP)editor.cancel()}
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(editor,journey.step,journey.values){editor.prepare(language)}
    ConstructionExportEffect(editor.exported,editor::acknowledgeExport,export)
    val scroll=rememberScrollState()
    LaunchedEffect(journey.step){scroll.scrollTo(0)}
    ConstructionWindow(embedded,{editor.back(dismiss)}) {
        val focus=LocalFocusManager.current
        val select:(Int)->Unit={focus.clearFocus();editor.select(it)}
        Surface(Modifier.fillMaxSize().testTag("construction-workspace")) {
            BoxWithConstraints(Modifier.fillMaxSize().then(if(embedded)Modifier else Modifier.safeDrawingPadding())) {
                val compact=maxHeight<360.dp
                Column(Modifier.fillMaxSize().then(if(compact)Modifier.verticalScroll(scroll) else Modifier)) {
                ConstructionHeader(journey.step,editor::available,select,{editor.save(language)},{editor.requestClose(dismiss)})
                Column(Modifier.then(if(compact)Modifier else Modifier.weight(1f).verticalScroll(scroll)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    ConstructionStatus(load.isFailure,editor.failed,editor.saved,editor.analysis,editor.exported)
                    ConstructionStep(editor,language)
                    ConstructionWorkers(editor.analysis,editor.exported,editor::cancel)
                    ConstructionHelp()
                }
                ConstructionFooter(journey.step,editor::available,select)
                }
            }
        }
        if(editor.leaving)ConstructionLeaveDialog(editor,language,dismiss)
    }
}

@Composable
private fun ConstructionWindow(embedded:Boolean,back:()->Unit,content:@Composable ()->Unit) {
    if(embedded) {
        BackHandler(onBack=back)
        content()
    } else Dialog(onDismissRequest=back,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        BackHandler(onBack=back)
        content()
    }
}

@Composable
private fun ConstructionHelp() {
    var expanded by remember {mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded}){Text(stringResource(R.string.ui_guide))}
    if(expanded)Text(stringResource(R.string.construction_uncertainty),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConstructionStep(editor:ConstructionEditor,language:WorkshopLanguage) {
    val journey=editor.draft.value
    val run:(String)->Unit={editor.run(it,language)}
    when(journey.step) {
        0->ConstructionTargetFields(journey.values,editor::set,editor.name(language))
        1->ConstructionMethodFields(journey.values,editor::set,editor::saveCatalog,editor::loadCatalog,editor.load.isSuccess)
        2->ConstructionValidation(editor,run)
        else->if(editor.canPlan)ConstructionPlanActions(editor.analysis,editor.exported is AnalysisState.Loading,run){plan,format->
            editor.export(plan,format,ConstructionPdf.Labels(language,journey.values["target"]?.toDoubleOrNull()))
        } else Text(stringResource(R.string.wizard_unavailable))
    }
}

@Composable
private fun ConstructionValidation(editor:ConstructionEditor,run:(String)->Unit) {
    Button(onClick={run("plan")},modifier=Modifier.testTag("construction-validate")){Text(stringResource(R.string.wizard_validate))}
    val result=editor.current
    if(result==null)Text(stringResource(R.string.wizard_validation_help)) else {
        ConstructionReadinessLabel(result.plan,editor.draft.value.accepted)
        Text(stringResource(R.string.wizard_predictions),style=MaterialTheme.typography.titleSmall)
        ConstructionPlanView(result)
        if(result.plan.options.method==ConstructionMethod.PVC && !ConstructionJourney.canPlan(result.plan,false))ConstructionApproval(editor,result.plan)
    }
    ConstructionCandidates(editor.analysis.value?.candidates.orEmpty(),{run("search")},editor::choose,{run("plan")})
}

@Composable
private fun ConstructionApproval(editor:ConstructionEditor,plan:ConstructionPlan) {
    Text(stringResource(R.string.wizard_pvc_alternative),modifier=Modifier.testTag("construction-approximation"))
    Text(stringResource(R.string.wizard_difference,exportLanguage().dimension(ConstructionJourney.deviation(plan)),exportLanguage().dimension(plan.lengthMm-plan.ideal.lengthMm)))
    Row {Checkbox(editor.draft.value.accepted,editor::approve,Modifier.testTag("construction-accept-alternative"));Text(stringResource(R.string.wizard_accept))}
}

@Composable
private fun ConstructionFooter(step:Int,available:(Int)->Boolean,select:(Int)->Unit) {
    FlowRow(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        if(step>0)TextButton(onClick={select(step-1)}){Text(stringResource(R.string.wizard_back))}
        if(step<3)Button(onClick={select(step+1)},enabled=available(step+1),modifier=Modifier.testTag("construction-continue")){Text(stringResource(R.string.wizard_continue))}
    }
    if(step<3 && !available(step+1))Text(stringResource(R.string.wizard_unavailable),Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConstructionLeaveDialog(editor:ConstructionEditor,language:WorkshopLanguage,dismiss:()->Unit) {
    AlertDialog(onDismissRequest={editor.leaving=false},title={Text(stringResource(R.string.wizard_unsaved))},
        confirmButton={TextButton(onClick={if(editor.save(language))dismiss()}){Text(stringResource(R.string.ui_save))}},
        dismissButton={FlowRow {TextButton(onClick={editor.leaving=false}){Text(stringResource(R.string.wizard_keep_editing))};TextButton(onClick=dismiss){Text(stringResource(R.string.wizard_leave))}}})
}

@Composable
private fun ConstructionHeader(stage:Int,available:(Int)->Boolean,select:(Int)->Unit,save:()->Unit,dismiss:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
        Text(stringResource(R.string.construction_title),Modifier.weight(1f).testTag("construction-heading"),style=MaterialTheme.typography.titleMedium)
        TextButton(onClick=save,modifier=Modifier.testTag("wizard-save-draft")){Text(stringResource(R.string.ui_save))}
        TextButton(onClick=dismiss){Text(stringResource(R.string.ui_close))}
    }
    ScrollableTabRow(stage,edgePadding=0.dp) {
        listOf(R.string.wizard_design,R.string.wizard_material,R.string.wizard_validation,R.string.construction_open).forEachIndexed {i,title->
            val status=stringResource(if(i==stage)R.string.wizard_current else if(!available(i))R.string.wizard_unavailable else if(i<stage)R.string.wizard_completed else R.string.wizard_available)
            Tab(stage==i,{select(i)},enabled=available(i),text={Text(stringResource(title))},modifier=Modifier.testTag("construction-step-$i").semantics {stateDescription=status})
        }
    }
}

@Composable
private fun ConstructionExportEffect(exported:AnalysisState<ConstructionExport,ByteArray>,acknowledge:()->Unit,export:(String,String,ByteArray)->Unit) {
    LaunchedEffect(exported) {
        val ready=exported as? AnalysisState.Ready<ConstructionExport,ByteArray>
        if(ready!=null) {
            val request=ready.request!!;val bytes=ready.result
            acknowledge()
            val extension=if(request.format=="tiles")"pdf" else request.format
            val mime=mapOf("pdf" to "application/pdf","csv" to "text/csv","svg" to "image/svg+xml").getValue(extension)
            export(WorkshopDocument.filename(request.plan,request.format,request.labels.language,java.util.UUID.randomUUID().toString().take(8)),mime,bytes)
        }
    }
}

@Composable
private fun ConstructionStatus(unreadable:Boolean,failed:Boolean,saved:Boolean,analysis:AnalysisState<ConstructionRequest,ConstructionResult>,exported:AnalysisState<ConstructionExport,ByteArray>) {
    if(unreadable)Text(stringResource(R.string.construction_storage_error),color=MaterialTheme.colorScheme.error)
    if(failed || analysis is AnalysisState.Failed || exported is AnalysisState.Failed)Text(stringResource(R.string.construction_error),color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("construction-error"))
    val limit=exportLanguage().word(WorkshopTerm.TOO_DENSE)
    if(exported.failure?.message==limit)Text(limit,color=MaterialTheme.colorScheme.error)
    if(saved)Text(stringResource(R.string.construction_saved))
}

@Composable
private fun ConstructionWorkers(analysis:AnalysisState<ConstructionRequest,ConstructionResult>,exported:AnalysisState<ConstructionExport,ByteArray>,cancel:()->Unit) {
    if(analysis is AnalysisState.Loading || exported is AnalysisState.Loading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(onClick=cancel){Text(stringResource(R.string.ui_cancel_search))}
    }
}

@Composable
private fun ConstructionCandidates(candidates:List<ConstructionCandidate>,search:()->Unit,choose:(ConstructionCandidate)->Unit,current:()->Unit) {
    Button(onClick=search,modifier=Modifier.testTag("construction-search")){Text(stringResource(R.string.construction_search))}
    candidates.forEachIndexed {i,c->ConstructionCandidateCard(c,i){choose(c)}}
    TextButton(onClick=current){Text(stringResource(R.string.construction_current))}
}

@Composable
private fun ConstructionPlanActions(state:AnalysisState<ConstructionRequest,ConstructionResult>,exporting:Boolean,
    run:(String)->Unit,export:(ConstructionPlan,String)->Unit) {
    Button(onClick={run("plan")},modifier=Modifier.testTag("construction-generate")){Text(stringResource(R.string.construction_preview))}
    val result=state.value
    if(result==null || state !is AnalysisState.Ready || state.request?.action=="search")return
    ConstructionPlanView(result)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("pdf" to R.string.construction_pdf,"tiles" to R.string.construction_tiles,"csv" to R.string.ui_export_csv,"svg" to R.string.construction_svg).forEach {(format,title)->
            OutlinedButton(onClick={export(result.plan,format)},enabled=!exporting,modifier=Modifier.testTag("construction-export-$format")){Text(stringResource(title))}
        }
    }
    OutlinedButton(onClick={run("tolerance")},modifier=Modifier.testTag("construction-tolerance")){Text(stringResource(R.string.construction_tolerance_analysis))}
    Text(stringResource(R.string.construction_tolerance_help),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConstructionField(key:String,label:Int,values:Map<String,String>,set:(String,String)->Unit,multiline:Boolean=false) {
    OutlinedTextField(values.getValue(key),{set(key,it)},label={Text(stringResource(label))},singleLine=!multiline,
        minLines=if(multiline)3 else 1,maxLines=if(multiline)8 else 1,modifier=Modifier.fillMaxWidth().testTag("construction-$key"))
}

@Composable
private fun ConstructionTargetFields(v:Map<String,String>,set:(String,String)->Unit,name:String) {
    OutlinedTextField(v["planName"] ?: WorkshopDocument.humanName(name),{set("planName",it)},label={Text(exportLanguage().wood(WoodTerm.NAME))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("construction-planName"))
    var notes by remember {mutableStateOf(false)}
    ConstructionField("target",R.string.construction_hz,v,set)
    Box {
        TextButton(onClick={notes=true}){Text(stringResource(R.string.construction_note))}
        DropdownMenu(notes,{notes=false}){(23..55).forEach {midi->DropdownMenuItem(text={Text(WorkshopDocument.note(Music.frequency(midi),exportLanguage()))},onClick={set("target",Music.frequency(midi).toString());notes=false})}}
    }
    val shape=runCatching {ConstructionForm.shape(v)}.getOrDefault(AcousticModel.Shape())
    BoreGraph(shape,Modifier.fillMaxWidth().height(150.dp))
    DimensionControl(stringResource(R.string.ui_length),shape.lengthMm,500..3000,"mm",{value->
        set("geometry",ConstructionForm.defaults(resizeProfile(shape,value)).getValue("geometry"));set("preferred",value.toString())
    },{})
    DimensionControl(stringResource(R.string.ui_mouthpiece),shape.mouthMm,15..180,"mm",{value->
        set("geometry",ConstructionForm.defaults(changeDiameter(shape,0,value)).getValue("geometry"));set("mouth",value.toString())
    },{})
    DimensionControl(stringResource(R.string.ui_bell),shape.bellMm,15..200,"mm",{value->
        set("geometry",ConstructionForm.defaults(changeDiameter(shape,2,value)).getValue("geometry"))
    },{})
    ConstructionField("maximumLength",R.string.construction_maximum,v,set)
    var advanced by remember {mutableStateOf(false)}
    TextButton(onClick={advanced=!advanced},modifier=Modifier.testTag("construction-advanced")){Text(stringResource(R.string.construction_advanced))}
    if(advanced) {
        ConstructionField("higher",R.string.construction_higher,v,set)
        ConstructionField("minimumBore",R.string.construction_min_bore,v,set)
        ConstructionField("maximumBore",R.string.construction_max_bore,v,set)
        ConstructionField("bell",R.string.construction_bell,v,set)
        ConstructionField("geometry",R.string.construction_geometry,v,set,true)
    }
    Text(stringResource(R.string.construction_limits),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConstructionMethodFields(v:Map<String,String>,set:(String,String)->Unit,saveCatalog:()->Unit,loadCatalog:()->Unit,writable:Boolean) {
    if(v.getValue("method")=="LAMINATED")Text(stringResource(R.string.wizard_legacy),style=MaterialTheme.typography.bodySmall)
    val names=listOf(R.string.construction_sandwich,R.string.construction_pvc)
    ConstructionForm.activeMethods().forEachIndexed {i,method->
        FilterChip(v.getValue("method")==method.name,{set("method",method.name)},label={Text(stringResource(names[i]))},modifier=Modifier.testTag("construction-method-${method.name}"))
    }
    ConstructionField("interval",R.string.construction_interval,v,set)
    if(v.getValue("method")!="PVC") {
        ConstructionField("wall",R.string.construction_wall,v,set)
        ConstructionField("tolerance",R.string.construction_tolerance,v,set)
    }
    if(v.getValue("method")=="SANDWICH")WoodStockFields(v,set)
    if(v.getValue("method")=="PVC") {
        Text(stringResource(R.string.construction_schema),style=MaterialTheme.typography.bodySmall)
        ConstructionField("pipes",R.string.construction_pipes,v,set,true)
        ConstructionField("fittings",R.string.construction_fittings,v,set,true)
        ConstructionField("assembly",R.string.construction_assembly,v,set,true)
        ConstructionField("clearance",R.string.construction_clearance,v,set)
        FlowRow {
            TextButton(onClick=saveCatalog,enabled=writable,modifier=Modifier.testTag("construction-save-catalog")){Text(stringResource(R.string.construction_save_catalog))}
            TextButton(onClick=loadCatalog){Text(stringResource(R.string.construction_load_catalog))}
        }
    }
    Text(stringResource(R.string.construction_instructions),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConstructionCandidateCard(c:ConstructionCandidate,index:Int,use:()->Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("${index+1}: ${hzLabel(c.hz)} | ${exportLanguage().dimension(c.cents)} cents")
            Text("${constructionDimensions(c.plan)} | ${exportLanguage().word(WorkshopTerm.QUANTITY)}: ${c.plan.parts.size}")
            Text(c.higher.joinToString(" / "){hzLabel(it)},style=MaterialTheme.typography.bodySmall)
            TextButton(onClick=use,modifier=Modifier.testTag("construction-candidate-$index")){Text(stringResource(R.string.construction_choose))}
            if(c.plan.options.method==ConstructionMethod.SANDWICH)WoodFitMessage(WoodFit.assess(c.plan.ideal,c.plan.options.woodStock,c.plan.options.wallMm,c.plan.options.radialToleranceMm))
            ConstructionProfile(c.plan)
        }
    }
}

@Composable
private fun ConstructionPlanView(r:ConstructionResult) {
    val p=r.plan
    Text("${p.name.substringAfter(" / ")}\n${constructionDimensions(p)}",modifier=Modifier.testTag("construction-plan-summary"))
    if(p.options.method==ConstructionMethod.SANDWICH)WoodFitMessage(WoodFit.assess(p.ideal,p.options.woodStock,p.options.wallMm,p.options.radialToleranceMm))
    Text(r.prediction.frequenciesHz.take(5).joinToString(" / "){hzLabel(it)})
    ConstructionProfile(p)
    var details by remember {mutableStateOf(false)}
    TextButton(onClick={details=!details},modifier=Modifier.testTag("construction-dimensions")){Text(stringResource(R.string.construction_stations))}
    if(details) {
    Text(stringResource(R.string.construction_stations),style=MaterialTheme.typography.titleMedium)
    ConstructionDrawing.rows(p).take(60).forEach {row->Text("${exportLanguage().dimension(row.xMm)} | ${row.idealMm?.let {exportLanguage().dimension(it)} ?: "-"} | ${exportLanguage().dimension(row.beforeMm)}/${exportLanguage().dimension(row.afterMm)}",style=MaterialTheme.typography.bodySmall)}
    Text(stringResource(R.string.construction_preview_limit),style=MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.construction_parts),style=MaterialTheme.typography.titleMedium)
    Text(WorkshopDocument.csv(p,exportLanguage()),style=MaterialTheme.typography.bodySmall)
    }
    r.sensitivity.forEach {Text("${WorkshopDocument.sensitivity(it.dimension,exportLanguage())}: ${it.deltaHz?.let {hz->exportLanguage().dimension(hz)+" Hz"} ?: stringResource(R.string.construction_unavailable)}",modifier=Modifier.testTag("construction-sensitivity"))}
}

@Composable
internal fun exportLanguage()=if(LocalConfiguration.current.locales[0].language=="es")WorkshopLanguage.SPANISH else WorkshopLanguage.ENGLISH

@Composable
private fun constructionDimensions(plan:ConstructionPlan):String {
    val l=exportLanguage()
    return "${l.word(WorkshopTerm.LENGTH)}: ${l.dimension(plan.lengthMm)} mm\n${l.word(WorkshopTerm.MOUTH)}: ${l.dimension(plan.pieces.first().inletMm)} mm; ${l.word(WorkshopTerm.BELL)}: ${l.dimension(plan.pieces.last().outletMm)} mm"
}

@Composable
private fun ConstructionProfile(plan:ConstructionPlan) {
    val label=stringResource(R.string.construction_compare)
    Text(label,style=MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(130.dp).semantics {contentDescription=label}.testTag("construction-profile")) {
        val length=maxOf(plan.lengthMm,plan.ideal.lengthMm.toDouble())
        val diameter=maxOf(plan.pieces.maxOf {maxOf(it.inletMm,it.outletMm)},plan.ideal.controlPoints().maxOf {it.diameterMm})
        fun draw(points:List<Pair<Double,Double>>,color:Color) {
            val path=Path()
            points.forEachIndexed {i,(x,y)->val px=(x/length*size.width).toFloat();val py=(size.height/2+y/diameter*size.height*.8).toFloat();if(i==0)path.moveTo(px,py) else path.lineTo(px,py)}
            drawPath(path,color,style=androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
        for(sign in listOf(-1,1)) {
            draw(plan.ideal.controlPoints().map {it.positionMm.toDouble() to sign*it.diameterMm/2},Color.Gray)
            draw(ConstructionDrawing.outline(plan,sign),Color(0xFF8FE0BD))
        }
    }
}
