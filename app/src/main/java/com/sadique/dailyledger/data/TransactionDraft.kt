package com.sadique.dailyledger.data
import java.time.LocalDate

data class TransactionDraft(val type:String,val amountMinor:Long,val category:String,val note:String,val date:String) {
    fun validated():TransactionDraft {
        require(type in listOf("INCOME","EXPENSE")) { "Only income and expense entries are supported." }
        require(amountMinor in 1..1_000_000_000_000L) { "Enter a valid positive amount." }
        require(category.isNotBlank() && category.length <= 60) { "Category must be 1–60 characters." }
        require(note.length <= 500) { "Note must be no more than 500 characters." }
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)); LocalDate.parse(date)
        return copy(category=category.trim(),note=note.trim())
    }
}
