package ar.didgetaller.audio

import android.graphics.Bitmap
import androidx.compose.runtime.*
import ar.didgetaller.core.*
import ar.didgetaller.data.*
import kotlinx.coroutines.*

data class VisualRequest(val startMs:Long,val endMs:Long,val lowHz:Double=0.0,val highHz:Double=500.0,
                         val logarithmic:Boolean=false,val rangeDb:Double=70.0,val reference:Double=0.0)
data class VisualState(val sessionId:String?=null,val durationMs:Long=0,val rate:Int=44100,val frames:Long=0,
                       val signature:String="",val wave:AudioVisuals.Wave?=null,val spectrogram:AudioVisuals.Spectrogram?=null,
                       val bitmap:Bitmap?=null,val loading:Boolean=false,val progress:Int=0,val error:String?=null)

/** One active viewport and a disk LRU; no full-recording spectrogram in Compose state. */
class SessionWorkspace(private val scope:CoroutineScope,private val recordings:RecordingStore,private val cache:AnalysisCache) {
    var state by mutableStateOf(VisualState());private set
    private var job:Job?=null
    private var version=0
    private var waveKey="";private var spectrumKey=""
    fun open(item:RecordingStore.Session){clear();val serial=version;state=VisualState(item.id,loading=true)
        job=scope.launch {
            try {val info=withContext(Dispatchers.IO){Pcm16WavReader(recordings.file(item.id)).use {source ->
                val signature=cache.fingerprint(recordings.file(item.id)){!isActive}
                VisualState(item.id,source.frameCount*1000/source.sampleRate,source.sampleRate,source.frameCount,signature)
            }};if(serial==version)state=info}
            catch(e:CancellationException){throw e}catch(e:Exception){if(serial==version)state=state.copy(loading=false,error=e.message)}
        }
    }
    fun cancel(){if(!state.loading)return;version++;job?.cancel();state=state.copy(loading=false,error="Visualización cancelada.")}
    fun clear(){version++;job?.cancel();waveKey="";spectrumKey="";state=VisualState()}
    fun render(item:RecordingStore.Session,request:VisualRequest,spectrogram:Boolean) {
        if(state.sessionId!=item.id || state.frames<2)return
        job?.cancel();val serial=++version;val info=state
        val key="${request.startMs}-${request.endMs}-${request.lowHz}-${request.highHz}-${request.logarithmic}"
        val oldWave=info.wave.takeIf {waveKey==key};val oldTile=info.spectrogram.takeIf {spectrumKey==key}
        // Keep the completed tile visible in its original coordinates while a replacement loads.
        state=state.copy(loading=true,progress=0,error=null)
        job=scope.launch {
            try {
                val result=withContext(Dispatchers.Default){
                    val task=currentCoroutineContext();val cancelled={!task.isActive}
                    Pcm16WavReader(recordings.file(item.id)).use {source ->
                        val start=(request.startMs*source.sampleRate/1000).coerceIn(0,source.frameCount-2)
                        val end=(request.endMs*source.sampleRate/1000).coerceIn(start+2,source.frameCount)
                        if(spectrogram){
                            val tile=oldTile ?: cache.visual(item.id,info.signature,key){AudioVisuals.spectrogram(source,start,end,request.lowHz,request.highHz,request.logarithmic,cancelled=cancelled,progress={percent ->
                                scope.launch {if(serial==version)state=state.copy(progress=percent)}
                            })}
                            ensureActive()
                            val pixels=IntArray(tile.columns*tile.rows)
                            val reference=request.reference.takeIf {it>1e-16} ?: tile.reference
                            for(x in 0 until tile.columns)for(y in 0 until tile.rows){
                                val db=if(reference>1e-16)10*kotlin.math.log10(kotlin.math.max(1e-30,tile.power[x*tile.rows+y].toDouble())/reference) else null
                                pixels[(tile.rows-1-y)*tile.columns+x]=SpectralPalette.color(db,request.rangeDb)
                            }
                            val bitmap=Bitmap.createBitmap(pixels,tile.columns,tile.rows,Bitmap.Config.ARGB_8888)
                            stateSnapshot(info,tile,bitmap)
                        }else info.copy(wave=oldWave ?: AudioVisuals.waveform(source,start,end,cancelled=cancelled),loading=false,error=null)
                    }
                }
                if(serial==version){state=result.copy(progress=100);if(spectrogram)spectrumKey=key else waveKey=key}
            }catch(e:CancellationException){throw e}catch(e:Exception){if(serial==version)state=state.copy(loading=false,error=e.message)}
        }
    }
    private fun stateSnapshot(info:VisualState,tile:AudioVisuals.Spectrogram,bitmap:Bitmap)=info.copy(spectrogram=tile,bitmap=bitmap,loading=false,error=null)
}
