package ar.didgetaller.data

import android.content.Context
import android.content.SharedPreferences
import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.Music
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Local-only persistence. Original Java release used same prefs and JSON keys. */
class LocalStore(context: Context) {
    private val trash=TrashStore(context)
    private val prefs: SharedPreferences=context.getSharedPreferences("local_data", Context.MODE_PRIVATE)
    data class Measurement(val id:String,val date: String,val hz: Double,val note: String,val cents:Int)
    data class Design(val id:String,val name: String,val shape: AcousticModel.Shape)

    private fun document(key: String) = JsonPreferenceDocument.array(prefs, key)
    private fun array(key: String): JSONArray = document(key).read()
    fun measurements(): List<Measurement> = array("measurements").let { arr ->
        (0 until arr.length()).mapNotNull { index ->
            arr.optJSONObject(index)?.let { MeasurementMetadata.decode(it, index) }
        }.asReversed()
    }
    fun designs(includeTrash:Boolean=false): List<Design> = array("designs").let { arr ->
        (0 until arr.length()).mapNotNull { index ->
            val value=arr.optJSONObject(index) ?: return@mapNotNull null
            runCatching { Design(value.optString("id","legacy-$index"),value.optString("name","Sin nombre"), AcousticModel.Shape(
                value.getInt("length"),value.getInt("mouth"),value.getInt("mid"),value.getInt("bell"),
                value.optJSONArray("points")?.let { nodes ->
                    (0 until nodes.length()).mapNotNull { j ->
                        val node=nodes.optJSONArray(j) ?: return@mapNotNull null
                        if(node.length()<2) null else AcousticModel.BorePoint(node.optInt(0),node.optDouble(1))
                    }
                } ?: emptyList()
            )) }.getOrNull()
        }.asReversed().filter {includeTrash || it.id !in trash.ids("design")}
    }
    fun saveMeasurement(hz: Double) {
        require(hz.isFinite() && hz>0)
        document("measurements").update { arr -> arr.put(JSONObject().put("id",UUID.randomUUID().toString())
            .put("time",SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(Date()))
            .put("hz",hz).put("note",Music.note(hz)).put("cents",Music.cents(hz))) }
    }
    fun saveDesign(name:String,shape:AcousticModel.Shape)=saveDesign(UUID.randomUUID().toString(),name,shape)
    /** The transaction owns publication; the codec preserves identity and unknown legacy fields. */
    fun saveDesign(id:String,name:String,shape:AcousticModel.Shape) {
        document("designs").update {DesignMetadata.upsert(it,id,name,shape)}
    }
    fun deleteMeasurement(id:String)=delete("measurements",id)
    fun deleteDesign(id:String)=delete("designs",id)
    private fun delete(key:String,id:String) {
        document(key).update { arr ->
            for(i in arr.length()-1 downTo 0) if(arr.optJSONObject(i)?.optString("id","legacy-$i")==id)arr.remove(i)
        }
    }
    fun measurementsCsv(): String = buildString {
        append("fecha,nota,frecuencia_hz\n")
        measurements().forEach { entry ->
            fun quote(s:String)="\"${s.replace("\"","\"\"")}\""
            append("${quote(entry.date)},${quote(entry.note)},${String.format(Locale.US,"%.2f",entry.hz)}\n")
        }
    }
}
