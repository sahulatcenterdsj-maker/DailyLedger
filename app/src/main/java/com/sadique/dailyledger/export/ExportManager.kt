package com.sadique.dailyledger.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sadique.dailyledger.data.LedgerRepository
import com.sadique.dailyledger.ui.plainAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExportManager {
    private fun stamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

    suspend fun csv(context: Context, repo: LedgerRepository): String = withContext(Dispatchers.IO) {
        val rows = mutableListOf("section,date,type,category,description,amount_pkr")
        repo.transactions.first().forEach {
            rows += "transaction,${it.date},${it.type},${esc(it.category)},${esc(it.note)},${plainAmount(it.amountMinor)}"
        }
        repo.savings.first().forEach {
            rows += "saving,${it.date},${it.kind},,${esc(it.note)},${plainAmount(it.amountMinor)}"
        }
        val loans = repo.loans.first()
        repo.loanPayments.first().forEach { p ->
            val who = loans.firstOrNull { it.id == p.loanId }?.person.orEmpty()
            rows += "loan_payment,${p.date},,${esc(who)},${esc(p.note)},${plainAmount(p.amountMinor)}"
        }
        save(context, "DailyLedger-${stamp()}.csv", "text/csv", rows.joinToString("\n").toByteArray(Charsets.UTF_8))
    }

    suspend fun pdf(context: Context, repo: LedgerRepository): String = withContext(Dispatchers.IO) {
        val tx = repo.transactions.first()
        val doc = PdfDocument()
        val paint = Paint().apply { textSize = 13f }
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
        var y = 45f
        page.canvas.drawText("Daily Ledger report", 40f, y, paint)
        y += 28f
        for (t in tx) {
            if (y > 800f) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
                y = 45f
            }
            val sign = if (t.type == "EXPENSE") "-" else "+"
            page.canvas.drawText("${t.date}  ${t.category.take(24)}  $sign PKR ${plainAmount(t.amountMinor)}", 40f, y, paint)
            y += 19f
        }
        doc.finishPage(page)
        val buffer = ByteArrayOutputStream()
        doc.writeTo(buffer)
        doc.close()
        save(context, "DailyLedger-${stamp()}.pdf", "application/pdf", buffer.toByteArray())
    }

    /** Writes into the public Downloads folder (scoped storage on API 29+, app-specific folder before that). */
    private fun save(context: Context, name: String, mime: String, bytes: ByteArray): String {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create $name")
            val out = context.contentResolver.openOutputStream(uri) ?: error("Cannot open $name for writing")
            out.use { it.write(bytes) }
            return uri.toString()
        }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        dir.mkdirs()
        val file = File(dir, name)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    private fun esc(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""
}
