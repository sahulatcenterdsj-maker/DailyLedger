package com.sadique.dailyledger.ai

import com.sadique.dailyledger.data.TransactionDraft
import java.util.Locale

/**
 * Canonical AI category rules for Daily Ledger.
 *
 * AI-created entries should use the app's real categories instead of inventing
 * labels such as "Other groceries" or "Other kameti". "Other" is deliberately
 * the last fallback after trying the known category vocabulary.
 */
object CategoryRules {
    private data class Rule(val category: String, val aliases: List<String>)

    private val dedicatedLedgerAliases = listOf(
        "loan", "udhar", "qarz", "قرض",
        "kameti", "committee", "کمیٹی",
        "saving", "savings", "bachat", "بچت",
    )

    // Order matters: specific categories are checked before broader ones.
    private val expenseRules = listOf(
        Rule("School transport", listOf("school van", "school bus", "school transport", "van")),
        Rule("Vehicle insurance", listOf("car insurance", "vehicle insurance", "bike insurance")),
        Rule("Health insurance", listOf("health insurance", "medical insurance")),
        Rule("Religious travel", listOf("umrah", "hajj")),
        Rule("Sadqa & donations", listOf("sadqa", "sadaqah", "donation", "charity", "khairat")),
        Rule("Weddings & events", listOf("shaadi", "shadi", "wedding", "walima", "event")),
        Rule("Eid & festivals", listOf("eid", "festival")),
        Rule("Zakat", listOf("zakat", "زکوٰۃ", "زکات")),

        Rule("Groceries", listOf("rashan", "ration", "grocery", "groceries", "راشن")),
        Rule("Flour", listOf("aata", "atta", "flour", "آٹا")),
        Rule("Rice", listOf("chawal", "rice", "چاول")),
        Rule("Lentils", listOf("daal", "dal", "lentil", "lentils", "دال")),
        Rule("Cooking oil", listOf("cooking oil", "ghee", "tel", "oil", "گھی")),
        Rule("Spices", listOf("masala", "spice", "spices", "مصالحہ")),
        Rule("Sugar & tea", listOf("cheeni", "sugar", "patti", "tea leaves", "چینی")),
        Rule("Milk", listOf("doodh", "dodh", "milk", "دودھ")),
        Rule("Vegetables", listOf("sabzi", "sabziyan", "vegetable", "vegetables", "سبزی")),
        Rule("Fruit", listOf("phal", "fruit", "fruits", "پھل")),
        Rule("Meat & chicken", listOf("gosht", "meat", "chicken", "murghi", "murgi", "گوشت", "مرغی")),
        Rule("Fish", listOf("machli", "fish", "مچھلی")),
        Rule("Eggs & bread", listOf("anday", "anda", "eggs", "egg", "bread", "roti", "انڈے")),
        Rule("Bakery", listOf("bakery", "biscuit", "biscuits", "cake")),
        Rule("Dairy & breakfast", listOf("dahi", "yogurt", "yoghurt", "butter", "makhan", "cheese", "paneer", "jam", "cream")),
        Rule("Beverages", listOf("juice", "cold drink", "soft drink", "drink", "sharbat", "syrup")),

        Rule("Takeaway", listOf("takeaway", "delivery", "foodpanda", "food panda")),
        Rule("Tea & coffee", listOf("chai", "coffee", "cafe", "چائے")),
        Rule("Snacks", listOf("snack", "snacks", "papar", "chips", "icecream", "ice cream", "samosa", "samosay")),
        Rule("Restaurant", listOf("restaurant", "dinner", "lunch", "biryani", "burger", "pizza", "khana", "کھانا")),

        Rule("Rent", listOf("house rent", "shop rent", "dukan rent", "makan kiraya", "kiraya makan", "makan ka kiraya", "rent")),
        Rule("Repairs", listOf("repair", "repairs", "plumber", "electrician", "mistri")),
        Rule("Furniture", listOf("furniture", "sofa", "bed", "table", "chair")),
        Rule("Appliances", listOf("appliance", "appliances", "fridge", "washing machine", "ac", "air conditioner")),
        Rule("Cleaning supplies", listOf("safai", "cleaning", "detergent", "surf", "washing powder", "bleach", "phenyl", "floor cleaner", "harpic", "toilet cleaner")),
        Rule("Dishwashing", listOf("dishwash", "dish wash", "dishwashing", "vim", "bartan soap", "bartan sabun", "bartan liquid")),
        Rule("Household essentials", listOf("tissue", "tissues", "toilet roll", "kitchen roll", "foil", "aluminium foil", "garbage bag", "trash bag", "match box", "machis", "mosquito coil")),
        Rule("Domestic help", listOf("maid", "helper", "house help", "nokrani")),
        Rule("Home supplies", listOf("bartan", "kitchen", "kitchen item", "kitchen items", "home supplies")),

        Rule("Electricity", listOf("bijli", "electricity", "electric bill", "electricity bill", "بجلی")),
        Rule("Gas", listOf("gas bill", "sui gas", "sui", "gas", "سوئی گیس")),
        Rule("Water", listOf("water bill", "pani bill", "water", "pani", "پانی")),
        Rule("Internet", listOf("internet", "wifi", "wi-fi", "broadband")),
        Rule("Mobile package", listOf("mobile package", "mobile", "easyload", "easy load", "mobile load", "load", "package", "balance")),
        Rule("TV & cable", listOf("cable", "tv bill", "cable bill", "tv")),

        Rule("Fuel", listOf("petrol", "diesel", "fuel", "cng", "پٹرول")),
        Rule("Public transport", listOf("bus", "train", "metro", "public transport", "kiraya")),
        Rule("Taxi & rides", listOf("taxi", "rickshaw", "rikshaw", "careem", "indrive", "in drive", "uber", "fare")),
        Rule("Parking & tolls", listOf("parking", "toll", "tolls", "motorway toll")),
        Rule("Vehicle service", listOf("car service", "bike service", "vehicle service", "service", "mechanic", "oil change")),
        Rule("Vehicle parts", listOf("tyre", "tire", "parts", "vehicle parts", "car parts", "bike parts")),

        Rule("Medicines", listOf("dawa", "dawai", "medicine", "medicines", "pharmacy", "دوائی", "دوا")),
        Rule("Doctor", listOf("doctor", "clinic", "checkup", "check up")),
        Rule("Lab tests", listOf("lab", "test", "lab test", "lab tests", "blood test", "scan", "xray", "x-ray")),
        Rule("Hospital", listOf("hospital", "operation", "admission")),
        Rule("Dental", listOf("dentist", "dental", "tooth", "teeth")),
        Rule("Glasses", listOf("glasses", "chashma", "spectacles", "چشمہ")),

        Rule("School fees", listOf("school fee", "school fees", "monthly fee", "school ki fee", "bachon ki fee", "bache ki fee", "fees")),
        Rule("Admission & exam fees", listOf("admission fee", "admission fees", "exam fee", "exam fees", "paper fee", "registration fee", "annual charges", "school charges")),
        Rule("College & university", listOf("college", "university", "uni fee", "semester fee", "semester fees")),
        Rule("Tuition", listOf("tuition", "tutor", "academy", "academy fee", "tuition fee")),
        Rule("Books", listOf("books", "book", "kitab", "kitabein", "textbook", "textbooks", "course book", "کتاب")),
        Rule("Stationery", listOf("stationery", "stationary", "pen", "pencil", "copy", "copies", "notebook", "register", "eraser", "rubber", "sharpener", "marker", "highlighter", "scale", "ruler", "geometry", "geometry box", "file folder")),
        Rule("School uniform", listOf("school uniform", "uniform", "school dress", "school shoes", "school belt", "school tie", "school socks")),
        Rule("School projects & supplies", listOf("school project", "project material", "chart", "chart paper", "colors", "colour", "crayons", "glue", "school bag", "school bottle", "lunch box", "school supplies")),
        Rule("Courses", listOf("course", "courses", "training")),

        Rule("Diapers & baby care", listOf("diaper", "diapers", "baby", "baby care", "pampers", "wipes", "baby wipes")),
        Rule("Baby food & formula", listOf("formula milk", "baby formula", "baby food", "cerelac", "lactogen", "nan milk", "baby cereal")),
        Rule("Toys", listOf("toy", "toys", "khilona", "khilonay")),
        Rule("Pocket money", listOf("pocket", "pocket money", "jaib kharch")),
        Rule("Family support", listOf("family", "family support", "parents", "walidain", "ghar walon ko")),
        Rule("Childcare", listOf("childcare", "daycare", "day care")),

        Rule("Clothing", listOf("kapray", "kapre", "clothes", "clothing", "suit", "dress", "garments", "کپڑے")),
        Rule("Shoes", listOf("shoes", "shoe", "jootay", "jootey", "جوتے")),
        Rule("Haircut", listOf("haircut", "hair cut", "barber", "salon cut")),
        Rule("Cosmetics", listOf("cosmetic", "cosmetics", "makeup", "make up", "lipstick", "foundation", "nail polish")),
        Rule("Soap & body care", listOf("soap", "sabun", "saban", "saboon", "body wash", "shower gel", "body lotion", "lotion")),
        Rule("Shampoo & hair care", listOf("shampoo", "conditioner", "hair oil", "hair serum", "hair cream", "shampoo sachet")),
        Rule("Oral care", listOf("toothpaste", "tooth paste", "toothbrush", "tooth brush", "manjan", "mouthwash", "mouth wash")),
        Rule("Sanitary & hygiene", listOf("sanitary pad", "sanitary pads", "pads", "napkin", "sanitary napkin", "tampon", "personal hygiene", "hand wash", "handwash", "sanitizer")),
        Rule("Toiletries", listOf("toiletries", "personal care item", "personal care items")),
        Rule("Laundry", listOf("laundry", "dhobi", "dry clean", "dryclean")),

        Rule("Subscriptions", listOf("subscription", "subscriptions", "netflix", "spotify", "youtube premium")),
        Rule("Cinema & outings", listOf("cinema", "movie", "outing", "picnic")),
        Rule("Games & hobbies", listOf("game", "games", "gaming", "hobby", "hobbies")),
        Rule("Gym & sports", listOf("gym", "sport", "sports", "fitness")),
        Rule("Travel & holidays", listOf("holiday", "holidays", "tour", "trip", "travel")),
        Rule("Hotel stay", listOf("hotel", "hotel stay", "lodging", "room booking")),

        Rule("Gifts", listOf("gift", "gifts", "tohfa", "tohfe", "تحفہ")),
        Rule("Pet food", listOf("pet food", "cat food", "dog food")),
        Rule("Veterinary", listOf("vet", "veterinary", "pet doctor")),
        Rule("Pet supplies", listOf("pet supplies", "litter", "pet accessories")),

        Rule("Business expenses", listOf("business", "shop", "business expense", "business expenses", "shop expense", "dukan kharcha")),
        Rule("Office supplies", listOf("office", "office supplies", "office expense", "printer ink")),
        Rule("Bank fees", listOf("bank", "charges", "bank fee", "bank fees", "bank charges", "atm fee")),
        Rule("Taxes & documents", listOf("tax", "taxes", "passport", "document fee", "documents")),
        Rule("Postage", listOf("courier", "postage", "parcel")),
        Rule("Emergency", listOf("emergency", "urgent expense")),
    )

    private val incomeRules = listOf(
        Rule("Salary", listOf("salary", "tankhwa", "tankhwah", "tankha", "تنخواہ")),
        Rule("Bonus", listOf("bonus")),
        Rule("Freelance income", listOf("freelance", "freelancing", "client payment")),
        Rule("Rental income", listOf("rental income", "rent received", "kiraya mila", "kiraya received")),
        Rule("Business income", listOf("business income", "profit", "munafa", "sale income")),
        Rule("Gift received", listOf("gift received", "tohfa mila", "gift mila")),
        Rule("Refund", listOf("refund", "cashback", "cash back")),
    )

    val expenseCategories: List<String> = (expenseRules.map { it.category } + "Other").distinct()
    val incomeCategories: List<String> = (incomeRules.map { it.category } + "Other income").distinct()
    val allCategories: List<String> = (expenseCategories + incomeCategories).distinct()

    fun isDedicatedLedgerText(text: String): Boolean {
        val normalized = normalizeText(text)
        return dedicatedLedgerAliases.any { containsAlias(normalized, it) }
    }

    fun infer(type: String, text: String): String {
        val normalized = normalizeText(text)
        val rules = if (type.equals("INCOME", ignoreCase = true)) incomeRules else expenseRules
        return rules.flatMap { rule -> (rule.aliases + rule.category).map { alias -> rule.category to normalizeText(alias) } }
            .filter { (_, alias) -> containsAlias(normalized, alias) }
            .maxByOrNull { (_, alias) -> alias.length }?.first
            ?: if (type.equals("INCOME", ignoreCase = true)) "Other income" else "Other"
    }

    fun normalize(type: String, proposed: String, sourceText: String): String {
        val isIncome = type.equals("INCOME", ignoreCase = true)
        val canonical = if (isIncome) incomeCategories else expenseCategories
        val proposedClean = proposed.trim()

        // Fix legacy/model-made labels first.
        val legacy = when (normalizeText(proposedClean)) {
            "other groceries", "other grocery", "grocery", "groceries", "rashan", "ration" -> "Groceries"
            "medical", "medicine" -> "Medicines"
            "bills", "electric bill", "electricity bill" -> "Electricity"
            "mobile", "mobile load" -> "Mobile package"
            "food", "food dining", "food & dining" -> "Restaurant"
            "shopping" -> infer("EXPENSE", sourceText).takeIf { it != "Other" } ?: "Clothing"
            "transport" -> infer("EXPENSE", sourceText).takeIf { it in setOf("Fuel", "Public transport", "Taxi & rides", "Parking & tolls", "Vehicle service", "Vehicle parts", "Vehicle insurance") } ?: "Public transport"
            "other income", "income" -> "Other income"
            else -> null
        }
        if (legacy != null && legacy in canonical) return legacy

        canonical.firstOrNull { it.equals(proposedClean, ignoreCase = true) }?.let { matched ->
            // Even if a model proposed Other, try the actual text one more time first.
            if (matched == "Other" || matched == "Other income") {
                val inferred = infer(type, sourceText)
                if (inferred != matched) return inferred
            }
            return matched
        }

        val inferred = infer(type, "$sourceText $proposedClean")
        if (inferred in canonical) return inferred
        return if (isIncome) "Other income" else "Other"
    }

    fun normalizeDraft(draft: TransactionDraft): TransactionDraft =
        draft.copy(category = normalize(draft.type, draft.category, draft.note)).validated()

    fun normalizeResult(result: AiDraftResult): AiDraftResult =
        result.copy(entries = result.entries.map(::normalizeDraft))

    private fun normalizeText(text: String): String = text
        .lowercase(Locale.ROOT)
        .replace('&', ' ')
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun containsAlias(normalizedText: String, alias: String): Boolean {
        val normalizedAlias = normalizeText(alias)
        if (normalizedAlias.isBlank()) return false
        return " $normalizedText ".contains(" $normalizedAlias ")
    }
}
