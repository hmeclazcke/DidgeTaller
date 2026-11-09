package ar.didgetaller.audio

import kotlinx.coroutines.*

/** Shared scheduling policy; domain controllers alone decide whether a callback may publish. */
internal fun <T> submitProgressWork(scope: CoroutineScope, dispatcher: CoroutineDispatcher,
    progress: (Int) -> Unit, complete: (Result<T>) -> Unit,
    compute: (() -> Boolean, (Int) -> Unit) -> T): Result<Job> = try {
    scope.ensureActive()
    val job = scope.launch {
        val owner = this
        val result = try {
            Result.success(withContext(dispatcher) {
                val worker = currentCoroutineContext()
                compute({ !worker.isActive }, { percent -> owner.launch { progress(percent) } })
            })
        } catch (error: Exception) { Result.failure(error) }
        // Observer failures are not input, decoder or cache failures.
        complete(result)
        (result.exceptionOrNull() as? CancellationException)?.let { throw it }
    }
    Result.success(job)
} catch (error: Exception) { Result.failure(error) }
