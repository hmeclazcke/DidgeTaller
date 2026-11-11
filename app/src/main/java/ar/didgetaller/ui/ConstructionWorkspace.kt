package ar.didgetaller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import ar.didgetaller.audio.CoroutineAnalysisLoader
import ar.didgetaller.core.*
import ar.didgetaller.data.*
import kotlinx.coroutines.Dispatchers

internal data class ConstructionExport(val plan:ConstructionPlan,val format:String,val labels:ConstructionPdf.Labels)

internal fun constructionBytes(r:ConstructionExport):ByteArray = when(r.format) {
    "csv"->WorkshopDocument.csv(r.plan,r.labels.language).toByteArray(Charsets.UTF_8)
    "svg"->WorkshopDocument.svg(r.plan,r.labels.language).toByteArray(Charsets.UTF_8)
    else->ConstructionPdf.create(r.plan,r.labels,r.format=="tiles")
}

/** Scoped, revocable workers reuse the existing analysis owner; exports never mutate the saved bore. */
@Composable
internal fun ConstructionWorkspace(design:LocalStore.Design,export:(String,String,ByteArray)->Unit,storageContext:android.content.Context?=null,dismiss:()->Unit) {
    val context=LocalContext.current
    val store=remember {ConstructionStore(storageContext ?: context)}
    val load=remember(design.id){runCatching {ConstructionForm.defaults(design.shape)+store.catalog()+store.project(design.id)}}
    val fields=remember(design.id){
        val initial=mutableStateMapOf<String,String>()
        initial.putAll(load.getOrElse {ConstructionForm.defaults(design.shape)})
        initial
    }
    var stage by remember {mutableIntStateOf(0)}
    var failed by remember {mutableStateOf(false)}
    var saved by remember {mutableStateOf(false)}
    var analysis by remember {mutableStateOf<AnalysisState<ConstructionRequest,ConstructionResult>>(AnalysisState.Idle)}
    var exported by remember {mutableStateOf<AnalysisState<ConstructionExport,ByteArray>>(AnalysisState.Idle)}
    val scope=rememberCoroutineScope()
    val owner=remember {LatestAnalysis<ConstructionRequest,ConstructionResult>(CoroutineAnalysisLoader(scope,Dispatchers.Default){r:ConstructionRequest,cancelled,_->calculateConstruction(r,cancelled)}){analysis=it}}
    val exportOwner=remember {LatestAnalysis<ConstructionExport,ByteArray>(CoroutineAnalysisLoader(scope,Dispatchers.Default){r:ConstructionExport,_,_->constructionBytes(r)}){exported=it}}
    DisposableEffect(owner,exportOwner){onDispose {owner.close();exportOwner.close()}}
    fun set(key:String,value:String){owner.clear();exportOwner.clear();fields[key]=value.take(64000);failed=false;saved=false}
    fun run(action:String){failed=false;owner.replace(ConstructionRequest(ConstructionForm.name(fields,design.name),fields.toMap(),action))}
    ConstructionExportEffect(exported,{exportOwner.clear()},export)
    val language=exportLanguage()
    val labels=ConstructionPdf.Labels(language,fields["target"]?.toDoubleOrNull())
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val focus=LocalFocusManager.current
        val select:(Int)->Unit={focus.clearFocus();stage=it}
        Surface(Modifier.fillMaxSize().testTag("construction-workspace")) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                ConstructionHeader(stage,select,dismiss)
                val scroll=key(stage){rememberScrollState()}
                Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    ConstructionStatus(load.isFailure,failed,saved,analysis,exported)
                    when(stage) {
                        0->ConstructionTargetFields(fields,::set,design.name)
                        1->ConstructionCandidates(analysis.value?.candidates.orEmpty(),{run("search")},{c->
                            set("geometry",ConstructionForm.defaults(c.plan.ideal).getValue("geometry"))
                            set("assembly",c.plan.options.assembly);select(2)
                        },{set("geometry",ConstructionForm.defaults(design.shape).getValue("geometry"));select(2)})
                        2->ConstructionMethodFields(fields,::set,{
                            runCatching {store.saveCatalog(fields.getValue("pipes"),fields.getValue("fittings"))}
                                .onSuccess {saved=true}.onFailure {failed=true}
                        },{
                            runCatching {store.catalog().forEach {(k,v)->set(k,v)}}
                                .onSuccess {saved=true}.onFailure {failed=true}
                        },load.isSuccess)
                        else->ConstructionPlanActions(analysis,exported is AnalysisState.Loading,load.isSuccess,::run,{
                            runCatching {store.saveProject(design.id,fields.toMap())}.onSuccess {saved=true}.onFailure {failed=true}
                        }){plan,format->exportOwner.replace(ConstructionExport(plan,format,labels))}
                    }
                    ConstructionWorkers(analysis,exported){owner.cancel();exportOwner.cancel()}
                    Text(stringResource(R.string.construction_uncertainty),style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ConstructionHeader(stage:Int,select:(Int)->Unit,dismiss:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
        Text(stringResource(R.string.construction_title),Modifier.weight(1f).testTag("construction-heading"),style=MaterialTheme.typography.titleLarge)
        TextButton(onClick=dismiss){Text(stringResource(R.string.ui_close))}
    }
    ScrollableTabRow(stage,edgePadding=0.dp) {
        listOf(R.string.construction_target,R.string.construction_candidates,R.string.construction_method,R.string.construction_plan).forEachIndexed {i,title->
            Tab(stage==i,{select(i)},text={Text(stringResource(title))},modifier=Modifier.testTag("construction-step-$i"))
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
private fun ConstructionPlanActions(state:AnalysisState<ConstructionRequest,ConstructionResult>,exporting:Boolean,writable:Boolean,
    run:(String)->Unit,save:()->Unit,export:(ConstructionPlan,String)->Unit) {
    Button(onClick={run("plan")},modifier=Modifier.testTag("construction-generate")){Text(stringResource(R.string.construction_preview))}
    val result=state.value
    if(result==null || state !is AnalysisState.Ready || state.request?.action=="search")return
    ConstructionPlanView(result)
    Button(enabled=writable,onClick=save,modifier=Modifier.testTag("construction-save")){Text(stringResource(R.string.ui_save))}
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
        DropdownMenu(notes,{notes=false}){(23..55).forEach {midi->DropdownMenuItem(text={Text(Music.pitch(Music.frequency(midi))!!.note)},onClick={set("target",Music.frequency(midi).toString());notes=false})}}
    }
    ConstructionField("higher",R.string.construction_higher,v,set)
    ConstructionField("mouth",R.string.construction_mouth,v,set)
    ConstructionField("preferred",R.string.construction_preferred,v,set)
    ConstructionField("maximumLength",R.string.construction_maximum,v,set)
    var advanced by remember {mutableStateOf(false)}
    TextButton(onClick={advanced=!advanced}){Text(stringResource(R.string.construction_advanced))}
    if(advanced) {
        ConstructionField("minimumBore",R.string.construction_min_bore,v,set)
        ConstructionField("maximumBore",R.string.construction_max_bore,v,set)
        ConstructionField("bell",R.string.construction_bell,v,set)
        ConstructionField("geometry",R.string.construction_geometry,v,set,true)
    }
    Text(stringResource(R.string.construction_limits),style=MaterialTheme.typography.bodySmall)
    var wood by remember {mutableStateOf(false)}
    TextButton(onClick={wood=!wood},modifier=Modifier.testTag("construction-target-stock")){Text(exportLanguage().wood(WoodTerm.STOCK))}
    if(wood)WoodStockFields(v,set)
}

@Composable
private fun ConstructionMethodFields(v:Map<String,String>,set:(String,String)->Unit,saveCatalog:()->Unit,loadCatalog:()->Unit,writable:Boolean) {
    val names=listOf(R.string.construction_sandwich,R.string.construction_laminated,R.string.construction_pvc)
    ConstructionMethod.entries.forEachIndexed {i,method->
        FilterChip(v.getValue("method")==method.name,{set("method",method.name)},label={Text(stringResource(names[i]))},modifier=Modifier.testTag("construction-method-${method.name}"))
    }
    ConstructionField("interval",R.string.construction_interval,v,set)
    if(v.getValue("method")!="PVC") {
        ConstructionField("wall",R.string.construction_wall,v,set)
        ConstructionField("tolerance",R.string.construction_tolerance,v,set)
    }
    if(v.getValue("method")=="SANDWICH")WoodStockFields(v,set)
    if(v.getValue("method")=="LAMINATED")ConstructionField("layers",R.string.construction_layers,v,set)
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
            Text("${index+1}: ${hzLabel(c.hz)} | ${WorkshopDocument.number(c.cents)} cents")
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
    ConstructionDrawing.rows(p).take(60).forEach {row->Text("${WorkshopDocument.number(row.xMm)} | ${row.idealMm?.let(WorkshopDocument::number) ?: "-"} | ${WorkshopDocument.number(row.beforeMm)}/${WorkshopDocument.number(row.afterMm)}",style=MaterialTheme.typography.bodySmall)}
    Text(stringResource(R.string.construction_preview_limit),style=MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.construction_parts),style=MaterialTheme.typography.titleMedium)
    Text(WorkshopDocument.csv(p,exportLanguage()),style=MaterialTheme.typography.bodySmall)
    }
    r.sensitivity.forEach {Text("${WorkshopDocument.sensitivity(it.dimension,exportLanguage())}: ${it.deltaHz?.let {hz->WorkshopDocument.number(hz)+" Hz"} ?: stringResource(R.string.construction_unavailable)}",modifier=Modifier.testTag("construction-sensitivity"))}
}

@Composable
internal fun exportLanguage()=if(LocalConfiguration.current.locales[0].language=="es")WorkshopLanguage.SPANISH else WorkshopLanguage.ENGLISH

@Composable
private fun constructionDimensions(plan:ConstructionPlan):String {
    val l=exportLanguage()
    return "${l.word(WorkshopTerm.LENGTH)}: ${WorkshopDocument.number(plan.lengthMm)} mm\n${l.word(WorkshopTerm.MOUTH)}: ${WorkshopDocument.number(plan.pieces.first().inletMm)} mm; ${l.word(WorkshopTerm.BELL)}: ${WorkshopDocument.number(plan.pieces.last().outletMm)} mm"
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
