package ar.didgetaller.data

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Derived data only: independent writers publish whole values, never each other's temporary bytes. */
internal object AtomicCacheFile {
    fun write(destination: File, block: (DataOutputStream) -> Unit) {
        val target = destination.absoluteFile
        val temporary = File.createTempFile("cache-${target.name}.", ".tmp", target.parentFile)
        try {
            DataOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use(block)
            // Same-directory replacement has atomic visibility; it is not a durable session transaction.
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }
}
