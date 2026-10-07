package com.sadique.dailyledger.security

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

/** SQLCipher Room adapter whose corruption handler never deletes the user's ledger. */
class EncryptedOpenHelperFactory(private val password: ByteArray) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val helper = object : SQLiteOpenHelper(configuration.context, configuration.name, password, null,
            configuration.callback.version, 0,
            { throw IllegalStateException("Database integrity check failed. The original file was preserved.") }, null, false) {
            override fun onCreate(db: SQLiteDatabase) = configuration.callback.onCreate(db)
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = configuration.callback.onUpgrade(db, oldVersion, newVersion)
            override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = configuration.callback.onDowngrade(db, oldVersion, newVersion)
            override fun onOpen(db: SQLiteDatabase) = configuration.callback.onOpen(db)
            override fun onConfigure(db: SQLiteDatabase) = configuration.callback.onConfigure(db)
        }
        return object : SupportSQLiteOpenHelper {
            override val databaseName: String? get() = helper.databaseName
            override val writableDatabase: SupportSQLiteDatabase get() = helper.writableDatabase
            override val readableDatabase: SupportSQLiteDatabase get() = helper.readableDatabase
            override fun setWriteAheadLoggingEnabled(enabled: Boolean) = helper.setWriteAheadLoggingEnabled(enabled)
            override fun close() = helper.close()
        }
    }
}
