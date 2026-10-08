package com.sadique.dailyledger.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreditLedgerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun reviewedDatesDuplicatesPaymentsAndBackupsSurviveWithAccountIsolation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val owner = LedgerRepository(db, "owner")
            val other = LedgerRepository(db, "other")
            val draft = CreditDraft("Aslam Store", "Rashan", 500000, "Groceries", "2026-10-01", "2026-10-15")
            val batch = UUID.randomUUID().toString()
            owner.saveCreditDrafts(listOf(draft, draft), batch)
            owner.saveCreditDrafts(listOf(draft, draft), batch)
            assertEquals(2, owner.creditPurchases.first().size) // Intentional duplicates survive, retries don't duplicate them.
            assertTrue(owner.creditPurchases.first().all { it.purchaseDate == "2026-10-01" })
            assertTrue(other.creditPurchases.first().isEmpty())
            assertTrue(owner.transactions.first().isEmpty()) // Credit is a separate ledger, not a cash expense.
            val id = owner.creditPurchases.first().first().id
            owner.addCreditPayment(id, 100000, "2026-10-08", "First", "Bank")
            assertFalse(owner.creditPurchases.first().first { it.id == id }.closed)
            reject { owner.addCreditPayment(id, 400001, "2026-10-09", "Too much") }
            reject { other.addCreditPayment(id, 1, "2026-10-09", "Wrong owner") }
            owner.addCreditPayment(id, 400000, "2026-10-10", "Last")
            assertTrue(owner.creditPurchases.first().first { it.id == id }.closed)
            owner.saveCreditDrafts(listOf(draft, draft), batch) // An old retry cannot reopen a paid credit.
            assertTrue(owner.creditPurchases.first().first { it.id == id }.closed)
            val json = owner.exportJson()
            other.importJson(json)
            assertEquals(2, other.creditPurchases.first().size)
            val copiedId = other.creditPurchases.first().first { it.closed }.id
            assertTrue(other.creditPayments.first().all { it.ownerId == "other" && it.creditId == copiedId })
            assertEquals(setOf("2026-10-08", "2026-10-10"), other.creditPayments.first().map { it.date }.toSet())
            val corrupt = JSONObject(json).apply { getJSONArray("creditPayments").getJSONObject(0).put("amountMinor", 500001) }
            reject { other.importJson(corrupt.toString()) }
            assertEquals(2, other.creditPayments.first().size) // Entire import rolls back.
            val incomplete = JSONObject(json).apply { remove("creditPurchases") }
            reject { other.importJson(incomplete.toString()) }
            val duplicatePayment = JSONObject(json).apply { val payments = getJSONArray("creditPayments"); payments.put(payments.getJSONObject(0)) }
            reject { other.importJson(duplicatePayment.toString()) }
            other.deleteCreditPurchase(other.creditPurchases.first().first { it.id == copiedId })
            assertTrue(other.creditPayments.first().isEmpty())
            assertEquals(2, owner.creditPayments.first().size)
        } finally { db.close() }
    }
    @Test fun creditOnlyOfflineProfilesAreFoundAndMigrated() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val offline = LedgerRepository(db, "offline-credit")
            offline.saveCreditDrafts(listOf(CreditDraft("Shop", "Soap", 15000, "Soap & body care", "2026-10-01")), UUID.randomUUID().toString())
            assertTrue(offline.hasRecords())
            assertEquals(listOf("offline-credit"), db.ledgerDao().observeOfflineOwners().first())
            assertEquals(1, db.ledgerDao().recordCount("offline-credit"))
            val online = LedgerRepository(db, "online")
            online.migrateOwner("offline-credit")
            assertFalse(offline.hasRecords())
            assertEquals("online", online.creditPurchases.first().single().ownerId)
        } finally { db.close() }
    }
    private suspend fun reject(block: suspend () -> Unit) {
        try { block(); fail("Unsafe credit change accepted") } catch (_: IllegalArgumentException) { }
    }
}
