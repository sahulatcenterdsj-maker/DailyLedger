package com.sadique.dailyledger.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions", indices = [Index("ownerId"), Index("date")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val type: String, // INCOME | EXPENSE
    val amountMinor: Long,
    val category: String,
    val note: String,
    val date: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "loans", indices = [Index("ownerId")])
data class LoanEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val direction: String, // BORROWED | LENT
    val person: String,
    val principalMinor: Long,
    val dueDate: String?,
    val note: String,
    val createdAt: Long,
    val closed: Boolean = false,
)

@Entity(tableName = "loan_payments", indices = [Index("ownerId"), Index("loanId")])
data class LoanPaymentEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val loanId: String,
    val amountMinor: Long,
    val date: String,
    val note: String,
    val createdAt: Long,
)

@Entity(tableName = "committees", indices = [Index("ownerId")])
data class CommitteeEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val monthlyAmountMinor: Long,
    val totalInstallments: Int,
    val startMonth: String,
    val payoutInstallment: Int?,
    val received: Boolean,
    val active: Boolean,
    val note: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "1") val shares: Int = 1,
)

@Entity(tableName = "committee_payments", indices = [Index("ownerId"), Index("committeeId")])
data class CommitteePaymentEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val committeeId: String,
    val installmentNumber: Int,
    val month: String,
    val amountMinor: Long,
    val paidAt: Long,
)

@Entity(tableName = "committee_receipts", indices = [Index("ownerId"), Index("committeeId")])
data class CommitteeReceiptEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val committeeId: String,
    val amountMinor: Long,
    val date: String?, // Older "received" flags did not record a receiving date.
    val note: String,
    val createdAt: Long,
)

@Entity(tableName = "savings", indices = [Index("ownerId"), Index("date")])
data class SavingEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val kind: String, // DIRECT | LEFTOVER
    val amountMinor: Long,
    val date: String,
    val note: String,
    val createdAt: Long,
)
