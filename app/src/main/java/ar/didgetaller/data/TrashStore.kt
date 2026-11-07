package ar.didgetaller.data

import android.content.Context

/** Durable tombstones: nothing is irreversibly deleted until the user empties the trash. */
class TrashStore(context:Context) {
    private val prefs=context.getSharedPreferences("local_data",0)
    private fun document(kind:String)=JsonPreferenceDocument.objectValue(prefs,"trash_$kind")
    fun ids(kind:String):Set<String> = document(kind).read().keys().asSequence().toSet()
    fun put(kind:String,id:String){document(kind).update {it.put(id,System.currentTimeMillis())}}
    fun restore(kind:String,id:String){document(kind).update {it.remove(id)}}
}
