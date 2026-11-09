package ar.didgetaller.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import ar.didgetaller.core.PitchDetector
import ar.didgetaller.core.PitchStream
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.RecordingCapture
import ar.didgetaller.core.CaptureTerminal
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Dedicated audio thread, no continuous allocations on UI thread beyond UI snapshots.
 * Start ONLY after RECORD_AUDIO permission has been granted by the Activity.
 */
class MicrophoneEngine(
    private val onReading: (PitchDetector.Reading)->Unit,
    private val onError: (String)->Unit,
    private val onTerminal: (CaptureTerminal)->Unit
) {
    companion object { const val RATE=44100; const val MAX_RECORD_SECONDS=900; private const val CHUNK=4096 }
    private val active=AtomicBoolean(false)
    private val mainHandler=Handler(Looper.getMainLooper())
    @Volatile private var audio:AudioRecord?=null
    @Volatile private var thread:Thread?=null
    @Volatile private var capture:RecordingCapture?=null
    val captureId get()=capture?.id
    @Volatile var instrument=InstrumentProfile.DIDGERIDOO
    @Volatile var minHz=35
    @Volatile var maxHz=140

    @SuppressLint("MissingPermission")
    @Synchronized fun start(): Boolean {
        if(active.get())return true
        // A failed replacement attempt also retires queued errors from the previous worker.
        val ownedCapture=RecordingCapture()
        capture=ownedCapture
        val buffer=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        if(buffer<=0){onError("El micrófono no admite audio PCM a 44,1 kHz.");return false}
        val record=try {
            AudioRecord(MediaRecorder.AudioSource.MIC,RATE,AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,maxOf(buffer,CHUNK*4))
        }catch(ex:SecurityException) { onError("No hay permiso de micrófono.");return false }
        catch(ex:Exception) {onError("No se pudo inicializar el micrófono: ${ex.message}");return false}
        if(record.state!=AudioRecord.STATE_INITIALIZED){record.release();onError("Micrófono no disponible.");return false}
        try {record.startRecording()}catch(ex:Exception){record.release();onError("No se pudo comenzar: ${ex.message}");return false}
        audio=record;active.set(true)
        thread=Thread({readLoop(record,ownedCapture)},"didge-mic").apply {start()}
        return true
    }
    @Synchronized fun stop() {
        active.set(false)
        val r=audio
        audio=null
        runCatching { r?.stop() } // unblock read; release happens on worker
        thread?.interrupt()
        thread=null
    }
    fun isRunning()=active.get()
    fun startOwned()=if(start())captureId else null
    @Synchronized fun startRecording(file:File,limitSeconds:Int=MAX_RECORD_SECONDS):Boolean {
        if(!active.get())return false
        return try {capture?.start(file,RATE,limitSeconds) == true}
        catch(e:Exception){onError("No se pudo grabar: ${e.message}");false}
    }
    @Synchronized fun discardRecording():Boolean {
        if(capture?.discard()!=true)return false
        stop()
        return true
    }
    private fun finishRecording(ownedCapture:RecordingCapture,released:Boolean) {
        val event=ownedCapture.complete(released) ?: return
        // File finalization stays on its worker; all state transitions share one main-thread delivery.
        mainHandler.post {onTerminal(event)}
    }
    private fun readLoop(record:AudioRecord,ownedCapture:RecordingCapture) {
        // Freeze one consistent profile/range for this capture. UI changes require stopping first.
        val stream=PitchStream(instrument,minHz,maxHz);val chunk=ShortArray(CHUNK)
        try {
            while(active.get() && audio===record && !Thread.currentThread().isInterrupted) {
                val n=record.read(chunk,0,CHUNK,AudioRecord.READ_BLOCKING)
                if(n<=0) {
                    if(active.get() && audio === record)captureError(ownedCapture,"Error al leer el micrófono ($n).")
                    break
                }
                val reachedLimit=ownedCapture.append(chunk,n)
                stream.append(chunk,n)?.let {result->
                    // A queued result from an older capture must not reach a new profile/session.
                    mainHandler.post {if(active.get() && audio===record)onReading(result)}
                }
                if(reachedLimit)break
            }
        }catch(ex:Exception){if(active.get() && audio === record)captureError(ownedCapture,"Falló la captura: ${ex.message}")}
        finally{
            synchronized(this) {
                if (audio === record) {active.set(false);audio=null}
            }
            runCatching {record.stop()}
            val released=runCatching {record.release()}.isSuccess
            if(!released)captureError(ownedCapture,"No se pudo liberar el micrófono.")
            finishRecording(ownedCapture,released)
        }
    }
    private fun captureError(ownedCapture:RecordingCapture,message:String) {
        mainHandler.post {if(capture===ownedCapture)onError(message)}
    }
}
