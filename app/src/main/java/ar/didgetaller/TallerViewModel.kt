package ar.didgetaller

import ar.didgetaller.core.LivePitch
import ar.didgetaller.core.AcousticDiagnostics
import ar.didgetaller.core.SavedPitchEvidence
import ar.didgetaller.core.SwipeAnalysis
import ar.didgetaller.core.SwipeCurve
import ar.didgetaller.core.DroneComponentAnalysis
import ar.didgetaller.core.DroneComponentCurve
import ar.didgetaller.core.SavedYinReplay
import ar.didgetaller.core.AxisWindow

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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ln

enum class SessionPhase { IDLE, CAPTURING, SAVING }
enum class PitchLayerMode { FUNDAMENTAL, COMPONENT, BOTH }
private const val MIC_PERMISSION_NOTICE="Para medir hace falta permiso de micrófono. Podés concederlo desde Ajustes."
data class RecordingAnalysisState(val sessionId:String?=null,val running:Boolean=false,val progress:Int=0,
                                  val result:WavAnalysis.Result?=null,val error:String?=null, val startMs:Long=0,val endMs:Long?=null)
data class UndoEntry(val kind:String,val id:String)
data class ComparisonState(val running:Boolean=false,val progress:Int=0,val a:WavAnalysis.Result?=null,val b:WavAnalysis.Result?=null,val error:String?=null)
data class SavedEvidenceState(val sessionId:String?=null,val running:Boolean=false,
    val result:SavedPitchEvidence.Snapshot?=null,val failed:Boolean=false)

data class SwipeComparisonState(val sessionId:String?=null,val enabled:Boolean=false,val running:Boolean=false,
    val progress:Int=0,val curve:SwipeCurve?=null,val failed:Boolean=false)
data class DroneComponentState(val sessionId:String?=null,val running:Boolean=false,val progress:Int=0,
    val curve:DroneComponentCurve?=null,val failed:Boolean=false)
data class SavedYinReplayState(val sessionId:String?=null,val running:Boolean=false,
    val result:SavedYinReplay.Result?=null,val failed:Boolean=false)

class TallerViewModel(application: Application): AndroidViewModel(application) {
    private val store=LocalStore(application)
    private val recordingStore=RecordingStore(application)
    private val recoveredCount=recordingStore.recoverPending()
    private val cache=AnalysisCache(application)
    private val trash=TrashStore(application)
    private val tools=SessionToolsStore(application)
    val workspace=SessionWorkspace(viewModelScope,recordingStore,cache)
    private val explorers=mutableMapOf<String,ar.didgetaller.ui.ExplorerState>()
    fun explorer(id:String)=explorers.getOrPut(id){ar.didgetaller.ui.ExplorerState(sessions.firstOrNull {it.id==id}?.instrument ?: InstrumentProfile.DIDGERIDOO)}
    private val previews=mutableMapOf<String,ar.didgetaller.ui.ExplorerState>()
    fun preview(id:String)=previews.getOrPut(id){explorer(id).let {ar.didgetaller.ui.ExplorerState(it.instrument,it.selection)}}
    private var activeDetail:String?=null
    var viewerOriginalOrientation:Int?=null
    var instrument by mutableStateOf(tools.instrument());private set
    private var captureInstrument=instrument
    fun selectInstrument(profile:InstrumentProfile){
        if(listening || sessionPhase!=SessionPhase.IDLE)return
        instrument=profile;tools.setInstrument(profile);minHz=profile.minHz;maxHz=profile.initialMaxHz
        mic.instrument=profile;mic.minHz=minHz;mic.maxHz=maxHz
        reading=PitchDetector.Reading();filteredHz=0.0;consecutiveReadings=0
        clearAcousticDiagnostics()
    }
    var recordingLimitSeconds by mutableStateOf(tools.limitSeconds());private set
    var undoEntry by mutableStateOf<UndoEntry?>(null);private set
    var trashCount by mutableStateOf(trash.ids("session").size+trash.ids("design").size);private set
    var trashedSessions by mutableStateOf(recordingStore.list(true).filter {it.id in trash.ids("session")});private set
    var trashedDesigns by mutableStateOf(store.designs(true).filter {it.id in trash.ids("design")});private set
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
    fun associate(item:RecordingStore.Session,designId:String?){if(item.instrument!=InstrumentProfile.DIDGERIDOO && designId!=null)return;tools.associate(item.id,designId);loadTools(item)}
    fun addMarker(item:RecordingStore.Session,time:Long,type:String){tools.addMarker(item.id,time,type,"");markers=tools.markers(item.id)}
    fun removeMarker(item:RecordingStore.Session,id:String){tools.deleteMarker(item.id,id);markers=tools.markers(item.id)}
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
    private fun refreshLibrary(){sessions=recordingStore.list();designs=store.designs()
        trashCount=trash.ids("session").size+trash.ids("design").size
        trashedSessions=recordingStore.list(true).filter {it.id in trash.ids("session")}
        trashedDesigns=store.designs(true).filter {it.id in trash.ids("design")}}
    fun restore(kind:String,id:String){runCatching {trash.restore(kind,id);refreshLibrary();undoEntry=null}.onFailure {message="No se pudo restaurar: ${it.message}"}}
    fun undoDelete(){undoEntry?.let {restore(it.kind,it.id)}}
    fun clearUndo(){undoEntry=null}
    fun emptyTrash(){viewModelScope.launch {try {withContext(Dispatchers.IO){
        for(id in trash.ids("session")){recordingStore.delete(id);cache.remove(id);tools.forget(id);trash.restore("session",id)}
        for(id in trash.ids("design")){store.deleteDesign(id);tools.forgetDesign(id);trash.restore("design",id)}
    };refreshLibrary();undoEntry=null;message="Papelera vaciada."}catch(e:Exception){refreshLibrary();message="No se pudo completar el vaciado: ${e.message}"}}}

    private val executor=Executors.newSingleThreadExecutor()
    private val sessionExecutor=Executors.newSingleThreadExecutor()
    private val analysisExecutor=Executors.newSingleThreadExecutor()
    private val analysisVersion=AtomicInteger(0)
    var recordingAnalysis by mutableStateOf(RecordingAnalysisState());private set
    var savedEvidence by mutableStateOf(SavedEvidenceState());private set
    private var evidenceJob:Job?=null
    var swipeComparison by mutableStateOf(SwipeComparisonState());private set
    var droneComponent by mutableStateOf(DroneComponentState());private set
    var savedYinReplay by mutableStateOf(SavedYinReplayState());private set
    var pitchLayerMode by mutableStateOf(PitchLayerMode.BOTH);private set
    fun choosePitchLayer(mode:PitchLayerMode){pitchLayerMode=mode}
    private var swipeJob:Job?=null
    private var droneComponentJob:Job?=null
    private var savedYinReplayJob:Job?=null
    private val swipeDispatcher=Dispatchers.IO.limitedParallelism(1)
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
                    Pcm16WavReader(sessionFile(item)).use { source ->
                        DroneComponentAnalysis().analyze(source,cancelled={!task.isActive},progress={p->
                            main.post {if(task.isActive && droneComponent.sessionId==item.id)
                                droneComponent=droneComponent.copy(progress=p)}
                        })
                    }
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
        if(item.instrument!=InstrumentProfile.DIDGERIDOO)return
        swipeJob?.cancel()
        val range=if(enabled)AxisWindow(item.instrument.minHz*.95,item.instrument.initialMaxHz*1.05) else null
        explorer(item.id).configureComparisonRange(range);preview(item.id).configureComparisonRange(range)
        val cached=swipeComparison.takeIf {it.sessionId==item.id}?.curve
        swipeComparison=SwipeComparisonState(item.id,enabled,enabled && cached==null,curve=cached)
        if(!enabled || cached!=null)return
        swipeJob=viewModelScope.launch {
            try {
                val result=withContext(swipeDispatcher){
                    val task=currentCoroutineContext()
                    Pcm16WavReader(sessionFile(item)).use {source->
                        SwipeAnalysis().analyze(source,item.instrument,cancelled={!task.isActive},progress={p->
                            main.post {if(task.isActive && swipeComparison.sessionId==item.id && swipeComparison.enabled)
                                swipeComparison=swipeComparison.copy(progress=p)}
                        })
                    }
                }
                ensureActive()
                if(activeDetail==item.id && swipeComparison.enabled)swipeComparison=swipeComparison.copy(running=false,progress=100,curve=result)
            }catch(e:CancellationException){throw e}
            catch(e:Exception){ensureActive();if(activeDetail==item.id)swipeComparison=swipeComparison.copy(running=false,failed=true)}
        }
    }
    private fun clearSwipeComparison(){
        swipeJob?.cancel()
        swipeComparison.sessionId?.let {id->explorers[id]?.configureComparisonRange(null);previews[id]?.configureComparisonRange(null)}
        swipeComparison=SwipeComparisonState()
    }

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
    private var cleared=false
    private val main=Handler(Looper.getMainLooper())
    private val simVersion=AtomicInteger(0)
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
    var listening by mutableStateOf(false);private set
    var sessionPhase by mutableStateOf(SessionPhase.IDLE);private set
    var sessionStartedAtMs by mutableStateOf(0L);private set
    private var sessionStartedAtWallMs=0L
    var sessionTimeline by mutableStateOf<List<SessionAnalysis.Point>>(emptyList());private set
    var sessions by mutableStateOf(recordingStore.list());private set
    val stableReading:Boolean get()=listening && consecutiveReadings>=5 && reading.hz>0 && filteredHz>0
    private var pendingTimeline:List<SessionAnalysis.Point> = emptyList()
    var playingId by mutableStateOf<String?>(null);private set
    var playbackPaused by mutableStateOf(false);private set
    private val player=SessionPlayer({id,paused ->playingId=id;playbackPaused=paused},{message=it})
    private var consecutiveReadings=0
    var message by mutableStateOf("");private set
    var minHz by mutableStateOf(instrument.minHz);private set
    var maxHz by mutableStateOf(instrument.initialMaxHz);private set
    var shape by mutableStateOf(AcousticModel.Shape());private set
    var simulation by mutableStateOf<AcousticModel.Simulation?>(null);private set
    var simBusy by mutableStateOf(true);private set
    var optimizationActive by mutableStateOf(false); private set
    var optimizationGeneration by mutableStateOf(0); private set
    var optimizationBest by mutableStateOf<BoreOptimizer.Candidate?>(null); private set
    var optimizationTarget by mutableStateOf(BoreOptimizer.Target()); private set
    var measurements by mutableStateOf(store.measurements());private set
    var designs by mutableStateOf(store.designs());private set

    private val mic=MicrophoneEngine(onReading={incoming ->
        reading=incoming
        val live=LivePitch.next(LivePitch.State(filteredHz,consecutiveReadings,lastMeasuredHz),incoming,instrument)
        acousticSample=incoming.diagnostic?.let {diagnosticHistory.record(SystemClock.elapsedRealtime(),incoming,live,instrument)}
        filteredHz=live.hz;consecutiveReadings=live.consecutive;lastMeasuredHz=live.lastMeasuredHz
        if(sessionPhase==SessionPhase.CAPTURING) {
            sessionTimeline=sessionTimeline+SessionAnalysis.Point(
                (SystemClock.elapsedRealtime()-sessionStartedAtMs).coerceAtLeast(0),
                live.storedHz,incoming.confidence)
        }
    },onRecordingStopped={
        if(sessionPhase==SessionPhase.CAPTURING)finishSession()
    },onRecordingReady={file ->
        main.post {
            if(sessionPhase==SessionPhase.CAPTURING)finishSession()
            val points=pendingTimeline;val profile=captureInstrument
            sessionExecutor.execute {
                val result=runCatching {
                    check(file.length()>44) {"No se capturó audio. Intentá de nuevo."}
                    recordingStore.importCapture(file,points,sessionStartedAtWallMs,profile)
                }
                main.post {
                    pendingTimeline=emptyList();sessionPhase=SessionPhase.IDLE
                    result.onSuccess {sessions=recordingStore.list();message="Sesión guardada en Biblioteca."}
                        .onFailure {message="No se pudo guardar la sesión: ${it.message}"}
                    if(cleared)sessionExecutor.shutdown()
                }
            }
        }
    },onRecordingFailed={error ->
        if(sessionPhase==SessionPhase.CAPTURING)finishSession()
        pendingTimeline=emptyList();sessionPhase=SessionPhase.IDLE;listening=false
        microphoneError=error;message=error
        if(cleared)sessionExecutor.shutdown()
    },onError={error ->
        microphoneError=error;message=error
        if(sessionPhase==SessionPhase.CAPTURING)finishSession() else listening=false
    })

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
            pitchLayerMode=PitchLayerMode.BOTH
            stopPlayback();cancelRecordingAnalysis(clear=true);cancelSavedEvidence();clearSwipeComparison();clearDroneComponent();clearSavedYinReplay();workspace.clear();cancelComparison(clear=true);associationJob?.cancel()
            activeDetail?.let {explorers.remove(it);previews.remove(it)}
        }
        activeDetail=sessionId
    }
    fun analyzeRecording(item:RecordingStore.Session,startMs:Long=0,endMs:Long?=null,force:Boolean=false) {
        val serial=analysisVersion.incrementAndGet()
        recordingAnalysis=RecordingAnalysisState(item.id,running=true,startMs=startMs,endMs=endMs)
        analysisExecutor.execute {
            val result=runCatching {cache.analyze(item.id,recordingStore.file(item.id),startMs,endMs,
                cancelled={serial!=analysisVersion.get() || Thread.currentThread().isInterrupted},force=force,instrument=item.instrument,progress={percent ->
                    main.post {if(serial==analysisVersion.get())recordingAnalysis=recordingAnalysis.copy(progress=percent)}
                })}
            main.post {if(serial==analysisVersion.get() && !cleared) {
                recordingAnalysis=RecordingAnalysisState(item.id,result=result.getOrNull(),
                    progress=if(result.isSuccess)100 else 0,error=result.exceptionOrNull()?.let {it.message ?: "Error al leer el WAV."},startMs=startMs,endMs=endMs)
            }}
        }
    }
    fun cancelRecordingAnalysis(clear:Boolean=false) {
        analysisVersion.incrementAndGet()
        recordingAnalysis=if(clear)RecordingAnalysisState() else recordingAnalysis.copy(running=false,error="Análisis cancelado.")
    }
    fun onPermissionRequest(){requestingPermission=true;permissionDenied=false}
    fun onPermissionResult(granted:Boolean,forSession:Boolean){requestingPermission=false;permissionDenied=!granted
        if(granted){if(forSession)startSession() else microphoneStart()}
        else message=MIC_PERMISSION_NOTICE
    }
    fun microphoneStart() {
        stopPlayback()
        microphoneError=null;permissionDenied=false
        listening=mic.start()
        if(listening) {permissionAvailable();reading=PitchDetector.Reading();filteredHz=0.0;consecutiveReadings=0;clearAcousticDiagnostics()}
    }
    fun startSession() {
        if(sessionPhase!=SessionPhase.IDLE)return
        if(listening)microphoneStop()
        captureInstrument=instrument
        microphoneStart()
        if(!listening)return
        sessionTimeline=emptyList();pendingTimeline=emptyList()
        sessionStartedAtMs=SystemClock.elapsedRealtime()
        sessionStartedAtWallMs=System.currentTimeMillis()
        if(mic.startRecording(recordingStore.newCapture(),recordingLimitSeconds))sessionPhase=SessionPhase.CAPTURING
        else {microphoneStop();message="No se pudo iniciar la grabación."}
    }
    fun finishSession() {
        if(sessionPhase!=SessionPhase.CAPTURING)return
        sessionPhase=SessionPhase.SAVING
        pendingTimeline=sessionTimeline
        mic.stop();listening=false
    }
    fun microphoneStop(){
        if(sessionPhase==SessionPhase.CAPTURING){finishSession();return}
        mic.stop();listening=false;reading=PitchDetector.Reading();filteredHz=0.0
        consecutiveReadings=0
    }
    fun notify(text:String){message=text}
    fun clearMessage(){message=""}
    fun permissionAvailable(){permissionDenied=false;if(message==MIC_PERMISSION_NOTICE)message=""}
    fun saveDesign(name:String){if(name.isBlank()){message="Escribí un nombre para el diseño.";return}
        store.saveDesign(name.trim(),shape);designs=store.designs();message="Diseño guardado."
    }
    fun selectDesign(item:LocalStore.Design){shape=item.shape;labTab=0;calculate()}
    fun duplicateDesign(item:LocalStore.Design){runCatching {store.saveDesign("${item.name} (copia)",item.shape)}
        .onSuccess {designs=store.designs()}.onFailure {message="No se pudo duplicar: ${it.message}"}}
    fun deleteDesign(item:LocalStore.Design){runCatching {trash.put("design",item.id);refreshLibrary();undoEntry=UndoEntry("design",item.id)}
        .onFailure {message="No se pudo mover a la papelera: ${it.message}"}}
    fun deleteMeasurement(item:LocalStore.Measurement){runCatching {store.deleteMeasurement(item.id)}
        .onSuccess {measurements=store.measurements()}.onFailure {message="No se pudo borrar: ${it.message}"}}
    fun renameSession(item:RecordingStore.Session,name:String){
        runCatching {recordingStore.rename(item.id,name)}.onSuccess {sessions=recordingStore.list()}
            .onFailure {message="No se pudo renombrar: ${it.message}"}
    }
    fun deleteSession(item:RecordingStore.Session){
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
        val serial=simVersion.incrementAndGet();val selected=shape
        simBusy=true
        executor.execute {
            val result=runCatching { AcousticModel.evaluate(selected) }
            main.post {if(serial==simVersion.get()){
                simulation=result.getOrNull();simBusy=false
                result.exceptionOrNull()?.let {message="Error de simulación: ${it.message}"}
            }}
        }
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
        cleared=true;onAppStopped();executor.shutdownNow()
        analysisVersion.incrementAndGet();analysisExecutor.shutdownNow()
        if(sessionPhase==SessionPhase.IDLE)sessionExecutor.shutdown()
        optimizationVersion.incrementAndGet();optimizerExecutor.shutdownNow();super.onCleared()
    }
}
