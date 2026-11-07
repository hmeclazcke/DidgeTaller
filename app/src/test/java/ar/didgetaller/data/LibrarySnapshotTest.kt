package ar.didgetaller.data

import ar.didgetaller.core.AcousticModel
import org.junit.Assert.*
import org.junit.Test

class LibrarySnapshotTest {
    private fun session(id: String) = RecordingStore.Session(id, id, 0, 0, 44, null, emptyList(), true)
    private fun design(id: String) = LocalStore.Design(id, id, AcousticModel.Shape())

    @Test fun visibleAndTrashedCollectionsDescribeTheSamePublication() {
        val snapshot = LibrarySnapshot.create(listOf(session("a"), session("b")), listOf(design("a"), design("b")),
            emptyList(), setOf("a"), setOf("b"), true)
        assertEquals(listOf("b"), snapshot.sessions.map { it.id })
        assertEquals(listOf("a"), snapshot.trashedSessions.map { it.id })
        assertEquals(listOf("a"), snapshot.designs.map { it.id })
        assertEquals(listOf("b"), snapshot.trashedDesigns.map { it.id })
        assertEquals(2, snapshot.trashCount)
        assertTrue(snapshot.indexDamaged)
    }

    @Test fun tombstonesForMissingFilesRetainTheirCountWithoutInventingSessions() {
        val snapshot = LibrarySnapshot.create(emptyList(), emptyList(), emptyList(), setOf("missing"), setOf("other"), false)
        assertTrue(snapshot.sessions.isEmpty())
        assertTrue(snapshot.designs.isEmpty())
        assertTrue(snapshot.measurements.isEmpty())
        assertTrue(snapshot.trashedSessions.isEmpty())
        assertTrue(snapshot.trashedDesigns.isEmpty())
        assertEquals(2, snapshot.trashCount)
        assertFalse(snapshot.indexDamaged)
    }

    @Test fun publishedCollectionsDoNotAliasMutableLoaderLists() {
        val sessions = mutableListOf(session("a"))
        val designs = mutableListOf(design("b"))
        val measurement = LocalStore.Measurement("m", "historical", 100.0, "Stored", 0)
        val measurements = mutableListOf(measurement)
        val snapshot = LibrarySnapshot.create(sessions, designs, measurements, emptySet(), emptySet(), false)
        sessions.clear(); designs.clear(); measurements.clear()
        assertEquals(listOf("a"), snapshot.sessions.map { it.id })
        assertEquals(listOf("b"), snapshot.designs.map { it.id })
        assertEquals(listOf(measurement), snapshot.measurements)
        assertEquals(0, snapshot.trashCount)
    }
}
