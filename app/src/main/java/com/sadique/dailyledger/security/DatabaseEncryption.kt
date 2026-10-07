package com.sadique.dailyledger.security

import android.content.Context
import android.system.Os
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.Base64

object DatabaseEncryption {
    private val plainHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    init { System.loadLibrary("sqlcipher") }

    fun password(context: Context, file: File): ByteArray {
        val store = SecureSecretStore(context, "local-database-v1")
        val saved = store.loadBytes()
        if (saved != null) {
            require(saved.size == 44) { "Device database key is damaged." }
            return saved
        }
        check(!file.exists() || file.length() == 0L || isPlaintext(file)) {
            "Device database key is missing. Do not clear data; recover with a verified account backup."
        }
        val random = EnvelopeCrypto.randomKey()
        val password = Base64.getEncoder().encode(random)
        random.fill(0)
        store.saveBytes(password)
        return password
    }

    fun isPlaintext(file: File): Boolean = file.exists() && file.length() >= 16 &&
        FileInputStream(file).use { ByteArray(16).let { header -> it.read(header) == 16 && header.contentEquals(plainHeader) } }

    /** Called before Room opens the database. Original stays intact until a verified atomic rename. */
    fun migrate(file: File, password: ByteArray, beforeReplace: () -> Unit = {}) {
        if (!isPlaintext(file)) return
        val temporary = File(file.parentFile, file.name + ".encrypted-migration")
        // A crash before rename leaves only this encrypted temporary file; the original is authoritative.
        temporary.delete()
        File(temporary.path + "-wal").delete(); File(temporary.path + "-shm").delete()
        android.database.sqlite.SQLiteDatabase.openDatabase(file.path, null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            { throw IllegalStateException("Original database is damaged; it was preserved.") }).use { old ->
            old.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { c ->
                check(c.moveToFirst() && c.getInt(0) == 0) { "Database is busy. Please retry." }
            }
            old.disableWriteAheadLogging()
        }
        SQLiteDatabase.openDatabase(file.path, byteArrayOf(), null, SQLiteDatabase.OPEN_READWRITE,
            { _, _ -> throw IllegalStateException("Original database could not be read; it was not erased.") }, null).use { old ->
            val version = old.version
            old.execSQL("ATTACH DATABASE ? AS encrypted KEY ?", arrayOf(temporary.path, String(password, Charsets.UTF_8)))
            try {
                old.rawQuery("SELECT sqlcipher_export('encrypted')", emptyArray<String>()).use { c -> check(c.moveToFirst()) }
                old.execSQL("PRAGMA encrypted.user_version = $version")
            } finally { old.execSQL("DETACH DATABASE encrypted") }
            SQLiteDatabase.openDatabase(temporary.path, password, null, SQLiteDatabase.OPEN_READONLY,
                { _, _ -> throw IllegalStateException("Encrypted database verification failed.") }, null).use { encrypted ->
                check(encrypted.version == version)
                encrypted.rawQuery("PRAGMA integrity_check", emptyArray<String>()).use { c ->
                    check(c.moveToFirst() && c.getString(0) == "ok") { "Encrypted database verification failed." }
                }
                old.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", emptyArray<String>()).use { tables ->
                    while (tables.moveToNext()) {
                        val table = tables.getString(0).replace("\"", "\"\"")
                        fun count(db: SQLiteDatabase) = db.rawQuery("SELECT count(*) FROM \"$table\"", emptyArray<String>()).use { it.moveToFirst(); it.getLong(0) }
                        check(count(old) == count(encrypted)) { "Database migration was incomplete." }
                    }
                }
            }
        }
        check(!isPlaintext(temporary)) { "Encrypted database was not created." }
        RandomAccessFile(temporary, "rw").use { it.fd.sync() }
        beforeReplace() // Test injection: failure here must leave the original readable.
        File(file.path + "-wal").delete(); File(file.path + "-shm").delete()
        Os.rename(temporary.path, file.path)
    }
}
