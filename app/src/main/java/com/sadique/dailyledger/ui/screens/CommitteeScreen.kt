package com.sadique.dailyledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.sadique.dailyledger.ui.isValidMonth
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.money
import com.sadique.dailyledger.ui.parseMinor

@Composable
fun CommitteeScreen(
    committees: List<CommitteeEntity>,
    payments: List<CommitteePaymentEntity>,
    onAdd: (String, Long, Int, String, Int?, String) -> Unit,
    onPaid: (CommitteeEntity, Int, String) -> Unit,
    onToggleReceived: (CommitteeEntity) -> Unit,
    onDelete: (CommitteeEntity) -> Unit,
) {
    var add by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { add = true }) { Icon(Icons.Default.Add, null) } }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item {
                Text("Kameti", style = MaterialTheme.typography.headlineMedium)
                Text("Each committee has its own installment history and payout status.")
            }
            items(committees, key = { it.id }) { committee ->
                val ps = payments.filter { it.committeeId == committee.id }
                val next = (ps.maxOfOrNull { it.installmentNumber } ?: 0) + 1
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(committee.name, style = MaterialTheme.typography.titleMedium)
                                Text("${money(committee.monthlyAmountMinor)} monthly • ${ps.size}/${committee.totalInstallments} paid")
                            }
                            IconButton(onClick = { onDelete(committee) }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                        committee.payoutInstallment?.let {
                            Text("Payout installment: $it • ${if (committee.received) "Received" else "Not received"}")
                        }
                        Row {
                            Button(onClick = { if (next <= committee.totalInstallments) onPaid(committee, next, java.time.YearMonth.now().toString()) }, enabled = next <= committee.totalInstallments) {
                                Text("Pay #$next")
                            }
                            Spacer(Modifier.width(8.dp))
                            if (committee.payoutInstallment != null) OutlinedButton(onClick = { onToggleReceived(committee) }) {
                                Text(if (committee.received) "Mark not received" else "Mark received")
                            }
                        }
                    }
                }
            }
        }
    }
    if (add) CommitteeDialog({ add = false }) { n, a, t, s, payout, note -> onAdd(n, a, t, s, payout, note); add = false }
}

@Composable
private fun CommitteeDialog(dismiss: () -> Unit, save: (String, Long, Int, String, Int?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("12") }
    var start by remember { mutableStateOf(LedgerRepository.thisMonth()) }
    var payout by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("New kameti") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(amount, { amount = it }, label = { Text("Monthly amount PKR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(total, { total = it }, label = { Text("Total installments") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(start, { start = it }, label = { Text("Start month YYYY-MM") }, isError = !isValidMonth(start), singleLine = true)
            OutlinedTextField(payout, { payout = it }, label = { Text("Your payout installment (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") })
        }
    }, confirmButton = {
        Button(
            enabled = isValidMonth(start),
            onClick = {
                val a = parseMinor(amount)
                val t = total.toIntOrNull()
                if (a != null && a > 0 && t != null && t > 0) save(name.ifBlank { "Kameti" }, a, t, start.trim(), payout.toIntOrNull(), note)
            },
        ) { Text("Save") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
