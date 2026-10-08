package com.sadique.dailyledger.ai

import com.sadique.dailyledger.data.TransactionDraft
import java.time.LocalDate

/** Offline date/category extraction. Ambiguous batches are never partially saved. */
object OfflineMiniAi {
    private val incomeWords = listOf("salary", "tankhwa", "tankhwah", "tankha", "تنخواہ", "mili", "mila", "received", "income", "bonus", "profit", "aamdani", "آمدنی", "kamaya", "kamai", "refund")
    private val expenseWords = listOf("kharcha", "spent", "paid", "diya", "خرید", "خرچ")

    fun usesDedicatedLedger(input: String) = CategoryRules.isDedicatedLedgerText(input)

    fun drafts(input: String, today: LocalDate = LocalDate.now()): AiDraftResult? {
        if (input.isBlank() || input.length > AiProtocol.MAX_INPUT || usesDedicatedLedger(input)) return null
        val clean = EntryText.normalize(input)
        if (clean.none(Char::isDigit)) return null
        val dates = EntryDateParser.extract(clean, today)
        val masked = EntryDateParser.mask(clean, dates.map { it.range })
        if (listOf("total", "baqi", "balance").any { EntryText.has(masked, it) }) return null
        val pieces = EntryText.parts(masked) ?: return null
        val rows = pieces.map { part ->
            val income = incomeWords.any { EntryText.has(part.label, it) }
            if (income && expenseWords.any { EntryText.has(part.label, it) }) return null
            val type = if (income) "INCOME" else "EXPENSE"
            TransactionDraft(type, part.amountMinor, CategoryRules.infer(type, part.label), part.label.take(500),
                EntryDateParser.forPart(part.range, dates, today).toString()).validated()
        }
        val yearHint = if (dates.any { it.yearOmitted }) " Year omitted: ${today.year} used." else ""
        return AiDraftResult("Offline Mini AI • ${rows.size} entries. Amount, category aur har entry ki date review karein.$yearHint", rows)
    }
}
