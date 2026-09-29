package com.dailyledger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String? = null,
    val type: String = "offline", // "offline" or "google"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val type: String, // "income", "expense", "transfer"
    val amountPaisa: Long,
    val category: String,
    val date: String, // "YYYY-MM-DD"
    val paymentMethod: String = "Cash",
    val note: String? = null,
    val counterparty: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val type: String, // "given" (owed to you), "taken" (you owe)
    val counterpartyName: String,
    val counterpartyContact: String? = null,
    val principalPaisa: Long,
    val repaidPaisa: Long = 0L,
    val startDate: String,
    val dueDate: String,
    val status: String = "active", // "active", "cleared", "defaulted"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loan_repayments")
data class LoanRepaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loanId: String,
    val amountPaisa: Long,
    val date: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kametis")
data class KametiEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val title: String,
    val organizerName: String,
    val totalMembers: Int,
    val monthlyInstallmentPaisa: Long,
    val totalPoolPaisa: Long,
    val payoutMonthIndex: Int,
    val payoutStatus: String = "pending", // "pending", "received"
    val payoutDate: String? = null,
    val startDate: String, // "YYYY-MM"
    val status: String = "active", // "active", "completed"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kameti_installments")
data class KametiInstallmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kametiId: String,
    val monthIndex: Int,
    val paid: Boolean = false,
    val paidDate: String? = null
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val title: String,
    val category: String,
    val targetPaisa: Long,
    val currentPaisa: Long = 0L,
    val targetDate: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_history")
data class SavingsHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val type: String, // "deposit", "withdraw"
    val amountPaisa: Long,
    val date: String,
    val note: String? = null
)

@Entity(tableName = "leftover_sweeps")
data class LeftoverSweepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val month: String, // "YYYY-MM"
    val amountPaisa: Long,
    val date: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
