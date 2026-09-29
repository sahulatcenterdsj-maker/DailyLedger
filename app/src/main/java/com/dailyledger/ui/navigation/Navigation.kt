package com.dailyledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val route: String, val title: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Default.Dashboard),
    Transactions("transactions", "Transactions", Icons.Default.ReceiptLong),
    Loans("loans", "Loans & Udhaar", Icons.Default.PriceCheck),
    Kameti("kameti", "Kameti", Icons.Default.Groups),
    Savings("savings", "Savings & Reserves", Icons.Default.AccountBalance), // Vault/Safe icon, no pig!
    Settings("settings", "Settings", Icons.Default.Settings)
}
