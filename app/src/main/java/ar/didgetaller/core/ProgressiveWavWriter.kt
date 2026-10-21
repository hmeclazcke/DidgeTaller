package ar.didgetaller.core

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/** PCM16 mono, constant memory. Checkpoint headers are repaired from actual length after a crash. */
class ProgressiveWavWriter(val file:File,val sampleRate:Int=44100,
                           private val freeBytes:()->Long={file.parentFile!!.usableSpace}) : Closeable {
    private val output=RandomAccessFile(file,"rw")
    private var buffer=ByteArray(8192)
    var samplesWritten=0L;private set
    private var checkpoint=0L
    private var closed=false
    init {
        try {require(sampleRate>0);check(file.length()==0L){"El archivo de captura ya existe."};ensureSpace(44);output.write(WavCodec.encode(ShortArray(0),0,sampleRate))}
        catch(e:Exception){output.close();throw e}
    }
    private fun ensureSpace(bytes:Int){check(freeBytes()>bytes+2L*1024*1024){"Espacio insuficiente. Se conserva el audio capturado."}}
    fun append(samples:ShortArray,count:Int) {
        check(!closed);require(count in 0..samples.size)
        require((samplesWritten+count)*2<0xfffffff0L){"Se alcanzó el tamaño máximo de WAV."}
        ensureSpace(count*2)
        if(buffer.size<count*2)buffer=ByteArray(count*2)
        repeat(count){i ->val v=samples[i].toInt();buffer[i*2]=v.toByte();buffer[i*2+1]=(v ushr 8).toByte()}
        output.write(buffer,0,count*2);samplesWritten+=count
        if(samplesWritten-checkpoint>=sampleRate){writeHeader(output,samplesWritten*2);output.fd.sync();checkpoint=samplesWritten}
    }
    override fun close(){if(!closed){closed=true;try{writeHeader(output,samplesWritten*2);output.fd.sync()}finally{output.close()}}}
    companion object {
        private fun le32(output:RandomAccessFile,value:Long){repeat(4){output.write((value ushr (it*8)).toInt() and 255)}}
        private fun writeHeader(output:RandomAccessFile,bytes:Long){val pos=output.filePointer;output.seek(4);le32(output,36+bytes);output.seek(40);le32(output,bytes);output.seek(pos)}
        /** Only for our UUID.part files, never for arbitrary historic WAVs. */
        fun repair(file:File):Boolean {
            if(file.length()<=44)return false
            RandomAccessFile(file,"rw").use {output ->
                val signature=ByteArray(4);output.readFully(signature)
                require(signature.toString(Charsets.US_ASCII)=="RIFF"){"Captura parcial no compatible."}
                val bytes=(output.length()-44)/2*2
                output.setLength(44+bytes);writeHeader(output,bytes);output.fd.sync()
            }
            return true
        }
    }
}
