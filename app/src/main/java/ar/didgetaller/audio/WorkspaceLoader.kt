package ar.didgetaller.audio

import ar.didgetaller.core.*
import java.io.Closeable
import kotlinx.coroutines.*

/** Scheduling adapter; dispatchers and image creation are controlled independently in tests. */
internal class CoroutineWorkspaceLoader<Source, Image>(private val scope: CoroutineScope,
    private val data: WorkspaceData<Source>, private val image: (WorkspaceRaster) -> Image,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.Default) : WorkspaceLoader<Image>
    where Source : MonoPcmSource, Source : Closeable {

    override fun open(sessionId: String, complete: (Result<WorkspaceSource>) -> Unit): WorkspaceTask =
        submit(ioDispatcher, {}, complete) { cancelled, _ -> data.open(sessionId, cancelled) }

    override fun render(work: VisualWork, progress: (Int) -> Unit,
                        complete: (Result<PreparedVisual<Image>>) -> Unit): WorkspaceTask =
        submit(renderDispatcher, progress, complete) { cancelled, update ->
            when (work.kind) {
                VisualKind.WAVE -> PreparedVisual.Wave(data.wave(work, cancelled))
                VisualKind.SPECTROGRAM -> {
                    val raster = data.spectral(work, cancelled, update)
                    PreparedVisual.Spectral(raster.tile, image(raster))
                }
            }
        }

    private fun <T> submit(dispatcher: CoroutineDispatcher, progress: (Int) -> Unit,
                           complete: (Result<T>) -> Unit,
                           compute: (() -> Boolean, (Int) -> Unit) -> T): WorkspaceTask {
        scope.ensureActive()
        val job = scope.launch {
            val owner = this
            val outcome = try {
                Result.success(withContext(dispatcher) {
                    val task = currentCoroutineContext()
                    compute({ !task.isActive }, { percent -> owner.launch { progress(percent) } })
                })
            } catch (error: Exception) { Result.failure(error) }
            // Observer exceptions are not file/analysis failures and must not be swallowed.
            complete(outcome)
            (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
        }
        return WorkspaceTask { job.cancel() }
    }
}
