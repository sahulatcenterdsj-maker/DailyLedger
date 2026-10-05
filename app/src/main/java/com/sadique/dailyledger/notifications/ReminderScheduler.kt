package com.sadique.dailyledger.notifications
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
object ReminderScheduler { fun schedule(c:Context){ WorkManager.getInstance(c).enqueueUniquePeriodicWork("ledger-reminders",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<ReminderWorker>(1,TimeUnit.DAYS).build()) } }
