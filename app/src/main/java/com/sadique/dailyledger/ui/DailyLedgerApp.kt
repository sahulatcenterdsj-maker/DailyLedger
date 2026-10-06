package com.sadique.dailyledger.ui

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadique.dailyledger.auth.UserProfile
import com.sadique.dailyledger.auth.AccountManager
import com.sadique.dailyledger.sync.SyncScheduler
import kotlin.coroutines.cancellation.CancellationException
import com.sadique.dailyledger.data.SettingsStore
import com.sadique.dailyledger.security.Biometrics
import com.sadique.dailyledger.ui.screens.CommitteeScreen
import com.sadique.dailyledger.ui.screens.DashboardScreen
import com.sadique.dailyledger.ui.screens.LoansScreen
import com.sadique.dailyledger.ui.screens.SavingsScreen
import com.sadique.dailyledger.ui.screens.SettingsScreen
import com.sadique.dailyledger.ui.screens.TransactionsScreen
import com.sadique.dailyledger.ui.screens.TransactionDialog
import kotlinx.coroutines.launch

private data class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLedgerApp(
    user: UserProfile,
    settings: SettingsStore,
    theme: String,
    biometric: Boolean,
    driveEnabled: Boolean,
    lastSync: Long,
    cloudEnabled: Boolean,
    lastCloudSync: Long,
    cloudStatus: String,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel(
        key = user.id,
        factory = MainViewModel.Factory(context.applicationContext as Application, user.id),
    )
    val tx by vm.transactions.collectAsState()
    val loans by vm.loans.collectAsState()
    val lp by vm.loanPayments.collectAsState()
    val committees by vm.committees.collectAsState()
    val cp by vm.committeePayments.collectAsState()
    val receipts by vm.committeeReceipts.collectAsState()
    val savings by vm.savings.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var settingsOpen by remember { mutableStateOf(false) }
    var quickTransaction by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val saveError by vm.error.collectAsState()
    LaunchedEffect(user.id) { SyncScheduler.syncNow(context, immediate = true) }
    LaunchedEffect(saveError) {
        saveError?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.clearError() }
    }

    val tabs = remember {
        listOf(
            Tab("Home", Icons.Outlined.Dashboard, Icons.Default.Dashboard),
            Tab("Transactions", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
            Tab("Loans", Icons.Outlined.Handshake, Icons.Default.Handshake),
            Tab("Kameti", Icons.Outlined.Groups, Icons.Default.Groups),
            Tab("Savings", Icons.Outlined.Savings, Icons.Default.Savings),
        )
    }

    BackHandler(enabled = settingsOpen) { settingsOpen = false }

    if (settingsOpen) {
        SettingsScreen(
            context = context,
            vm = vm,
            user = user,
            cloudEnabled = cloudEnabled,
            lastCloudSync = lastCloudSync,
            cloudStatus = cloudStatus,
            theme = theme,
            biometric = biometric,
            driveEnabled = driveEnabled,
            lastSync = lastSync,
            onBack = { settingsOpen = false },
            onTheme = { scope.launch { settings.setTheme(it) } },
            onBiometric = { enable ->
                if (enable && Biometrics.pick(context) == null) {
                    Toast.makeText(context, "Set up a screen lock or fingerprint in Android settings first", Toast.LENGTH_LONG).show()
                } else {
                    scope.launch { settings.setBiometric(enable) }
                }
            },
            onDriveEnabled = { scope.launch { settings.setDriveEnabled(user.id, it) } },
            onCloudEnabled = { enabled -> scope.launch {
                settings.setCloudEnabled(user.id, enabled)
                if (enabled) SyncScheduler.syncNow(context, immediate = true)
                else SyncScheduler.cancelImmediate(context)
            } },
            onLogout = { scope.launch {
                try { AccountManager(context).signOut(); onLogout() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { Toast.makeText(context, "Could not sign out. Try again.", Toast.LENGTH_LONG).show() }
            } },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Ledger") },
                actions = {
                    IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.selectedIcon else t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).consumeWindowInsets(pad)) {
            when (tab) {
                0 -> DashboardScreen(tx, loans, lp, committees, cp, receipts, savings,
                    onAddSalary = { quickTransaction = "INCOME" },
                    onAddExpense = { quickTransaction = "EXPENSE" },
                    onOpenSavings = { tab = 4 }, onOpenKameti = { tab = 3 })
                1 -> TransactionsScreen(
                    items = tx,
                    onSave = { t, a, c, n, d, e -> vm.launch { saveTransaction(t, a, c, n, d, e) } },
                    onDelete = { x -> vm.launch { deleteTransaction(x) } },
                )
                2 -> LoansScreen(
                    loans = loans,
                    payments = lp,
                    onAdd = { d, p, a, due, n -> vm.launch { saveLoan(d, p, a, due, n) } },
                    onPayment = { id, a, d, n -> vm.launch { addLoanPayment(id, a, d, n) } },
                    onDelete = { x -> vm.launch { deleteLoan(x) } },
                )
                3 -> CommitteeScreen(
                    committees = committees,
                    payments = cp,
                    receipts = receipts,
                    onAdd = { n, a, t, s, p, note, shares -> vm.launch { saveCommittee(n, a, t, s, p, note, shares) } },
                    onPaid = { c, i, m -> vm.launch { markCommitteePaid(c, i, m) } },
                    onReceive = { id, a, d, n -> vm.launch { addCommitteeReceipt(id, a, d, n) } },
                    onDeleteReceipt = { r -> vm.launch { deleteCommitteeReceipt(r) } },
                    onDelete = { c -> vm.launch { deleteCommittee(c) } },
                )
                else -> SavingsScreen(
                    savings = savings,
                    onAdd = { k, a, d, n -> vm.launch { saveSaving(k, a, d, n) } },
                    onDelete = { x -> vm.launch { deleteSaving(x) } },
                )
            }
        }
    }
    quickTransaction?.let { type ->
        TransactionDialog(null, dismiss = { quickTransaction = null },
            initialType = type, initialCategory = if (type == "INCOME") "Salary" else "",
            save = { t, a, c, n, d ->
                vm.launch { saveTransaction(t, a, c, n, d) }
                quickTransaction = null
            })
    }

}
