package com.sadique.dailyledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class, LoanEntity::class, LoanPaymentEntity::class, CommitteeEntity::class, CommitteePaymentEntity::class, SavingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "daily-ledger.db")
                .build().also { INSTANCE = it }
        }
    }
}
