package com.sadique.dailyledger.sync

import android.content.Context
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.data.AppDatabase
import com.sadique.dailyledger.data.LedgerRepository
import com.sadique.dailyledger.data.SettingsStore
import com.sadique.dailyledger.security.BackupCrypto
import com.sadique.dailyledger.security.SecureSecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DriveSync(private val context: Context, private val ownerId: String) {
    private val repo = LedgerRepository(AppDatabase.get(context), ownerId)
    private val settings = SettingsStore(context)
    private val secrets = SecureSecretStore(context, ownerId)
    private fun checkAccount() {
        check(FirebaseRuntime.auth(context).currentUser?.uid == ownerId) { "Please sign in again." }
    }
    suspend fun upload(token: String) = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            val passphrase = secrets.loadPassphrase() ?: error("Set a Drive backup passphrase first.")
            DriveApi(token).upload(BackupCrypto.encrypt(repo.exportJson().toByteArray(), passphrase.toCharArray()))
            settings.setLastSync(ownerId, System.currentTimeMillis())
        }
    }
    suspend fun restore(token: String) = BackupLock.mutex.withLock {
        withContext(Dispatchers.IO) {
            checkAccount()
            val passphrase = secrets.loadPassphrase() ?: error("Enter the original Drive backup passphrase first.")
            val data = DriveApi(token).download() ?: error("No Drive backup found.")
            val json = try { String(BackupCrypto.decrypt(data, passphrase.toCharArray()), Charsets.UTF_8) }
            catch (e: javax.crypto.AEADBadTagException) { error("The Drive passphrase is incorrect or the backup is damaged.") }
            checkAccount()
            repo.importJson(json)
            settings.setLastSync(ownerId, System.currentTimeMillis())
        }
    }
}
