package com.sadique.dailyledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.money
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneId

@Composable
fun DashboardScreen(
    tx: List<TransactionEntity>,
    loans: List<LoanEntity>,
    lp: List<LoanPaymentEntity>,
    committees: List<CommitteeEntity>,
    cp: List<CommitteePaymentEntity>,
    savings: List<SavingEntity>,
) {
    val month = YearMonth.now().toString()
    val monthTx = tx.filter { it.date.startsWith(month) }
    val income = monthTx.filter { it.type == "INCOME" }.sumOf { it.amountMinor }
    val expenseItems = monthTx.filter { it.type == "EXPENSE" }
    val expense = expenseItems.sumOf { it.amountMinor }
    val direct = savings.filter { it.date.startsWith(month) }.sumOf { it.amountMinor }
    val committee = cp.filter { it.month == month }.sumOf { it.amountMinor }
    val borrowed = loans.filter { it.direction == "BORROWED" }.sumOf { loan ->
        (loan.principalMinor - lp.filter { it.loanId == loan.id }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }
    val lent = loans.filter { it.direction == "LENT" }.sumOf { loan ->
        (loan.principalMinor - lp.filter { it.loanId == loan.id }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }
    val loanPrincipalCash = loans.filter {
        YearMonth.from(Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault())) == YearMonth.now()
    }.sumOf { if (it.direction == "BORROWED") it.principalMinor else -it.principalMinor }
    val loanPaymentCash = lp.filter { it.date.startsWith(month) }.sumOf { payment ->
        val loan = loans.firstOrNull { it.id == payment.loanId }
        if (loan?.direction == "BORROWED") -payment.amountMinor else payment.amountMinor
    }
    val available = income - expense + loanPrincipalCash + loanPaymentCash - direct - committee
    val categories = expenseItems.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amountMinor } }
        .entries.sortedByDescending { it.value }.take(5)
    val maxCategory = categories.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("This month", style = MaterialTheme.typography.headlineMedium) }
        item { Metric("Available after set-aside", money(available)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { Metric("Income", money(income)) }
                Box(Modifier.weight(1f)) { Metric("Expenses", money(expense)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { Metric("Direct savings", money(direct)) }
                Box(Modifier.weight(1f)) { Metric("Kameti paid", money(committee)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { Metric("I owe", money(borrowed)) }
                Box(Modifier.weight(1f)) { Metric("Owed to me", money(lent)) }
            }
        }
        if (categories.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Top expense categories", style = MaterialTheme.typography.titleMedium)
                        categories.forEach { (name, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name)
                                Text(money(value))
                            }
                            LinearProgressIndicator(
                                progress = { value.toFloat() / maxCategory.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        item {
            Text("Tip: record leftover cash in Savings → Leftover so your available balance stays realistic.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Metric(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
