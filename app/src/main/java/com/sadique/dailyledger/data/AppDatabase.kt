package com.sadique.dailyledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sadique.dailyledger.security.DatabaseEncryption
import com.sadique.dailyledger.security.EncryptedOpenHelperFactory

@Database(
    entities = [TransactionEntity::class, LoanEntity::class, LoanPaymentEntity::class, CommitteeEntity::class, CommitteePaymentEntity::class, CommitteeReceiptEntity::class, SavingEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE committees ADD COLUMN shares INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE TABLE IF NOT EXISTS committee_receipts (id TEXT NOT NULL, ownerId TEXT NOT NULL, committeeId TEXT NOT NULL, amountMinor INTEGER NOT NULL, date TEXT, note TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_committee_receipts_ownerId ON committee_receipts(ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_committee_receipts_committeeId ON committee_receipts(committeeId)")
                db.execSQL("INSERT INTO committee_receipts (id, ownerId, committeeId, amountMinor, date, note, createdAt) SELECT id || '-payout', ownerId, id, monthlyAmountMinor * totalInstallments, NULL, 'Earlier received status', createdAt FROM committees WHERE received = 1")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val file = context.getDatabasePath("daily-ledger.db")
                val password = DatabaseEncryption.password(context.applicationContext, file)
                DatabaseEncryption.migrate(file, password)
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "daily-ledger.db")
                    .openHelperFactory(EncryptedOpenHelperFactory(password))
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
