package ar.didgetaller.data

import android.content.Context
import org.json.JSONObject

/** Durable tombstones: nothing is irreversibly deleted until the user empties the trash. */
class TrashStore(context:Context) {
    private val prefs=context.getSharedPreferences("local_data",0)
    fun ids(kind:String):Set<String> = JSONObject(prefs.getString("trash_$kind","{}") ?: "{}").keys().asSequence().toSet()
    fun put(kind:String,id:String){val r=JSONObject(prefs.getString("trash_$kind","{}") ?: "{}");r.put(id,System.currentTimeMillis());check(prefs.edit().putString("trash_$kind",r.toString()).commit())}
    fun restore(kind:String,id:String){val r=JSONObject(prefs.getString("trash_$kind","{}") ?: "{}");r.remove(id);check(prefs.edit().putString("trash_$kind",r.toString()).commit())}
}
