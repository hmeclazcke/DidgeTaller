package ar.didgetaller.core

/** Quantity counts retain provenance; spectral support is never relabeled as a verified pitch. */
data class TraceEvidence(val components: Int, val yin: Int, val highRegister: Int) {
    companion object {
        fun of(trace: MusicalTrace?, historical: List<PitchTrace.Sample>): TraceEvidence {
            if (trace == null) return TraceEvidence(0, historical.count { !it.uncertain }, 0)
            val valid = trace.samples.count { it.hz != null }
            return when (trace.meaning) {
                MusicalTrace.Meaning.ESTIMATED_PITCH -> TraceEvidence(0, valid, trace.highRegisterCandidates.size)
                MusicalTrace.Meaning.SPECTRAL_COMPONENT -> TraceEvidence(valid, 0, trace.highRegisterCandidates.size)
            }
        }
    }
}
