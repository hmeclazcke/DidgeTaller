package ar.didgetaller.data

import android.content.Context
import ar.didgetaller.core.*
import java.io.*
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** Regenerable, versioned and bounded. No changes to original audio or the legacy index. */
class AnalysisCache(context:Context) {
    private val root=File(context.filesDir,"analysis-cache").apply {mkdirs()}
    companion object {const val VERSION=2;const val VISUAL_VERSION=3;private const val LIMIT=64L*1024*1024}
    fun fingerprint(file:File,cancelled:()->Boolean={false}):String {
        val digest=MessageDigest.getInstance("SHA-256");val bytes=ByteArray(65536)
        file.inputStream().buffered().use {input ->while(true){if(cancelled())throw CancellationException();val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
    private fun cacheFile(id:String,key:String):File {
        require(java.util.UUID.fromString(id).toString()==id)
        val hash=MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString(""){"%02x".format(it)}
        return File(root,"$id-$hash.bin")
    }
    private fun trim(){val files=root.listFiles().orEmpty().filter {it.extension=="bin"}.sortedBy {it.lastModified()};var size=files.sumOf {it.length()}
        for(file in files){if(size<=LIMIT)break;val length=file.length();if(file.delete())size-=length}}
    private fun write(file:File,block:(DataOutputStream)->Unit){val temp=File(root,file.name+".tmp")
        try{DataOutputStream(BufferedOutputStream(FileOutputStream(temp))).use(block);check(temp.renameTo(file));trim()}finally{temp.delete()}}
    fun remove(id:String){require(java.util.UUID.fromString(id).toString()==id);root.listFiles().orEmpty().filter {it.name.startsWith("$id-")}.forEach {it.delete()}}
    fun analyze(id:String,file:File,startMs:Long=0,endMs:Long?=null,cancelled:()->Boolean={false},progress:(Int)->Unit={},force:Boolean=false,instrument:InstrumentProfile=InstrumentProfile.DIDGERIDOO):WavAnalysis.Result {
        val signature=fingerprint(file,cancelled)
        val cached=cacheFile(id,"analysis-$VERSION-$signature-$startMs-$endMs-${instrument.name}")
        if(!force)runCatching {read(cached)}.getOrNull()?.let {if(cancelled())throw CancellationException();cached.setLastModified(System.currentTimeMillis());progress(100);return it}
        val result=Pcm16WavReader(file).use {source ->
            WavAnalysis.analyze(source,(startMs*source.sampleRate/1000).coerceIn(0,source.frameCount-2),
                endMs?.let {(it*source.sampleRate/1000).coerceAtMost(source.frameCount)} ?: source.frameCount,
                settings=WavAnalysis.Settings(minPitchHz=instrument.minHz,maxPitchHz=instrument.maxHz,instrument=instrument),cancelled=cancelled,progress=progress)
        }
        if(cancelled())throw CancellationException()
        runCatching {write(cached){out ->
            out.writeInt(VERSION);out.writeLong(result.startFrame);out.writeLong(result.endFrame);out.writeInt(result.sampleRate);out.writeInt(result.windows)
            out.writeInt(result.spectrum.fftSize);result.spectrum.density.forEach {out.writeDouble(it)}
            out.writeInt(result.timeline.size);result.timeline.forEach {out.writeLong(it.timeMs);out.writeDouble(it.hz ?: Double.NaN);out.writeDouble(it.confidence)}
            out.writeBoolean(result.summary!=null);result.summary?.let {out.writeDouble(it.typicalHz);out.writeDouble(it.lowHz);out.writeDouble(it.highHz);out.writeInt(it.stabilityPercent);out.writeInt(it.validPoints);out.writeInt(it.totalPoints)}
            out.writeDouble(result.medianValidHz ?: Double.NaN);out.writeDouble(result.meanValidHz ?: Double.NaN);out.writeDouble(result.validWindowFraction);out.writeDouble(result.medianPeriodicityConfidence ?: Double.NaN)
        }} // Cache failure must never discard a successful analysis.
        return result
    }
    private fun read(file:File):WavAnalysis.Result=DataInputStream(BufferedInputStream(FileInputStream(file))).use {input ->
        require(input.readInt()==VERSION);val start=input.readLong();val end=input.readLong();val rate=input.readInt();val windows=input.readInt()
        val n=input.readInt();require(n in 4096..65536 && n and (n-1)==0 && rate in 8000..192000 && windows in 1..10000)
        val density=DoubleArray(n/2+1){input.readDouble().also {require(it.isFinite() && it>=0)}}
        val count=input.readInt();require(count in 0..40000)
        val points=List(count){SessionAnalysis.Point(input.readLong(),input.readDouble().takeIf {it.isFinite()},input.readDouble())}
        val summary=if(input.readBoolean())SessionAnalysis.Summary(input.readDouble(),input.readDouble(),input.readDouble(),input.readInt(),input.readInt(),input.readInt()) else null
        val median=input.readDouble().takeIf {it.isFinite()};val mean=input.readDouble().takeIf {it.isFinite()};val fraction=input.readDouble();val confidence=input.readDouble().takeIf {it.isFinite()}
        val spectrum=PowerSpectrum(rate,n,density)
        WavAnalysis.Result(start,end,rate,windows,spectrum,HarmonicAnalysis.analyze(spectrum,summary?.typicalHz),points,summary,median,mean,fraction,confidence)
    }
    fun visual(id:String,signature:String,key:String,compute:()->AudioVisuals.Spectrogram):AudioVisuals.Spectrogram {
        val file=cacheFile(id,"visual-$VISUAL_VERSION-$signature-$key")
        runCatching {DataInputStream(BufferedInputStream(FileInputStream(file))).use {input ->
            require(input.readInt()==VISUAL_VERSION);val start=input.readLong();val end=input.readLong();val rate=input.readInt();val low=input.readDouble();val high=input.readDouble();val log=input.readBoolean()
            val columns=input.readInt();val rows=input.readInt();require(columns in 1..512 && rows in 16..512)
            val reference=input.readDouble();AudioVisuals.Spectrogram(start,end,rate,low,high,log,columns,rows,FloatArray(columns*rows){input.readFloat()},reference,32768,16384)
        }}.getOrNull()?.let {file.setLastModified(System.currentTimeMillis());return it}
        val tile=compute()
        runCatching {write(file){out ->out.writeInt(VISUAL_VERSION);out.writeLong(tile.start);out.writeLong(tile.end);out.writeInt(tile.rate);out.writeDouble(tile.lowHz);out.writeDouble(tile.highHz);out.writeBoolean(tile.logarithmic)
            out.writeInt(tile.columns);out.writeInt(tile.rows);out.writeDouble(tile.reference);tile.power.forEach {out.writeFloat(it)}}}
        return tile
    }
}
