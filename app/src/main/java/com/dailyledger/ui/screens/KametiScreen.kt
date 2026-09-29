package com.dailyledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.dailyledger.data.local.entity.KametiEntity
import com.dailyledger.data.local.entity.KametiInstallmentEntity
import com.dailyledger.data.model.Money
import com.dailyledger.ui.theme.*

@Composable
fun KametiScreen(
    kametis: List<KametiEntity>,
    installmentsMap: Map<String, List<KametiInstallmentEntity>>,
    onOpenAdd: () -> Unit,
    onToggleInstallment: (KametiInstallmentEntity) -> Unit,
    onDelete: (KametiEntity) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = Blue500,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Kameti")
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
                        text = "Kameti (ROSCA) Ledger",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Rotating savings committee without bank interest",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }

            if (kametis.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active Kametis. Tap '+' to create your first committee.", color = Slate500)
                    }
                }
            } else {
                items(kametis, key = { it.id }) { kameti ->
                    val installments = installmentsMap[kameti.id] ?: emptyList()
                    val paidCount = installments.count { it.paid }

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
                                Column {
                                    Text(kameti.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Organizer: ${kameti.organizerName}", fontSize = 11.sp, color = Slate500)
                                }
                                IconButton(onClick = { onDelete(kameti) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate500)
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Pool Pot", fontSize = 11.sp, color = Slate500)
                                    Text(Money.formatPkr(kameti.totalPoolPaisa), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column {
                                    Text("Monthly Installment", fontSize = 11.sp, color = Slate500)
                                    Text(Money.formatPkr(kameti.monthlyInstallmentPaisa), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emerald600)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Your Turn", fontSize = 11.sp, color = Slate500)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Amber500, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Month ${kameti.payoutMonthIndex}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Amber500)
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Text("Installments Tracker (Tap to toggle):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                            Spacer(Modifier.height(6.dp))

                            // Installment Grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(installments) { ins ->
                                    val isTurnMonth = ins.monthIndex == kameti.payoutMonthIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (ins.paid) Emerald100 else Slate100)
                                            .clickable { onToggleInstallment(ins) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (isTurnMonth) {
                                                Icon(Icons.Default.EmojiEvents, contentDescription = "Turn", tint = Amber500, modifier = Modifier.size(10.dp))
                                            }
                                            Text(
                                                "M${ins.monthIndex}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (ins.paid) Emerald700 else Slate700
                                            )
                                            Icon(
                                                if (ins.paid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (ins.paid) Emerald600 else Slate500,
                                                modifier = Modifier.size(12.dp)
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
    }
}
