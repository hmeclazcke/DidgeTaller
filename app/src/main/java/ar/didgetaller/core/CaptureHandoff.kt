package ar.didgetaller.core

/** Opaque identity of one microphone worker, including its recording and finalization. */
class CaptureId

data class CaptureTerminal(val id: CaptureId, val outcome: RecordingCapture.Outcome?)

/** Main-thread ownership boundary: ending an old recording does not end a replacement monitor.
 * Payloads are immutable recording snapshots; terminal delivery claims each snapshot exactly once.
 */
class CaptureHandoff<T : Any> {
    private var microphone: CaptureId? = null
    private var latestMicrophone: CaptureId? = null
    private var recording: CaptureId? = null
    private val pending = mutableMapOf<CaptureId, T>()
    private val publishing = mutableSetOf<CaptureId>()

    data class Delivery<T>(val event: CaptureTerminal, val snapshot: T?, val endsMicrophone: Boolean,
                           val ownsRecording: Boolean)

    fun start(id: CaptureId) { microphone = id; latestMicrophone = id }
    fun starting() { latestMicrophone = null }
    fun stop() { microphone = null }
    fun canNotify(id: CaptureId): Boolean = latestMicrophone === id

    fun record(id: CaptureId, snapshot: T) {
        check(microphone === id) { "Recording requires its active microphone" }
        check(id !in pending && id !in publishing) { "Recording already registered" }
        pending[id] = snapshot
        recording = id
    }

    fun update(id: CaptureId, transform: (T) -> T): T? = pending[id]?.let { transform(it).also { next -> pending[id] = next } }

    fun updateCurrent(transform: (T) -> T): T? = microphone?.let { update(it, transform) }

    fun terminal(event: CaptureTerminal): Delivery<T>? {
        val endsMicrophone = microphone === event.id
        if (endsMicrophone) microphone = null
        val snapshot = pending.remove(event.id)
        if (snapshot == null && !endsMicrophone) return null
        val ownsRecording = snapshot != null && recording === event.id
        if (snapshot != null && event.outcome is RecordingCapture.Outcome.Ready) publishing.add(event.id)
        else if (ownsRecording) recording = null
        return Delivery(event, snapshot, endsMicrophone, ownsRecording)
    }

    /** A publication acknowledgement can end only the recording whose snapshot was claimed. */
    fun published(id: CaptureId): Boolean {
        if (!publishing.remove(id) || recording !== id) return false
        recording = null
        return true
    }
}
