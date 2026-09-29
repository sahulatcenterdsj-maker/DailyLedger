package com.dailyledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dailyledger.data.local.dao.*
import com.dailyledger.data.local.entity.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class,
        KametiEntity::class,
        KametiInstallmentEntity::class,
        SavingsGoalEntity::class,
        SavingsHistoryEntity::class,
        LeftoverSweepEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun kametiDao(): KametiDao
    abstract fun savingsDao(): SavingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_ledger.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
