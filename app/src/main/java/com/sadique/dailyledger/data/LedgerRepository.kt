package com.sadique.dailyledger.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class LedgerRepository(private val db: AppDatabase, val ownerId: String) {
    private val dao = db.ledgerDao()
    val transactions = dao.observeTransactions(ownerId)
    val loans = dao.observeLoans(ownerId)
    val loanPayments = dao.observeLoanPayments(ownerId)
    val committees = dao.observeCommittees(ownerId)
    val committeePayments = dao.observeCommitteePayments(ownerId)
    val committeeReceipts = dao.observeCommitteeReceipts(ownerId)
    val savings = dao.observeSavings(ownerId)

    suspend fun saveTransaction(type: String, amountMinor: Long, category: String, note: String, date: String, existing: TransactionEntity? = null) {
        val now = System.currentTimeMillis()
        dao.upsertTransaction(TransactionEntity(existing?.id ?: newId(), ownerId, type, amountMinor, category.trim(), note.trim(), date, existing?.createdAt ?: now, now))
    }
    suspend fun deleteTransaction(item: TransactionEntity) = dao.deleteTransaction(item)

    suspend fun saveLoan(direction: String, person: String, principalMinor: Long, dueDate: String?, note: String) {
        dao.upsertLoan(LoanEntity(newId(), ownerId, direction, person.trim(), principalMinor, dueDate, note.trim(), System.currentTimeMillis()))
    }
    suspend fun deleteLoan(item: LoanEntity) = db.withTransaction { dao.deleteLoanPayments(ownerId, item.id); dao.deleteLoan(item) }
    suspend fun addLoanPayment(loanId: String, amountMinor: Long, date: String, note: String) {
        dao.upsertLoanPayment(LoanPaymentEntity(newId(), ownerId, loanId, amountMinor, date, note.trim(), System.currentTimeMillis()))
    }

    suspend fun saveCommittee(name: String, monthlyMinor: Long, total: Int, startMonth: String, payout: Int?, note: String, shares: Int = 1) {
        require(monthlyMinor > 0 && total > 0 && shares > 0) { "Enter a valid monthly amount, number of kametis and installments." }
        Math.multiplyExact(Math.multiplyExact(monthlyMinor, shares.toLong()), total.toLong())
        require(payout == null || payout in 1..total) { "Payout installment must be within the total installments." }
        YearMonth.parse(startMonth)
        dao.upsertCommittee(CommitteeEntity(newId(), ownerId, name.trim(), monthlyMinor, total, startMonth, payout, false, true, note.trim(), System.currentTimeMillis(), shares))
    }
    suspend fun markCommitteePaid(committee: CommitteeEntity, installment: Int, month: String) = db.withTransaction {
        val current = requireNotNull(dao.committeeNow(ownerId, committee.id)) { "This kameti no longer exists." }
        require(installment in 1..current.totalInstallments) { "Invalid installment number." }
        YearMonth.parse(month)
        require(dao.committeePaymentsNow(ownerId).none { it.committeeId == current.id && it.installmentNumber == installment }) { "This installment is already paid." }
        dao.upsertCommitteePayment(CommitteePaymentEntity(newId(), ownerId, current.id, installment, month, current.monthlyContribution(), System.currentTimeMillis()))
    }
    suspend fun addCommitteeReceipt(committeeId: String, amountMinor: Long, date: String, note: String) = db.withTransaction {
        val committee = requireNotNull(dao.committeeNow(ownerId, committeeId)) { "This kameti no longer exists." }
        val balance = committeeBalance(committee, dao.committeeReceiptsNow(ownerId))
        require(amountMinor > 0L && amountMinor <= balance.remaining) { "Received amount must be greater than zero and no more than the remaining payout." }
        LocalDate.parse(date)
        dao.upsertCommitteeReceipt(CommitteeReceiptEntity(newId(), ownerId, committeeId, amountMinor, date, note.trim(), System.currentTimeMillis()))
        dao.upsertCommittee(committee.copy(received = balance.received + amountMinor >= balance.expected))
    }
    suspend fun deleteCommitteeReceipt(receipt: CommitteeReceiptEntity) = db.withTransaction {
        require(receipt.ownerId == ownerId) { "This receipt belongs to another account." }
        dao.deleteCommitteeReceipt(receipt)
        dao.committeeNow(ownerId, receipt.committeeId)?.let { committee ->
            dao.upsertCommittee(committee.copy(received = committeeBalance(committee, dao.committeeReceiptsNow(ownerId)).complete))
        }
    }
    suspend fun deleteCommittee(item: CommitteeEntity) = db.withTransaction {
        require(item.ownerId == ownerId)
        dao.deleteCommitteePayments(ownerId, item.id)
        dao.deleteCommitteeReceipts(ownerId, item.id)
        dao.deleteCommittee(item)
    }

    suspend fun saveSaving(kind: String, amountMinor: Long, date: String, note: String) {
        dao.upsertSaving(SavingEntity(newId(), ownerId, kind, amountMinor, date, note.trim(), System.currentTimeMillis()))
    }
    suspend fun deleteSaving(item: SavingEntity) = dao.deleteSaving(item)

    private fun newId() = "$ownerId:${UUID.randomUUID()}"
    private fun ownedId(id: String) = "$ownerId:${id.substringAfterLast(':')}"

    suspend fun hasRecords(): Boolean = dao.transactionsNow(ownerId).isNotEmpty() ||
        dao.loansNow(ownerId).isNotEmpty() || dao.loanPaymentsNow(ownerId).isNotEmpty() ||
        dao.committeesNow(ownerId).isNotEmpty() || dao.committeePaymentsNow(ownerId).isNotEmpty() ||
        dao.savingsNow(ownerId).isNotEmpty() || dao.committeeReceiptsNow(ownerId).isNotEmpty()

    suspend fun migrateOwner(oldOwner: String) = db.withTransaction {
        if (oldOwner != ownerId) {
            dao.migrateTransactions(oldOwner, ownerId); dao.migrateLoans(oldOwner, ownerId)
            dao.migrateLoanPayments(oldOwner, ownerId); dao.migrateCommittees(oldOwner, ownerId)
            dao.migrateCommitteePayments(oldOwner, ownerId); dao.migrateSavings(oldOwner, ownerId)
            dao.migrateCommitteeReceipts(oldOwner, ownerId)
        }
    }

    suspend fun exportJson(): String = db.withTransaction {
        fun JSONObject.putNullable(key: String, value: Any?) = apply { if (value == null) put(key, JSONObject.NULL) else put(key, value) }
        val root = JSONObject().put("version", 2).put("ownerId", ownerId).put("exportedAt", System.currentTimeMillis())
        root.put("transactions", JSONArray().apply { dao.transactionsNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("type",x.type).put("amountMinor",x.amountMinor).put("category",x.category).put("note",x.note).put("date",x.date).put("createdAt",x.createdAt).put("updatedAt",x.updatedAt)) } })
        root.put("loans", JSONArray().apply { dao.loansNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("direction",x.direction).put("person",x.person).put("principalMinor",x.principalMinor).putNullable("dueDate",x.dueDate).put("note",x.note).put("createdAt",x.createdAt).put("closed",x.closed)) } })
        root.put("loanPayments", JSONArray().apply { dao.loanPaymentsNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("loanId",ownedId(x.loanId)).put("amountMinor",x.amountMinor).put("date",x.date).put("note",x.note).put("createdAt",x.createdAt)) } })
        root.put("committees", JSONArray().apply { dao.committeesNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("name",x.name).put("monthlyAmountMinor",x.monthlyAmountMinor).put("totalInstallments",x.totalInstallments).put("startMonth",x.startMonth).putNullable("payoutInstallment",x.payoutInstallment).put("received",x.received).put("shares",x.shares).put("active",x.active).put("note",x.note).put("createdAt",x.createdAt)) } })
        root.put("committeePayments", JSONArray().apply { dao.committeePaymentsNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("committeeId",ownedId(x.committeeId)).put("installmentNumber",x.installmentNumber).put("month",x.month).put("amountMinor",x.amountMinor).put("paidAt",x.paidAt)) } })
        root.put("savings", JSONArray().apply { dao.savingsNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("kind",x.kind).put("amountMinor",x.amountMinor).put("date",x.date).put("note",x.note).put("createdAt",x.createdAt)) } })
        root.put("committeeReceipts", JSONArray().apply { dao.committeeReceiptsNow(ownerId).sortedBy { ownedId(it.id) }.forEach { x -> put(JSONObject().put("id",ownedId(x.id)).put("committeeId",ownedId(x.committeeId)).put("amountMinor",x.amountMinor).putNullable("date",x.date).put("note",x.note).put("createdAt",x.createdAt)) } })
        root.toString()
    }

    suspend fun importJson(json: String) = db.withTransaction {
        val root = JSONObject(json)
        val version = root.optInt("version")
        require(version in 1..2) { "Unsupported backup version. Update the app to restore it." }
        require(version == 1 || root.optJSONArray("committeeReceipts") != null) { "Backup is missing kameti receipts; your local data was not changed." }
        listOf("transactions", "loans", "loanPayments", "committees", "committeePayments", "savings").forEach {
            require(root.optJSONArray(it) != null) { "Backup is incomplete; your local data was not changed." }
        }
        fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
        val tx = root.optJSONArray("transactions") ?: JSONArray()
        val loans = root.optJSONArray("loans") ?: JSONArray()
        val lp = root.optJSONArray("loanPayments") ?: JSONArray()
        val committees = root.optJSONArray("committees") ?: JSONArray()
        val cp = root.optJSONArray("committeePayments") ?: JSONArray()
        val savings = root.optJSONArray("savings") ?: JSONArray()
        dao.clearCommitteeReceipts(ownerId)
        dao.clearLoanPayments(ownerId); dao.clearCommitteePayments(ownerId); dao.clearTransactions(ownerId); dao.clearLoans(ownerId); dao.clearCommittees(ownerId); dao.clearSavings(ownerId)
        dao.insertTransactions(tx.objects().map { o -> TransactionEntity(ownedId(o.getString("id")),ownerId,o.getString("type"),o.getLong("amountMinor"),o.optString("category"),o.optString("note"),o.getString("date"),o.getLong("createdAt"),o.optLong("updatedAt",o.getLong("createdAt"))) })
        dao.insertLoans(loans.objects().map { o -> LoanEntity(ownedId(o.getString("id")),ownerId,o.getString("direction"),o.optString("person"),o.getLong("principalMinor"),o.optString("dueDate").takeIf{it.isNotBlank()&&it!="null"},o.optString("note"),o.getLong("createdAt"),o.optBoolean("closed")) })
        dao.insertLoanPayments(lp.objects().map { o -> LoanPaymentEntity(ownedId(o.getString("id")),ownerId,ownedId(o.getString("loanId")),o.getLong("amountMinor"),o.getString("date"),o.optString("note"),o.getLong("createdAt")) })
        dao.insertCommittees(committees.objects().map { o -> CommitteeEntity(ownedId(o.getString("id")),ownerId,o.optString("name"),o.getLong("monthlyAmountMinor"),o.getInt("totalInstallments"),o.getString("startMonth"),if(o.isNull("payoutInstallment")) null else o.getInt("payoutInstallment"),o.optBoolean("received"),o.optBoolean("active",true),o.optString("note"),o.getLong("createdAt"),o.optInt("shares",1)) })
        dao.insertCommitteePayments(cp.objects().map { o -> CommitteePaymentEntity(ownedId(o.getString("id")),ownerId,ownedId(o.getString("committeeId")),o.getInt("installmentNumber"),o.getString("month"),o.getLong("amountMinor"),o.getLong("paidAt")) })
        dao.insertSavings(savings.objects().map { o -> SavingEntity(ownedId(o.getString("id")),ownerId,o.getString("kind"),o.getLong("amountMinor"),o.getString("date"),o.optString("note"),o.getLong("createdAt")) })
        val savedCommittees = dao.committeesNow(ownerId)
        savedCommittees.forEach { require(it.shares > 0 && it.totalInstallments > 0 && it.monthlyAmountMinor > 0); it.expectedPayout() }
        val receipts = if (version == 1) {
            savedCommittees.filter { it.received }.map { CommitteeReceiptEntity(it.id + "-payout", ownerId, it.id, it.expectedPayout(), null, "Earlier received status", it.createdAt) }
        } else {
            root.getJSONArray("committeeReceipts").objects().map { o ->
                CommitteeReceiptEntity(ownedId(o.getString("id")), ownerId, ownedId(o.getString("committeeId")), o.getLong("amountMinor"), o.optString("date").takeIf { it.isNotBlank() && it != "null" }, o.optString("note"), o.getLong("createdAt"))
            }
        }
        require(receipts.map { it.id }.distinct().size == receipts.size) { "Duplicate kameti receipts in backup." }
        receipts.forEach { receipt ->
            require(receipt.amountMinor > 0 && savedCommittees.any { it.id == receipt.committeeId }) { "Invalid kameti receipt in backup." }
            receipt.date?.let { LocalDate.parse(it) }
        }
        dao.insertCommitteeReceipts(receipts)
        savedCommittees.forEach { committee ->
            val balance = committeeBalance(committee, receipts)
            require(balance.received <= balance.expected) { "Kameti receipts exceed the expected payout." }
            dao.upsertCommittee(committee.copy(received = balance.complete))
        }
    }

    companion object {
        fun today() = LocalDate.now().toString()
        fun thisMonth() = YearMonth.now().toString()
    }
}
