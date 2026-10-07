package com.sadique.dailyledger.sync

import android.content.Context
import android.util.Base64
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageMetadata
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.auth.awaitResult
import com.sadique.dailyledger.data.AppDatabase
import com.sadique.dailyledger.data.LedgerRepository
import com.sadique.dailyledger.data.SettingsStore
import com.sadique.dailyledger.security.BackupCrypto
import com.sadique.dailyledger.security.DekWrapResult
import com.sadique.dailyledger.security.KeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.crypto.BadPaddingException
import kotlin.coroutines.cancellation.CancellationException

class BackupConflictException : IllegalStateException(
    "A different backup exists for this account. Restore it or explicitly replace it with this phone's data."
)

class MigrationFailedException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

data class BackupInfo(
    val revision: String,
    val formatVersion: Long,
    val updatedAt: Long,
)

class CloudBackup(private val context: Context, private val ownerId: String) {
    private val repo = LedgerRepository(AppDatabase.get(context), ownerId)
    private val settings = SettingsStore(context)
    private val keyManager = KeyManager(context, ownerId)
    private val firestore get() = FirebaseRuntime.firestore(context)
    private val storage get() = FirebaseRuntime.storage(context)
    private val document get() = firestore.collection("users").document(ownerId).collection("backups").document("latest")

    private fun checkAccount() {
        check(FirebaseRuntime.auth(context).currentUser?.uid == ownerId) { "Please sign in to this account again." }
    }

    private fun revision(snapshot: DocumentSnapshot): String? {
        if (!snapshot.exists()) return null
        val ver = snapshot.getLong("formatVersion") ?: 1L
        require(ver in 1L..3L) { "Unsupported cloud backup format version: $ver" }
        return snapshot.getString("revision") ?: throw IllegalStateException("Cloud backup revision missing")
    }

    private fun validPath(path: String, revision: String) =
        revision.matches(Regex("[a-f0-9-]{36}")) && path == "users/$ownerId/backups/$revision.enc"
    private suspend fun localJson() = JSONObject(repo.exportJson()).apply { remove("exportedAt") }.toString()

    /** Read-only discovery for the fresh-install Restore / Skip prompt. */
    suspend fun backupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        checkAccount()
        FirebaseRuntime.prepareBackup(context)
        val snapshot = document.get(Source.SERVER).awaitResult()
        checkAccount()
        if (!snapshot.exists()) return@withContext null
        val rev = requireNotNull(revision(snapshot))
        BackupInfo(
            revision = rev,
            formatVersion = snapshot.getLong("formatVersion") ?: 1L,
            updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
        )
    }

    suspend fun sync(): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            FirebaseRuntime.prepareBackup(context)
            val remote = document.get(Source.SERVER).awaitResult()
            checkAccount()
            val state = settings.cloudState(ownerId)
            val json = localJson()
            val format = remote.getLong("formatVersion") ?: 1L
            val localPlainHash = if (remote.exists() && format == 3L) {
                val dek = keyManager.getExistingDek(requireNotNull(remote.getString("wrappedKey")), requireNotNull(revision(remote)), format)
                BackupCrypto.plainFingerprint(json, dek, ownerId)
            } else SnapshotCodec.hash(json)
            val remoteRevision = revision(remote)

            // V2 stores a plaintext canonical hash for change detection only; ciphertext is separately AEAD-authenticated.
            val remotePlainHash = if (remote.exists() && remote.getLong("formatVersion") in listOf(2L, 3L)) {
                remote.getString("plainHash")
            } else if (remote.exists() && (remote.getLong("formatVersion") ?: 1L) == 1L) {
                remote.getString("contentHash")
            } else null

            when (BackupPolicy.decide(
                state.ready, repo.hasRecords(), state.revision,
                remoteRevision, localPlainHash, remotePlainHash
            )) {
                BackupPolicy.Decision.RESTORE -> {
                    // Never auto-restore on a fresh install. UI must ask Restore / Skip first.
                    settings.cloudStatus(ownerId, "Backup found for this account. Choose Restore or Skip in the app.")
                    "Backup found. Waiting for your restore choice."
                }
                BackupPolicy.Decision.UNCHANGED -> {
                    if (format < 3L) writeEncryptedSnapshot(json, remoteRevision)
                    else {
                        settings.cloudComplete(ownerId, requireNotNull(remoteRevision), "Encrypted backup is up to date.")
                        "Encrypted backup is up to date."
                    }
                }
                BackupPolicy.Decision.UPLOAD -> writeEncryptedSnapshot(json, remoteRevision)
                BackupPolicy.Decision.CONFLICT -> throw BackupConflictException()
            }
        }
    }

    suspend fun restore(expectedRevision: String? = null): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            FirebaseRuntime.prepareBackup(context)
            val snapshot = document.get(Source.SERVER).awaitResult()
            check(snapshot.exists()) { "No backup was found for this account." }
            if (expectedRevision != null && revision(snapshot) != expectedRevision) throw BackupConflictException()
            restoreSnapshot(snapshot)
        }
    }

    suspend fun replaceWithLocal(): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            FirebaseRuntime.prepareBackup(context)
            val remote = document.get(Source.SERVER).awaitResult()
            val json = localJson()
            val result = writeEncryptedSnapshot(json, revision(remote))
            settings.setSkippedRestoreRevision(ownerId, "")
            result
        }
    }

    suspend fun deleteBackup(): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            FirebaseRuntime.prepareBackup(context)
            val remote = document.get(Source.SERVER).awaitResult()
            if (remote.exists()) {
                val storagePath = remote.getString("storagePath")
                // Delete authoritative metadata first. If object cleanup then fails, only an unreachable
                // ciphertext orphan remains; we never leave metadata pointing at a deliberately deleted object.
                val expected = revision(remote)
                settings.setCloudEnabled(ownerId, false)
                firestore.runTransaction { transaction ->
                    checkAccount()
                    if (revision(transaction.get(document)) != expected) throw BackupConflictException()
                    transaction.delete(document)
                }.awaitResult()
                if (!storagePath.isNullOrEmpty() && validPath(storagePath, requireNotNull(revision(remote)))) {
                    runCatching { storage.reference.child(storagePath).delete().awaitResult() }
                }
            }
            keyManager.clearLocalCache()
            settings.setCloudEnabled(ownerId, false)
            settings.resetCloudState(ownerId, "Cloud backup deleted. Automatic backup is off until you enable it again.")
            "Cloud backup was deleted and automatic backup was turned off."
        }
    }

    private suspend fun restoreSnapshot(snapshot: DocumentSnapshot): String {
        checkAccount()
        val formatVersion = snapshot.getLong("formatVersion") ?: 1L
        val rev = requireNotNull(revision(snapshot))

        val json: String = if (formatVersion >= 2L) {
            val storagePath = requireNotNull(snapshot.getString("storagePath")) { "Missing backup storage path" }
            require(validPath(storagePath, rev)) { "Invalid backup storage path." }
            val ivBase64 = requireNotNull(snapshot.getString("iv")) { "Missing encryption IV" }
            val expectedCipherHash = requireNotNull(snapshot.getString("contentHash")) { "Missing content hash" }

            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            require(iv.size == 12) { "Invalid encryption IV." }
            val ciphertext = storage.reference.child(storagePath)
                .getBytes(20 * 1024 * 1024L)
                .awaitResult()
            require(BackupCrypto.sha256Hex(ciphertext) == expectedCipherHash) { "Backup ciphertext integrity check failed." }

            val wrappedKey = requireNotNull(snapshot.getString("wrappedKey")) { "Missing wrapped backup key" }
            val plainBytes = decryptV2WithSingleKeyRefresh(ciphertext, iv, wrappedKey, rev, formatVersion)
            try {
                val decoded = String(plainBytes, Charsets.UTF_8)
                val dek = keyManager.getExistingDek(wrappedKey, rev, formatVersion)
                val fingerprint = if (formatVersion == 3L) BackupCrypto.plainFingerprint(decoded, dek, ownerId) else SnapshotCodec.hash(decoded)
                require(fingerprint == snapshot.getString("plainHash")) { "Backup content verification failed." }
                decoded
            } finally { plainBytes.fill(0) }
        } else {
            val payload = requireNotNull(snapshot.getString("payload")) { "Legacy backup payload missing" }
            val decoded = SnapshotCodec.decode(payload)
            val expectedHash = snapshot.getString("contentHash")
            if (expectedHash != null) {
                require(SnapshotCodec.hash(decoded) == expectedHash) { "Legacy backup integrity check failed." }
            }
            decoded
        }

        require(JSONObject(json).getString("ownerId") == ownerId) { "This backup belongs to a different account." }
        // LedgerRepository.importJson is a Room transaction, so replace-on-restore is atomic.
        checkAccount()
        repo.importJson(json)
        settings.cloudComplete(ownerId, rev, "Backup restored to this phone.")
        settings.setSkippedRestoreRevision(ownerId, "")

        if (formatVersion < 3L) {
            // V1 has no wrapped KMS key. Force a brand-new random DEK and only replace V1 metadata
            // after the encrypted Storage object upload succeeds and the Firestore transaction commits.
            try {
                val freshKey = keyManager.createAndWrapNewDek()
                writeEncryptedSnapshot(json, rev, forcedDek = freshKey)
                settings.setSkippedRestoreRevision(ownerId, "")
                return "Your account backup was restored and upgraded to encrypted v3 format."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                throw MigrationFailedException(
                    "Records were restored to this phone. Cloud encryption upgrade is pending; check Firebase setup and try Back up now. Your earlier remote backup was retained.",
                    e
                )
            }
        }

        settings.cloudComplete(ownerId, rev, "Your account backup was restored to this phone.")
        settings.setSkippedRestoreRevision(ownerId, "")
        return "Your account backup was restored to this phone."
    }

    /**
     * A cached key may be stale after another device rotates/recreates the backup.
     * Retry exactly once with a server-unwrapped current key. A second AEAD failure is treated as corruption/tamper.
     */
    private suspend fun decryptV2WithSingleKeyRefresh(ciphertext: ByteArray, iv: ByteArray, wrappedKey: String, revision: String, format: Long): ByteArray {
        val aad = if (format == 3L) BackupCrypto.snapshotAad(ownerId, revision) else byteArrayOf()
        val firstKey = keyManager.getExistingDek(wrappedKey, revision, format)
        return try {
            BackupCrypto.decryptPayload(ciphertext, iv, firstKey, aad)
        } catch (first: BadPaddingException) {
            val refreshed = keyManager.refreshExistingDek(wrappedKey, revision, format)
            try {
                BackupCrypto.decryptPayload(ciphertext, iv, refreshed, aad)
            } catch (second: BadPaddingException) {
                second.addSuppressed(first)
                throw IllegalStateException(
                    "Backup authentication failed after refreshing the account key. The backup may be damaged or changed.",
                    second
                )
            }
        }
    }

    private suspend fun writeEncryptedSnapshot(
        json: String,
        expectedRevision: String?,
        forcedDek: DekWrapResult? = null,
    ): String {
        checkAccount()

        // Fetch metadata before choosing a key. Missing/V1 metadata must always create a new wrapped key,
        // even if this device still has an unrelated cached DEK.
        val remoteBefore = document.get(Source.SERVER).awaitResult()
        if (revision(remoteBefore) != expectedRevision) throw BackupConflictException()
        val remoteVersion = if (remoteBefore.exists()) remoteBefore.getLong("formatVersion") ?: 1L else 0L

        val dekWrap = forcedDek ?: if (remoteVersion == 3L) {
            val existingWrapped = requireNotNull(remoteBefore.getString("wrappedKey")) { "Encrypted backup key metadata is missing." }
            DekWrapResult(keyManager.getExistingDek(existingWrapped, requireNotNull(expectedRevision), remoteVersion), existingWrapped)
        } else {
            keyManager.createAndWrapNewDek()
        }
        require(dekWrap.wrappedKey.isNotBlank()) { "Wrapped backup key is missing." }

        val nextRevision = UUID.randomUUID().toString()
        val plainHash = BackupCrypto.plainFingerprint(json, dekWrap.dek, ownerId)
        val plainBytes = json.toByteArray(Charsets.UTF_8)
        require(plainBytes.size <= 16 * 1024 * 1024) { "Backup is too large. Export your data instead." }
        val encrypted = try { BackupCrypto.encryptPayload(plainBytes, dekWrap.dek, BackupCrypto.snapshotAad(ownerId, nextRevision)) }
            finally { plainBytes.fill(0) }
        val newStoragePath = "users/$ownerId/backups/$nextRevision.enc"
        val oldStoragePath = if (remoteVersion >= 2L) remoteBefore.getString("storagePath") else null

        val newStorageRef = storage.reference.child(newStoragePath)
        val metadata = StorageMetadata.Builder()
            .setContentType("application/octet-stream")
            .setCustomMetadata("ownerId", ownerId)
            .setCustomMetadata("revision", nextRevision)
            .build()

        checkAccount()
        newStorageRef.putBytes(encrypted.ciphertext, metadata).awaitResult()

        try {
            firestore.runTransaction { transaction ->
                checkAccount()
                val current = transaction.get(document)
                if (revision(current) != expectedRevision) throw BackupConflictException()

                transaction.set(document, mapOf(
                    "formatVersion" to 3L,
                    "revision" to nextRevision,
                    "storagePath" to newStoragePath,
                    "wrappedKey" to dekWrap.wrappedKey,
                    "contentHash" to encrypted.contentHash,
                    "plainHash" to plainHash,
                    "iv" to Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ))
                nextRevision
            }.awaitResult()
        } catch (e: Exception) {
            // A timeout/cancellation can arrive AFTER commit. Deleting here could destroy the valid backup.
            // Only an explicit revision conflict proves this transaction did not commit.
            if (e is BackupConflictException) runCatching { newStorageRef.delete().awaitResult() }
            throw e
        }

        // Old valid data is removed only after new encrypted metadata is committed.
        if (!oldStoragePath.isNullOrEmpty() && oldStoragePath != newStoragePath && validPath(oldStoragePath, expectedRevision.orEmpty())) {
            runCatching { storage.reference.child(oldStoragePath).delete().awaitResult() }
        }

        settings.cloudComplete(ownerId, nextRevision, "Backed up securely to your account.")
        return "Encrypted backup saved to your account."
    }
}
