package com.sadique.dailyledger.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sadique.dailyledger.R
import com.sadique.dailyledger.data.AppDatabase
import com.sadique.dailyledger.data.SettingsStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val user = SettingsStore(applicationContext).user.first() ?: return Result.success()
        val dao = AppDatabase.get(applicationContext).ledgerDao()
        val loans = dao.loansNow(user.id)
        val committees = dao.committeesNow(user.id)
        val committeePayments = dao.committeePaymentsNow(user.id)
        val transactions = dao.transactionsNow(user.id)
        val today = LocalDate.now()

        val dueLoans = loans.count {
            !it.closed && it.dueDate?.let { date ->
                runCatching { LocalDate.parse(date) }.getOrNull()?.let { d ->
                    !d.isBefore(today) && !d.isAfter(today.plusDays(3))
                }
            } == true
        }
        val currentMonth = YearMonth.now().toString()
        val committeeDue = committees.count { committee ->
            committee.active && committeePayments.none { it.committeeId == committee.id && it.month == currentMonth }
        }

        val messages = mutableListOf<String>()
        if (dueLoans > 0) messages += "$dueLoans loan due soon"
        if (committeeDue > 0) messages += "$committeeDue kameti payment due"
        if (today.dayOfMonth == 1) {
            val previous = YearMonth.now().minusMonths(1).toString()
            val income = transactions.filter { it.type == "INCOME" && it.date.startsWith(previous) }.sumOf { it.amountMinor }
            val expense = transactions.filter { it.type == "EXPENSE" && it.date.startsWith(previous) }.sumOf { it.amountMinor }
            messages += "Last month: income PKR ${income / 100}, expense PKR ${expense / 100}"
        }
        if (messages.isNotEmpty()) notify(messages.joinToString(" • "))
        return Result.success()
    }

    private fun notify(text: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("reminders", "Ledger reminders", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(
            101,
            NotificationCompat.Builder(applicationContext, "reminders")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Daily Ledger")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build()
        )
    }
}
