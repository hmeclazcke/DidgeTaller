package ar.didgetaller.ui

import android.content.Context
import androidx.compose.runtime.*
import ar.didgetaller.audio.CoroutineAnalysisLoader
import ar.didgetaller.core.*
import ar.didgetaller.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Commands share the saveable draft; workers only publish immutable, input-bound results. */
internal class ConstructionEditor(private val context:Context,private val design:LocalStore.Design,
    val draft:MutableState<ConstructionJourney>,private val initial:ConstructionJourney,
    val load:Result<Map<String,String>>,scope:CoroutineScope) {
    private val store=ConstructionStore(context)
    private var savedShape=design.shape
    var failed by mutableStateOf(false); private set
    var saved by mutableStateOf(false); private set
    var leaving by mutableStateOf(false)
    var analysis by mutableStateOf<AnalysisState<ConstructionRequest,ConstructionResult>>(AnalysisState.Idle); private set
    var exported by mutableStateOf<AnalysisState<ConstructionExport,ByteArray>>(AnalysisState.Idle); private set
    private val owner=LatestAnalysis<ConstructionRequest,ConstructionResult>(CoroutineAnalysisLoader(scope,Dispatchers.Default){r:ConstructionRequest,cancelled,_->calculateConstruction(r,cancelled)}){analysis=it}
    private val exportOwner=LatestAnalysis<ConstructionExport,ByteArray>(CoroutineAnalysisLoader(scope,Dispatchers.Default){r:ConstructionExport,_,_->constructionBytes(r)}){exported=it}
    val current get()=analysis.value?.takeIf {analysis is AnalysisState.Ready && analysis.request?.action!="search" && analysis.request?.values==draft.value.values}
    val canPlan get()=current?.let {ConstructionJourney.canPlan(it.plan,draft.value.accepted)}==true
    fun available(step:Int)=ConstructionJourney.available(draft.value.values,step,canPlan)
    fun set(key:String,value:String) {
        owner.clear();exportOwner.clear()
        draft.value=draft.value.edit(key,value.take(64000)).copy(step=minOf(draft.value.step,2))
        failed=false;saved=false
    }
    fun name(language:WorkshopLanguage):String {
        val values=draft.value.values
        val note=values["target"]?.toDoubleOrNull()?.takeIf {it.isFinite() && it>0}?.let {WorkshopDocument.note(it,language)}.orEmpty()
        val fallback=design.name.ifBlank {"Didgeridoo $note"}
        return values["planName"]?.takeIf {it.isNotBlank()} ?: fallback
    }
    fun run(action:String,language:WorkshopLanguage) {
        failed=false;owner.replace(ConstructionRequest(name(language),draft.value.values.toMap(),action))
    }
    fun prepare(language:WorkshopLanguage) {
        if(draft.value.step>=2 && analysis is AnalysisState.Idle && available(2))run("plan",language)
    }
    fun select(step:Int) {if(available(step))draft.value=draft.value.copy(step=step)}
    fun approve(accepted:Boolean) {
        draft.value=draft.value.copy(acceptedValues=if(accepted)draft.value.values else null);saved=false
    }
    fun save(language:WorkshopLanguage):Boolean=runCatching {
        check(load.isSuccess) {"Unreadable construction metadata"}
        val journey=draft.value
        store.saveProject(design.id,journey.values+mapOf("_wizardStep" to journey.step.toString(),"_wizardAccepted" to journey.accepted.toString()))
        val shape=runCatching {ConstructionForm.shape(journey.values)}.getOrDefault(savedShape)
        LocalStore(context).saveDesign(design.id,name(language),shape)
        savedShape=shape
        saved=true
    }.onFailure {failed=true}.isSuccess
    fun saveCatalog() {
        runCatching {store.saveCatalog(draft.value.values.getValue("pipes"),draft.value.values.getValue("fittings"))}.onFailure {failed=true}
    }
    fun loadCatalog() {runCatching {store.catalog().forEach {(k,v)->set(k,v)}}.onFailure {failed=true}}
    fun choose(candidate:ConstructionCandidate) {
        set("geometry",ConstructionForm.defaults(candidate.plan.ideal).getValue("geometry"));set("assembly",candidate.plan.options.assembly)
    }
    fun export(plan:ConstructionPlan,format:String,labels:ConstructionPdf.Labels) {
        if(analysis.request?.values==draft.value.values && ConstructionJourney.canPlan(plan,draft.value.accepted))
            exportOwner.replace(ConstructionExport(plan,format,labels))
    }
    fun acknowledgeExport(){exportOwner.clear()}
    fun cancel(){owner.cancel();exportOwner.cancel()}
    fun dispose(){owner.close();exportOwner.close()}
    fun requestClose(dismiss:()->Unit) {
        if(!saved && (draft.value.values!=initial.values || draft.value.accepted!=initial.accepted))leaving=true else dismiss()
    }
    fun back(dismiss:()->Unit) {if(draft.value.step>0)select(draft.value.step-1) else requestClose(dismiss)}
}
