package com.sadique.dailyledger.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sadique.dailyledger.ai.*
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.money
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AutoFillScreen(enabled:Boolean,onEnable:()->Unit,onBack:()->Unit,generate:suspend(String)->AiDraftResult,save:suspend(List<TransactionDraft>,String)->Unit){
    val scope=rememberCoroutineScope();val focus=LocalFocusManager.current
    var input by remember{mutableStateOf("")};var entries by remember{mutableStateOf(emptyList<TransactionDraft>())}
    var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var batch by remember{mutableStateOf(UUID.randomUUID().toString())};var editing by remember{mutableStateOf<Int?>(null)}
    BackHandler{if(!busy)onBack()}
    Scaffold(topBar={TopAppBar(title={Text("AI Auto Fill")},navigationIcon={IconButton(onClick=onBack,enabled=!busy){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}})}){pad->
        LazyColumn(Modifier.fillMaxSize().padding(pad).imePadding(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item{Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(30.dp),tint=MaterialTheme.colorScheme.primary);Text("Write it once. Review every entry.",style=MaterialTheme.typography.titleLarge);Text("Roman Urdu, Urdu or English. Income and expenses only; savings, loans and kameti use their own tabs.")}
            if(!enabled)item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Enable cloud AI",style=MaterialTheme.typography.titleMedium);Text("Auto Fill sends your entered text to Groq through our service. Automatic suggestions send category totals, without account details or transaction notes. Turn AI off any time in Settings.");Button(onClick=onEnable){Text("Enable AI")}}}}
            item{
                OutlinedTextField(input,{input=it.take(AiProtocol.MAX_INPUT)},Modifier.fillMaxWidth().testTag("autofill-input"),label={Text("Describe your expenses or income")},placeholder={Text("Aaj doodh 300, petrol 2000 aur salary 60000 mili")},minLines=3,enabled=!busy&&entries.isEmpty(),supportingText={Text("${input.length}/${AiProtocol.MAX_INPUT} • Internet required")})
                Button(onClick={focus.clearFocus();busy=true;message="";scope.launch{try{val r=generate(input);entries=r.entries;message=r.message;batch=UUID.randomUUID().toString()}catch(e:CancellationException){throw e}catch(e:Exception){message=(e as? AiException)?.message?:"Could not prepare entries. Nothing was saved."}finally{busy=false}}},enabled=enabled&&!busy&&input.isNotBlank()&&entries.isEmpty(),modifier=Modifier.testTag("autofill-generate")){Text(if(busy)"Please wait…" else "Prepare entries")}
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth());if(message.isNotBlank())Text(message,Modifier.padding(top=8.dp).testTag("autofill-message"),color=MaterialTheme.colorScheme.primary)
            }
            if(entries.isNotEmpty())item{Text("Review ${entries.size} entries",style=MaterialTheme.typography.titleLarge);Text("Check the amount, date and category. Nothing is saved yet.")}
            itemsIndexed(entries){i,e->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(e.category,style=MaterialTheme.typography.titleMedium);Text("${e.type} • ${money(e.amountMinor)}",style=MaterialTheme.typography.titleLarge);Text(e.date);if(e.note.isNotBlank())Text(e.note);Row{TextButton(onClick={editing=i},enabled=!busy,modifier=Modifier.testTag("draft-edit-$i")){Text("Edit")};TextButton(onClick={entries=entries.filterIndexed{j,_->i!=j}},enabled=!busy){Text("Remove")}}}}}
            if(entries.isNotEmpty())item{
                Button(onClick={busy=true;scope.launch{try{val count=entries.size;save(entries,batch);entries=emptyList();input="";message="$count entries saved to your ledger."}catch(e:CancellationException){throw e}catch(e:Exception){message=if(e is IllegalArgumentException||e is IllegalStateException)e.message?:"Could not save entries." else "Could not save entries. Try again."}finally{busy=false}}},enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("autofill-save")){Text("Save reviewed entries")}
                TextButton(onClick={entries=emptyList();message="Drafts discarded."},enabled=!busy){Text("Discard drafts")}
            }
        }
    }
    editing?.let{i->entries.getOrNull(i)?.let{d->TransactionDialog(TransactionEntity("draft","",d.type,d.amountMinor,d.category,d.note,d.date,0,0),{editing=null},save={t,a,c,n,date->entries=entries.mapIndexed{j,old->if(i==j)TransactionDraft(t,a,c,n,date)else old};editing=null})}}
}
