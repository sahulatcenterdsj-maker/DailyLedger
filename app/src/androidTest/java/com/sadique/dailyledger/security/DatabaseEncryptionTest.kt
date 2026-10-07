package com.sadique.dailyledger.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test

class DatabaseEncryptionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun interruptedMigrationPreservesPlaintextAndRetryEncryptsIt() {
        val file = context.getDatabasePath("encrypted-migration-test.db")
        context.deleteDatabase(file.name); file.parentFile?.mkdirs()
        val password = "test-password-with-random-content".toByteArray()
        PlainDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE records(note TEXT)")
            db.execSQL("INSERT INTO records VALUES ('private salary note')")
            db.version = 2
        }
        try {
            try { DatabaseEncryption.migrate(file, password) { error("Simulated interruption") }; fail("Should stop") }
            catch (e: IllegalStateException) { assertEquals("Simulated interruption", e.message) }
            assertTrue(DatabaseEncryption.isPlaintext(file))
            PlainDatabase.openDatabase(file.path,null,PlainDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT note FROM records",null).use { c -> assertTrue(c.moveToFirst()); assertEquals("private salary note",c.getString(0)) }
            }
            DatabaseEncryption.migrate(file,password)
            assertFalse(DatabaseEncryption.isPlaintext(file))
            assertFalse(String(file.readBytes(),Charsets.ISO_8859_1).contains("private salary note"))
            val before = file.readBytes()
            try {
                SQLiteDatabase.openDatabase(file.path,"wrong".toByteArray(),null,SQLiteDatabase.OPEN_READONLY,
                    { _, _ -> throw IllegalStateException("Preserved") },null).use { it.rawQuery("SELECT * FROM records",emptyArray<String>()).close() }
                fail("Wrong key accepted")
            } catch (_: Exception) { }
            assertArrayEquals(before,file.readBytes())
            SQLiteDatabase.openDatabase(file.path,password,null,SQLiteDatabase.OPEN_READONLY,null).use { db ->
                assertEquals(2,db.version)
                db.rawQuery("SELECT note FROM records",emptyArray<String>()).use { c -> assertTrue(c.moveToFirst()); assertEquals("private salary note",c.getString(0)) }
            }
        } finally { context.deleteDatabase(file.name) }
    }
    @Test fun deviceSecretSurvivesReopenAndIsIsolatedByNamespace() {
        val a=SecureSecretStore(context,"test-key-a"); val b=SecureSecretStore(context,"test-key-b")
        a.clearBytes(); b.clearBytes()
        try {
            val key=EnvelopeCrypto.randomKey();a.saveBytes(key)
            assertArrayEquals(key,SecureSecretStore(context,"test-key-a").loadBytes())
            assertNull(b.loadBytes())
        } finally { a.clearBytes(); b.clearBytes() }
    }
}
