package com.sadique.dailyledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.SummaryCard
import com.sadique.dailyledger.ui.money
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    tx: List<TransactionEntity>,
    loans: List<LoanEntity>,
    lp: List<LoanPaymentEntity>,
    committees: List<CommitteeEntity>,
    cp: List<CommitteePaymentEntity>,
    receipts: List<CommitteeReceiptEntity>,
    savings: List<SavingEntity>,
    onAddSalary: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenKameti: () -> Unit,
) {
    val month = YearMonth.now()
    val summary = monthlySalarySummary(tx, month.toString())
    val expenseItems = tx.filter { it.date.startsWith("$month-") && it.type == "EXPENSE" }
    val categories = expenseItems.groupBy { it.category }.mapValues { it.value.sumOf { row -> row.amountMinor } }
        .entries.sortedByDescending { it.value }.take(5)
    val maxCategory = categories.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    val borrowed = loans.filter { it.direction == "BORROWED" }.sumOf { loan ->
        (loan.principalMinor - lp.filter { it.loanId == loan.id }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }
    val lent = loans.filter { it.direction == "LENT" }.sumOf { loan ->
        (loan.principalMinor - lp.filter { it.loanId == loan.id }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }
    val received = receipts.sumOf { it.amountMinor }
    val pending = committees.sumOf { committeeBalance(it, receipts).remaining }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Your monthly overview", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            val overBudget = summary.remaining < 0
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = if (overBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(28.dp))
                    Text("Remaining salary", style = MaterialTheme.typography.titleMedium)
                    Text(money(summary.remaining), style = MaterialTheme.typography.headlineLarge)
                    Text("Monthly salary − monthly expenses", style = MaterialTheme.typography.bodySmall)
                    if (summary.salary > 0L) {
                        LinearProgressIndicator(
                            progress = { (summary.expenses.toFloat() / summary.salary.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (overBudget) Text("Expenses are ${money(-summary.remaining)} above salary.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Monthly salary", summary.salary, Icons.Outlined.Payments, Modifier.weight(1f))
                SummaryCard("Monthly expenses", summary.expenses, Icons.Outlined.ShoppingBag, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onAddSalary, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add salary")
                }
                OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add expense")
                }
            }
        }
        if (summary.otherIncome > 0L) {
            item { SummaryCard("Other income this month", summary.otherIncome, Icons.Outlined.TrendingUp, detail = "Recorded separately from salary. Salary entries use the Salary category.") }
        }
        item {
            Text("Savings & Kameti", style = MaterialTheme.typography.titleLarge)
            Text("Separate totals • not deducted from remaining salary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Savings total", savings.sumOf { it.amountMinor }, Icons.Outlined.Savings, Modifier.weight(1f), "All saved amounts", onOpenSavings)
                SummaryCard("Kameti paid", cp.sumOf { it.amountMinor }, Icons.Outlined.Groups, Modifier.weight(1f), "Received: ${money(received)}\nTo receive: ${money(pending)}", onOpenKameti)
            }
        }
        if (loans.isNotEmpty()) {
            item { Text("Loans", style = MaterialTheme.typography.titleLarge) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("I owe", borrowed, Icons.Outlined.Handshake, Modifier.weight(1f))
                    SummaryCard("Owed to me", lent, Icons.Outlined.AccountBalance, Modifier.weight(1f))
                }
            }
        }
        if (categories.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Where you spent", style = MaterialTheme.typography.titleMedium)
                        categories.forEach { (name, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, modifier = Modifier.weight(1f)); Text(money(value))
                            }
                            LinearProgressIndicator(progress = { value.toFloat() / maxCategory.toFloat() }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
