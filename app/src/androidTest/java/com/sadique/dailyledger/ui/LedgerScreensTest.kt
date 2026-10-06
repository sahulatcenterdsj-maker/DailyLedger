package com.sadique.dailyledger.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sadique.dailyledger.data.*
import com.sadique.dailyledger.ui.screens.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class LedgerScreensTest {
    @get:Rule val compose = createComposeRule()
    private val month = YearMonth.now().toString()
    private val kameti = CommitteeEntity("k", "test", "Office kameti", 500000, 12, month, null, false, true, "", 1, 2)

    @Test fun dashboardKeepsSavingsAndKametiOutsideSalaryAndOpensTheirTabs() {
        var opened = ""
        val tx = listOf(
            TransactionEntity("salary", "test", "INCOME", 10000000, "Salary", "", "$month-01", 1, 1),
            TransactionEntity("expense", "test", "EXPENSE", 3000000, "Food", "", "$month-02", 1, 1)
        )
        compose.setContent {
            DailyLedgerTheme("LIGHT") {
                DashboardScreen(tx, emptyList(), emptyList(), listOf(kameti),
                    listOf(CommitteePaymentEntity("p", "test", "k", 1, month, 1000000, 1)),
                    emptyList(), listOf(SavingEntity("s", "test", "DIRECT", 2000000, "$month-01", "", 1)),
                    onAddSalary = { opened = "salary" }, onAddExpense = { opened = "expense" },
                    onOpenSavings = { opened = "savings" }, onOpenKameti = { opened = "kameti" })
            }
        }
        compose.onNodeWithText("Remaining salary").assertIsDisplayed()
        compose.onNodeWithText(money(7000000)).assertIsDisplayed()
        screenshot("dashboard")
        compose.onNodeWithText("Add salary").performClick()
        assertEquals("salary", opened)
        compose.onNodeWithText("Savings total").performScrollTo().performClick()
        assertEquals("savings", opened)
        compose.onNodeWithText("Kameti paid").performScrollTo().performClick()
        assertEquals("kameti", opened)
        screenshot("dashboard-separate-totals")
    }

    @Test fun firstKametiCanBeReceivedBeforeSecond() {
        val receipts = mutableStateOf(emptyList<CommitteeReceiptEntity>())
        compose.setContent {
            DailyLedgerTheme("LIGHT") {
                CommitteeScreen(listOf(kameti), emptyList(), receipts.value,
                    onAdd = { _, _, _, _, _, _, _ -> }, onPaid = { _, _, _ -> },
                    onReceive = { id, amount, date, note -> receipts.value = receipts.value + CommitteeReceiptEntity("r", "test", id, amount, date, note, 1) },
                    onDeleteReceipt = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("Receive amount").performScrollTo().performClick()
        compose.onNodeWithText("Received amount PKR").performTextReplacement("60000")
        compose.onNodeWithText("Save receiving").performClick()
        compose.onNodeWithText("Partially received").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Receive amount").assertIsEnabled()
        assertEquals(6000000L, receipts.value.single().amountMinor)
        screenshot("kameti-partial")
        compose.onNodeWithText("Receive amount").performClick()
        compose.onNodeWithText("Received amount PKR").performTextReplacement("60001")
        compose.onNodeWithText("Save receiving").assertIsNotEnabled()
        compose.onNodeWithText("Received amount PKR").performTextReplacement("60000")
        compose.onNodeWithText("Save receiving").performClick()
        compose.onNodeWithText("Fully received").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Received").filter(hasClickAction()).onFirst().assertIsNotEnabled()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // Keep previews outside app storage: Gradle removes the test app after the run.
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/dailyledger-$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
