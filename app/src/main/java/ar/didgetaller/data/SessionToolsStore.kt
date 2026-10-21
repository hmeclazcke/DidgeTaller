package ar.didgetaller.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Additive metadata only. Original recordings/designs and WAVs stay untouched. */
class SessionToolsStore(context:Context) {
    private val prefs=context.getSharedPreferences("session_tools",0)
    data class Marker(val id:String,val timeMs:Long,val type:String,val label:String)
    private fun root()=JSONObject(prefs.getString("extras","{}") ?: "{}")
    private fun save(root:JSONObject){check(prefs.edit().putString("extras",root.toString()).commit())}
    fun designId(id:String):String?=root().optJSONObject(id)?.optString("designId")?.takeIf {it.isNotBlank()}
    fun associate(id:String,designId:String?){val r=root();val value=r.optJSONObject(id) ?: JSONObject();if(designId==null)value.remove("designId") else value.put("designId",designId);r.put(id,value);save(r)}
    fun markers(id:String):List<Marker>{val arr=root().optJSONObject(id)?.optJSONArray("markers") ?: return emptyList()
        return (0 until arr.length()).mapNotNull {i ->arr.optJSONObject(i)?.let {Marker(it.getString("id"),it.getLong("timeMs"),it.getString("type"),it.optString("label"))}}.sortedBy {it.timeMs}}
    fun addMarker(id:String,timeMs:Long,type:String,label:String){require(timeMs>=0);val r=root();val value=r.optJSONObject(id) ?: JSONObject();val arr=value.optJSONArray("markers") ?: JSONArray()
        arr.put(JSONObject().put("id",UUID.randomUUID().toString()).put("timeMs",timeMs).put("type",type).put("label",label.take(120)))
        value.put("markers",arr);r.put(id,value);save(r)}
    fun deleteMarker(id:String,markerId:String){val r=root();val value=r.optJSONObject(id) ?: return;val old=value.optJSONArray("markers") ?: return
        val arr=JSONArray();for(i in 0 until old.length())if(old.getJSONObject(i).optString("id")!=markerId)arr.put(old.get(i));value.put("markers",arr);save(r)}
    fun forget(id:String){val r=root();r.remove(id);save(r)}
    fun forgetDesign(id:String){val r=root();r.keys().forEach {key ->r.optJSONObject(key)?.takeIf {it.optString("designId")==id}?.remove("designId")};save(r)}
    fun limitSeconds()=prefs.getInt("limitSeconds",900).coerceIn(60,3600)
    fun setLimitSeconds(seconds:Int){require(seconds in 60..3600);check(prefs.edit().putInt("limitSeconds",seconds).commit())}
}
