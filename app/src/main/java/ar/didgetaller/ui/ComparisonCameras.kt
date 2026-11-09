package ar.didgetaller.ui

import ar.didgetaller.core.SwipeComparisonStatus
import ar.didgetaller.core.swipeComparisonWindow

/** Camera selection is UI state, independent of Android lifecycle, PCM work and progress updates. */
internal fun reconcileComparisonCameras(previous: SwipeComparisonStatus, next: SwipeComparisonStatus,
    existing: Map<String, ExplorerState>, create: (String) -> ExplorerState) {
    val previousId = previous.request?.sessionId?.takeIf { previous.enabled }
    val nextId = next.request?.sessionId?.takeIf { next.enabled }
    if (previousId == nextId) return
    previousId?.let { existing[it]?.configureComparisonRange(null) }
    nextId?.let { create(it).configureComparisonRange(swipeComparisonWindow(true)) }
}
