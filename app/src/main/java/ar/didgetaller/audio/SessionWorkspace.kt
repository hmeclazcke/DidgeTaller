package ar.didgetaller.audio

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ar.didgetaller.core.VisualKind
import ar.didgetaller.core.Pcm16WavReader
import ar.didgetaller.core.WorkspaceData
import ar.didgetaller.core.WorkspaceController
import ar.didgetaller.core.WorkspaceState
import ar.didgetaller.data.AnalysisCache
import ar.didgetaller.data.RecordingStore
import kotlinx.coroutines.CoroutineScope

typealias VisualRequest = ar.didgetaller.core.VisualRequest
typealias VisualState = WorkspaceState<Bitmap>

/** Read-only Compose projection; the pure controller is the only state-transition owner. */
class SessionWorkspace(scope: CoroutineScope, recordings: RecordingStore, cache: AnalysisCache) {
    var state by mutableStateOf(VisualState()); private set
    private val data = WorkspaceData({ id -> Pcm16WavReader(recordings.file(id)) },
        { id, cancelled -> cache.fingerprint(recordings.file(id), cancelled) }, cache::visual)
    private val loader = CoroutineWorkspaceLoader(scope, data, { raster ->
        Bitmap.createBitmap(raster.pixels, raster.tile.columns, raster.tile.rows, Bitmap.Config.ARGB_8888)
    })
    private val owner = WorkspaceController(loader) { state = it }

    fun open(item: RecordingStore.Session) = owner.open(item.id)
    fun render(item: RecordingStore.Session, request: VisualRequest, kind: VisualKind) =
        owner.render(item.id, request, kind)
    fun cancel() = owner.cancel()
    fun clear() = owner.clear()
    fun close() = owner.close()
}
