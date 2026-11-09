package ar.didgetaller

import ar.didgetaller.core.LivePitch
import ar.didgetaller.core.CaptureId
import ar.didgetaller.core.RecordingCoordinator
import ar.didgetaller.core.RecordingPhase
import ar.didgetaller.core.CapturedSession
import ar.didgetaller.core.AcousticDiagnostics
import ar.didgetaller.core.SavedPitchEvidence
import ar.didgetaller.core.SwipeComparisonLoader
import ar.didgetaller.core.SwipeComparisonController
import ar.didgetaller.core.SwipeComparisonRequest
import ar.didgetaller.core.SwipeComparisonStatus
import ar.didgetaller.core.SwipeAnalysis
import ar.didgetaller.core.SwipeCurve
import ar.didgetaller.core.DroneComponentAnalysis
import ar.didgetaller.core.DroneComponentCurve
import ar.didgetaller.core.MusicalTrace
import ar.didgetaller.core.SavedYinReplay
import ar.didgetaller.core.AxisWindow
import ar.didgetaller.core.SimulationLoader
import ar.didgetaller.core.SimulationController
import ar.didgetaller.core.SimulationState
import ar.didgetaller.core.RecordingAnalysisLoader
import ar.didgetaller.core.RecordingAnalysisRequest
import ar.didgetaller.core.RecordingAnalysisController
import ar.didgetaller.core.RecordingAnalysisStatus
import ar.didgetaller.audio.CoroutineRecordingAnalysisLoader
import ar.didgetaller.audio.CoroutineSwipeComparisonLoader
import ar.didgetaller.ui.reconcileComparisonCameras
import ar.didgetaller.lab.CoroutineSimulationLoader

import android.app.Application
import android.os.Handler
import android.os.Looper
import ar.didgetaller.audio.SessionPlayer
import ar.didgetaller.audio.SessionWorkspace
import ar.didgetaller.data.AnalysisCache
import ar.didgetaller.data.TrashStore
import ar.didgetaller.data.SessionToolsStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.File
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ar.didgetaller.audio.MicrophoneEngine
import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.BoreOptimizer
import ar.didgetaller.core.Music
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.PitchDetector
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.Pcm16WavReader
import ar.didgetaller.core.WavAnalysis
import ar.didgetaller.data.LocalStore
import ar.didgetaller.data.RecordingStore
import ar.didgetaller.data.LibraryCatalog
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ln

typealias SessionPhase = RecordingPhase
private const val MIC_PERMISSION_NOTICE="Para medir hace falta permiso de micrófono. Podés concederlo desde Ajustes."

data class UndoEntry(val kind:String,val id:String)
data class ComparisonState(val running:Boolean=false,val progress:Int=0,val a:WavAnalysis.Result?=null,val b:WavAnalysis.Result?=null,val error:String?=null)
data class SavedEvidenceState(val sessionId:String?=null,val running:Boolean=false,
    val result:SavedPitchEvidence.Snapshot?=null,val failed:Boolean=false)

data class DroneComponentState(val sessionId:String?=null,val running:Boolean=false,val progress:Int=0,
    val curve:DroneComponentCurve?=null,val failed:Boolean=false)
data class SavedYinReplayState(val sessionId:String?=null,val running:Boolean=false,
    val result:SavedYinReplay.Result?=null,val failed:Boolean=false)

class TallerViewModel internal constructor(
    application: Application, simulationLoader: SimulationLoader?,
    analysisLoader: RecordingAnalysisLoader? = null,
    swipeLoader: SwipeComparisonLoader? = null,
): AndroidViewModel(application) {
    constructor(application: Application) : this(application, null)
    private val store=LocalStore(application)
    private val recordingStore=RecordingStore(application)
    private val recoveredCount=recordingStore.recoverPending()
    private val cache=AnalysisCache(application)
    private val trash=TrashStore(application)
    private val tools=SessionToolsStore(application)
    private val catalog=LibraryCatalog(application,recordingStore,store,trash)
    private var library by mutableStateOf(catalog.load())
    val workspace=SessionWorkspace(viewModelScope,recordingStore,cache)
    private val explorers=mutableMapOf<String,ar.didgetaller.ui.ExplorerState>()
    fun explorer(id:String)=explorers.getOrPut(id){ar.didgetaller.ui.ExplorerState(sessions.firstOrNull {it.id==id}?.instrument ?: InstrumentProfile.DIDGERIDOO)}
    private val previews=mutableMapOf<String,ar.didgetaller.ui.ExplorerState>()
    fun preview(id:String)=previews.getOrPut(id){explorer(id).let {ar.didgetaller.ui.ExplorerState(it.instrument,it.selection)}}
    private var activeDetail:String?=null
    var viewerOriginalOrientation:Int?=null
    var instrument by mutableStateOf(tools.instrument());private set
    fun selectInstrument(profile:InstrumentProfile){
        if(listening || sessionPhase!=SessionPhase.IDLE)return
        instrument=profile;tools.setInstrument(profile);minHz=profile.minHz;maxHz=profile.initialMaxHz
        mic.instrument=profile;mic.minHz=minHz;mic.maxHz=maxHz
        reading=PitchDetector.Reading();filteredHz=0.0;consecutiveReadings=0
        clearAcousticDiagnostics()
    }
    var recordingLimitSeconds by mutableStateOf(tools.limitSeconds());private set
    var undoEntry by mutableStateOf<UndoEntry?>(null);private set
    val trashCount get()=library.trashCount
    val trashedSessions get()=library.trashedSessions
    val trashedDesigns get()=library.trashedDesigns
    val sessionIndexDamaged get()=library.indexDamaged
    var markers by mutableStateOf<List<SessionToolsStore.Marker>>(emptyList());private set
    var associatedDesignId by mutableStateOf<String?>(null);private set
    var associatedSimulation by mutableStateOf<AcousticModel.Simulation?>(null);private set
    private var associationJob:Job?=null
    var comparison by mutableStateOf(ComparisonState());private set
    private var comparisonJob:Job?=null
    fun setRecordingLimit(seconds:Int){tools.setLimitSeconds(seconds);recordingLimitSeconds=seconds}
    fun availableMegabytes()=getApplication<Application>().filesDir.usableSpace/(1024*1024)
    fun sessionFile(item:RecordingStore.Session)=recordingStore.file(item.id)
    fun openTools(item:RecordingStore.Session){if(workspace.state.sessionId!=item.id){workspace.open(item);loadTools(item)}}
    private fun loadTools(item:RecordingStore.Session){markers=tools.markers(item.id);associatedDesignId=tools.designId(item.id)
        associationJob?.cancel();associatedSimulation=null
        val selected=store.designs(true).find {it.id==associatedDesignId}
        if(selected!=null)associationJob=viewModelScope.launch {associatedSimulation=withContext(Dispatchers.Default){AcousticModel.evaluate(selected.shape)}}
    }
    private fun persistMetadata(change:()->Unit){
        runCatching(change).onFailure {message=getApplication<Application>().getString(R.string.library_metadata_write_failed)}
    }
    fun associate(item:RecordingStore.Session,designId:String?){if(item.instrument!=InstrumentProfile.DIDGERIDOO && designId!=null)return
        persistMetadata {tools.associate(item.id,designId);loadTools(item)}}
    fun addMarker(item:RecordingStore.Session,time:Long,type:String){persistMetadata {tools.addMarker(item.id,time,type,"");markers=tools.markers(item.id)}}
    fun removeMarker(item:RecordingStore.Session,id:String){persistMetadata {tools.deleteMarker(item.id,id);markers=tools.markers(item.id)}}
    fun compare(a:RecordingStore.Session,b:RecordingStore.Session,aStart:Long=0,aEnd:Long?=null,bStart:Long=0,bEnd:Long?=null){
        comparisonJob?.cancel();comparison=ComparisonState(running=true)
        comparisonJob=viewModelScope.launch {try {
            val results=withContext(Dispatchers.Default){
                val first=cache.analyze(a.id,sessionFile(a),aStart,aEnd,instrument=a.instrument,cancelled={!isActive},progress={p->main.post {if(isActive)comparison=comparison.copy(progress=p/2)}})
                val second=cache.analyze(b.id,sessionFile(b),bStart,bEnd,instrument=b.instrument,cancelled={!isActive},progress={p->main.post {if(isActive)comparison=comparison.copy(progress=50+p/2)}})
                first to second
            };comparison=ComparisonState(a=results.first,b=results.second,progress=100)
        } catch(e:CancellationException){throw e} catch(e:Exception){comparison=ComparisonState(error=e.message ?: "Error de comparación.")}}
    }
    fun cancelComparison(clear:Boolean=false){comparisonJob?.cancel();comparison=if(clear)ComparisonState() else comparison.copy(running=false)}
    private fun refreshLibrary(){library=catalog.load()}
    fun recoverSessionIndex(){
        runCatching {recordingStore.recoverIndex();refreshLibrary()}
            .onSuccess {message=getApplication<Application>().getString(R.string.library_index_restored)}
            .onFailure {message=getApplication<Application>().getString(R.string.library_index_recovery_failed)}
    }
    fun restore(kind:String,id:String){runCatching {trash.restore(kind,id);refreshLibrary();undoEntry=null}.onFailure {message="No se pudo restaurar: ${it.message}"}}
    fun undoDelete(){undoEntry?.let {restore(it.kind,it.id)}}
    fun clearUndo(){undoEntry=null}
    fun emptyTrash(){viewModelScope.launch {try {withContext(Dispatchers.IO){
        for(id in trash.ids("session")){recordingStore.delete(id);cache.remove(id);tools.forget(id);trash.restore("session",id)}
        for(id in trash.ids("design")){store.deleteDesign(id);tools.forgetDesign(id);trash.restore("design",id)}
    };refreshLibrary();undoEntry=null;message="Papelera vaciada."}catch(e:Exception){refreshLibrary();message="No se pudo completar el vaciado: ${e.message}"}}}

    private val sessionExecutor=Executors.newSingleThreadExecutor()
    private var analysisState by mutableStateOf<RecordingAnalysisStatus>(RecordingAnalysisStatus.Idle)
    val recordingAnalysis get()=RecordingAnalysisState(analysisState)
    private val analysisOwner=RecordingAnalysisController(analysisLoader ?: CoroutineRecordingAnalysisLoader(
        viewModelScope, { request,cancelled,progress ->
            cache.analyze(request.sessionId,recordingStore.file(request.sessionId),request.startMs,request.endMs,
                cancelled=cancelled,progress=progress,force=request.force,instrument=request.instrument)
        }), { analysisState=it })
    var savedEvidence by mutableStateOf(SavedEvidenceState());private set
    private var evidenceJob:Job?=null
    private val swipeDispatcher=Dispatchers.IO.limitedParallelism(1)
    private var swipeState by mutableStateOf<SwipeComparisonStatus>(SwipeComparisonStatus.Idle)
    val swipeComparison get()=SwipeComparisonState(swipeState)
    private val swipeOwner=SwipeComparisonController(swipeLoader ?: CoroutineSwipeComparisonLoader(
        viewModelScope, swipeDispatcher, { request,cancelled,progress ->
            Pcm16WavReader(recordingStore.file(request.sessionId)).use { source ->
                SwipeAnalysis().analyze(source,InstrumentProfile.DIDGERIDOO,cancelled,progress)
            }
        }), ::publishSwipeComparison)

    var droneComponent by mutableStateOf(DroneComponentState());private set
    private val musicalTraces=mutableStateMapOf<String,MusicalTrace>()
    private val fluteTraces=mutableMapOf<String,MusicalTrace>()
    private val musicalTraceJobs=mutableMapOf<String,Job>()
    private val musicalTraceFailures=mutableStateMapOf<String,Boolean>()
    fun musicalTrace(item:RecordingStore.Session):MusicalTrace? =
        if(item.instrument==InstrumentProfile.NATIVE_AMERICAN_FLUTE)
            fluteTraces.getOrPut(item.id){MusicalTrace.flute(item.timeline,item.durationMs)} else musicalTraces[item.id]
    fun musicalTraceFailed(id:String)=musicalTraceFailures[id]==true
    fun requestMusicalTrace(item:RecordingStore.Session){
        if(item.instrument!=InstrumentProfile.DIDGERIDOO)return
        if(musicalTraces.containsKey(item.id)){
            if(activeDetail==item.id && droneComponent.sessionId!=item.id)analyzeDroneComponent(item)
            return
        }
        if(musicalTraceJobs[item.id]?.isActive==true || musicalTraceFailures[item.id]==true)return
        musicalTraceJobs[item.id]=viewModelScope.launch {
            try {
                val curve=withContext(swipeDispatcher){
                    val task=currentCoroutineContext()
                    cache.droneComponent(item.id,sessionFile(item),cancelled={!task.isActive})
                }
                ensureActive()
                musicalTraces[item.id]=MusicalTrace.didgeridoo(curve,item.durationMs,item.timeline)
                if(activeDetail==item.id)droneComponent=DroneComponentState(item.id,progress=100,curve=curve)
                // The disk cache is authoritative; retain only a few decoded series in memory.
                while(musicalTraces.size>6){
                    val oldest=musicalTraces.keys.firstOrNull {it!=activeDetail && it!=item.id} ?: break
                    musicalTraces.remove(oldest)
                }
            }catch(e:CancellationException){throw e}
            catch(e:Exception){ensureActive();musicalTraceFailures[item.id]=true
                if(activeDetail==item.id)droneComponent=DroneComponentState(item.id,failed=true)
            }finally{if(musicalTraceJobs[item.id]===currentCoroutineContext()[Job])
                musicalTraceJobs.remove(item.id)}
        }
    }
    fun releaseMusicalTraceRequest(id:String){
        if(activeDetail!=id)musicalTraceJobs.remove(id)?.cancel()
    }
    var savedYinReplay by mutableStateOf(SavedYinReplayState());private set
    private var droneComponentJob:Job?=null
    private var savedYinReplayJob:Job?=null
    fun analyzeSavedYin(item:RecordingStore.Session){
        if(item.instrument!=InstrumentProfile.DIDGERIDOO || savedYinReplay.sessionId==item.id)return
        savedYinReplayJob?.cancel()
        savedYinReplay=SavedYinReplayState(item.id,running=true)
        savedYinReplayJob=viewModelScope.launch {
            try {
                val result=withContext(swipeDispatcher){
                    val task=currentCoroutineContext()
                    Pcm16WavReader(sessionFile(item)).use { source ->
                        SavedYinReplay.analyze(source,
                            expandedRange=SavedYinReplay.usesExpandedRange(item.timeline),
                            cancelled={!task.isActive})
                    }
                }
                ensureActive()
                if(activeDetail==item.id)savedYinReplay=SavedYinReplayState(item.id,result=result)
            }catch(e:CancellationException){throw e}
            catch(e:Exception){ensureActive();if(activeDetail==item.id)
                savedYinReplay=SavedYinReplayState(item.id,failed=true)}
        }
    }
    private fun clearSavedYinReplay(){
        savedYinReplayJob?.cancel()
        savedYinReplay=SavedYinReplayState()
    }
    fun analyzeDroneComponent(item:RecordingStore.Session){
        if(item.instrument!=InstrumentProfile.DIDGERIDOO || droneComponent.sessionId==item.id)return
        droneComponentJob?.cancel()
        droneComponent=DroneComponentState(item.id,running=true)
        droneComponentJob=viewModelScope.launch {
            try {
                val result=withContext(swipeDispatcher){
                    val task=currentCoroutineContext()
                    cache.droneComponent(item.id,sessionFile(item),cancelled={!task.isActive},progress={p->
                            main.post {if(task.isActive && droneComponent.sessionId==item.id)
                                droneComponent=droneComponent.copy(progress=p)}
                        })
                }
                ensureActive()
                if(activeDetail==item.id)droneComponent=DroneComponentState(item.id,progress=100,curve=result)
            }catch(e:CancellationException){throw e}
            catch(e:Exception){ensureActive();if(activeDetail==item.id)
                droneComponent=DroneComponentState(item.id,failed=true)}
        }
    }
    private fun clearDroneComponent(){
        droneComponentJob?.cancel()
        droneComponent=DroneComponentState()
    }
    fun setSwipeComparison(item:RecordingStore.Session,enabled:Boolean){
        if(item.instrument==InstrumentProfile.DIDGERIDOO)
            swipeOwner.select(SwipeComparisonRequest(item.id),enabled)
    }
    private fun publishSwipeComparison(next:SwipeComparisonStatus){
        val previous=swipeState
        swipeState=next
        reconcileComparisonCameras(previous,next,explorers,::explorer)
    }
    private fun clearSwipeComparison(){swipeOwner.clear()}

    fun inspectSavedEvidence(item:RecordingStore.Session,positionMs:Long){
        cancelSavedEvidence()
        savedEvidence=SavedEvidenceState(item.id,running=true)
        evidenceJob=viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO){
                    val task=currentCoroutineContext()
                    Pcm16WavReader(sessionFile(item)).use {source->
                        SavedPitchEvidence.inspect(source,positionMs,item.instrument){!task.isActive}
                    }
                }
                ensureActive()
                if(activeDetail==item.id)savedEvidence=SavedEvidenceState(item.id,result=result)
            }catch(e:CancellationException){throw e}
            catch(e:Exception){if(activeDetail==item.id)savedEvidence=SavedEvidenceState(item.id,failed=true)}
        }
    }
    fun cancelSavedEvidence(){evidenceJob?.cancel();savedEvidence=SavedEvidenceState()}
    private val main=Handler(Looper.getMainLooper())
    private val optimizerExecutor=Executors.newSingleThreadExecutor()
    var pendingExport:ByteArray?=null
    var pendingExportFile:File?=null
    private val optimizationVersion=AtomicInteger(0)
    var labTab by mutableStateOf(0)
    var libraryTab by mutableStateOf(0)
    var fullScreen by mutableStateOf(false)
    var permissionDenied by mutableStateOf(false);private set
    var requestingPermission by mutableStateOf(false);private set
    var microphoneError by mutableStateOf<String?>(null);private set
    var reading by mutableStateOf(PitchDetector.Reading());private set
    private val diagnosticHistory=AcousticDiagnostics()
    var acousticSample by mutableStateOf<AcousticDiagnostics.Sample?>(null);private set
    val recentAcousticDiagnostics:List<AcousticDiagnostics.Sample> get()=diagnosticHistory.samples
    private fun clearAcousticDiagnostics(){diagnosticHistory.clear();acousticSample=null}
    var filteredHz by mutableStateOf(0.0); private set
    var lastMeasuredHz by mutableStateOf(0.0); private set
    private var recordingState by mutableStateOf(RecordingCoordinator.State())
    val listening get()=recordingState.listening
    val sessionPhase get()=recordingState.phase
    val sessionStartedAtMs get()=recordingState.startedAtMs
    val sessionTimeline get()=recordingState.points
    val sessions get()=library.sessions
    val stableReading:Boolean get()=listening && consecutiveReadings>=5 && reading.hz>0 && filteredHz>0
    var playingId by mutableStateOf<String?>(null);private set
    var playbackPaused by mutableStateOf(false);private set
    private val player=SessionPlayer({id,paused ->playingId=id;playbackPaused=paused},{message=it})
    private var consecutiveReadings=0
    var message by mutableStateOf("");private set
    var minHz by mutableStateOf(instrument.minHz);private set
    var maxHz by mutableStateOf(instrument.initialMaxHz);private set
    var shape by mutableStateOf(AcousticModel.Shape());private set
    private var simulationState by mutableStateOf<SimulationState>(SimulationState.Empty)
    val simulation get()=simulationState.result
    val simBusy get()=simulationState is SimulationState.Working
    private val simulationOwner=SimulationController(
        simulationLoader ?: CoroutineSimulationLoader(viewModelScope),::onSimulationState)
    var optimizationActive by mutableStateOf(false); private set
    var optimizationGeneration by mutableStateOf(0); private set
    var optimizationBest by mutableStateOf<BoreOptimizer.Candidate?>(null); private set
    var optimizationTarget by mutableStateOf(BoreOptimizer.Target()); private set
    val measurements get()=library.measurements
    val designs get()=library.designs

    private fun onLiveReading(incoming:PitchDetector.Reading) {
        reading=incoming
        val live=LivePitch.next(LivePitch.State(filteredHz,consecutiveReadings,lastMeasuredHz),incoming,instrument)
        acousticSample=incoming.diagnostic?.let {diagnosticHistory.record(SystemClock.elapsedRealtime(),incoming,live,instrument)}
        filteredHz=live.hz;consecutiveReadings=live.consecutive;lastMeasuredHz=live.lastMeasuredHz
        recording.append(SessionAnalysis.Point(
            (SystemClock.elapsedRealtime()-sessionStartedAtMs).coerceAtLeast(0),live.storedHz,incoming.confidence))
    }
    private val recording=RecordingCoordinator(object:RecordingCoordinator.Observer {
        override fun changed(state:RecordingCoordinator.State) {
            recordingState=state
        }
        override fun saved(){message=getApplication<Application>().getString(R.string.recording_saved)}
        override fun saveFailed(error:Throwable){message=getApplication<Application>().getString(R.string.recording_save_failed,error.message.orEmpty())}
        override fun discarded(success:Boolean) {
            microphoneError=if(success)null else getApplication<Application>().getString(R.string.recording_discard_failed)
            message=getApplication<Application>().getString(if(success) R.string.recording_discarded else R.string.recording_discard_failed)
        }
        override fun finalizationFailed(error:Throwable) {
            microphoneError=getApplication<Application>().getString(R.string.recording_finalize_failed,error.message.orEmpty())
            message=microphoneError.orEmpty()
        }
        override fun libraryChanged(){refreshLibrary()}
        override fun idleAfterDisposal(){sessionExecutor.shutdown()}
    },::publishCapture)

    private fun onMicrophoneError(error:String) {
        recording.microphoneFailed {microphoneError=error;message=error;mic.stop()}
    }
    private val mic=MicrophoneEngine(onReading=::onLiveReading,onError=::onMicrophoneError,onTerminal=recording::terminal)

    private fun publishCapture(file:File,snapshot:CapturedSession,complete:(Result<Unit>)->Unit) {
        // This immutable request outlives its screen and never reads a replacement capture's state.
        sessionExecutor.execute {
            val result=runCatching {
                check(file.length()>44) {getApplication<Application>().getString(R.string.recording_empty)}
                recordingStore.importCapture(file,snapshot.points,snapshot.startedAt,snapshot.instrument)
            }
            main.post {complete(result.map {})}
        }
    }

    init { mic.instrument=instrument;mic.minHz=minHz;mic.maxHz=maxHz;calculate();if(recoveredCount>0)message="Se recuperaron $recoveredCount grabaciones interrumpidas. Están en Biblioteca." }
    fun setRange(expanded:Boolean){if(instrument!=InstrumentProfile.DIDGERIDOO || listening)return;minHz=35;maxHz=if(expanded)220 else 140;mic.minHz=minHz;mic.maxHz=maxHz}
    /** Navigation owns the back stack; audio must not outlive its destination. */
    private var inMeasurementDestination=false
    fun onDestinationChanged(sessionId:String?=null,measurement:Boolean=false){
        // Normal/wide are presentations of the SAME capture, including repeated entries.
        if(measurement && inMeasurementDestination)return
        inMeasurementDestination=measurement
        microphoneStop()
        if(activeDetail!=sessionId || sessionId==null){
            stopPlayback();cancelRecordingAnalysis(clear=true);cancelSavedEvidence();clearSwipeComparison();clearDroneComponent();clearSavedYinReplay();workspace.clear();cancelComparison(clear=true);associationJob?.cancel()
            activeDetail?.let {explorers.remove(it);previews.remove(it)}
        }
        activeDetail=sessionId
    }
    fun analyzeRecording(item:RecordingStore.Session,startMs:Long=0,endMs:Long?=null,force:Boolean=false) {
        analysisOwner.request(RecordingAnalysisRequest(item.id,item.instrument,startMs,endMs,force))
    }
    fun cancelRecordingAnalysis(clear:Boolean=false) {
        analysisOwner.cancel(clear)
    }
    fun onPermissionRequest(){requestingPermission=true;permissionDenied=false}
    fun onPermissionResult(granted:Boolean,forSession:Boolean){requestingPermission=false;permissionDenied=!granted
        if(granted){if(forSession)startSession() else microphoneStart()}
        else message=MIC_PERMISSION_NOTICE
    }
    fun microphoneStart() {
        stopPlayback()
        microphoneError=null;permissionDenied=false
        if(recording.startMonitor(mic::startOwned)) {permissionAvailable();reading=PitchDetector.Reading();filteredHz=0.0;consecutiveReadings=0;clearAcousticDiagnostics()}
    }
    fun startSession() {
        if(sessionPhase!=SessionPhase.IDLE)return
        if(listening)microphoneStop()
        microphoneStart()
        if(!listening)return
        if(mic.startRecording(recordingStore.newCapture(),recordingLimitSeconds)) {
            registerCapture(checkNotNull(mic.captureId))
        }
        else {microphoneStop();message="No se pudo iniciar la grabación."}
    }
    private fun registerCapture(id:CaptureId) {
        recording.begin(id,System.currentTimeMillis(),instrument,SystemClock.elapsedRealtime())
    }
    fun finishSession() {
        if(sessionPhase!=SessionPhase.CAPTURING)return
        recording.saving()
        mic.stop()
    }
    fun discardSession() {
        if(sessionPhase!=SessionPhase.CAPTURING)return
        if(mic.discardRecording()) {
            recording.discarding()
            reading=PitchDetector.Reading();filteredHz=0.0;consecutiveReadings=0
            clearAcousticDiagnostics()
        } else {
            finishSession()
            message=getApplication<Application>().getString(R.string.recording_discard_too_late)
        }
    }
    fun microphoneStop(){
        if(sessionPhase==SessionPhase.CAPTURING){finishSession();return}
        mic.stop();recording.monitorStopped();reading=PitchDetector.Reading();filteredHz=0.0
        consecutiveReadings=0
    }
    fun notify(text:String){message=text}
    fun clearMessage(){message=""}
    fun permissionAvailable(){permissionDenied=false;if(message==MIC_PERMISSION_NOTICE)message=""}
    fun saveDesign(name:String){if(name.isBlank()){message="Escribí un nombre para el diseño.";return}
        persistMetadata {store.saveDesign(name.trim(),shape);refreshLibrary();message="Diseño guardado."}
    }
    fun selectDesign(item:LocalStore.Design){shape=item.shape;labTab=0;calculate()}
    fun duplicateDesign(item:LocalStore.Design){runCatching {store.saveDesign("${item.name} (copia)",item.shape)}
        .onSuccess {refreshLibrary()}.onFailure {message="No se pudo duplicar: ${it.message}"}}
    fun deleteDesign(item:LocalStore.Design){runCatching {trash.put("design",item.id);refreshLibrary();undoEntry=UndoEntry("design",item.id)}
        .onFailure {message="No se pudo mover a la papelera: ${it.message}"}}
    fun deleteMeasurement(item:LocalStore.Measurement){runCatching {store.deleteMeasurement(item.id)}
        .onSuccess {refreshLibrary()}.onFailure {message="No se pudo borrar: ${it.message}"}}
    fun renameSession(item:RecordingStore.Session,name:String){
        runCatching {recordingStore.rename(item.id,name)}.onSuccess {refreshLibrary()}
            .onFailure {message="No se pudo renombrar: ${it.message}"}
    }
    fun deleteSession(item:RecordingStore.Session){
        musicalTraceJobs.remove(item.id)?.cancel();musicalTraces.remove(item.id);fluteTraces.remove(item.id)
        if(playingId==item.id)stopPlayback()
        if(recordingAnalysis.sessionId==item.id)cancelRecordingAnalysis(clear=true)
        if(savedEvidence.sessionId==item.id)cancelSavedEvidence()
        if(swipeComparison.sessionId==item.id)clearSwipeComparison()
        if(droneComponent.sessionId==item.id)clearDroneComponent()
        if(savedYinReplay.sessionId==item.id)clearSavedYinReplay()
        runCatching {trash.put("session",item.id);refreshLibrary();undoEntry=UndoEntry("session",item.id)}
            .onFailure {message="No se pudo mover a la papelera: ${it.message}"}
    }
    fun recordingBytes(item:RecordingStore.Session):ByteArray?=runCatching {recordingStore.file(item.id).readBytes()}
        .onFailure {message="No se pudo abrir el WAV: ${it.message}"}.getOrNull()
    fun togglePlayback(item:RecordingStore.Session){microphoneStop();player.toggle(item.id,sessionFile(item))}
    fun restartPlayback(item:RecordingStore.Session){microphoneStop();player.restart(item.id,sessionFile(item))}
    fun seekPlayback(item:RecordingStore.Session,position:Long){microphoneStop();player.seek(item.id,sessionFile(item),position)}
    fun setPlaybackLoop(item:RecordingStore.Session,region:LongRange?){if(playingId!=item.id)seekPlayback(item,region?.first ?: 0);player.loop(region)}
    fun setPlaybackLoopOff(){player.loop(null)}
    fun playbackPositionMs():Int = player.position()
    fun stopPlayback(){player.stop()}
    fun updateShape(next:AcousticModel.Shape){shape=next;calculate()}
    fun applyPreset(index:Int) {
        shape=when(index) {
            0 -> AcousticModel.Shape(1500,40,40,40)
            1 -> AcousticModel.Shape(1500,30,53,83)
            else -> AcousticModel.Shape(1500,30,37,105)
        }
        calculate()
    }
    private fun calculate() {
        simulationOwner.request(shape)
    }
    private fun onSimulationState(state:SimulationState) {
        simulationState=state
        if(state is SimulationState.Failed)message="Error de simulación: ${state.cause.message}"
    }
    fun updateTarget(drone: Double?=null, second: Double?=null, third: Double?=null) {
        val old = optimizationTarget
        optimizationTarget=BoreOptimizer.Target(drone ?: old.droneHz,second ?: old.secondHz,third ?: old.thirdHz)
    }
    fun optimize() {
        if (optimizationActive) return
        val serial=optimizationVersion.incrementAndGet()
        val start=shape; val goal=optimizationTarget
        optimizationActive=true;optimizationGeneration=0;optimizationBest=null
        optimizerExecutor.execute {
            val result=runCatching {
                BoreOptimizer.search(start,goal,
                    cancelled={serial!=optimizationVersion.get() || Thread.currentThread().isInterrupted},
                    progress={step,best -> main.post {
                        if (serial==optimizationVersion.get()) {
                            optimizationGeneration=step;optimizationBest=best
                        }
                    }} )
            }
            main.post { if(serial==optimizationVersion.get()) {
                optimizationActive=false
                result.getOrNull()?.let { optimizationBest=it;message="Búsqueda terminada: probá el diseño encontrado." }
                result.exceptionOrNull()?.let { message="Error de búsqueda: ${it.message}" }
            }}
        }
    }
    fun cancelOptimization() {
        optimizationVersion.incrementAndGet();optimizationActive=false
        message="Búsqueda cancelada. Podés aplicar el mejor resultado parcial."
    }
    fun applyOptimization() {
        val selected=optimizationBest ?: return
        shape=selected.shape;calculate();labTab=0
        message="Perfil de la búsqueda aplicado. Guardalo para conservarlo."
    }
    fun updatePoints(nodes: List<AcousticModel.BorePoint>) {
        runCatching { updateShape(shape.withControlPoints(nodes)) }
            .onFailure { message="Perfil inválido: ${it.message}" }
    }
    fun exportProfile()=shape.csv().toByteArray(Charsets.UTF_8)
    fun exportMeasurements()=store.measurementsCsv().toByteArray(Charsets.UTF_8)
    fun measuredComparison():String {
        val real=lastMeasuredHz;val simulated=simulation?.baseHz ?: 0.0
        if(real<=0 || simulated<=0)return "Medí tu didge y simulá su geometría para comparar."
        val cents=(1200*ln(real/simulated)/ln(2.0)).toInt()
        return "Medido ${Music.hzText(real)} · Modelo ${Music.hzText(simulated)} · Desvío $cents cents (aprox.)"
    }
    fun onAppStopped(changingConfiguration:Boolean=false){
        // The retained ViewModel owns capture across Activity recreation, not a background service.
        if(changingConfiguration)return
        microphoneStop()
        stopPlayback();activeDetail?.let {explorers[it]?.looping=false};if(recordingAnalysis.running)cancelRecordingAnalysis();cancelSavedEvidence();clearSwipeComparison();clearDroneComponent();clearSavedYinReplay();workspace.cancel();cancelComparison()}
    override fun onCleared(){
        analysisOwner.close();simulationOwner.close();swipeOwner.close();onAppStopped()
        workspace.close()
        recording.dispose()
        optimizationVersion.incrementAndGet();optimizerExecutor.shutdownNow();super.onCleared()
    }
}
