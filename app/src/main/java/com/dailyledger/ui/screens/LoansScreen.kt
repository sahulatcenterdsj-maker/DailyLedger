package com.dailyledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.dailyledger.data.local.entity.LoanEntity
import com.dailyledger.data.model.Money
import com.dailyledger.ui.theme.*

@Composable
fun LoansScreen(
    loans: List<LoanEntity>,
    onOpenAdd: () -> Unit,
    onRepayment: (LoanEntity) -> Unit,
    onDelete: (LoanEntity) -> Unit
) {
    val totalGivenRemaining = loans.filter { it.type == "given" && it.status == "active" }
        .sumOf { it.principalPaisa - it.repaidPaisa }
    val totalTakenRemaining = loans.filter { it.type == "taken" && it.status == "active" }
        .sumOf { it.principalPaisa - it.repaidPaisa }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = Amber500,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Udhaar & Loans",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Keep track of money lent to friends/family or borrowed from others",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }

            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald50)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("YOU ARE OWED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Money.formatPkr(totalGivenRemaining),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald700
                            )
                            Text("Lent out", fontSize = 10.sp, color = Slate500)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("YOU OWE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Amber500)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Money.formatPkr(totalTakenRemaining),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Amber500
                            )
                            Text("Borrowed", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            }

            if (loans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active Udhaar records. Tap '+' to create.", color = Slate500)
                    }
                }
            } else {
                items(loans, key = { it.id }) { loan ->
                    val isGiven = loan.type == "given"
                    val remaining = loan.principalPaisa - loan.repaidPaisa
                    val isCleared = remaining <= 0L

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isGiven) Emerald100 else Amber500.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isGiven) Emerald700 else Amber500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(loan.counterpartyName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            if (isGiven) "You lent to them" else "You borrowed from them",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (isCleared) "CLEARED" else "ACTIVE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Principal", fontSize = 11.sp, color = Slate500)
                                    Text(Money.formatPkr(loan.principalPaisa), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Column {
                                    Text("Repaid", fontSize = 11.sp, color = Slate500)
                                    Text(Money.formatPkr(loan.repaidPaisa), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Emerald600)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining", fontSize = 11.sp, color = Slate500)
                                    Text(
                                        Money.formatPkr(remaining),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (isGiven) Emerald700 else Amber500
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Due Date: ${loan.dueDate}",
                                fontSize = 11.sp,
                                color = Slate500
                            )

                            if (!isCleared) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onRepayment(loan) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Record Payment", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { onDelete(loan) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate500)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
