package com.dailyledger

import android.app.Application
import com.dailyledger.data.local.AppDatabase
import com.dailyledger.data.local.entity.AccountEntity
import com.dailyledger.data.repository.LedgerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DailyLedgerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: LedgerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = LedgerRepository(database)

        // Seed default profile if no account exists
        CoroutineScope(Dispatchers.IO).launch {
            val accounts = database.accountDao().getAllAccounts().firstOrNull()
            if (accounts.isNullOrEmpty()) {
                val defaultAccount = AccountEntity(
                    id = "default_offline",
                    name = "Personal Ledger",
                    type = "offline"
                )
                database.accountDao().insertAccount(defaultAccount)
                repository.setActiveAccount(defaultAccount.id)
            } else {
                repository.setActiveAccount(accounts.first().id)
            }
        }
    }
}
