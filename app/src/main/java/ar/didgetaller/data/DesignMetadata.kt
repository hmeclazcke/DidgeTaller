package ar.didgetaller.data

import ar.didgetaller.core.AcousticModel
import org.json.JSONArray
import org.json.JSONObject

/** Canonical design fields evolve additively. Only an explicitly selected identity is updated. */
internal object DesignMetadata {
    fun upsert(rows:JSONArray,id:String,name:String,shape:AcousticModel.Shape) {
        require(id.isNotBlank() && name.isNotBlank())
        val matches=(0 until rows.length()).filter {rows.optJSONObject(it)?.optString("id","legacy-$it")==id}
        require(matches.size<=1) {"Ambiguous design identity"}
        val index=matches.firstOrNull()
        val value=index?.let {rows.getJSONObject(it)} ?: JSONObject()
        value.put("id",id).put("name",name.take(70)).put("length",shape.lengthMm)
            .put("mouth",shape.mouthMm).put("mid",shape.middleMm).put("bell",shape.bellMm)
            .put("points",JSONArray().also {points->shape.points.forEach {points.put(JSONArray().put(it.positionMm).put(it.diameterMm))}})
        if(index==null)rows.put(value) else rows.put(index,value)
    }
}
