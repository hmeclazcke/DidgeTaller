package ar.didgetaller.core

/** Parsed recording units, separate from editable localized text and Android controls. */
class AnalysisInterval private constructor(val startMs: Long, val endMs: Long?) {
    fun request(id: String, instrument: InstrumentProfile) = RecordingAnalysisRequest(id, instrument, startMs, endMs)

    companion object {
        fun parse(start: String, end: String, durationMs: Long): AnalysisInterval? {
            // Whole-file analysis uses actual WAV bounds, including historical missing duration metadata.
            if (start.isBlank() && end.isBlank()) return AnalysisInterval(0, null)
            val from = if (start.isBlank()) 0 else milliseconds(start) ?: return null
            val until = if (end.isBlank()) null else milliseconds(end) ?: return null
            return bounded(from, until, durationMs)
        }

        private fun milliseconds(text: String): Long? {
            val seconds = text.replace(',', '.').toDoubleOrNull() ?: return null
            val value = seconds * 1000
            if (!value.isFinite() || value < 0 || value >= Long.MAX_VALUE.toDouble()) return null
            return value.toLong()
        }

        private fun bounded(start: Long, end: Long?, duration: Long): AnalysisInterval? {
            if (duration <= 0 || start >= duration) return null
            if (end != null && (end <= start || end > duration)) return null
            return AnalysisInterval(start, end)
        }
    }
}

/** Pure admission rules keep destination/resource policy out of Android effects. */
object AnalysisAdmission {
    fun opensWorkspace(selected: String?, requested: String, loaded: String?) = selected == requested && loaded != requested
    fun acceptsDidgeridoo(selected: String?, requested: String, instrument: InstrumentProfile) =
        selected == requested && instrument == InstrumentProfile.DIDGERIDOO
}
