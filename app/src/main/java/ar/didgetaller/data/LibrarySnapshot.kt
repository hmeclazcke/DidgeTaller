package ar.didgetaller.data

/** One coherent publication replaces independent lists and counters in the ViewModel. */
internal data class LibrarySnapshot(
    val sessions: List<RecordingStore.Session>,
    val designs: List<LocalStore.Design>,
    val measurements: List<LocalStore.Measurement>,
    val trashedSessions: List<RecordingStore.Session>,
    val trashedDesigns: List<LocalStore.Design>,
    val trashCount: Int,
    val indexDamaged: Boolean,
) {
    companion object {
        fun create(sessions: List<RecordingStore.Session>, designs: List<LocalStore.Design>,
                   measurements: List<LocalStore.Measurement>, sessionTrash: Set<String>, designTrash: Set<String>,
                   indexDamaged: Boolean): LibrarySnapshot {
            val (trashedSessions, visibleSessions) = sessions.partition { it.id in sessionTrash }
            val (trashedDesigns, visibleDesigns) = designs.partition { it.id in designTrash }
            return LibrarySnapshot(visibleSessions, visibleDesigns, measurements.toList(), trashedSessions,
                trashedDesigns, sessionTrash.size + designTrash.size, indexDamaged)
        }
    }
}
