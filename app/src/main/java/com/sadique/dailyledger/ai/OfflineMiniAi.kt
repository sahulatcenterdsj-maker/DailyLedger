package com.sadique.dailyledger.ai

import com.sadique.dailyledger.data.TransactionDraft
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale

/** Small offline rules engine. Ambiguous batches fall back as a whole; nothing is silently dropped. */
object OfflineMiniAi {
    private val amount = Regex("(?<![\\p{L}\\d.\\-])(\\d{1,9}(?:\\.\\d{1,2})?)\\s*(k|thousand|hazar|hazaar|ہزار|lakh|lac|لاکھ)?(?![\\p{L}\\d.])", RegexOption.IGNORE_CASE)
    private val groupedAmount = Regex("(?<![\\d.,])\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?(?![\\d.]|,\\d)")
    private val separators = Regex("[,;،\\n]+|\\s+(?:aur|and|phir|plus|اور)\\s+", RegexOption.IGNORE_CASE)
    private val dedicated = listOf("loan", "udhar", "qarz", "قرض", "kameti", "committee", "کمیٹی", "saving", "savings", "bachat", "بچت")
    private val incomeWords = listOf("salary", "tankhwa", "tankhwah", "tankha", "تنخواہ", "mili", "mila", "received", "income", "bonus", "profit", "aamdani", "آمدنی", "kamaya", "kamai", "refund")
    private val expenseWords = listOf("kharcha", "spent", "paid", "diya", "خرید", "خرچ")
    private val categories = linkedMapOf(
        "Salary" to listOf("salary", "tankhwa", "tankhwah", "tankha", "تنخواہ"),
        "Rent" to listOf("house rent", "kiraya makan", "makan ka kiraya", "rent"),
        "Milk" to listOf("doodh", "dodh", "milk", "دودھ"),
        "Vegetables" to listOf("sabzi", "vegetable", "vegetables", "سبزی"),
        "Fruit" to listOf("fruit", "phal", "پھل"),
        "Lentils" to listOf("daal", "dal", "lentils", "دال"),
        "Flour" to listOf("atta", "aata", "flour", "آٹا"),
        "Rice" to listOf("chawal", "rice", "چاول"),
        "Meat & chicken" to listOf("gosht", "chicken", "murghi", "گوشت"),
        "Eggs & bread" to listOf("anday", "eggs", "bread", "roti", "انڈے"),
        "Other groceries" to listOf("groceries", "grocery", "ration", "rashan", "راشن"),
        "Electricity" to listOf("bijli", "electricity", "electric", "بجلی"),
        "Gas" to listOf("gas bill", "sui gas", "گیس"),
        "Water" to listOf("water bill", "pani bill"),
        "Internet" to listOf("internet", "wifi"),
        "Mobile package" to listOf("mobile", "load", "package", "easyload"),
        "Fuel" to listOf("petrol", "diesel", "fuel", "cng", "پٹرول"),
        "Taxi & rides" to listOf("rickshaw", "taxi", "careem", "indrive"),
        "Public transport" to listOf("bus", "train", "transport", "fare", "kiraya"),
        "Medicines" to listOf("medicine", "dawai", "dawa", "medical", "دوائی"),
        "Doctor" to listOf("doctor", "clinic"),
        "Hospital" to listOf("hospital"),
        "School fees" to listOf("school", "fee", "fees", "اسکول"),
        "Books" to listOf("book", "books", "kitab"),
        "Tuition" to listOf("academy", "tuition"),
        "College & university" to listOf("college", "university"),
        "Tea & coffee" to listOf("tea", "chai", "coffee", "چائے"),
        "Snacks" to listOf("snack", "snacks", "papar", "chips", "samosa"),
        "Restaurant" to listOf("restaurant", "burger", "pizza", "biryani", "کھانا"),
        "Clothing" to listOf("clothes", "kapray", "dress", "garments"),
        "Shoes" to listOf("shoes", "jootay"),
        "Subscriptions" to listOf("subscription", "netflix"),
    )

    private fun has(text: String, word: String): Boolean = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(word)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE).containsMatchIn(text)
    fun usesDedicatedLedger(input: String): Boolean = dedicated.any { has(input, it) }

    fun drafts(input: String): AiDraftResult? {
        if (input.isBlank() || input.length > AiProtocol.MAX_INPUT || usesDedicatedLedger(input)) return null
        var clean = input.trim().map { c -> Character.digit(c, 10).takeIf { it >= 0 }?.let { ('0'.code + it).toChar() } ?: c }.joinToString("")
        clean = groupedAmount.replace(clean) { it.value.replace(",", "") }
        // Dates, negative amounts, decimal commas and past/future wording need an explicit review path.
        if (Regex("\\d,\\d|[-−]\\s*\\d|\\d/\\d").containsMatchIn(clean) || listOf("kal", "yesterday", "tomorrow", "کل", "total", "baqi", "balance").any { has(clean, it) }) return null
        val chunks = clean.split(separators).map(String::trim).filter(String::isNotBlank)
        val pieces = mutableListOf<String>()
        for (chunk in chunks) {
            val matches = amount.findAll(chunk).toList()
            if (matches.isEmpty()) return null
            if (matches.size == 1) pieces += chunk else {
                // Compact batches use label-amount order: "doodh 150 petrol 2k".
                var start = 0
                matches.forEachIndexed { index, match ->
                    val label = chunk.substring(start, match.range.first).trim()
                    if (label.isBlank()) return null
                    val end = if (index == matches.lastIndex) chunk.length else match.range.last + 1
                    pieces += chunk.substring(start, end)
                    start = match.range.last + 1
                }
            }
        }
        if (pieces.size !in 1..10) return null
        val rows = pieces.map { parseChunk(it) ?: return null }
        return AiDraftResult("Offline Mini AI • ${rows.size} entries mili hain. Har amount, category aur aaj ki date review karein.", rows)
    }

    private fun parseChunk(chunk: String): TransactionDraft? {
        val match = amount.find(chunk) ?: return null
        val minor = parseAmountMinor(match.groupValues[1], match.groupValues[2]) ?: return null
        val note = chunk.removeRange(match.range).trim(' ', '-', ':').take(120)
        if (note.isBlank()) return null
        val income = incomeWords.any { has(note, it) }
        if (income && expenseWords.any { has(note, it) }) return null
        val type = if (income) "INCOME" else "EXPENSE"
        val category = if (income) when {
            categories.getValue("Salary").any { has(note, it) } -> "Salary"
            has(note, "bonus") -> "Bonus"
            has(note, "profit") -> "Business income"
            has(note, "refund") -> "Refund"
            else -> "Other income"
        } else categories.entries.firstOrNull { (_, words) -> words.any { has(note, it) } }?.key ?: "Other"
        return runCatching { TransactionDraft(type, minor, category, note, LocalDate.now().toString()).validated() }.getOrNull()
    }

    private fun parseAmountMinor(number: String, suffix: String): Long? {
        val multiplier = when (suffix.lowercase(Locale.ROOT)) {
            "k", "thousand", "hazar", "hazaar", "ہزار" -> BigDecimal(1_000)
            "lakh", "lac", "لاکھ" -> BigDecimal(100_000)
            else -> BigDecimal.ONE
        }
        return runCatching { BigDecimal(number).multiply(multiplier).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact() }
            .getOrNull()?.takeIf { it in 1..1_000_000_000_000L }
    }
}
