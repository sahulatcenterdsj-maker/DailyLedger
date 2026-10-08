package com.sadique.dailyledger.data

import java.time.LocalDate

data class CreditDraft(
    val creditor: String,
    val item: String,
    val amountMinor: Long,
    val category: String,
    val purchaseDate: String,
    val dueDate: String? = null,
    val note: String = "",
    val phone: String = "",
    val whatsapp: String = "",
) {
    fun validated(): CreditDraft {
        require(amountMinor in 1..1_000_000_000_000L) { "Enter a valid udhar amount." }
        require(item.isNotBlank() && item.length <= 120) { "Item name is required." }
        require(category.isNotBlank() && category.length <= 60) { "Choose a category." }
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(purchaseDate))
        LocalDate.parse(purchaseDate)
        dueDate?.let { require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(it)); LocalDate.parse(it) }
        require(note.length <= 500)
        return copy(
            creditor = creditor.trim().ifBlank { "Unknown shop/person" }.take(80),
            item = item.trim().take(120),
            category = category.trim().take(60),
            note = note.trim().take(500),
            phone = phone.trim().take(40),
            whatsapp = whatsapp.trim().take(40),
        )
    }
}

internal fun CreditDraft.entity(id: String, owner: String, now: Long) = CreditPurchaseEntity(
    id, owner, creditor, item, amountMinor, category, purchaseDate, dueDate, note, now,
    phone = phone, whatsapp = whatsapp,
)
