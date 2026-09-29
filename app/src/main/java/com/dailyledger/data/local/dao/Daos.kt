package com.dailyledger.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailyledger.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteAllByAccount(accountId: String)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE accountId = :accountId ORDER BY dueDate ASC")
    fun getLoansByAccount(accountId: String): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity)

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY date DESC")
    fun getRepaymentsForLoan(loanId: String): Flow<List<LoanRepaymentEntity>>
}

@Dao
interface KametiDao {
    @Query("SELECT * FROM kametis WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getKametisByAccount(accountId: String): Flow<List<KametiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKameti(kameti: KametiEntity)

    @Update
    suspend fun updateKameti(kameti: KametiEntity)

    @Delete
    suspend fun deleteKameti(kameti: KametiEntity)

    @Query("SELECT * FROM kameti_installments WHERE kametiId = :kametiId ORDER BY monthIndex ASC")
    fun getInstallmentsForKameti(kametiId: String): Flow<List<KametiInstallmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<KametiInstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: KametiInstallmentEntity)
}

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_goals WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getGoalsByAccount(accountId: String): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SavingsHistoryEntity)

    @Query("SELECT * FROM savings_history WHERE goalId = :goalId ORDER BY date DESC")
    fun getHistoryForGoal(goalId: String): Flow<List<SavingsHistoryEntity>>

    @Query("SELECT * FROM leftover_sweeps WHERE accountId = :accountId ORDER BY month DESC")
    fun getLeftoverSweeps(accountId: String): Flow<List<LeftoverSweepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeftoverSweep(sweep: LeftoverSweepEntity)

    @Delete
    suspend fun deleteLeftoverSweep(sweep: LeftoverSweepEntity)
}
