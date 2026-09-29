package com.dailyledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.dailyledger.data.local.entity.*
import com.dailyledger.data.model.Money
import com.dailyledger.ui.components.*
import com.dailyledger.ui.navigation.Screen
import com.dailyledger.ui.screens.*
import com.dailyledger.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DailyLedgerApp
        val repo = app.repository

        setContent {
            DailyLedgerTheme {
                var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

                val coroutineScope = rememberCoroutineScope()
                val accounts by repo.getAllAccounts().collectAsState(initial = emptyList())
                val activeAccountId by repo.activeAccountId.collectAsState()
                val activeAccount = accounts.find { it.id == activeAccountId } ?: accounts.firstOrNull()

                val currentAccountId = activeAccount?.id ?: "default_offline"

                val transactions by repo.getTransactions(currentAccountId).collectAsState(initial = emptyList())
                val loans by repo.getLoans(currentAccountId).collectAsState(initial = emptyList())
                val kametis by repo.getKametis(currentAccountId).collectAsState(initial = emptyList())
                val savingsGoals by repo.getSavingsGoals(currentAccountId).collectAsState(initial = emptyList())
                val leftovers by repo.getLeftoverSweeps(currentAccountId).collectAsState(initial = emptyList())

                // Calculated stats
                val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amountPaisa }
                val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amountPaisa }
                val netBalancePaisa = totalIncome - totalExpense

                val currentMonthStr = remember { LocalDate.now().toString().take(7) }
                val monthIncome = transactions.filter { it.type == "income" && it.date.startsWith(currentMonthStr) }
                    .sumOf { it.amountPaisa }
                val monthExpense = transactions.filter { it.type == "expense" && it.date.startsWith(currentMonthStr) }
                    .sumOf { it.amountPaisa }

                val loansGiven = loans.filter { it.type == "given" && it.status == "active" }
                    .sumOf { it.principalPaisa - it.repaidPaisa }
                val loansTaken = loans.filter { it.type == "taken" && it.status == "active" }
                    .sumOf { it.principalPaisa - it.repaidPaisa }

                val directSavings = savingsGoals.sumOf { it.currentPaisa }
                val leftoverSavings = leftovers.sumOf { it.amountPaisa }

                // Dialog states
                var showAddTxDialog by remember { mutableStateOf(false) }
                var showAddLoanDialog by remember { mutableStateOf(false) }
                var showAddKametiDialog by remember { mutableStateOf(false) }
                var showAddSavingsGoalDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Emerald600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Daily Ledger",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Emerald100)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    "PKR",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Emerald900
                                                )
                                            }
                                        }
                                        Text(
                                            activeAccount?.name ?: "Personal Ledger",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            Screen.values().forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            screen.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Emerald700,
                                        selectedTextColor = Emerald700,
                                        indicatorColor = Emerald100
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                activeAccount = activeAccount,
                                transactions = transactions,
                                totalBalancePaisa = netBalancePaisa,
                                monthIncomePaisa = monthIncome,
                                monthExpensePaisa = monthExpense,
                                loansGivenRemainingPaisa = loansGiven,
                                loansTakenRemainingPaisa = loansTaken,
                                kametiTotalPaidPaisa = 0L,
                                kametiExpectedPayoutPaisa = 0L,
                                directSavingsTotalPaisa = directSavings,
                                leftoverSavingsTotalPaisa = leftoverSavings,
                                onNavigate = { currentScreen = it },
                                onOpenAddTransaction = { showAddTxDialog = true },
                                onOpenAddLoan = { showAddLoanDialog = true },
                                onOpenAddKameti = { showAddKametiDialog = true },
                                onOpenAddSavings = { showAddSavingsGoalDialog = true }
                            )

                            Screen.Transactions -> TransactionsScreen(
                                transactions = transactions,
                                onOpenAdd = { showAddTxDialog = true },
                                onDelete = { tx ->
                                    coroutineScope.launch { repo.deleteTransaction(tx) }
                                }
                            )

                            Screen.Loans -> LoansScreen(
                                loans = loans,
                                onOpenAdd = { showAddLoanDialog = true },
                                onRepayment = { loan ->
                                    coroutineScope.launch {
                                        repo.recordLoanRepayment(loan.id, loan.principalPaisa - loan.repaidPaisa, "Full Repayment")
                                        repo.updateLoan(loan.copy(repaidPaisa = loan.principalPaisa, status = "cleared"))
                                    }
                                },
                                onDelete = { loan ->
                                    coroutineScope.launch { repo.deleteLoan(loan) }
                                }
                            )

                            Screen.Kameti -> KametiScreen(
                                kametis = kametis,
                                installmentsMap = emptyMap(),
                                onOpenAdd = { showAddKametiDialog = true },
                                onToggleInstallment = { ins ->
                                    coroutineScope.launch { repo.toggleInstallment(ins) }
                                },
                                onDelete = { k ->
                                    coroutineScope.launch { repo.deleteKameti(k) }
                                }
                            )

                            Screen.Savings -> SavingsScreen(
                                savingsGoals = savingsGoals,
                                leftovers = leftovers,
                                onOpenAddGoal = { showAddSavingsGoalDialog = true },
                                onOpenAddSweep = {
                                    coroutineScope.launch {
                                        repo.addLeftoverSweep(
                                            LeftoverSweepEntity(
                                                accountId = currentAccountId,
                                                month = currentMonthStr,
                                                amountPaisa = 1000000L, // 10k default or custom
                                                date = LocalDate.now().toString()
                                            )
                                        )
                                    }
                                },
                                onDeposit = { goal ->
                                    coroutineScope.launch {
                                        repo.recordSavingsDeposit(goal, 500000L, "Deposit")
                                    }
                                },
                                onWithdraw = { goal ->
                                    coroutineScope.launch {
                                        repo.recordSavingsWithdraw(goal, 500000L, "Withdraw")
                                    }
                                },
                                onDeleteGoal = { goal ->
                                    coroutineScope.launch { repo.deleteSavingsGoal(goal) }
                                },
                                onDeleteSweep = { sweep ->
                                    coroutineScope.launch { repo.deleteLeftoverSweep(sweep) }
                                }
                            )

                            Screen.Settings -> SettingsScreen(
                                accounts = accounts,
                                activeAccount = activeAccount,
                                onSwitchAccount = { acc -> repo.setActiveAccount(acc.id) },
                                onCreateAccount = { name, email ->
                                    coroutineScope.launch { repo.createAccount(name, email) }
                                },
                                onExportCsv = { /* CSV exporter */ }
                            )
                        }
                    }
                }

                // Add Transaction Dialog
                AddTransactionDialog(
                    isOpen = showAddTxDialog,
                    onDismiss = { showAddTxDialog = false },
                    onSubmit = { type, amountPaisa, category, note ->
                        coroutineScope.launch {
                            repo.addTransaction(
                                TransactionEntity(
                                    accountId = currentAccountId,
                                    type = type,
                                    amountPaisa = amountPaisa,
                                    category = category,
                                    date = LocalDate.now().toString(),
                                    note = note
                                )
                            )
                        }
                    }
                )

                // Add Loan Dialog
                AddLoanDialog(
                    isOpen = showAddLoanDialog,
                    onDismiss = { showAddLoanDialog = false },
                    onSubmit = { type, name, amountPaisa, dueDate ->
                        coroutineScope.launch {
                            repo.addLoan(
                                LoanEntity(
                                    accountId = currentAccountId,
                                    type = type,
                                    counterpartyName = name,
                                    principalPaisa = amountPaisa,
                                    startDate = LocalDate.now().toString(),
                                    dueDate = dueDate
                                )
                            )
                        }
                    }
                )

                // Add Kameti Dialog
                AddKametiDialog(
                    isOpen = showAddKametiDialog,
                    onDismiss = { showAddKametiDialog = false },
                    onSubmit = { title, organizer, members, installmentPaisa, turnMonth ->
                        coroutineScope.launch {
                            val kametiId = java.util.UUID.randomUUID().toString()
                            val kameti = KametiEntity(
                                id = kametiId,
                                accountId = currentAccountId,
                                title = title,
                                organizerName = organizer,
                                totalMembers = members,
                                monthlyInstallmentPaisa = installmentPaisa,
                                totalPoolPaisa = installmentPaisa * members,
                                payoutMonthIndex = turnMonth,
                                startDate = currentMonthStr
                            )
                            val installments = (1..members).map { m ->
                                KametiInstallmentEntity(
                                    kametiId = kametiId,
                                    monthIndex = m,
                                    paid = false
                                )
                            }
                            repo.addKameti(kameti, installments)
                        }
                    }
                )

                // Add Savings Goal Dialog
                AddSavingsGoalDialog(
                    isOpen = showAddSavingsGoalDialog,
                    onDismiss = { showAddSavingsGoalDialog = false },
                    onSubmit = { title, category, targetPaisa ->
                        coroutineScope.launch {
                            repo.addSavingsGoal(
                                SavingsGoalEntity(
                                    accountId = currentAccountId,
                                    title = title,
                                    category = category,
                                    targetPaisa = targetPaisa
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
