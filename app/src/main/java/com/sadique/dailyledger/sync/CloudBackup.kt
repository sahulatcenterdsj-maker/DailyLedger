package com.sadique.dailyledger.sync

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.auth.awaitResult
import com.sadique.dailyledger.data.AppDatabase
import com.sadique.dailyledger.data.LedgerRepository
import com.sadique.dailyledger.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class BackupConflictException : IllegalStateException(
    "A different backup exists for this account. In Settings, restore it or explicitly replace it with this phone's data."
)

/** Account backups use Firebase transport/storage encryption and per-UID server rules. */
class CloudBackup(private val context: Context, private val ownerId: String) {
    private val repo = LedgerRepository(AppDatabase.get(context), ownerId)
    private val settings = SettingsStore(context)
    private val firestore get() = FirebaseRuntime.firestore(context)
    private val document get() = firestore.collection("users").document(ownerId).collection("backups").document("latest")

    private fun checkAccount() {
        check(FirebaseRuntime.auth(context).currentUser?.uid == ownerId) { "Please sign in to this account again." }
    }
    private fun revision(snapshot: DocumentSnapshot): String? {
        if (!snapshot.exists()) return null
        require(snapshot.getLong("formatVersion") == 1L) { "Unsupported cloud backup version" }
        return requireNotNull(snapshot.getString("revision")) { "Cloud backup is incomplete" }
    }
    private suspend fun localJson() = JSONObject(repo.exportJson()).apply { remove("exportedAt") }.toString()

    suspend fun sync(): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            val remote = document.get(Source.SERVER).awaitResult()
            checkAccount()
            val state = settings.cloudState(ownerId)
            val json = localJson()
            val hash = SnapshotCodec.hash(json)
            val remoteRevision = revision(remote)
            when (BackupPolicy.decide(state.ready, repo.hasRecords(), state.revision,
                remoteRevision, hash, remote.getString("contentHash"))) {
                BackupPolicy.Decision.RESTORE -> restoreSnapshot(remote)
                BackupPolicy.Decision.UNCHANGED -> {
                    settings.cloudComplete(ownerId, requireNotNull(remoteRevision), "Backup is up to date.")
                    "Backup is up to date."
                }
                BackupPolicy.Decision.UPLOAD -> writeSnapshot(json, hash, remoteRevision)
                BackupPolicy.Decision.CONFLICT -> throw BackupConflictException()
            }
        }
    }

    /** UI must confirm that existing local records will be replaced. */
    suspend fun restore(): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            val snapshot = document.get(Source.SERVER).awaitResult()
            check(snapshot.exists()) { "No backup was found for this account." }
            restoreSnapshot(snapshot)
        }
    }

    /** UI must separately confirm that the current remote snapshot will be replaced. */
    suspend fun replaceWithLocal(): String = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            val remote = document.get(Source.SERVER).awaitResult()
            val json = localJson()
            writeSnapshot(json, SnapshotCodec.hash(json), revision(remote))
        }
    }

    private suspend fun restoreSnapshot(snapshot: DocumentSnapshot): String {
        checkAccount()
        val rev = requireNotNull(revision(snapshot))
        val json = SnapshotCodec.decode(requireNotNull(snapshot.getString("payload")))
        require(SnapshotCodec.hash(json) == snapshot.getString("contentHash")) { "Backup integrity check failed." }
        require(JSONObject(json).getString("ownerId") == ownerId) { "This backup belongs to a different account." }
        repo.importJson(json)
        settings.cloudComplete(ownerId, rev, "Your account backup was restored to this phone.")
        return "Your account backup was restored to this phone."
    }

    private suspend fun writeSnapshot(json: String, hash: String, expectedRevision: String?): String {
        checkAccount()
        val payload = SnapshotCodec.encode(json)
        val nextRevision = UUID.randomUUID().toString()
        // Transaction retries never replace another phone's newer snapshot.
        firestore.runTransaction { transaction ->
            checkAccount()
            val current = transaction.get(document)
            if (revision(current) != expectedRevision) throw BackupConflictException()
            transaction.set(document, mapOf(
                "formatVersion" to 1L, "revision" to nextRevision, "payload" to payload,
                "contentHash" to hash, "updatedAt" to FieldValue.serverTimestamp(),
            ))
            nextRevision
        }.awaitResult()
        settings.cloudComplete(ownerId, nextRevision, "Backed up to your account.")
        return "Backed up to your account."
    }
}
