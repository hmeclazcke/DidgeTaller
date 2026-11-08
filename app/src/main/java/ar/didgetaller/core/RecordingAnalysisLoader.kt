package ar.didgetaller.core

data class RecordingAnalysisRequest(val sessionId: String, val instrument: InstrumentProfile,
    val startMs: Long = 0, val endMs: Long? = null, val force: Boolean = false)

fun interface RecordingAnalysisTask { fun cancel() }

/** Progress and completion return on the owner's thread, including failures to schedule. */
fun interface RecordingAnalysisLoader {
    fun submit(request: RecordingAnalysisRequest, progress: (Int) -> Unit,
               complete: (Result<WavAnalysis.Result>) -> Unit): Result<RecordingAnalysisTask>
}
