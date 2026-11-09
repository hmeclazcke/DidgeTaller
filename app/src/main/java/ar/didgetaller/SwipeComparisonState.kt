package ar.didgetaller

import ar.didgetaller.core.*

/** Read-only compatibility presentation of the single comparison owner. */
data class SwipeComparisonState(private val owned: SwipeComparisonStatus = SwipeComparisonStatus.Idle) {
    val sessionId = owned.request?.sessionId
    val enabled = owned.enabled
    val running = owned.running
    val progress = owned.progress
    val curve = owned.curve
    val failed = owned.failure != null
    val range = swipeComparisonWindow(enabled)
}
