package com.example

import com.example.data.export.ExportBackupManager
import com.example.data.local.DefaultCategories
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.DateRangePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.util.ArabicDateUtils
import com.example.util.FinanceCalculator
import com.example.util.SypFormatter
import com.example.util.TransactionFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MasroofyCalculatorTest {

    private val zone = ZoneId.of("Asia/Damascus")

    private val sampleCategories = DefaultCategories.all.mapIndexed { idx, cat ->
        cat.copy(id = (idx + 1).toLong())
    }
    private val catsById = sampleCategories.associateBy { it.id }

    @Test
    fun sypFormatter_formatsWholeNumbersAndNormalizesArabicDigits() {
        assertEquals("125,000 ل.س", SypFormatter.format(125_000L))
        assertEquals("-50,000 ل.س", SypFormatter.format(-50_000L))
        assertEquals("+10,000 ل.س", SypFormatter.format(10_000L, showPlusSign = true))
        assertEquals("125000", SypFormatter.normalizeDigits("١٢٥٠٠٠"))
        assertEquals(125_000L, SypFormatter.parsePositiveWholeAmount("١٢٥,٠٠٠"))
        assertNull(SypFormatter.parsePositiveWholeAmount("0"))
    }

    @Test
    fun dashboardSummary_computesExactTotalsForAddEditDelete() {
        val today = LocalDate.of(2026, 11, 23)
        val nowMillis = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val earlierThisMonth = today.minusDays(5).atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val lastMonth = today.minusMonths(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val foodCat = sampleCategories.first { it.nameAr == "طعام وشراب" }
        val salaryCat = sampleCategories.first { it.nameAr == "راتب" }

        val txs = listOf(
            TransactionEntity(1L, TransactionType.INCOME, 1_500_000L, salaryCat.id, earlierThisMonth, PaymentMethod.BANK_TRANSFER, "راتب تشرين الثاني"),
            TransactionEntity(2L, TransactionType.EXPENSE, 125_000L, foodCat.id, nowMillis, PaymentMethod.CASH, "غداء"),
            TransactionEntity(3L, TransactionType.EXPENSE, 75_000L, foodCat.id, earlierThisMonth, PaymentMethod.CARD, "سوبرماركت"),
            TransactionEntity(4L, TransactionType.EXPENSE, 100_000L, foodCat.id, lastMonth, PaymentMethod.CASH, "مصروف قديم")
        )

        val summary = FinanceCalculator.computeDashboardSummary(txs, catsById, nowMillis, zone)
        assertEquals(1_500_000L, summary.totalIncome)
        assertEquals(300_000L, summary.totalExpense)
        assertEquals(1_200_000L, summary.balance)
        assertEquals(1_500_000L, summary.monthIncome)
        assertEquals(200_000L, summary.monthExpense)
        assertEquals(125_000L, summary.todayExpense)
        assertEquals(1, summary.monthExpenseByCategory.size)
        assertEquals(200_000L, summary.monthExpenseByCategory.first().totalAmount)
        assertEquals(7, summary.last7Days.size)
        assertEquals(125_000L, summary.last7Days.last().expenseAmount)
    }

    @Test
    fun searchAndFilters_returnExactMatchingTransactions() {
        val today = LocalDate.of(2026, 11, 23)
        val nowMillis = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val threeDaysAgo = today.minusDays(2).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
        val lastMonth = today.minusMonths(1).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()

        val foodCat = sampleCategories.first { it.nameAr == "طعام وشراب" }
        val netCat = sampleCategories.first { it.nameAr == "إنترنت وهاتف" }
        val salaryCat = sampleCategories.first { it.nameAr == "راتب" }

        val txs = listOf(
            TransactionEntity(1L, TransactionType.EXPENSE, 50_000L, foodCat.id, nowMillis, PaymentMethod.CASH, "فطور صباحي"),
            TransactionEntity(2L, TransactionType.EXPENSE, 80_000L, netCat.id, threeDaysAgo, PaymentMethod.BANK_TRANSFER, "فاتورة راوتر"),
            TransactionEntity(3L, TransactionType.INCOME, 2_000_000L, salaryCat.id, lastMonth, PaymentMethod.CARD, "راتب شهري")
        )

        // Search by note
        assertEquals(
            listOf(2L),
            FinanceCalculator.filterTransactions(txs, catsById, TransactionFilter(searchQuery = "راوتر"), nowMillis, zone).map { it.id }
        )
        // Search by category name
        assertEquals(
            listOf(1L),
            FinanceCalculator.filterTransactions(txs, catsById, TransactionFilter(searchQuery = "طعام"), nowMillis, zone).map { it.id }
        )
        // Filter by type
        assertEquals(
            listOf(3L),
            FinanceCalculator.filterTransactions(txs, catsById, TransactionFilter(type = TransactionType.INCOME), nowMillis, zone).map { it.id }
        )
        // Filter by payment method
        assertEquals(
            listOf(2L),
            FinanceCalculator.filterTransactions(txs, catsById, TransactionFilter(paymentMethod = PaymentMethod.BANK_TRANSFER), nowMillis, zone).map { it.id }
        )
        // Filter by Today
        assertEquals(
            listOf(1L),
            FinanceCalculator.filterTransactions(txs, catsById, TransactionFilter(dateRangePreset = DateRangePreset.TODAY), nowMillis, zone).map { it.id }
        )
        // Filter by This Week (Saturday Nov 21 .. Friday Nov 27)
        assertEquals(
            listOf(1L, 2L),
            FinanceCalculator.filterTransactions(txs, catsById, TransactionFilter(dateRangePreset = DateRangePreset.THIS_WEEK), nowMillis, zone).map { it.id }
        )
        // Filter by Custom Range
        assertEquals(
            listOf(3L),
            FinanceCalculator.filterTransactions(
                txs,
                catsById,
                TransactionFilter(
                    dateRangePreset = DateRangePreset.CUSTOM,
                    customStartMillis = lastMonth,
                    customEndMillis = lastMonth
                ),
                nowMillis,
                zone
            ).map { it.id }
        )
    }

    @Test
    fun reportsAndCsvAndJson_roundTripAccurately() {
        val today = LocalDate.of(2026, 11, 23)
        val nowMillis = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val prevMonthMillis = today.minusMonths(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val foodCat = sampleCategories.first { it.nameAr == "طعام وشراب" }
        val rentCat = sampleCategories.first { it.nameAr == "إيجار" }

        val txs = listOf(
            TransactionEntity(1L, TransactionType.EXPENSE, 600_000L, rentCat.id, nowMillis, PaymentMethod.CASH, "إيجار البيت", RecurringRule.MONTHLY),
            TransactionEntity(2L, TransactionType.EXPENSE, 150_000L, foodCat.id, nowMillis, PaymentMethod.CARD, "مشتريات, خضار"),
            TransactionEntity(3L, TransactionType.EXPENSE, 500_000L, rentCat.id, prevMonthMillis, PaymentMethod.CASH, "إيجار سابق")
        )

        val report = FinanceCalculator.computePeriodReport(txs, catsById, isMonthly = true, periodOffset = 0, nowMillis = nowMillis, zoneId = zone)
        assertEquals(750_000L, report.totalExpense)
        assertEquals(500_000L, report.previousExpense)
        assertEquals(50L, report.expenseChangePercent)
        assertNotNull(report.biggestExpense)
        assertEquals(600_000L, report.biggestExpense?.amount)

        // CSV Export verification
        val csv = ExportBackupManager.buildCsvContent(txs, catsById, zone)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"مشتريات, خضار\""))
        assertTrue(csv.contains("600000"))
    }
}
