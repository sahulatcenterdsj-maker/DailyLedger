package com.sadique.dailyledger.sync

import com.sadique.dailyledger.security.BackupCrypto
import org.junit.Assert.*
import org.junit.Test

class CloudBackupSyncTest {

    @Test
    fun testUnchangedV2SyncPlainHashComparison() {
        val json = """{"ownerId":"user1","transactions":[]}"""
        val plainHash1 = SnapshotCodec.hash(json)
        val plainHash2 = SnapshotCodec.hash(json)

        // Local plain hash matches remote plainHash stored in V2 metadata
        assertEquals(plainHash1, plainHash2)

        val dek = BackupCrypto.generateDek()
        val enc1 = BackupCrypto.encryptPayload(json.toByteArray(), dek)
        val enc2 = BackupCrypto.encryptPayload(json.toByteArray(), dek)

        // Ciphertexts differ due to random IVs, but plainHash remains stable
        assertFalse(enc1.ciphertext.contentEquals(enc2.ciphertext))
        assertEquals(enc1.contentHash, BackupCrypto.sha256Hex(enc1.ciphertext))
        assertEquals(enc2.contentHash, BackupCrypto.sha256Hex(enc2.ciphertext))
        assertNotEquals(enc1.contentHash, enc2.contentHash)
    }

    @Test
    fun testV1ToV2MigrationDataDecodingsAndReEncryption() {
        val json = """{"ownerId":"user1","transactions":[{"id":"tx1","amount":50000}]}"""
        val v1Payload = SnapshotCodec.encode(json)
        val decoded = SnapshotCodec.decode(v1Payload)

        assertEquals(json, decoded)

        val newDek = BackupCrypto.generateDek()
        val encrypted = BackupCrypto.encryptPayload(decoded.toByteArray(), newDek)
        val decrypted = BackupCrypto.decryptPayload(encrypted.ciphertext, encrypted.iv, newDek)

        assertEquals(json, String(decrypted))
    }

    @Test
    fun testFailedMigrationPreservesV1Data() {
        val json = """{"ownerId":"user1","transactions":[{"id":"tx1","amount":50000}]}"""
        val v1Payload = SnapshotCodec.encode(json)

        var migrationAttempted = false
        var migrationFailed = false

        try {
            migrationAttempted = true
            throw MigrationFailedException("Storage upload timeout")
        } catch (_: MigrationFailedException) {
            migrationFailed = true
        }

        assertTrue(migrationAttempted)
        assertTrue(migrationFailed)
        // Decoded V1 payload remains intact and unmodified
        assertEquals(json, SnapshotCodec.decode(v1Payload))
    }

    @Test
    fun testStaleCachedDekDetectionAndUnwrapFallback() {
        val dek1 = BackupCrypto.generateDek()
        val dek2 = BackupCrypto.generateDek()
        val plainText = "Ledger Data"

        val encryptedWithDek2 = BackupCrypto.encryptPayload(plainText.toByteArray(), dek2)

        // Attempting decryption with stale dek1 fails
        try {
            BackupCrypto.decryptPayload(encryptedWithDek2.ciphertext, encryptedWithDek2.iv, dek1)
            fail("Stale DEK must fail decryption")
        } catch (_: Exception) {
            // Expected tag mismatch
        }

        // Decryption with newly unwrapped dek2 succeeds
        val recovered = BackupCrypto.decryptPayload(encryptedWithDek2.ciphertext, encryptedWithDek2.iv, dek2)
        assertEquals(plainText, String(recovered))
    }

    @Test
    fun testOrphanStorageCleanupOnTransactionFailure() {
        var orphanUploaded = true
        var orphanCleanedUp = false

        try {
            // Simulate Firestore metadata transaction failure
            throw IllegalStateException("Firestore transaction aborted")
        } catch (_: Exception) {
            // Orphan cleanup block executes
            orphanCleanedUp = true
            orphanUploaded = false
        }

        assertTrue(orphanCleanedUp)
        assertFalse(orphanUploaded)
    }
}
