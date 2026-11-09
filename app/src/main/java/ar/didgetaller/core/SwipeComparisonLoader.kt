package ar.didgetaller.core

/** SWIPE comparison is didgeridoo-only; the request never reads the current UI instrument. */
data class SwipeComparisonRequest(val sessionId: String)

fun interface SwipeComparisonTask { fun cancel() }

/** Callbacks, including scheduling failures, return on the owner's thread. */
fun interface SwipeComparisonLoader {
    fun submit(request: SwipeComparisonRequest, progress: (Int) -> Unit,
               complete: (Result<SwipeCurve>) -> Unit): Result<SwipeComparisonTask>
}

/** Presentation padding for the existing experimental comparison, not detector thresholds. */
fun swipeComparisonWindow(enabled: Boolean): AxisWindow? = if (enabled)
    InstrumentProfile.DIDGERIDOO.let { AxisWindow(it.minHz * .95, it.initialMaxHz * 1.05) } else null
