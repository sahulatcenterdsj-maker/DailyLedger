package com.dailyledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyledger.data.local.entity.TransactionEntity
import com.dailyledger.data.model.Money
import com.dailyledger.ui.theme.*

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    onOpenAdd: () -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("all") } // "all", "income", "expense"

    val filtered = remember(transactions, searchQuery, selectedType) {
        transactions.filter { tx ->
            val matchType = selectedType == "all" || tx.type == selectedType
            val matchQuery = searchQuery.isBlank() ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    (tx.counterparty?.contains(searchQuery, ignoreCase = true) == true)
            matchType && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = Emerald600,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transactions",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${filtered.size} records found",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search note, category, or person...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("all" to "All", "expense" to "Expenses", "income" to "Income")
                    items(filters) { (key, label) ->
                        val isSelected = selectedType == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald100,
                                selectedLabelColor = Emerald700
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching transactions", color = Slate500, fontSize = 14.sp)
                    }
                }
            } else {
                items(filtered, key = { it.id }) { tx ->
                    val isIncome = tx.type == "income"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) Emerald100 else Rose500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) Emerald700 else Rose600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = tx.note ?: tx.category,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${tx.date} • ${tx.category} • ${tx.paymentMethod}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                                if (!tx.counterparty.isNullOrBlank()) {
                                    Text(
                                        text = "Party: ${tx.counterparty}",
                                        fontSize = 11.sp,
                                        color = Emerald700
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isIncome) "+" else "-"}${Money.formatPkr(tx.amountPaisa)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isIncome) Emerald600 else Rose600
                                )
                                IconButton(
                                    onClick = { onDelete(tx) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
