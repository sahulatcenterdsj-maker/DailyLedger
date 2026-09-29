package com.dailyledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyledger.data.local.entity.AccountEntity
import com.dailyledger.data.local.entity.TransactionEntity
import com.dailyledger.data.model.Money
import com.dailyledger.ui.navigation.Screen
import com.dailyledger.ui.theme.*

@Composable
fun DashboardScreen(
    activeAccount: AccountEntity?,
    transactions: List<TransactionEntity>,
    totalBalancePaisa: Long,
    monthIncomePaisa: Long,
    monthExpensePaisa: Long,
    loansGivenRemainingPaisa: Long,
    loansTakenRemainingPaisa: Long,
    kametiTotalPaidPaisa: Long,
    kametiExpectedPayoutPaisa: Long,
    directSavingsTotalPaisa: Long,
    leftoverSavingsTotalPaisa: Long,
    onNavigate: (Screen) -> Unit,
    onOpenAddTransaction: () -> Unit,
    onOpenAddLoan: () -> Unit,
    onOpenAddKameti: () -> Unit,
    onOpenAddSavings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Welcome Header
        item {
            Column {
                Text(
                    text = "Assalam-o-Alaikum!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ledger overview for ${activeAccount?.name ?: "Personal Ledger"}",
                    fontSize = 13.sp,
                    color = Slate500
                )
            }
        }

        // Quick Action Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenAddTransaction,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Entry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onOpenAddLoan,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Udhaar", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenAddKameti,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Blue500, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kameti", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenAddSavings,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Modern Vault/Safe icon, no pig!
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Teal600, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Savings", fontSize = 12.sp)
                }
            }
        }

        // Net Ledger Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Emerald700, Emerald600, Teal600)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET LEDGER BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald100,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = Money.formatPkr(totalBalancePaisa),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(14.dp))
                        Divider(color = Color.White.copy(alpha = 0.25f))
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cash in hand & Accounts",
                                fontSize = 11.sp,
                                color = Emerald100
                            )
                            Text(
                                text = "Integer Paisa Model",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Inflow & Outflow Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("This Month In", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "+${Money.formatPkr(monthIncomePaisa)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("This Month Out", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Rose600, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "-${Money.formatPkr(monthExpensePaisa)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose600
                        )
                    }
                }
            }
        }

        // Module Summary Row: Udhaar, Kameti, Savings (Vault)
        item {
            Text(
                text = "Financial Modules",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Udhaar snapshot
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.Loans) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Amber500)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Udhaar & Loans", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Owed to you: ${Money.formatPkr(loansGivenRemainingPaisa)} • You owe: ${Money.formatPkr(loansTakenRemainingPaisa)}",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500)
                    }
                }

                // Kameti snapshot
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.Kameti) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Blue500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Blue500)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kameti (ROSCA)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Invested: ${Money.formatPkr(kametiTotalPaidPaisa)} • Expected Pot: ${Money.formatPkr(kametiExpectedPayoutPaisa)}",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500)
                    }
                }

                // Savings snapshot - Modern Vault / Safe Icon (NO pig icon)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.Savings) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Teal500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Vault / Safe icon
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Teal600)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Savings & Reserves", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Goals: ${Money.formatPkr(directSavingsTotalPaisa)} • Sweeps: ${Money.formatPkr(leftoverSavingsTotalPaisa)}",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500)
                    }
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald600,
                    modifier = Modifier.clickable { onNavigate(Screen.Transactions) }
                )
            }
        }

        // Recent Transactions List
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "No transactions recorded yet. Tap '+ Add Entry' to record your first income or expense.",
                        fontSize = 13.sp,
                        color = Slate500,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                val isIncome = tx.type == "income"
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
                                .background(if (isIncome) Emerald100 else Rose500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncome) Emerald700 else Rose600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = tx.note ?: tx.category,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${tx.date} • ${tx.paymentMethod}",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                        Text(
                            text = "${if (isIncome) "+" else "-"}${Money.formatPkr(tx.amountPaisa)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isIncome) Emerald600 else Rose600
                        )
                    }
                }
            }
        }
    }
}
