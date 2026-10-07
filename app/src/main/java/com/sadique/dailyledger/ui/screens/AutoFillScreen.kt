package com.sadique.dailyledger.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.ai.AiDraftResult
import com.sadique.dailyledger.ai.AiException
import com.sadique.dailyledger.ai.AiProtocol
import com.sadique.dailyledger.data.TransactionDraft
import com.sadique.dailyledger.data.TransactionEntity
import com.sadique.dailyledger.ui.money
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun AutoFillScreen(
    enabled: Boolean,
    onEnable: () -> Unit,
    onBack: () -> Unit,
    generate: suspend (String) -> AiDraftResult,
    save: suspend (List<TransactionDraft>, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var input by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(emptyList<TransactionDraft>()) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var batch by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var editing by remember { mutableStateOf<Int?>(null) }

    BackHandler { if (!busy) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Auto Fill") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding()
                .testTag("autofill-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    null,
                                    modifier = Modifier.padding(12.dp).size(26.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text("Write it once. Review every entry.", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Roman Urdu, Urdu or English. Income and expenses only; savings, loans and kameti use their own tabs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (!enabled) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enable cloud AI", style = MaterialTheme.typography.titleMedium)
                            Text("Auto Fill sends your entered text to your cloud AI service. Automatic suggestions send totals only. You can turn AI off any time in Settings.")
                            Button(onClick = onEnable) { Text("Enable AI") }
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.take(AiProtocol.MAX_INPUT) },
                            modifier = Modifier.fillMaxWidth().testTag("autofill-input"),
                            label = { Text("Describe your expenses or income") },
                            placeholder = { Text("aj doodh 150, sabzi 200, salary 60000") },
                            minLines = 4,
                            enabled = !busy && entries.isEmpty(),
                            supportingText = { Text("${input.length}/${AiProtocol.MAX_INPUT} • Internet required") },
                        )
                        Button(
                            onClick = {
                                focus.clearFocus(); busy = true; message = ""
                                scope.launch {
                                    try {
                                        val r = generate(input)
                                        entries = r.entries
                                        message = r.message
                                        batch = UUID.randomUUID().toString()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        message = (e as? AiException)?.message ?: "Could not prepare entries. Nothing was saved."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = enabled && !busy && input.isNotBlank() && entries.isEmpty(),
                            modifier = Modifier.fillMaxWidth().testTag("autofill-generate"),
                        ) {
                            Text(if (busy) "Please wait…" else "Prepare entries")
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (message.isNotBlank()) {
                            Text(
                                message,
                                modifier = Modifier.testTag("autofill-message"),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (entries.isNotEmpty()) {
                item {
                    Text("Review ${entries.size} entries", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Check amount, date and category before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(entries) { i, e ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(e.category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(e.type, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(money(e.amountMinor), style = MaterialTheme.typography.headlineSmall)
                        Text(e.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (e.note.isNotBlank()) Text(e.note)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { editing = i },
                                enabled = !busy,
                                modifier = Modifier.testTag("draft-edit-$i"),
                            ) {
                                Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Edit")
                            }
                            TextButton(onClick = { entries = entries.filterIndexed { j, _ -> i != j } }, enabled = !busy) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
            if (entries.isNotEmpty()) {
                item(key = "save-drafts") {
                    Button(
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    val count = entries.size
                                    save(entries, batch)
                                    entries = emptyList()
                                    input = ""
                                    message = "$count entries saved to your ledger."
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    message = if (e is IllegalArgumentException || e is IllegalStateException) {
                                        e.message ?: "Could not save entries."
                                    } else {
                                        "Could not save entries. Try again."
                                    }
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("autofill-save"),
                    ) { Text("Save reviewed entries") }
                    TextButton(onClick = { entries = emptyList(); message = "Drafts discarded." }, enabled = !busy) {
                        Text("Discard drafts")
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    editing?.let { i ->
        entries.getOrNull(i)?.let { d ->
            TransactionDialog(
                x = TransactionEntity("draft", "", d.type, d.amountMinor, d.category, d.note, d.date, 0, 0),
                dismiss = { editing = null },
                save = { t, a, c, n, date ->
                    entries = entries.mapIndexed { j, old -> if (i == j) TransactionDraft(t, a, c, n, date) else old }
                    editing = null
                },
            )
        }
    }
}
