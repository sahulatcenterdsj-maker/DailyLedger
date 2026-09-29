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
import com.dailyledger.data.local.entity.LeftoverSweepEntity
import com.dailyledger.data.local.entity.SavingsGoalEntity
import com.dailyledger.data.model.Money
import com.dailyledger.ui.theme.*

@Composable
fun SavingsScreen(
    savingsGoals: List<SavingsGoalEntity>,
    leftovers: List<LeftoverSweepEntity>,
    onOpenAddGoal: () -> Unit,
    onOpenAddSweep: () -> Unit,
    onDeposit: (SavingsGoalEntity) -> Unit,
    onWithdraw: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    onDeleteSweep: (LeftoverSweepEntity) -> Unit
) {
    val totalDirect = savingsGoals.sumOf { it.currentPaisa }
    val totalLeftover = leftovers.sumOf { it.amountPaisa }
    val grandTotal = totalDirect + totalLeftover

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddGoal,
                containerColor = Teal600,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Modern Vault/Safe icon (no pig!)
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Teal600, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Savings & Reserves",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Dedicated target goals and unspent month-end leftover sweeps",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            }

            // Total KPI Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Teal50)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Teal600),
                            contentAlignment = Alignment.Center
                        ) {
                            // Vault / Safe icon
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("TOTAL COMBINED SAVINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal900)
                            Text(Money.formatPkr(grandTotal), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Teal900)
                            Text("Direct: ${Money.formatPkr(totalDirect)} • Sweeps: ${Money.formatPkr(totalLeftover)}", fontSize = 11.sp, color = Slate500)
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenAddGoal,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Goal", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenAddSweep,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Teal600, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sweep Surplus", fontSize = 12.sp)
                    }
                }
            }

            // Direct Goals Section
            item {
                Text("Direct Savings Goals (${savingsGoals.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            if (savingsGoals.isEmpty()) {
                item {
                    Text("No direct savings goals created yet.", color = Slate500, fontSize = 13.sp)
                }
            } else {
                items(savingsGoals, key = { it.id }) { goal ->
                    val progress = if (goal.targetPaisa > 0) {
                        (goal.currentPaisa.toFloat() / goal.targetPaisa.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(goal.category, fontSize = 11.sp, color = Slate500)
                                }
                                IconButton(onClick = { onDeleteGoal(goal) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate500)
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Emerald600,
                                trackColor = Slate100
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saved: ${Money.formatPkr(goal.currentPaisa)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Emerald600)
                                Text("Target: ${Money.formatPkr(goal.targetPaisa)}", fontSize = 12.sp, color = Slate500)
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onDeposit(goal) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Deposit", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { onWithdraw(goal) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Withdraw", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Leftover Sweeps Section
            item {
                Spacer(Modifier.height(8.dp))
                Text("Leftover Sweeps Reserve (${leftovers.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            if (leftovers.isEmpty()) {
                item {
                    Text("No leftover sweeps recorded. Tap 'Sweep Surplus' at month end.", color = Slate500, fontSize = 13.sp)
                }
            } else {
                items(leftovers, key = { it.id }) { sweep ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Teal50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Teal600, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Month: ${sweep.month}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(sweep.note ?: "Unspent month leftover sweep", fontSize = 11.sp, color = Slate500)
                            }
                            Text(Money.formatPkr(sweep.amountPaisa), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Teal600)
                            IconButton(onClick = { onDeleteSweep(sweep) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate500, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
