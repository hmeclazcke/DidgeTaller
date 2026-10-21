package ar.didgetaller.core

/** The microphone's bounded PCM window and cadence, independent of Android and chart policy. */
class PitchStream(
    val instrument:InstrumentProfile,
    private val minHz:Int=instrument.minHz,
    private val maxHz:Int=instrument.initialMaxHz,
    private val includeVisuals:Boolean=true,
    private val observe:((PitchDetector.Diagnostic)->Unit)?=null
) {
    private val window=ShortArray(instrument.windowSamples)
    private var filled=0
    private var updates=0

    /** Called with each AudioRecord chunk; never retains the caller's buffer. */
    fun append(pcm:ShortArray,count:Int=pcm.size):PitchDetector.Reading? {
        require(count in 0..pcm.size)
        if(count==0)return null
        if(count>=window.size){
            pcm.copyInto(window,0,count-window.size,count);filled=window.size
        }else if(filled+count<=window.size){
            pcm.copyInto(window,filled,0,count);filled+=count
        }else{
            val keep=window.size-count
            window.copyInto(window,0,filled-keep,filled)
            pcm.copyInto(window,keep,0,count);filled=window.size
        }
        if(filled<window.size || ++updates%instrument.analysisChunks!=0)return null
        return PitchDetector.analyzeWithDiagnostics(window,44100,minHz,maxHz,includeVisuals,observe)
    }
}
