package com.sadique.dailyledger

import android.app.Application
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.notifications.ReminderScheduler
import com.sadique.dailyledger.sync.SyncScheduler

class DailyLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseRuntime.initAppCheck(this)
        SyncScheduler.schedule(this)
        SyncScheduler.syncNow(this)
        ReminderScheduler.schedule(this)
    }
}
