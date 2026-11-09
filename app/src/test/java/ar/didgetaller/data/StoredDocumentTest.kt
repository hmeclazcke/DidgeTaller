package ar.didgetaller.data

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StoredDocumentTest {
    private class Memory : StoredDocument.Storage {
        override val lock = Any()
        val values = mutableMapOf<String, String>()
        var failure = false
        var throws = false
        var calls = 0
        override fun read(key: String) = values[key]
        override fun commit(values: Map<String, String?>): Boolean {
            calls++
            values.forEach { (key, value) -> if (value == null) this.values.remove(key) else this.values[key] = value }
            if (throws) throw IllegalStateException("Injected disk error after memory update")
            return !failure
        }
    }
    private fun document(storage: Memory) = StoredDocument(storage, "index") {
        require(it.isNotEmpty() && it.all(Char::isDigit))
    }

    @Test fun missingAndMalformedAreDifferentAndReadsDoNotWrite() {
        val storage = Memory()
        val doc = document(storage)
        assertEquals(StoredDocument.Health.MISSING, doc.snapshot().health)
        assertNull(doc.displayText())
        for (raw in listOf("", " ", "broken", "123x")) {
            storage.values["index"] = raw
            assertEquals(StoredDocument.Health.DAMAGED, doc.snapshot().health)
            assertEquals(raw, doc.snapshot().text)
            assertNull(doc.displayText())
            assertThrows(StoredDocument.Unavailable::class.java) { doc.update { "1" to Unit } }
            assertEquals(raw, storage.values["index"])
        }
        assertEquals(0, storage.calls)
    }

    @Test fun completePublicationAndFreshInstancePreserveLatestBackup() {
        val storage = Memory()
        val doc = document(storage)
        assertEquals("result", doc.update { assertNull(it); "12" to "result" })
        assertEquals("12", storage.values["index.lastGood"])
        document(storage).update { assertEquals("12", it); "123" to Unit }
        assertEquals("123", doc.displayText())
        assertEquals("123", storage.values["index.lastGood"])
    }

    @Test fun damagedMainUsesBackupOnlyForDisplayAndRecoveryArchivesExactDamage() {
        val storage = Memory()
        val doc = document(storage)
        doc.update { "123" to Unit }
        storage.values["index"] = "truncated-123"
        val before = storage.values.toMap()
        assertEquals("123", document(storage).displayText())
        assertEquals(before, storage.values)
        assertThrows(StoredDocument.Unavailable::class.java) { doc.update { "9" to Unit } }
        assertTrue(doc.recover())
        assertEquals("123", document(storage).snapshot().text)
        assertEquals("truncated-123", storage.values.filterKeys { it.startsWith("index.quarantine.") }.values.single())
        assertFalse(doc.recover())
        assertEquals(1, storage.values.keys.count { it.startsWith("index.quarantine.") })
        val quarantine = storage.values.keys.single { it.startsWith("index.quarantine.") }
        val expectedHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest("truncated-123".toByteArray()).joinToString("") { "%02x".format(it) }
        assertEquals("index.quarantine.$expectedHash", quarantine)
    }

    @Test fun missingMainWithBackupCannotBeMistakenForNewInstallation() {
        val storage = Memory()
        storage.values["index.lastGood"] = "123"
        val doc = document(storage)
        assertEquals(StoredDocument.Health.DAMAGED, doc.snapshot().health)
        assertThrows(StoredDocument.Unavailable::class.java) { doc.update { "0" to Unit } }
        assertTrue(doc.recover())
        assertEquals("123", doc.displayText())
    }

    @Test fun absentOrInvalidBackupCannotAuthorizeRecovery() {
        val storage = Memory()
        storage.values["index"] = "broken"
        for (backup in listOf(null, "", "bad")) {
            if (backup == null) storage.values.remove("index.lastGood") else storage.values["index.lastGood"] = backup
            val before = storage.values.toMap()
            assertThrows(StoredDocument.Unavailable::class.java) { document(storage).recover() }
            assertEquals(before, storage.values)
        }
        assertEquals(0, storage.calls)
    }

    @Test fun invalidReplacementAndTransformFailureCannotPublishAnything() {
        val storage = Memory()
        storage.values["index"] = "123"
        val doc = document(storage)
        assertThrows(IllegalArgumentException::class.java) { doc.update { "invalid" to Unit } }
        assertThrows(UnsupportedOperationException::class.java) { doc.update<Unit> { throw UnsupportedOperationException() } }
        assertEquals("123", doc.displayText())
        assertEquals(0, storage.calls)
    }

    @Test fun failedAndThrowingCommitsRollBackMemoryAndDoNotClaimSuccess() {
        for (throws in listOf(false, true)) {
            val storage = Memory()
            val doc = document(storage)
            doc.update { "123" to Unit }
            storage.failure = true
            storage.throws = throws
            assertThrows(StoredDocument.PublicationFailed::class.java) { doc.update { "456" to Unit } }
            assertEquals("123", doc.displayText())
            assertEquals("123", storage.values["index.lastGood"])
            storage.failure = false
            storage.throws = false
            document(storage).update { "789" to Unit }
            assertEquals("789", doc.displayText())
        }
    }

    @Test fun interruptedThreadNeverPublishes() {
        val storage = Memory()
        try {
            Thread.currentThread().interrupt()
            var called = false
            assertThrows(InterruptedException::class.java) { document(storage).update { called = true; "123" to Unit } }
            assertFalse(called)
            assertEquals(0, storage.calls)
        } finally { Thread.interrupted() }
        assertThrows(InterruptedException::class.java) {
            try { document(storage).update { Thread.currentThread().interrupt(); "123" to Unit } }
            finally { Thread.interrupted() }
        }
        assertEquals(0, storage.calls)
    }

    @Test fun recoveryCancellationPreservesDamageAndBackup() {
        val storage = Memory()
        storage.values["index"] = "damaged"
        storage.values["index.lastGood"] = "123"
        val previous = storage.values.toMap()
        try {
            Thread.currentThread().interrupt()
            assertThrows(InterruptedException::class.java) { document(storage).recover() }
        } finally { Thread.interrupted() }
        assertEquals(previous, storage.values)
        assertEquals(0, storage.calls)
    }

    @Test fun independentOwnersSerializeReloadAndPublishWithoutLostUpdates() {
        val storage = Memory()
        val first = document(storage)
        val second = document(storage)
        first.update { "0" to Unit }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val start = CountDownLatch(1)
            val jobs = listOf(first, second).map { doc -> pool.submit {
                check(start.await(5, TimeUnit.SECONDS))
                repeat(100) { doc.update { raw -> (raw!!.toInt() + 1).toString() to Unit } }
            } }
            start.countDown()
            jobs.forEach { it.get(10, TimeUnit.SECONDS) }
            assertEquals("200", document(storage).displayText())
            assertEquals("200", storage.values["index.lastGood"])
        } finally { pool.shutdownNow() }
    }
}
