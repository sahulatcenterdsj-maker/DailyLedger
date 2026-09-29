package com.dailyledger.data.repository

import com.dailyledger.data.local.AppDatabase
import com.dailyledger.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LedgerRepository(private val db: AppDatabase) {

    private val _activeAccountId = MutableStateFlow("default_offline")
    val activeAccountId = _activeAccountId.asStateFlow()

    fun setActiveAccount(id: String) {
        _activeAccountId.value = id
    }

    // Accounts
    fun getAllAccounts(): Flow<List<AccountEntity>> = db.accountDao().getAllAccounts()
    suspend fun createAccount(name: String, email: String? = null, type: String = "offline") {
        db.accountDao().insertAccount(AccountEntity(name = name, email = email, type = type))
    }
    suspend fun deleteAccount(account: AccountEntity) = db.accountDao().deleteAccount(account)

    // Transactions
    fun getTransactions(accountId: String): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsByAccount(accountId)

    suspend fun addTransaction(tx: TransactionEntity) = db.transactionDao().insertTransaction(tx)
    suspend fun updateTransaction(tx: TransactionEntity) = db.transactionDao().updateTransaction(tx)
    suspend fun deleteTransaction(tx: TransactionEntity) = db.transactionDao().deleteTransaction(tx)

    // Loans
    fun getLoans(accountId: String): Flow<List<LoanEntity>> =
        db.loanDao().getLoansByAccount(accountId)

    suspend fun addLoan(loan: LoanEntity) = db.loanDao().insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = db.loanDao().updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = db.loanDao().deleteLoan(loan)

    suspend fun recordLoanRepayment(loanId: String, amountPaisa: Long, note: String?) {
        db.loanDao().insertRepayment(
            LoanRepaymentEntity(
                loanId = loanId,
                amountPaisa = amountPaisa,
                date = java.time.LocalDate.now().toString(),
                note = note
            )
        )
    }

    // Kametis
    fun getKametis(accountId: String): Flow<List<KametiEntity>> =
        db.kametiDao().getKametisByAccount(accountId)

    suspend fun addKameti(kameti: KametiEntity, installments: List<KametiInstallmentEntity>) {
        db.kametiDao().insertKameti(kameti)
        db.kametiDao().insertInstallments(installments)
    }

    suspend fun updateKameti(kameti: KametiEntity) = db.kametiDao().updateKameti(kameti)
    suspend fun deleteKameti(kameti: KametiEntity) = db.kametiDao().deleteKameti(kameti)

    fun getInstallments(kametiId: String): Flow<List<KametiInstallmentEntity>> =
        db.kametiDao().getInstallmentsForKameti(kametiId)

    suspend fun toggleInstallment(installment: KametiInstallmentEntity) {
        val updated = installment.copy(
            paid = !installment.paid,
            paidDate = if (!installment.paid) java.time.LocalDate.now().toString() else null
        )
        db.kametiDao().updateInstallment(updated)
    }

    // Savings
    fun getSavingsGoals(accountId: String): Flow<List<SavingsGoalEntity>> =
        db.savingsDao().getGoalsByAccount(accountId)

    suspend fun addSavingsGoal(goal: SavingsGoalEntity) = db.savingsDao().insertGoal(goal)
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = db.savingsDao().updateGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = db.savingsDao().deleteGoal(goal)

    suspend fun recordSavingsDeposit(goal: SavingsGoalEntity, amountPaisa: Long, note: String?) {
        val updated = goal.copy(currentPaisa = goal.currentPaisa + amountPaisa)
        db.savingsDao().updateGoal(updated)
        db.savingsDao().insertHistory(
            SavingsHistoryEntity(
                goalId = goal.id,
                type = "deposit",
                amountPaisa = amountPaisa,
                date = java.time.LocalDate.now().toString(),
                note = note
            )
        )
    }

    suspend fun recordSavingsWithdraw(goal: SavingsGoalEntity, amountPaisa: Long, note: String?) {
        val newBalance = (goal.currentPaisa - amountPaisa).coerceAtLeast(0L)
        val updated = goal.copy(currentPaisa = newBalance)
        db.savingsDao().updateGoal(updated)
        db.savingsDao().insertHistory(
            SavingsHistoryEntity(
                goalId = goal.id,
                type = "withdraw",
                amountPaisa = amountPaisa,
                date = java.time.LocalDate.now().toString(),
                note = note
            )
        )
    }

    fun getLeftoverSweeps(accountId: String): Flow<List<LeftoverSweepEntity>> =
        db.savingsDao().getLeftoverSweeps(accountId)

    suspend fun addLeftoverSweep(sweep: LeftoverSweepEntity) =
        db.savingsDao().insertLeftoverSweep(sweep)

    suspend fun deleteLeftoverSweep(sweep: LeftoverSweepEntity) =
        db.savingsDao().deleteLeftoverSweep(sweep)
}
