package com.sadique.dailyledger.ui.screens

import com.sadique.dailyledger.ai.AiConsent
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.auth.UserProfile
import com.sadique.dailyledger.data.SettingsStore
import com.sadique.dailyledger.export.ExportManager
import com.sadique.dailyledger.security.SecureSecretStore
import com.sadique.dailyledger.sync.*
import com.sadique.dailyledger.ui.MainViewModel
import com.sadique.dailyledger.weather.WeatherService
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    context: Context,
    vm: MainViewModel,
    user: UserProfile,
    theme: String,
    biometric: Boolean,
    driveEnabled: Boolean,
    lastSync: Long,
    cloudEnabled: Boolean,
    lastCloudSync: Long,
    cloudStatus: String,
    weatherEnabled: Boolean,
    weatherCity: String,
    weatherTemperature: String,
    weatherCondition: String,
    weatherUpdatedAt: Long,
    transactionSounds: Boolean,
    onBack: () -> Unit,
    onTheme: (String) -> Unit,
    onBiometric: (Boolean) -> Unit,
    onDriveEnabled: (Boolean) -> Unit,
    onCloudEnabled: (Boolean) -> Unit,
    onWeatherEnabled: (Boolean) -> Unit,
    onTransactionSounds: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val aiEnabled by vm.aiEnabled.collectAsState()
    var aiConsent by remember { mutableStateOf(false) }
    if (aiConsent) {
        AlertDialog(
            onDismissRequest = { aiConsent = false },
            title = { Text(AiConsent.TITLE) },
            text = { Text("Automatic suggestions send category totals and comparisons to Firebase AI. Auto Fill sends the text you enter. Account details and transaction notes are not included in summaries. Turn AI off here at any time.") },
            confirmButton = { TextButton(onClick = { vm.setAiEnabled(true); aiConsent = false }) { Text("Enable AI") } },
            dismissButton = { TextButton(onClick = { aiConsent = false }) { Text("Cancel") } },
        )
    }
    val secrets = remember(user.id) { SecureSecretStore(context, user.id) }
    var passphrase by remember { mutableStateOf("") }
    var passphraseReady by remember(user.id) { mutableStateOf(secrets.hasPassphrase()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    val offlineProfiles by vm.offlineProfiles.collectAsState()
    var offlineImport by remember { mutableStateOf<String?>(null) }
    var pendingDriveAction by remember { mutableStateOf("UPLOAD") }
    var cityInput by remember(weatherCity) { mutableStateOf(weatherCity) }
    val settingsStore = remember { SettingsStore(context.applicationContext) }

    fun cloud(action: String) {
        if (busy) return
        busy = true; message = null
        scope.launch {
            try {
                val backup = CloudBackup(context, user.id)
                message = when (action) {
                    "RESTORE" -> backup.restore()
                    "REPLACE" -> backup.replaceWithLocal()
                    "DELETE" -> backup.deleteBackup()
                    else -> backup.sync()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = backupError(e)
                SettingsStore(context).cloudStatus(user.id, message.orEmpty())
            } finally {
                busy = false
            }
        }
    }
    suspend fun runDrive(token: String, action: String) {
        try {
            if (action == "RESTORE") {
                DriveSync(context, vm.ownerId).restore(token)
                SyncScheduler.syncNow(context, immediate = true)
                message = "Google Drive backup restored."
            } else {
                DriveSync(context, vm.ownerId).upload(token)
                message = "An encrypted copy was saved to Google Drive."
            }
            onDriveEnabled(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message = e.message ?: "Google Drive operation failed."
        } finally {
            busy = false
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val token = if (result.resultCode == Activity.RESULT_OK) DriveAuth.tokenFromResult(context, result.data) else null
        if (token != null) scope.launch { runDrive(token, pendingDriveAction) }
        else { busy = false; message = "Google Drive access was not granted. Your data is unchanged." }
    }
    fun drive(action: String) {
        val email = user.googleEmail ?: return
        if (busy) return
        busy = true; pendingDriveAction = action; message = null
        scope.launch {
            try {
                when (val result = DriveAuth.authorize(context.findActivity(), email)) {
                    is DriveAuthOutcome.Token -> runDrive(result.value, action)
                    is DriveAuthOutcome.Resolution -> launcher.launch(result.request)
                    is DriveAuthOutcome.Error -> { busy = false; message = result.message }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                busy = false; message = e.message ?: "Google Drive access failed."
            }
        }
    }

    offlineImport?.let { oldOwner ->
        AlertDialog(
            onDismissRequest = { offlineImport = null },
            title = { Text("Import old offline records?") },
            text = { Text("These records will be moved into ${user.email} on this phone and included in this account's future backups. Existing account records are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    offlineImport = null
                    vm.importOffline(oldOwner)
                }) { Text("Import records") }
            },
            dismissButton = { TextButton(onClick = { offlineImport = null }) { Text("Cancel") } },
        )
    }

    confirmation?.let { action ->
        val restore = action.endsWith("RESTORE")
        val delete = action == "CLOUD_DELETE"
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = {
                Text(
                    when {
                        delete -> "Delete cloud backup?"
                        restore -> "Restore backup?"
                        else -> "Replace existing backup?"
                    }
                )
            },
            text = {
                Text(
                    when {
                        delete -> "This deletes the current cloud backup. Older retained copies may remain under the storage retention policy. Records already on this phone are kept, and automatic backup will be turned off."
                        restore -> "This replaces this account's records on this phone with the selected backup. Export any changes you want to keep first."
                        else -> "This saves this phone's records over the existing backup for this account. Changes from another phone will be replaced."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    when (action) {
                        "CLOUD_RESTORE" -> cloud("RESTORE")
                        "CLOUD_REPLACE" -> cloud("REPLACE")
                        "CLOUD_DELETE" -> cloud("DELETE")
                        "DRIVE_RESTORE" -> drive("RESTORE")
                        "DRIVE_UPLOAD" -> drive("UPLOAD")
                    }
                }) { Text(if (delete) "Delete backup" else if (restore) "Restore" else "Save this phone's data") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        Text(user.name, style = MaterialTheme.typography.titleLarge)
        Text(user.email, style = MaterialTheme.typography.bodyMedium)

        if (offlineProfiles.isNotEmpty()) {
            SectionCard("Previous offline data", "Choose an old profile on this phone to add its records to this account.") {
                offlineProfiles.forEachIndexed { index, profile ->
                    OutlinedButton(onClick = { offlineImport = profile.ownerId }, enabled = !busy) {
                        Text("Import profile ${index + 1} (${profile.records} records)")
                    }
                }
            }
        }

        SectionCard("Account backup", "Saved to your signed-in account. Use the same login on a new phone to recover your records.") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic backup")
                    Text("Same-account recovery • no backup passphrase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(cloudEnabled, onCheckedChange = onCloudEnabled, enabled = !busy)
            }
            StatusBadge(if (cloudEnabled) "Automatic backup enabled" else "Automatic backup disabled", cloudEnabled)
            Text(cloudStatus, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (lastCloudSync > 0) "Last successful check: ${formatTime(lastCloudSync)}" else "No successful account backup yet.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Backups run after changes when internet is available. Android may delay background work.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { cloud("SYNC") }, enabled = !busy) { Text("Back up now") }
                OutlinedButton(onClick = { confirmation = "CLOUD_RESTORE" }, enabled = !busy) { Text("Restore") }
            }
            TextButton(onClick = { confirmation = "CLOUD_REPLACE" }, enabled = !busy) {
                Text("Replace backup with this phone's data")
            }
            TextButton(onClick = { confirmation = "CLOUD_DELETE" }, enabled = !busy) {
                Text("Delete cloud backup", color = MaterialTheme.colorScheme.error)
            }
        }

        if (user.googleEmail != null) {
            SectionCard("Google Drive copy", "Optional extra backup for ${user.googleEmail}.") {
                Text(
                    "This copy is encrypted with your backup passphrase. Keep it safe; you will need it to restore on another phone.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    passphrase,
                    { passphrase = it },
                    label = { Text("Drive backup passphrase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Button(onClick = {
                    try {
                        secrets.savePassphrase(passphrase)
                        passphrase = ""
                        passphraseReady = true
                        message = "Drive passphrase saved securely for this account."
                    } catch (_: Exception) {
                        message = "Could not save the passphrase. Please try again."
                    }
                }, enabled = passphrase.length >= 6 && !busy) { Text("Save passphrase") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirmation = "DRIVE_UPLOAD" }, enabled = passphraseReady && !busy) { Text("Save copy") }
                    OutlinedButton(onClick = { confirmation = "DRIVE_RESTORE" }, enabled = passphraseReady && !busy) { Text("Restore copy") }
                }
                Text(
                    if (driveEnabled && lastSync > 0) "Last Drive operation: ${formatTime(lastSync)}" else "No Drive copy completed on this phone yet.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionCard("Saving suggestions & Auto Fill", "Automatic insights and reviewed Auto Fill entries. Free usage limits apply. Basic spending insights work offline.") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Cloud AI", Modifier.weight(1f))
                Switch(aiEnabled, onCheckedChange = { if (it) aiConsent = true else vm.setAiEnabled(false) })
            }
        }

        SectionCard("Dashboard experience", "Selected-city weather and soft retro transaction feedback.") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Weather on Home")
                    Text("Manual city • no GPS permission", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(weatherEnabled, onCheckedChange = onWeatherEnabled)
            }
            if (weatherEnabled) {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("City") },
                    placeholder = { Text("Gujranwala, Pakistan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (weatherTemperature.isNotBlank() || weatherCondition.isNotBlank()) {
                    StatusBadge(
                        listOf(weatherCity, weatherTemperature, weatherCondition).filter { it.isNotBlank() }.joinToString(" • "),
                        true,
                    )
                    if (weatherUpdatedAt > 0L) {
                        Text("Updated ${formatTime(weatherUpdatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                settingsStore.setWeatherCity(cityInput)
                                val weather = WeatherService().currentForCity(cityInput)
                                settingsStore.setWeatherSnapshot(weather.city, weather.temperature, weather.condition, weather.fetchedAt)
                                cityInput = weather.city
                                message = "Weather updated for ${weather.city}."
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                message = e.message ?: "Could not update weather. Try again when you are online."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && cityInput.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save city & refresh weather") }
                Text("Weather data by Open-Meteo. The app refreshes cached weather periodically while online.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Transaction sounds")
                    Text("Sound style: Retro Soft", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(transactionSounds, onCheckedChange = onTransactionSounds)
            }
            Text("Income, expense and AI batch saves use short custom retro tones when this is enabled.", style = MaterialTheme.typography.bodySmall)
        }

        SectionCard("Appearance", null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("SYSTEM", "LIGHT", "DARK", "AQUA", "SUNSET", "FOREST", "ROSE", "MIDNIGHT").forEach {
                    FilterChip(
                        selected = theme == it,
                        onClick = { onTheme(it) },
                        label = { Text(it.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Biometric / device lock", Modifier.weight(1f))
                Switch(biometric, onCheckedChange = onBiometric)
            }
        }

        SectionCard("Export", null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy, onClick = {
                    scope.launch { message = "CSV: ${exportSafely { ExportManager.csv(context, vm.repo) }}" }
                }) { Text("Export CSV") }
                OutlinedButton(enabled = !busy, onClick = {
                    scope.launch { message = "PDF: ${exportSafely { ExportManager.pdf(context, vm.repo) }}" }
                }) { Text("Export PDF") }
            }
        }

        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        })
    }
}


@Composable
private fun StatusBadge(text: String, positive: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(value: Long) = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(value))
private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> error("No Activity found in context chain")
}
private suspend fun exportSafely(block: suspend () -> String): String = try {
    block(); "saved to Downloads"
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    "failed: ${e.message ?: "unknown error"}"
}
