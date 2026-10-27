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
import ar.didgetaller.core.ProgressiveWavWriter
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/** Dedicated audio thread, no continuous allocations on UI thread beyond UI snapshots.
 * Start ONLY after RECORD_AUDIO permission has been granted by the Activity.
 */
class MicrophoneEngine(
    private val onReading: (PitchDetector.Reading)->Unit,
    private val onRecordingStopped: ()->Unit,
    private val onRecordingReady: (File)->Unit,
    private val onError: (String)->Unit,
    private val onRecordingFailed: (String)->Unit = onError,
    private val onRecordingDiscarded: (Boolean)->Unit = {}
) {
    companion object { const val RATE=44100; const val MAX_RECORD_SECONDS=900; private const val FRAME=16384; private const val CHUNK=4096 }
    private val active=AtomicBoolean(false)
    private val mainHandler=Handler(Looper.getMainLooper())
    @Volatile private var audio:AudioRecord?=null
    @Volatile private var thread:Thread?=null
    private val recordLock=Any()
    @Volatile private var recording=false
    private var discardRequested=false
    private var discardedFile:File?=null
    private var writer:ProgressiveWavWriter?=null
    private var limitSamples=RATE*MAX_RECORD_SECONDS.toLong()
    @Volatile var instrument=InstrumentProfile.DIDGERIDOO
    @Volatile var minHz=35
    @Volatile var maxHz=140

    @SuppressLint("MissingPermission")
    @Synchronized fun start(): Boolean {
        if(active.get())return true
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
        thread=Thread({readLoop(record)},"didge-mic").apply {start()}
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
    fun startRecording(file:File,limitSeconds:Int=MAX_RECORD_SECONDS):Boolean {
        synchronized(recordLock) {
            if(!active.get() || recording)return false
            try {writer=ProgressiveWavWriter(file,RATE);limitSamples=RATE*limitSeconds.toLong();discardRequested=false;discardedFile=null;recording=true}
            catch(e:Exception){onError("No se pudo grabar: ${e.message}");return false}
        }
        return true
    }
    fun discardRecording():Boolean {
        synchronized(recordLock) {
            if(!recording || discardRequested)return false
            val partial=writer?.file
            discardedFile=runCatching {
                requireNotNull(partial)
                require(partial.extension=="part" && java.util.UUID.fromString(partial.nameWithoutExtension).toString()==partial.nameWithoutExtension)
                val target=File(partial.parentFile,"${partial.nameWithoutExtension}.discard")
                check(!target.exists() && partial.renameTo(target))
                target
            }.getOrNull()
            discardRequested=true
        }
        stop()
        return true
    }
    private fun finishRecording(released:Boolean) {
        val (finished,discard,discardFile)=synchronized(recordLock) {
            if(!recording)return
            recording=false
            val result=Triple(writer,discardRequested,discardedFile)
            writer=null;discardRequested=false;discardedFile=null
            result
        }
        if(finished!=null) {
            val finalization=runCatching {finished.close()}
            if(discard) {
                val cleanup=runCatching {check(discardFile!=null && discardFile.delete())}
                mainHandler.post {onRecordingDiscarded(released && finalization.isSuccess && cleanup.isSuccess)}
                return
            }
            mainHandler.post(onRecordingStopped)
            finalization.onSuccess {onRecordingReady(finished.file)}
                .onFailure {error ->mainHandler.post {onRecordingFailed("No se pudo cerrar el WAV: ${error.message}. La captura queda pendiente de recuperación.")}}
        }
    }
    private fun readLoop(record:AudioRecord) {
        // Freeze one consistent profile/range for this capture. UI changes require stopping first.
        val stream=PitchStream(instrument,minHz,maxHz);val chunk=ShortArray(CHUNK)
        try {
            while(active.get() && audio===record && !Thread.currentThread().isInterrupted) {
                val n=record.read(chunk,0,CHUNK,AudioRecord.READ_BLOCKING)
                if(n<=0) {
                    if(active.get() && audio === record)mainHandler.post {onError("Error al leer el micrófono ($n).")}
                    break
                }
                var reachedLimit=false
                synchronized(recordLock) {
                    if(recording) {
                        val current=writer!!
                        val count=min(n.toLong(),limitSamples-current.samplesWritten).toInt()
                        if(count>0)current.append(chunk,count)
                        reachedLimit=current.samplesWritten>=limitSamples
                    }
                }
                stream.append(chunk,n)?.let {result->
                    // A queued result from an older capture must not reach a new profile/session.
                    mainHandler.post {if(active.get() && audio===record)onReading(result)}
                }
                if(reachedLimit)break
            }
        }catch(ex:Exception){if(active.get() && audio === record)mainHandler.post {onError("Falló la captura: ${ex.message}")}}
        finally{
            if (audio === record) {active.set(false);audio=null}
            runCatching {record.stop()}
            val released=runCatching {record.release()}.isSuccess
            if(!released)mainHandler.post {onError("No se pudo liberar el micrófono.")}
            finishRecording(released)
        }
    }
}
