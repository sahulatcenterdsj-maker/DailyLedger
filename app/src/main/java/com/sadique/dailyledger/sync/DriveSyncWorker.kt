package com.sadique.dailyledger.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.data.SettingsStore
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class DriveSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = SettingsStore(applicationContext).all.first()
        val user = state.user ?: return Result.success()
        val email = user.googleEmail ?: return Result.success()
        if (!state.driveEnabled || !FirebaseRuntime.configured(applicationContext)) return Result.success()
        if (FirebaseRuntime.auth(applicationContext).currentUser?.uid != user.id) return Result.success()
        val token = DriveAuth.silentToken(applicationContext, email) ?: return Result.success()
        return try {
            DriveSync(applicationContext, user.id).upload(token)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: IllegalStateException) { Result.failure() }
        catch (e: Exception) { if (runAttemptCount < 4) Result.retry() else Result.failure() }
    }
}
