package ar.didgetaller.core

/** Bounded, transient evidence. Never a source of session points, notes or chart links. */
class AcousticDiagnostics {
    enum class Signal { NO_RELEVANT_SIGNAL, ACCEPTED_PITCH, UNCERTAIN_PITCH }

    data class Sample(
        val observedAtMs:Long,
        val instrument:InstrumentProfile,
        val signal:Signal,
        val rms:Double,
        val periodicity:Double,
        val candidateHz:Double?,
        val detectorReason:String
    ) {
        val rejectionReason:String? get()=when {
            signal==Signal.ACCEPTED_PITCH -> null
            detectorReason=="accepted" -> "profile_gate"
            else -> detectorReason
        }
    }

    companion object { const val MAX_SAMPLES=128 }
    private val retained=ArrayDeque<Sample>()
    val samples:List<Sample> get()=retained.toList()

    fun record(observedAtMs:Long,reading:PitchDetector.Reading,live:LivePitch.State,instrument:InstrumentProfile):Sample {
        val detail=requireNotNull(reading.diagnostic) {"No diagnostic observation accompanies this reading"}
        val signal=when {
            detail.reason=="low_amplitude" -> Signal.NO_RELEVANT_SIGNAL
            live.storedHz!=null -> Signal.ACCEPTED_PITCH
            else -> Signal.UNCERTAIN_PITCH
        }
        val sample=Sample(observedAtMs,instrument,signal,reading.rms,reading.confidence,detail.candidateHz,detail.reason)
        if(retained.size==MAX_SAMPLES)retained.removeFirst()
        retained.addLast(sample)
        return sample
    }

    fun clear()=retained.clear()
}
