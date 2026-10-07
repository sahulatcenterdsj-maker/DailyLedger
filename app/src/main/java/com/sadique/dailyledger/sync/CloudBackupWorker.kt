package com.sadique.dailyledger.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.data.SettingsStore
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class CloudBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val settings = store.all.first()
        val user = settings.user ?: return Result.success()
        if (!settings.cloudEnabled || !FirebaseRuntime.configured(applicationContext)) return Result.success()
        if (FirebaseRuntime.auth(applicationContext).currentUser?.uid != user.id) return Result.success()
        return try {
            CloudBackup(applicationContext, user.id).sync()
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            store.cloudStatus(user.id, backupError(e))
            if (isRetryable(e) && runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }
}

fun isRetryable(error: Throwable): Boolean = error is FirebaseNetworkException ||
    error is java.util.concurrent.TimeoutException ||
    (error is FirebaseFirestoreException && error.code in setOf(
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.ABORTED,
    ))

fun backupError(error: Throwable): String = when {
    error is MigrationFailedException -> error.message.orEmpty()
    error is BackupConflictException -> error.message.orEmpty()
    error is com.google.firebase.functions.FirebaseFunctionsException -> when(error.code) {
        com.google.firebase.functions.FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> "Today's backup-key limit is reached. Your local records are safe; try later."
        com.google.firebase.functions.FirebaseFunctionsException.Code.ABORTED -> "Backup changed on another phone. Please retry."
        com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Sign in again to access your account backup."
        else -> "Encrypted backup setup is pending or unavailable. The app owner must check Functions, KMS and App Check. Your records remain on this phone."
    }
    error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Cloud access was denied. The app owner must deploy the current backup rules and check App Check."
    error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
        "The account server has reached its usage limit. Your data remains on this phone. Try later."
    isRetryable(error) -> "Backup is waiting for a connection. Your data is saved on this phone."
    error is java.security.GeneralSecurityException -> "Backup authentication failed. Your existing local records were not replaced."
    else -> "Backup could not be completed. Check your connection and Firebase backup setup before trying again."
}
