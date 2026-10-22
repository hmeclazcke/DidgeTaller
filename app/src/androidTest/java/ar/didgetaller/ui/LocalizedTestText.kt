package ar.didgetaller.ui

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/** Existing behavior assertions follow the emulator's real Android resource configuration. */
internal fun uiText(@StringRes resource: Int, vararg arguments: Any): String {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return if (arguments.isEmpty()) context.getString(resource) else context.getString(resource, *arguments)
}

/** Retain the uncertainty-wording assertion while its measured arguments vary by fixture. */
internal fun uiUncertainEstimatePrefix(): String = uiText(ar.didgetaller.R.string.ui_uncertain_estimate_confidence_yin_no_confirmed_note)
    .substringAfter("%1\$s").substringBefore("%2\$s").trim()
