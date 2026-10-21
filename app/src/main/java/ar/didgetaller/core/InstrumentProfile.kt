package ar.didgetaller.core

/** Only measurement/presentation differences. The design simulator remains didgeridoo-only. */
enum class InstrumentProfile(val title:String,val minHz:Int,val maxHz:Int,val initialMaxHz:Int,
                             val windowSamples:Int,val analysisChunks:Int,val spectralMaxHz:Double) {
    DIDGERIDOO("Didgeridoo",35,220,140,16384,2,500.0),
    NATIVE_AMERICAN_FLUTE("Flauta nativa",200,1600,1600,8192,1,2000.0);
    // A small guard covers interpolation error at the nominal flute range endpoints.
    val validMinHz get()=minHz*(if(this==NATIVE_AMERICAN_FLUTE).995 else 1.0)
    val validMaxHz get()=maxHz*(if(this==NATIVE_AMERICAN_FLUTE)1.005 else 1.0)
    fun accepts(point:SessionAnalysis.Point)=point.isUsable(validMinHz,validMaxHz)
    companion object {
        fun fromStored(value:String?)=entries.firstOrNull {it.name==value} ?: DIDGERIDOO
    }
}
