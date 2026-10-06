package com.sadique.dailyledger.data

data class SalarySummary(val salary: Long, val expenses: Long, val otherIncome: Long) {
    val remaining: Long get() = salary - expenses
}

fun TransactionEntity.isSalary(): Boolean = type == "INCOME" &&
    (category.trim().equals("Salary", ignoreCase = true) ||
        category.trim().equals("Monthly salary", ignoreCase = true) || category.trim() == "تنخواہ")

fun monthlySalarySummary(transactions: List<TransactionEntity>, month: String): SalarySummary {
    val current = transactions.filter { it.date.startsWith("$month-") }
    return SalarySummary(
        salary = current.filter { it.isSalary() }.sumOf { it.amountMinor },
        expenses = current.filter { it.type == "EXPENSE" }.sumOf { it.amountMinor },
        otherIncome = current.filter { it.type == "INCOME" && !it.isSalary() }.sumOf { it.amountMinor },
    )
}

fun CommitteeEntity.monthlyContribution(): Long = Math.multiplyExact(monthlyAmountMinor, shares.toLong())
fun CommitteeEntity.expectedPayout(): Long = Math.multiplyExact(monthlyContribution(), totalInstallments.toLong())

data class CommitteeBalance(val expected: Long, val received: Long) {
    val remaining: Long get() = (expected - received).coerceAtLeast(0L)
    val complete: Boolean get() = expected > 0L && received >= expected
    val status: String get() = when { complete -> "Fully received"; received > 0L -> "Partially received"; else -> "Not received" }
}

fun committeeBalance(committee: CommitteeEntity, receipts: List<CommitteeReceiptEntity>): CommitteeBalance =
    CommitteeBalance(committee.expectedPayout(), receipts.filter { it.committeeId == committee.id && it.ownerId == committee.ownerId }.fold(0L) { total, receipt -> Math.addExact(total, receipt.amountMinor) })
