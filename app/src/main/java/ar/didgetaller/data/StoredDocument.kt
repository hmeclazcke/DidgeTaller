package ar.didgetaller.data

import java.security.MessageDigest

/** One process-local owner covers the complete read/modify/publish operation, across instances. */
internal class StoredDocument(
    private val storage: Storage,
    private val key: String,
    private val validate: (String) -> Unit,
) {
    interface Storage {
        val lock: Any
        fun read(key: String): String?
        /** Atomically replaces these keys; null removes a key. False means durability is unknown. */
        fun commit(values: Map<String, String?>): Boolean
    }

    enum class Health { MISSING, READY, DAMAGED }
    data class Snapshot(val health: Health, val text: String?)
    class Unavailable : IllegalStateException("Stored metadata is damaged; recordings have been preserved.")
    class PublicationFailed : IllegalStateException("Metadata publication failed; durability is unknown.")

    private val backupKey = "$key.lastGood"

    fun snapshot(): Snapshot = synchronized(storage.lock) { inspect() }

    /** A validated backup is display-only until explicit recovery. Reading never rewrites storage. */
    fun displayText(): String? = synchronized(storage.lock) {
        val current = inspect()
        if (current.health != Health.DAMAGED) current.text else validBackup()
    }

    fun <T> update(change: (String?) -> Pair<String, T>): T = synchronized(storage.lock) {
        val current = inspect()
        if (current.health == Health.DAMAGED) throw Unavailable()
        interrupted()
        val (next, result) = change(current.text)
        validate(next)
        interrupted()
        publish(mapOf(key to next, backupKey to next))
        result
    }

    /** Recovery never invents an empty index. Preserve the exact damaged text before restoring a backup. */
    fun recover(): Boolean = synchronized(storage.lock) {
        val current = inspect()
        if (current.health != Health.DAMAGED) return false
        val backup = validBackup() ?: throw Unavailable()
        val damaged = current.text ?: "<missing document>"
        val hash = MessageDigest.getInstance("SHA-256").digest(damaged.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        interrupted()
        publish(mapOf("$key.quarantine.$hash" to damaged, key to backup))
        true
    }

    private fun inspect(): Snapshot {
        val text = storage.read(key)
        if (text == null) {
            return Snapshot(if (storage.read(backupKey) == null) Health.MISSING else Health.DAMAGED, null)
        }
        return Snapshot(if (valid(text)) Health.READY else Health.DAMAGED, text)
    }

    private fun validBackup(): String? = storage.read(backupKey)?.takeIf { valid(it) }

    private fun valid(text: String): Boolean = try {
        validate(text)
        true
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun publish(values: Map<String, String?>) {
        val previous = values.keys.associateWith(storage::read)
        try {
            if (storage.commit(values)) return
        } catch (failure: RuntimeException) {
            // SharedPreferences updates memory before attempting disk I/O, even when commit fails.
            rollback(previous)
            throw PublicationFailed().apply { initCause(failure) }
        }
        rollback(previous)
        throw PublicationFailed()
    }

    private fun rollback(previous: Map<String, String?>) {
        // Best effort only. The caller never receives permission to delete a WAV after a failed commit.
        try { storage.commit(previous) } catch (_: RuntimeException) { /* Preserve the publication failure. */ }
    }

    private fun interrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Metadata publication cancelled")
    }
}
