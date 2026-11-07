package ar.didgetaller.data

import android.content.Context

/** The stores' monitor prevents a reload from observing half of another publication. */
internal class LibraryCatalog(context: Context, private val recordings: RecordingStore,
                              private val local: LocalStore, private val trash: TrashStore) {
    private val owner = context.getSharedPreferences("local_data", Context.MODE_PRIVATE)
    fun load(): LibrarySnapshot = synchronized(owner) {
        LibrarySnapshot.create(recordings.list(true), local.designs(true), local.measurements(),
            trash.ids("session"), trash.ids("design"), recordings.indexDamaged)
    }
}
