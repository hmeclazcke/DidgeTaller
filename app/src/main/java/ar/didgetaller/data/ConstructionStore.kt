package ar.didgetaller.data

import android.content.Context
import org.json.JSONObject
import ar.didgetaller.core.ConstructionCatalog

/** Additive versioned metadata; never writes legacy designs or recording preferences. */
class ConstructionStore internal constructor(private val prefs:android.content.SharedPreferences) {
    constructor(context:Context):this(context.getSharedPreferences("construction_v1",Context.MODE_PRIVATE))
    private val projects=JsonPreferenceDocument.objectValue(prefs,"projects")
    private val catalog=JsonPreferenceDocument.objectValue(prefs,"catalog")
    fun project(id:String):Map<String,String> {
        projects.requireWritable()
        val root=projects.read()
        if(root.length()==0)return emptyMap()
        require(root.getInt("schema")==1)
        return fields(root.getJSONObject("designs").optJSONObject(id) ?: JSONObject())
    }
    fun catalog():Map<String,String> {
        catalog.requireWritable()
        val root=catalog.read()
        if(root.length()==0)return emptyMap()
        require(root.getInt("schema")==1)
        return fields(root.getJSONObject("values"))
    }
    fun saveProject(id:String,values:Map<String,String>) {
        require(id.isNotBlank())
        projects.update {root->
            if(root.length()==0)root.put("schema",1).put("designs",JSONObject())
            require(root.getInt("schema")==1)
            root.getJSONObject("designs").put(id,JSONObject(values))
        }
    }
    fun saveCatalog(pipes:String,fittings:String) {
        ConstructionCatalog.pipes(pipes);ConstructionCatalog.connectors(fittings)
        catalog.update {root->
            if(root.length()!=0)require(root.getInt("schema")==1)
            root.put("schema",1).put("values",JSONObject(mapOf("pipes" to pipes,"fittings" to fittings)))
        }
    }
    private fun fields(value:JSONObject):Map<String,String> = value.keys().asSequence().associateWith {key->
        require(value.get(key) is String)
        value.getString(key)
    }
}
