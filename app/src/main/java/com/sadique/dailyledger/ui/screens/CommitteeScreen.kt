package com.sadique.dailyledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.*

@Composable
fun CommitteeScreen(
    committees: List<CommitteeEntity>,
    payments: List<CommitteePaymentEntity>,
    receipts: List<CommitteeReceiptEntity>,
    onAdd: (String, Long, Int, String, Int?, String, Int) -> Unit,
    onPaid: (CommitteeEntity, Int, String) -> Unit,
    onReceive: (String, Long, String, String) -> Unit,
    onDeleteReceipt: (CommitteeReceiptEntity) -> Unit,
    onDelete: (CommitteeEntity) -> Unit,
) {
    var add by remember { mutableStateOf(false) }
    var receivingId by remember { mutableStateOf<String?>(null) }
    var deleteCommittee by remember { mutableStateOf<CommitteeEntity?>(null) }
    var deleteReceipt by remember { mutableStateOf<CommitteeReceiptEntity?>(null) }
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { add = true }) { Icon(Icons.Outlined.Add, "New kameti") }
    }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Kameti", style = MaterialTheme.typography.headlineMedium)
                Text("Contributions and receiving, separate from your salary.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Total paid", payments.sumOf { it.amountMinor }, Icons.Outlined.Payments, Modifier.weight(1f))
                    SummaryCard("Total received", receipts.sumOf { it.amountMinor }, Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f))
                }
            }
            if (committees.isEmpty()) item {
                Text("Add a kameti to track installments. If you have two at the same place, set Number of kametis to 2.", Modifier.padding(vertical = 16.dp))
            }
            items(committees, key = { it.id }) { committee ->
                val ps = payments.filter { it.committeeId == committee.id }
                val rs = receipts.filter { it.committeeId == committee.id }
                val balance = committeeBalance(committee, rs)
                val next = (1..committee.totalInstallments).firstOrNull { n -> ps.none { it.installmentNumber == n } }
                var history by remember(committee.id) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=if(balance.complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(committee.name, style = MaterialTheme.typography.titleMedium)
                                Text("${committee.shares} ${if (committee.shares == 1) "kameti" else "kametis"} · ${ps.size}/${committee.totalInstallments} installments paid", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { deleteCommittee = committee }) { Icon(Icons.Outlined.DeleteOutline, "Delete ${committee.name}") }
                        }
                        Text("Monthly total: ${money(committee.monthlyContribution())}")
                        if (committee.shares > 1) Text("${money(committee.monthlyAmountMinor)} per kameti", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        BalanceRow("Expected receiving", balance.expected)
                        BalanceRow("Received", balance.received)
                        BalanceRow("Remaining", balance.remaining)
                        LinearProgressIndicator(progress = { if (balance.expected > 0) (balance.received.toFloat() / balance.expected).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth())
                        Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(12.dp)){Text(balance.status,modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp),color=MaterialTheme.colorScheme.onSecondaryContainer,style=MaterialTheme.typography.labelLarge)}
                        committee.payoutInstallment?.let { Text("Planned payout installment: $it", style = MaterialTheme.typography.bodySmall) }
                        if (committee.note.isNotBlank()) Text(committee.note, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { next?.let { onPaid(committee, it, LedgerRepository.thisMonth()) } }, enabled = next != null, modifier = Modifier.weight(1f)) {
                                Text(next?.let { "Pay #$it" } ?: "All paid")
                            }
                            Button(onClick = { receivingId = committee.id }, enabled = balance.remaining > 0, modifier = Modifier.weight(1f)) {
                                Text(if (balance.complete) "Received" else "Receive amount")
                            }
                        }
                        if (ps.isNotEmpty() || rs.isNotEmpty()) {
                            TextButton(onClick = { history = !history }) {
                                Icon(if (history) Icons.Outlined.ExpandLess else Icons.Outlined.History, null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (history) "Hide history" else "Receiving & payment history")
                            }
                        }
                        if (history) {
                            if (rs.isNotEmpty()) Text("Receiving history", style = MaterialTheme.typography.titleSmall)
                            rs.sortedWith(compareByDescending<CommitteeReceiptEntity> { it.date }.thenByDescending { it.createdAt }).forEach { receipt ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(money(receipt.amountMinor), style = MaterialTheme.typography.titleSmall)
                                        Text(receipt.date ?: "Date not recorded", style = MaterialTheme.typography.bodySmall)
                                        if (receipt.note.isNotBlank()) Text(receipt.note, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { deleteReceipt = receipt }) { Icon(Icons.Outlined.DeleteOutline, "Delete receiving") }
                                }
                            }
                            if (ps.isNotEmpty()) Text("Installments paid", style = MaterialTheme.typography.titleSmall)
                            ps.sortedByDescending { it.installmentNumber }.forEach {
                                Text("#${it.installmentNumber} · ${it.month} · ${money(it.amountMinor)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    if (add) CommitteeDialog({ add = false }) { n, a, t, s, payout, note, shares -> onAdd(n, a, t, s, payout, note, shares); add = false }
    committees.firstOrNull { it.id == receivingId }?.let { committee ->
        ReceiveDialog(committee, committeeBalance(committee, receipts), { receivingId = null }) { amount, date, note ->
            onReceive(committee.id, amount, date, note)
            receivingId = null
        }
    }
    deleteCommittee?.let { committee ->
        AlertDialog(onDismissRequest = { deleteCommittee = null }, title = { Text("Delete ${committee.name}?") }, text = { Text("This removes this kameti and its payment and receiving history.") },
            confirmButton = { TextButton(onClick = { onDelete(committee); deleteCommittee = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteCommittee = null }) { Text("Cancel") } })
    }
    deleteReceipt?.let { receipt ->
        AlertDialog(onDismissRequest = { deleteReceipt = null }, title = { Text("Delete this receiving?") }, text = { Text("${money(receipt.amountMinor)} will be added back to the remaining receiving amount.") },
            confirmButton = { TextButton(onClick = { onDeleteReceipt(receipt); deleteReceipt = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteReceipt = null }) { Text("Cancel") } })
    }
}

@Composable
private fun BalanceRow(label: String, amount: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(money(amount), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ReceiveDialog(committee: CommitteeEntity, balance: CommitteeBalance, dismiss: () -> Unit, save: (Long, String, String) -> Unit) {
    var amount by remember(committee.id) { mutableStateOf(plainAmount(minOf(balance.remaining, committee.expectedPayout() / committee.shares))) }
    var date by remember { mutableStateOf(LedgerRepository.today()) }
    var note by remember { mutableStateOf("") }
    val parsed = parseMinor(amount)
    val validAmount = parsed != null && parsed > 0 && parsed <= balance.remaining
    AlertDialog(onDismissRequest = dismiss, title = { Text("Receive · ${committee.name}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Remaining: ${money(balance.remaining)}")
            Text("Enter only the amount received now. You can add the next receiving later.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(amount, { amount = it }, label = { Text("Received amount PKR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, isError = !validAmount)
            OutlinedTextField(date, { date = it }, label = { Text("Date YYYY-MM-DD") }, isError = !isValidDate(date), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note (e.g. first kameti)") })
        }
    }, confirmButton = { Button(enabled = validAmount && isValidDate(date), onClick = { parsed?.let { save(it, date.trim(), note) } }) { Text("Save receiving") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun CommitteeDialog(dismiss: () -> Unit, save: (String, Long, Int, String, Int?, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("12") }
    var shares by remember { mutableStateOf("1") }
    var start by remember { mutableStateOf(LedgerRepository.thisMonth()) }
    var payout by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val a = parseMinor(amount)
    val t = total.toIntOrNull()
    val s = shares.toIntOrNull()
    val p = payout.toIntOrNull()
    val combined = runCatching { Math.multiplyExact(a ?: 0, (s ?: 0).toLong()) }.getOrNull()
    val expected = runCatching { Math.multiplyExact(combined ?: 0, (t ?: 0).toLong()) }.getOrNull()
    val valid = a != null && a > 0 && t != null && t > 0 && s != null && s > 0 && expected != null && expected > 0 && isValidMonth(start) && (payout.isBlank() || (p != null && p in 1..t))
    AlertDialog(onDismissRequest = dismiss, title = { Text("New kameti") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name / place") })
            OutlinedTextField(shares, { shares = it }, label = { Text("Number of kametis at this place") }, supportingText = { Text("For 2 kametis at the same place, enter 2.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(amount, { amount = it }, label = { Text("Monthly PKR per kameti") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(total, { total = it }, label = { Text("Total monthly installments") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            if (valid) {
                Text("Monthly total: ${money(combined!!)}", style = MaterialTheme.typography.bodySmall)
                Text("Total expected receiving: ${money(expected!!)}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(start, { start = it }, label = { Text("Start month YYYY-MM") }, isError = !isValidMonth(start), singleLine = true)
            OutlinedTextField(payout, { payout = it }, label = { Text("First payout installment (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") })
        }
    }, confirmButton = { Button(enabled = valid, onClick = { save(name.ifBlank { "Kameti" }, a!!, t!!, start.trim(), p, note, s!!) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
