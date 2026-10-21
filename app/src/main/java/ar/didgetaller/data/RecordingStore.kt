package ar.didgetaller.data

import android.content.Context
import ar.didgetaller.core.SessionAnalysis
import ar.didgetaller.core.ProgressiveWavWriter
import ar.didgetaller.core.Pcm16WavReader
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** WAV files stay in app-private storage; metadata never points outside this directory. */
class RecordingStore(context: Context) {
    private val trash=TrashStore(context)
    data class Session(
        val id:String, val name:String, val createdAt:Long, val durationMs:Long, val sizeBytes:Long,
        val summary:SessionAnalysis.Summary?, val timeline:List<SessionAnalysis.Point>,
        val legacy:Boolean, val recovered:Boolean=false
    )
    private val directory=File(context.filesDir,"recordings").apply { mkdirs() }
    private val prefs=context.getSharedPreferences("local_data",Context.MODE_PRIVATE)

    private fun entries():JSONArray=runCatching {JSONArray(prefs.getString("recordings","[]"))}.getOrDefault(JSONArray())
    @Synchronized fun list(includeTrash:Boolean=false):List<Session> {
        val array=entries()
        val indexed=(0 until array.length()).mapNotNull {i ->
            val item=array.optJSONObject(i) ?: return@mapNotNull null
            val id=item.optString("id")
            val file=runCatching {file(id)}.getOrNull() ?: return@mapNotNull null
            if(!file.isFile) return@mapNotNull null
            val summary=item.optJSONObject("summary")?.let { value ->
                runCatching { SessionAnalysis.Summary(
                    value.getDouble("typicalHz"),value.getDouble("lowHz"),value.getDouble("highHz"),
                    value.getInt("stabilityPercent"),value.getInt("validPoints"),value.getInt("totalPoints")
                ) }.getOrNull()
            }
            val samples=item.optJSONArray("timeline")?.let { values ->
                (0 until values.length()).mapNotNull { index ->
                    val point=values.optJSONArray(index) ?: return@mapNotNull null
                    if(point.length()<3) return@mapNotNull null
                    val hz=if(point.isNull(1)) null else point.optDouble(1).takeIf { it.isFinite() && it>0 }
                    SessionAnalysis.Point(point.optLong(0),hz,point.optDouble(2))
                }
            }.orEmpty()
            Session(id,item.optString("name","Grabación"),item.optLong("createdAt"),
                item.optLong("durationMs"),file.length(),summary,samples,!item.has("timeline"),
                item.optBoolean("recovered",false))
        }
        val known=indexed.mapTo(mutableSetOf()) {it.id}
        val recovered=directory.listFiles().orEmpty().mapNotNull {candidate ->
            if(!candidate.isFile || !candidate.name.endsWith(".wav") || candidate.length()<=44)return@mapNotNull null
            val id=candidate.name.removeSuffix(".wav")
            if(id in known || !runCatching {UUID.fromString(id).toString()==id}.getOrDefault(false))return@mapNotNull null
            Session(id,"Audio recuperado",candidate.lastModified(),
                duration(candidate),
                candidate.length(),null,emptyList(),true,true)
        }
        return (indexed+recovered).filter {includeTrash || it.id !in trash.ids("session")}.sortedByDescending {it.createdAt}
    }
    fun newCapture():File=File(directory,"${UUID.randomUUID()}.part")
    fun recoverPending():Int {
        var count=0
        directory.listFiles().orEmpty().filter {it.extension=="part"}.forEach {partial ->
            val id=partial.nameWithoutExtension
            if(runCatching {UUID.fromString(id).toString()==id}.getOrDefault(false))runCatching {
                if(ProgressiveWavWriter.repair(partial) && !file(id).exists() && partial.renameTo(file(id)))count++
            }
        }
        return count
    }
    @Synchronized fun importCapture(partial:File,points:List<SessionAnalysis.Point>,createdAt:Long):Session {
        require(partial.parentFile?.canonicalFile==directory.canonicalFile && partial.extension=="part")
        val id=partial.nameWithoutExtension;val destination=file(id)
        val duration=Pcm16WavReader(partial).use {it.frameCount*1000/it.sampleRate}
        check(!destination.exists() && partial.renameTo(destination)){"No se pudo conservar la captura."}
        return indexCapture(id,duration,points,createdAt)
    }
    @Synchronized fun save(bytes:ByteArray,points:List<SessionAnalysis.Point> = emptyList(),
                           createdAt:Long=System.currentTimeMillis()):Session {
        require(bytes.size>44 && String(bytes,0,4,Charsets.US_ASCII)=="RIFF")
        val id=UUID.randomUUID().toString()
        val destination=file(id)
        val temporary=File(directory,"$id.tmp")
        try {
            temporary.outputStream().use {it.write(bytes);it.flush()}
            check(temporary.renameTo(destination)) {"No se pudo conservar el WAV"}
            val duration=duration(destination)
            return indexCapture(id,duration,points,createdAt)
        } finally {temporary.delete()}
    }
    private fun indexCapture(id:String,duration:Long,points:List<SessionAnalysis.Point>,createdAt:Long):Session {
            val destination=file(id)
            val now=createdAt
            val name="Sesión ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.getDefault()).format(java.util.Date(now))}"
            val summary=SessionAnalysis.summarize(points)
            val array=entries()
            val item=JSONObject().put("id",id).put("name",name).put("createdAt",now).put("durationMs",duration)
                .put("timeline",JSONArray().also { timeline ->
                    points.forEach { point -> timeline.put(JSONArray().put(point.timeMs)
                        .put(point.hz ?: JSONObject.NULL).put(point.confidence)) }
                })
            if(summary!=null)item.put("summary",JSONObject()
                .put("typicalHz",summary.typicalHz).put("lowHz",summary.lowHz).put("highHz",summary.highHz)
                .put("stabilityPercent",summary.stabilityPercent).put("validPoints",summary.validPoints)
                .put("totalPoints",summary.totalPoints))
            array.put(item)
            check(prefs.edit().putString("recordings",array.toString()).commit())
            return Session(id,name,now,duration,destination.length(),summary,points,false)
    }
    @Synchronized fun rename(id:String,name:String) {
        require(name.isNotBlank())
        val array=entries()
        var found=false
        for(i in 0 until array.length()) array.optJSONObject(i)?.takeIf {it.optString("id")==id}?.let {
            it.put("name",name.trim().take(80));found=true
        }
        if(!found) {
            val orphan=file(id)
            check(orphan.isFile) {"No se encontró el WAV"}
            array.put(JSONObject().put("id",id).put("name",name.trim().take(80))
                .put("createdAt",orphan.lastModified())
                .put("durationMs",duration(orphan))
                .put("recovered",true))
        }
        check(prefs.edit().putString("recordings",array.toString()).commit())
    }
    @Synchronized fun delete(id:String) {
        val source=entries();val next=JSONArray()
        for(i in 0 until source.length()) {
            val item=source.optJSONObject(i) ?: continue
            if(item.optString("id")!=id)next.put(item)
        }
        check(prefs.edit().putString("recordings",next.toString()).commit())
        check(!file(id).exists() || file(id).delete()){ "No se pudo eliminar el audio; permanece en la papelera." }
    }
    fun file(id:String):File {
        require(UUID.fromString(id).toString()==id) {"Identificador inválido"}
        return File(directory,"$id.wav")
    }
    private fun duration(file:File)=runCatching {Pcm16WavReader(file).use {it.frameCount*1000/it.sampleRate}}.getOrDefault(0L)
}
