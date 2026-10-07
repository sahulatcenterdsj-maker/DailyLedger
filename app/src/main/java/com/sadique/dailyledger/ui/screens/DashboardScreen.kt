package com.sadique.dailyledger.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.ai.SpendingSnapshot
import com.sadique.dailyledger.ai.SpendingTip
import com.sadique.dailyledger.data.monthlySalarySummary
import com.sadique.dailyledger.data.committeeBalance
import com.sadique.dailyledger.data.CommitteeEntity
import com.sadique.dailyledger.data.CommitteePaymentEntity
import com.sadique.dailyledger.data.CommitteeReceiptEntity
import com.sadique.dailyledger.data.CommitteeMemberEntity
import com.sadique.dailyledger.data.LoanEntity
import com.sadique.dailyledger.data.LoanPaymentEntity
import com.sadique.dailyledger.data.SavingEntity
import com.sadique.dailyledger.data.TransactionEntity
import com.sadique.dailyledger.ui.SummaryCard
import com.sadique.dailyledger.ui.money
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    tx: List<TransactionEntity>,
    loans: List<LoanEntity>,
    lp: List<LoanPaymentEntity>,
    committees: List<CommitteeEntity>,
    cp: List<CommitteePaymentEntity>,
    receipts: List<CommitteeReceiptEntity>,
    committeeMembers: List<CommitteeMemberEntity> = emptyList(),
    savings: List<SavingEntity>,
    userName: String,
    weatherEnabled: Boolean,
    weatherCity: String,
    weatherTemperature: String,
    weatherCondition: String,
    onAddSalary: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenKameti: () -> Unit,
    onAutoFill: (() -> Unit)? = null,
    snapshot: SpendingSnapshot? = null,
    aiTips: List<SpendingTip> = emptyList(),
    aiStatus: String = "",
    aiEnabled: Boolean = false,
    onEnableAi: () -> Unit = {},
) {
    val month = YearMonth.now()
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val summary = monthlySalarySummary(tx, month.toString())
    val expenseItems = tx.filter { it.date.startsWith("$month-") && it.type == "EXPENSE" }
    val categories = expenseItems.groupBy { it.category }
        .mapValues { it.value.sumOf { row -> row.amountMinor } }
        .entries.sortedByDescending { it.value }.take(5)
    val borrowed = loans.filter { it.direction == "BORROWED" }.sumOf { loan ->
        (loan.principalMinor - lp.filter { it.loanId == loan.id }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }
    val lent = loans.filter { it.direction == "LENT" }.sumOf { loan ->
        (loan.principalMinor - lp.filter { it.loanId == loan.id }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }
    val received = receipts.sumOf { it.amountMinor }
    val pending = committees.sumOf { committeeBalance(it, receipts).remaining }
    val salary = summary.salary.coerceAtLeast(0L)
    val spentRatio = if (salary > 0L) {
        (summary.expenses.toFloat() / salary.toFloat()).coerceIn(0f, 1f)
    } else if (summary.expenses > 0L) 1f else 0f
    val remainingRatio = (1f - spentRatio).coerceIn(0f, 1f)
    val spentPct = (spentRatio * 100).roundToInt()
    val remainingPct = (remainingRatio * 100).roundToInt()
    val overBudget = summary.remaining < 0
    val nextMyKametiTurn = committeeMembers
        .filter { it.isMe && !it.received }
        .mapNotNull { member -> runCatching { YearMonth.parse(member.turnMonth) }.getOrNull()?.let { it to member } }
        .filter { (m, _) -> !m.isBefore(month) }
        .minByOrNull { it.first }
    val animatedSpentRatio = animateFloatAsState(
        targetValue = spentRatio,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
        label = "budget-progress",
    ).value

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("dashboard-list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Assalam o Alaikum, ${userName.substringBefore(' ').ifBlank { "Friend" }}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (weatherEnabled && (weatherCity.isNotBlank() || weatherCondition.isNotBlank() || weatherTemperature.isNotBlank())) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            val line = listOf(weatherCity, weatherCondition, weatherTemperature)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            Text(if (line.isBlank()) "Weather not set" else line)
                        },
                        leadingIcon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (overBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)) {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(
                            text = "Remaining balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = money(summary.remaining),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = if (overBudget) "You are above budget this month." else "Your monthly salary minus expenses.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        LinearProgressIndicator(
                            progress = { animatedSpentRatio },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniStatPill("Spent", "$spentPct%")
                            MiniStatPill("Left", "$remainingPct%")
                            if (summary.otherIncome > 0L) MiniStatPill("Other income", money(summary.otherIncome))
                        }
                    }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick actions", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onAddSalary, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Add salary")
                    }
                    OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Add expense")
                    }
                }
                onAutoFill?.let { action ->
                    FilledTonalButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("AI Auto Fill")
                    }
                }
            }
        }
        item(key = "separate-totals") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Money spaces", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        "Savings total",
                        savings.sumOf { it.amountMinor },
                        Icons.Outlined.AccountBalanceWallet,
                        Modifier.weight(1f),
                        "All saved amounts",
                        onOpenSavings,
                    )
                    SummaryCard(
                        "Kameti paid",
                        cp.sumOf { it.amountMinor },
                        Icons.Outlined.Groups,
                        Modifier.weight(1f),
                        "Received: ${money(received)}\nPending: ${money(pending)}",
                        onOpenKameti,
                    )
                }
                nextMyKametiTurn?.let { (turnMonth, member) ->
                    val today = java.time.LocalDate.now()
                    val start = turnMonth.atDay(1)
                    val days = java.time.temporal.ChronoUnit.DAYS.between(today, start)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Your kameti turn • ${member.turnMonth}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                when {
                                    turnMonth == month -> "Your kameti month is here"
                                    days > 0 -> "$days days remaining"
                                    else -> "Turn month is due"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (loans.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard("I owe", borrowed, Icons.Outlined.Handshake, Modifier.weight(1f))
                        SummaryCard("Owed to me", lent, Icons.Outlined.AccountBalance, Modifier.weight(1f))
                    }
                }
            }
        }
        if (categories.isNotEmpty()) {
            item { SpendingDonutCard(categories.map { it.key to it.value }) }
        }
        val recentTransactions = tx.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.updatedAt }).take(4)
        if (recentTransactions.isNotEmpty()) {
            item { RecentTransactionsCard(recentTransactions) }
        }
        snapshot?.let { data ->
            item(key = "insights") { InsightsCard(data, aiTips, aiStatus, aiEnabled, onEnableAi) }
        }
    }
}

@Composable
private fun SpendingDonutCard(categories: List<Pair<String, Long>>) {
    val total = categories.sumOf { it.second }.coerceAtLeast(1L)
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline,
    )
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Spending by category", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(122.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        categories.forEachIndexed { index, (_, value) ->
                            val sweep = 360f * (value.toFloat() / total.toFloat())
                            drawArc(
                                color = palette[index % palette.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 22.dp.toPx()),
                            )
                            startAngle += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spent", style = MaterialTheme.typography.labelMedium)
                        Text(money(categories.sumOf { it.second }), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    categories.forEachIndexed { index, (name, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = palette[index % palette.size], modifier = Modifier.size(10.dp)) {}
                                Text(name, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${((value * 100.0) / total).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(items: List<TransactionEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recent transactions", style = MaterialTheme.typography.titleMedium)
            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.category.ifBlank { item.note.ifBlank { "Transaction" } }, style = MaterialTheme.typography.titleSmall)
                        Text(item.note.ifBlank { item.date }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(
                        text = (if (item.type == "INCOME") "+ " else "− ") + money(item.amountMinor),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
