package ar.didgetaller.ui

import android.content.Context
import ar.didgetaller.R
import ar.didgetaller.core.InstrumentProfile
import ar.didgetaller.core.Music
import ar.didgetaller.core.PitchTrace
import ar.didgetaller.data.RecordingStore

/** Presentation labels only: stored enum names, marker types and core diagnostics stay unchanged. */
internal fun Context.instrumentLabel(profile: InstrumentProfile): String = getString(when (profile) {
    InstrumentProfile.DIDGERIDOO -> R.string.instrument_didgeridoo
    InstrumentProfile.NATIVE_AMERICAN_FLUTE -> R.string.instrument_native_flute
})

internal fun Context.plotLabel(plot: AcousticPlot): String = getString(when (plot) {
    AcousticPlot.PITCH -> R.string.ui_fundamental
    AcousticPlot.SPECTROGRAM -> R.string.ui_spectrogram
    AcousticPlot.WAVE -> R.string.ui_waveform
    AcousticPlot.SPECTRUM -> R.string.ui_harmonics
})

internal fun Context.plotLabel(plot: AcousticPlot, instrument: InstrumentProfile): String =
    if (plot == AcousticPlot.PITCH && instrument == InstrumentProfile.DIDGERIDOO)
        getString(R.string.musical_drone_title) else plotLabel(plot)

internal fun Context.reasonLabel(reason: PitchTrace.Reason?): String = if (reason == null) "" else getString(when (reason) {
    PitchTrace.Reason.LOW_CONFIDENCE -> R.string.reason_low_confidence
    PitchTrace.Reason.OUTSIDE_PROFILE -> R.string.reason_outside_profile
    PitchTrace.Reason.ISOLATED_PERIOD -> R.string.reason_isolated_period
    PitchTrace.Reason.ISOLATED_RETURN -> R.string.reason_isolated_return
    PitchTrace.Reason.UNSUPPORTED_JUMP -> R.string.reason_unsupported_jump
})

internal fun Context.noteLabel(hz: Double): String {
    val pitch = Music.pitch(hz) ?: return "—"
    return resources.getStringArray(R.array.note_names)[Math.floorMod(pitch.midi, 12)] + (Math.floorDiv(pitch.midi, 12) - 1)
}

internal fun Context.noteBase(hz: Double): String {
    val pitch = Music.pitch(hz) ?: return "—"
    return resources.getStringArray(R.array.note_base_names)[Math.floorMod(pitch.midi, 12)]
}

/** A fallback is UI text; a stored name, even an identical one, is user content. */
internal fun Context.sessionName(session: RecordingStore.Session): String =
    if (session.hasStoredName) session.name else getString(
        if (session.recovered) R.string.ui_recovered_audio else R.string.recording_name_fallback
    )

internal fun Context.markerLabel(type: String): String = when (type) {
    "Posible toot" -> getString(R.string.marker_possible_toot)
    "Vocalización" -> getString(R.string.marker_vocalization)
    else -> type // User-authored content is displayed verbatim.
}

internal fun Context.presetLabel(key: String): String = when (key) {
    "Cónico" -> getString(R.string.ui_conical)
    "Campana" -> getString(R.string.ui_bell)
    else -> key
}

internal fun Context.categoryLabel(key: String): String = getString(when (key) {
    "medición" -> R.string.category_measurement
    "sesión" -> R.string.category_session
    else -> R.string.category_design
})
