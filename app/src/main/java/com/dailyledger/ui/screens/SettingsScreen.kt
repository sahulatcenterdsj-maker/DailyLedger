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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyledger.data.local.entity.AccountEntity
import com.dailyledger.ui.theme.*

@Composable
fun SettingsScreen(
    accounts: List<AccountEntity>,
    activeAccount: AccountEntity?,
    onSwitchAccount: (AccountEntity) -> Unit,
    onCreateAccount: (String, String?) -> Unit,
    onExportCsv: () -> Unit
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var pinLockEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Backup & Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage profiles, encrypted backup, and privacy",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }
        }

        // Accounts section
        item {
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
                        Text("Ledger Accounts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        IconButton(onClick = { showAddAccountDialog = true }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Profile", tint = Emerald600)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { acc ->
                            val isActive = acc.id == activeAccount?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) Emerald50 else Color.Transparent)
                                    .clickable { onSwitchAccount(acc) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Emerald600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            acc.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(acc.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(acc.type.uppercase(), fontSize = 10.sp, color = Slate500)
                                    }
                                }
                                if (isActive) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Emerald600, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Drive Encrypted Sync
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Emerald600)
                        Spacer(Modifier.width(8.dp))
                        Text("Google Drive Encrypted Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Backup your Room database to Google Drive appDataFolder with client-side AES-GCM encryption. Zero cleartext financial data leaves your phone.",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { /* trigger Drive backup sync */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sync to Google Drive", fontSize = 12.sp)
                    }
                }
            }
        }

        // Export Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Export Ledger Records", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Export all transactions, loans, kametis, and savings to CSV spreadsheets on your device storage.",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export CSV Files", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("New Ledger Profile") },
            text = {
                OutlinedTextField(
                    value = newAccountName,
                    onValueChange = { newAccountName = it },
                    label = { Text("Profile Name (e.g. Business / Personal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccountName.isNotBlank()) {
                            onCreateAccount(newAccountName.trim(), null)
                            newAccountName = ""
                            showAddAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
