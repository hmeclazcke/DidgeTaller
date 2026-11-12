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
import ar.didgetaller.core.AnalysisState
import ar.didgetaller.core.AnalysisAdmission
import ar.didgetaller.core.AnalysisLoader
import ar.didgetaller.core.AnalysisProgress
import ar.didgetaller.core.OptimizationRequest
import ar.didgetaller.core.OptimizationSnapshot
import ar.didgetaller.core.optimizeBore
import ar.didgetaller.core.optimizationNotice
import ar.didgetaller.core.LatestAnalysis
import ar.didgetaller.core.MusicalTraceCatalog
import ar.didgetaller.core.TraceSnapshot
import ar.didgetaller.core.YinReplayRequest
import ar.didgetaller.core.ComponentRequest
import ar.didgetaller.core.MusicalTraceRequest
import ar.didgetaller.core.EvidenceRequest
import ar.didgetaller.core.RecordingComparisonRequest
import ar.didgetaller.audio.CoroutineAnalysisLoader
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
import kotlin.math.abs
import kotlin.math.ln

typealias SessionPhase = RecordingPhase
private const val MIC_PERMISSION_NOTICE="Para medir hace falta permiso de micrófono. Podés concederlo desde Ajustes."

data class UndoEntry(val kind:String,val id:String)
class TallerViewModel internal constructor(
    application: Application, simulationLoader: SimulationLoader?,
    analysisLoader: RecordingAnalysisLoader? = null,
    swipeLoader: SwipeComparisonLoader? = null,
    diagnosticLoaders: SavedDiagnosticLoaders? = null,
    optimizationLoader: AnalysisLoader<OptimizationRequest,OptimizationSnapshot>? = null,
): AndroidViewModel(application) {
    constructor(application: Application) : this(application, null)
    val metronome = ar.didgetaller.audio.PracticeMetronome(application)
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
    private var activeDetail by mutableStateOf<String?>(null)
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
    private var associationState by mutableStateOf<AnalysisState<AcousticModel.Shape,AcousticModel.Simulation>>(AnalysisState.Idle)
    val associatedSimulation get()=associationState.value
    private val associationOwner=LatestAnalysis(CoroutineAnalysisLoader<AcousticModel.Shape,AcousticModel.Simulation>(viewModelScope,Dispatchers.Default) {
        shape:AcousticModel.Shape,_,_->AcousticModel.evaluate(shape)
    }) { associationState=it }
    private var comparisonState by mutableStateOf<AnalysisState<RecordingComparisonRequest,Pair<WavAnalysis.Result,WavAnalysis.Result>>>(AnalysisState.Idle)
    val comparison get()=comparisonState.comparisonState()
    private val comparisonOwner=LatestAnalysis(CoroutineAnalysisLoader<RecordingComparisonRequest,Pair<WavAnalysis.Result,WavAnalysis.Result>>(viewModelScope,Dispatchers.Default) {
        request:RecordingComparisonRequest,cancelled,progress->
        val a=request.first;val b=request.second
        val first=cache.analyze(a.sessionId,recordingStore.file(a.sessionId),a.startMs,a.endMs,
            instrument=a.instrument,cancelled=cancelled,progress={progress(AnalysisProgress(it/2))})
        val second=cache.analyze(b.sessionId,recordingStore.file(b.sessionId),b.startMs,b.endMs,
            instrument=b.instrument,cancelled=cancelled,progress={progress(AnalysisProgress(50+it/2))})
        first to second
    }) { comparisonState=it }
    fun setRecordingLimit(seconds:Int){tools.setLimitSeconds(seconds);recordingLimitSeconds=seconds}
    fun availableMegabytes()=getApplication<Application>().filesDir.usableSpace/(1024*1024)
    fun sessionFile(item:RecordingStore.Session)=recordingStore.file(item.id)
    fun openTools(item:RecordingStore.Session){if(AnalysisAdmission.opensWorkspace(activeDetail,item.id,workspace.state.sessionId)){workspace.open(item);loadTools(item)}}
    private fun loadTools(item:RecordingStore.Session){markers=tools.markers(item.id);associatedDesignId=tools.designId(item.id)
        associationOwner.clear()
        val selected=store.designs(true).find {it.id==associatedDesignId}
        if(selected!=null)associationOwner.select(selected.shape)
    }
    private fun persistMetadata(change:()->Unit){
        runCatching(change).onFailure {message=getApplication<Application>().getString(R.string.library_metadata_write_failed)}
    }
    fun associate(item:RecordingStore.Session,designId:String?){if(item.instrument!=InstrumentProfile.DIDGERIDOO && designId!=null)return
        persistMetadata {tools.associate(item.id,designId);loadTools(item)}}
    fun addMarker(item:RecordingStore.Session,time:Long,type:String){persistMetadata {tools.addMarker(item.id,time,type,"");markers=tools.markers(item.id)}}
    fun removeMarker(item:RecordingStore.Session,id:String){persistMetadata {tools.deleteMarker(item.id,id);markers=tools.markers(item.id)}}
    fun compare(a:RecordingStore.Session,b:RecordingStore.Session,aStart:Long=0,aEnd:Long?=null,bStart:Long=0,bEnd:Long?=null){
        comparisonOwner.replace(RecordingComparisonRequest(RecordingAnalysisRequest(a.id,a.instrument,aStart,aEnd),
            RecordingAnalysisRequest(b.id,b.instrument,bStart,bEnd)))
    }
    fun cancelComparison(clear:Boolean=false){if(clear)comparisonOwner.clear() else comparisonOwner.cancel()}
    fun compare(request:RecordingComparisonRequest){comparisonOwner.replace(request)}
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
    };ensureActive();refreshLibrary();undoEntry=null;message="Papelera vaciada."}
        catch(e:Exception){ensureActive();refreshLibrary();message="No se pudo completar el vaciado: ${e.message}"}}}

    private val sessionExecutor=Executors.newSingleThreadExecutor()
    private var analysisState by mutableStateOf<RecordingAnalysisStatus>(RecordingAnalysisStatus.Idle)
    val recordingAnalysis get()=RecordingAnalysisState(analysisState)
    private val analysisOwner=RecordingAnalysisController(analysisLoader ?: CoroutineRecordingAnalysisLoader(
        viewModelScope, { request,cancelled,progress ->
            cache.analyze(request.sessionId,recordingStore.file(request.sessionId),request.startMs,request.endMs,
                cancelled=cancelled,progress=progress,force=request.force,instrument=request.instrument)
        }), { analysisState=it })
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val swipeDispatcher=Dispatchers.IO.limitedParallelism(1)
    private val savedLoaders=diagnosticLoaders ?: SavedDiagnosticLoaders(
        CoroutineAnalysisLoader(viewModelScope,swipeDispatcher) { request:YinReplayRequest,cancelled,_->
            Pcm16WavReader(recordingStore.file(request.sessionId)).use {
                SavedYinReplay.analyze(it,request.expandedRange,cancelled)
            }
        },
        CoroutineAnalysisLoader(viewModelScope,swipeDispatcher) { request:ComponentRequest,cancelled,progress->
            cache.droneComponent(request.sessionId,recordingStore.file(request.sessionId),cancelled=cancelled,progress={progress(AnalysisProgress(it))})
        },
        CoroutineAnalysisLoader(viewModelScope,Dispatchers.IO) { request:EvidenceRequest,cancelled,_->
            Pcm16WavReader(recordingStore.file(request.sessionId)).use {
                SavedPitchEvidence.inspect(it,request.positionMs,request.instrument,cancelled)
            }
        })
    private var yinState by mutableStateOf<AnalysisState<YinReplayRequest,SavedYinReplay.Result>>(AnalysisState.Idle)
    private var componentState by mutableStateOf<AnalysisState<ComponentRequest,DroneComponentCurve>>(AnalysisState.Idle)
    private var evidenceState by mutableStateOf<AnalysisState<EvidenceRequest,SavedPitchEvidence.Snapshot>>(AnalysisState.Idle)
    private val yinOwner=LatestAnalysis(savedLoaders.yin) { yinState=it }
    private val componentOwner=LatestAnalysis(savedLoaders.component) { componentState=it }
    private val evidenceOwner=LatestAnalysis(savedLoaders.evidence) { evidenceState=it }
    val savedYinReplay get()=yinState.yinReplayState()
    val droneComponent get()=componentState.componentState(activeDetail,traceSnapshot)
    val savedEvidence get()=evidenceState.evidenceState()
    private var swipeState by mutableStateOf<SwipeComparisonStatus>(SwipeComparisonStatus.Idle)
    val swipeComparison get()=SwipeComparisonState(swipeState)
    private val swipeOwner=SwipeComparisonController(swipeLoader ?: CoroutineSwipeComparisonLoader(
        viewModelScope, swipeDispatcher, { request,cancelled,progress ->
            Pcm16WavReader(recordingStore.file(request.sessionId)).use { source ->
                SwipeAnalysis().analyze(source,InstrumentProfile.DIDGERIDOO,cancelled,progress)
            }
        }), ::publishSwipeComparison)

    private var traceSnapshot by mutableStateOf(TraceSnapshot())
    private val traceCatalog=MusicalTraceCatalog(savedLoaders.component,{traceSnapshot=it})
    private fun traceRequest(item:RecordingStore.Session)=MusicalTraceRequest(item.id,item.durationMs,item.timeline.toList(),item.instrument)
    fun musicalTrace(item:RecordingStore.Session)=traceCatalog.trace(traceRequest(item),traceSnapshot)
    fun musicalTraceFailed(id:String)=id in traceSnapshot.failures
    fun requestMusicalTrace(item:RecordingStore.Session){traceCatalog.request(traceRequest(item))}
    fun releaseMusicalTraceRequest(id:String){traceCatalog.release(id)}
    fun analyzeSavedYin(item:RecordingStore.Session){
        if(AnalysisAdmission.acceptsDidgeridoo(activeDetail,item.id,item.instrument))
            yinOwner.select(YinReplayRequest(item.id,SavedYinReplay.usesExpandedRange(item.timeline)))
    }
    private fun clearSavedYinReplay(){yinOwner.clear()}
    fun analyzeDroneComponent(item:RecordingStore.Session){
        if(AnalysisAdmission.acceptsDidgeridoo(activeDetail,item.id,item.instrument))componentOwner.select(ComponentRequest(item.id))
    }
    private fun clearDroneComponent(){componentOwner.clear()}
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
        if(activeDetail==item.id)evidenceOwner.replace(EvidenceRequest(item.id,positionMs,item.instrument))
    }
    fun cancelSavedEvidence(){evidenceOwner.clear()}
    private val main=Handler(Looper.getMainLooper())
    var pendingExport:ByteArray?=null
    var pendingExportFile:File?=null
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
    private var optimizationState by mutableStateOf<AnalysisState<OptimizationRequest,OptimizationSnapshot>>(AnalysisState.Idle)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val optimizationDispatcher=Dispatchers.Default.limitedParallelism(1)
    private val optimizationOwner=LatestAnalysis(optimizationLoader ?: CoroutineAnalysisLoader(viewModelScope,optimizationDispatcher,::optimizeBore),::publishOptimization)
    val optimizationActive get()=optimizationState is AnalysisState.Loading
    val optimizationGeneration get()=optimizationState.value?.generation ?: 0
    val optimizationBest get()=optimizationState.value?.best
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
            metronome.captureActive(state.listening || state.phase != RecordingPhase.IDLE)
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
        metronome.pause()
        // Normal/wide are presentations of the SAME capture, including repeated entries.
        if(measurement && inMeasurementDestination)return
        inMeasurementDestination=measurement
        microphoneStop()
        if(activeDetail!=sessionId || sessionId==null){
            stopPlayback();cancelRecordingAnalysis(clear=true);cancelSavedEvidence();clearSwipeComparison();clearDroneComponent();clearSavedYinReplay();workspace.clear();cancelComparison(clear=true);associationOwner.clear()
            activeDetail?.let {explorers.remove(it);previews.remove(it)}
        }
        activeDetail=sessionId;traceCatalog.pin(sessionId)
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
        metronome.captureActive(true)
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
    fun reloadDesigns(){refreshLibrary()}
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
        traceCatalog.remove(item.id)
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
        if(!optimizationActive)optimizationOwner.replace(OptimizationRequest(shape,optimizationTarget))
    }
    private fun publishOptimization(state:AnalysisState<OptimizationRequest,OptimizationSnapshot>){
        optimizationState=state
        optimizationNotice(state)?.let {message=it}
    }
    fun cancelOptimization(){optimizationOwner.cancel()}
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
        metronome.pause()
        // The retained ViewModel owns capture across Activity recreation, not a background service.
        if(changingConfiguration)return
        microphoneStop()
        stopPlayback();activeDetail?.let {explorers[it]?.looping=false};if(recordingAnalysis.running)cancelRecordingAnalysis();cancelSavedEvidence();clearSwipeComparison();clearDroneComponent();clearSavedYinReplay();workspace.cancel();cancelComparison()}
    override fun onCleared(){
        metronome.close()
        player.close()
        optimizationOwner.close()
        analysisOwner.close();simulationOwner.close();swipeOwner.close()
        traceCatalog.close();yinOwner.close();componentOwner.close();evidenceOwner.close()
        comparisonOwner.close();associationOwner.close();onAppStopped()
        workspace.close()
        recording.dispose()
        super.onCleared()
    }
}
