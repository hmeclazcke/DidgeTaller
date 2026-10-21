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

    private fun array(key: String): JSONArray = runCatching { JSONArray(prefs.getString(key,"[]")) }.getOrDefault(JSONArray())
    fun measurements(): List<Measurement> = array("measurements").let { arr ->
        (0 until arr.length()).mapNotNull { index ->
            arr.optJSONObject(index)?.let { value ->
                val hz=value.optString("hz").toDoubleOrNull() ?: value.optDouble("hz",0.0)
                if(hz<=0.0) null else Measurement(value.optString("id","legacy-$index"),value.optString("time"),
                    hz,value.optString("note",Music.note(hz)),value.optInt("cents",Music.cents(hz)))
            }
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
        require(hz>0)
        val arr=array("measurements")
        arr.put(JSONObject().put("id",UUID.randomUUID().toString())
            .put("time",SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(Date()))
            .put("hz",hz).put("note",Music.note(hz)).put("cents",Music.cents(hz)))
        check(prefs.edit().putString("measurements",arr.toString()).commit())
    }
    fun saveDesign(name: String, shape: AcousticModel.Shape) {
        val arr=array("designs")
        arr.put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name.take(70)).put("length",shape.lengthMm)
            .put("mouth",shape.mouthMm).put("mid",shape.middleMm).put("bell",shape.bellMm)
            .put("points",JSONArray().also { arr ->
                shape.points.forEach { p -> arr.put(JSONArray().put(p.positionMm).put(p.diameterMm)) }
            }))
        check(prefs.edit().putString("designs",arr.toString()).commit())
    }
    fun deleteMeasurement(id:String)=delete("measurements",id)
    fun deleteDesign(id:String)=delete("designs",id)
    private fun delete(key:String,id:String) {
        val arr=array(key);val next=JSONArray()
        for(i in 0 until arr.length()) if(arr.optJSONObject(i)?.optString("id","legacy-$i")!=id)next.put(arr.get(i))
        check(prefs.edit().putString(key,next.toString()).commit())
    }
    fun measurementsCsv(): String = buildString {
        append("fecha,nota,frecuencia_hz\n")
        measurements().forEach { entry ->
            fun quote(s:String)="\"${s.replace("\"","\"\"")}\""
            append("${quote(entry.date)},${quote(entry.note)},${String.format(Locale.US,"%.2f",entry.hz)}\n")
        }
    }
}
