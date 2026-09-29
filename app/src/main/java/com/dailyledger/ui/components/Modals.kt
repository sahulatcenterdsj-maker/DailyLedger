package com.dailyledger.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyledger.data.model.Money
import com.dailyledger.ui.theme.Emerald600

@Composable
fun AddTransactionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (type: String, amountPaisa: Long, category: String, note: String?) -> Unit
) {
    if (!isOpen) return

    var type by remember { mutableStateOf("expense") }
    var amountRupees by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Grocery") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "expense",
                        onClick = { type = "expense" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == "income",
                        onClick = { type = "income" },
                        label = { Text("Income") }
                    )
                }

                OutlinedTextField(
                    value = amountRupees,
                    onValueChange = { amountRupees = it },
                    label = { Text("Amount (PKR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rupees = amountRupees.toDoubleOrNull()
                    if (rupees != null && rupees > 0) {
                        onSubmit(type, Money.rupeesToPaisa(rupees), category.trim(), note.trim().ifEmpty { null })
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddLoanDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (type: String, name: String, amountPaisa: Long, dueDate: String) -> Unit
) {
    if (!isOpen) return

    var type by remember { mutableStateOf("given") }
    var name by remember { mutableStateOf("") }
    var amountRupees by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(java.time.LocalDate.now().plusMonths(1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Udhaar Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "given",
                        onClick = { type = "given" },
                        label = { Text("Lent (Given)") }
                    )
                    FilterChip(
                        selected = type == "taken",
                        onClick = { type = "taken" },
                        label = { Text("Borrowed (Taken)") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountRupees,
                    onValueChange = { amountRupees = it },
                    label = { Text("Principal Amount (PKR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rupees = amountRupees.toDoubleOrNull()
                    if (name.isNotBlank() && rupees != null && rupees > 0) {
                        onSubmit(type, name.trim(), Money.rupeesToPaisa(rupees), dueDate.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddKametiDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, organizer: String, members: Int, installmentPaisa: Long, turnMonth: Int) -> Unit
) {
    if (!isOpen) return

    var title by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("Self") }
    var members by remember { mutableStateOf("10") }
    var installmentRupees by remember { mutableStateOf("") }
    var turnMonth by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Kameti", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Kameti Title (e.g. Family 100k)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    label = { Text("Organizer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = members,
                    onValueChange = { members = it },
                    label = { Text("Total Members / Months") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = installmentRupees,
                    onValueChange = { installmentRupees = it },
                    label = { Text("Monthly Installment (PKR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = turnMonth,
                    onValueChange = { turnMonth = it },
                    label = { Text("Your Payout Turn Month (1 to N)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inst = installmentRupees.toDoubleOrNull()
                    val totalM = members.toIntOrNull() ?: 10
                    val turn = turnMonth.toIntOrNull() ?: 1
                    if (title.isNotBlank() && inst != null && inst > 0) {
                        onSubmit(title.trim(), organizer.trim(), totalM, Money.rupeesToPaisa(inst), turn)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("Start Kameti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddSavingsGoalDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, targetPaisa: Long) -> Unit
) {
    if (!isOpen) return

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Emergency Fund") }
    var targetRupees by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Bike / Umrah)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetRupees,
                    onValueChange = { targetRupees = it },
                    label = { Text("Target Amount (PKR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetRupees.toDoubleOrNull()
                    if (title.isNotBlank() && target != null && target > 0) {
                        onSubmit(title.trim(), category.trim(), Money.rupeesToPaisa(target))
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
