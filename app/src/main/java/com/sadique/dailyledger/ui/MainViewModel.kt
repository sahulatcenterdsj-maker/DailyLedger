package com.sadique.dailyledger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sadique.dailyledger.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.sadique.dailyledger.sync.SyncScheduler
import com.sadique.dailyledger.sync.BackupLock
import kotlinx.coroutines.sync.withLock
import com.sadique.dailyledger.auth.FirebaseRuntime
import kotlin.coroutines.cancellation.CancellationException

data class OfflineProfile(val ownerId: String, val records: Int)

class MainViewModel(app:Application,val ownerId:String):AndroidViewModel(app){
    val repo=LedgerRepository(AppDatabase.get(app),ownerId)
    private val dao = AppDatabase.get(app).ledgerDao()
    val offlineProfiles = dao.observeOfflineOwners().map { owners ->
        owners.map { OfflineProfile(it, dao.recordCount(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun importOffline(oldOwner: String) = launch {
        require(oldOwner.startsWith("offline-")) { "Only a previous offline profile can be imported here." }
        migrateOwner(oldOwner)
    }
    val transactions=repo.transactions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val loans=repo.loans.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val loanPayments=repo.loanPayments.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val committees=repo.committees.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val committeePayments=repo.committeePayments.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val committeeReceipts=repo.committeeReceipts.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val savings=repo.savings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }
    fun launch(block: suspend LedgerRepository.() -> Unit) = viewModelScope.launch {
        try {
            BackupLock.mutex.withLock {
                check(FirebaseRuntime.auth(getApplication<Application>()).currentUser?.uid == ownerId) {
                    "This account is no longer signed in. Please sign in again."
                }
                repo.block()
            }
            SyncScheduler.syncNow(getApplication<Application>())
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _error.value = e.message ?: "Your change could not be saved. Please try again." }
    }
    class Factory(private val app:Application,private val owner:String):ViewModelProvider.Factory{override fun <T:ViewModel> create(modelClass:Class<T>):T=MainViewModel(app,owner) as T}
}
