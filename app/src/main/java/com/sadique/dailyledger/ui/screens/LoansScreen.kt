package com.sadique.dailyledger.ui.screens

import androidx.compose.foundation.layout.*
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
import com.sadique.dailyledger.ui.isValidDate
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.money
import com.sadique.dailyledger.ui.parseMinor

@Composable
fun LoansScreen(
    loans: List<LoanEntity>,
    payments: List<LoanPaymentEntity>,
    onAdd: (String, String, Long, String?, String) -> Unit,
    onPayment: (String, Long, String, String) -> Unit,
    onDelete: (LoanEntity) -> Unit,
) {
    var add by remember { mutableStateOf(false) }
    var pay by remember { mutableStateOf<LoanEntity?>(null) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { add = true }) { Icon(Icons.Default.Add, null) } }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item {
                Text("Loans", style = MaterialTheme.typography.headlineMedium)
                Text("Borrowed and lent money are tracked separately with repayments.")
            }
            items(loans, key = { it.id }) { loan ->
                val paid = payments.filter { it.loanId == loan.id }.sumOf { it.amountMinor }
                val left = (loan.principalMinor - paid).coerceAtLeast(0L)
                Card(onClick = { pay = loan }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${loan.person} • ${if (loan.direction == "BORROWED") "Borrowed" else "Lent"}", style = MaterialTheme.typography.titleMedium)
                            Text("Outstanding ${money(left)} / ${money(loan.principalMinor)}")
                            loan.dueDate?.let { Text("Due $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        IconButton(onClick = { onDelete(loan) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
    if (add) LoanDialog({ add = false }) { d, p, a, due, n -> onAdd(d, p, a, due, n); add = false }
    pay?.let { loan -> PaymentDialog(loan, { pay = null }) { a, date, n -> onPayment(loan.id, a, date, n); pay = null } }
}

@Composable
private fun LoanDialog(dismiss: () -> Unit, save: (String, String, Long, String?, String) -> Unit) {
    var dir by remember { mutableStateOf("BORROWED") }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("New loan") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                FilterChip(dir == "BORROWED", { dir = "BORROWED" }, { Text("Borrowed") })
                Spacer(Modifier.width(8.dp))
                FilterChip(dir == "LENT", { dir = "LENT" }, { Text("Lent") })
            }
            OutlinedTextField(person, { person = it }, label = { Text("Person") })
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount PKR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(due, { due = it }, label = { Text("Due date YYYY-MM-DD (optional)") }, isError = due.isNotBlank() && !isValidDate(due), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") })
        }
    }, confirmButton = {
        Button(
            enabled = due.isBlank() || isValidDate(due),
            onClick = { parseMinor(amount)?.takeIf { it > 0 }?.let { save(dir, person.ifBlank { "Unknown" }, it, due.trim().ifBlank { null }, note) } },
        ) { Text("Save") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun PaymentDialog(loan: LoanEntity, dismiss: () -> Unit, save: (Long, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LedgerRepository.today()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (loan.direction == "BORROWED") "Record repayment" else "Record amount received") }, text = {
        Column {
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount PKR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(date, { date = it }, label = { Text("Date YYYY-MM-DD") }, isError = !isValidDate(date), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") })
        }
    }, confirmButton = { Button(enabled = isValidDate(date), onClick = { parseMinor(amount)?.takeIf { it > 0 }?.let { save(it, date.trim(), note) } }) { Text("Record") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
