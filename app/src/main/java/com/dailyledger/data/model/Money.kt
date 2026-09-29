package com.dailyledger.data.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Money calculations in integer paisa (1 PKR = 100 paisa).
 * Prevents floating-point rounding issues in financial records.
 */
object Money {
    fun formatPkr(paisa: Long): String {
        val isNegative = paisa < 0
        val absPaisa = kotlin.math.abs(paisa)
        val rupees = absPaisa / 100
        val remPaisa = absPaisa % 100

        val nf = NumberFormat.getNumberInstance(Locale.US)
        val formattedRupees = nf.format(rupees)

        val prefix = if (isNegative) "-" else ""
        return if (remPaisa == 0L) {
            "${prefix}Rs $formattedRupees"
        } else {
            "${prefix}Rs $formattedRupees.${remPaisa.toString().padStart(2, '0')}"
        }
    }

    fun rupeesToPaisa(rupees: Double): Long {
        return kotlin.math.round(rupees * 100.0).toLong()
    }

    fun paisaToRupees(paisa: Long): Double {
        return paisa.toDouble() / 100.0
    }
}
