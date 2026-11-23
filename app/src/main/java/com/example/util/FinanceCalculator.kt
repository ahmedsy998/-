package com.example.util

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.DateRangePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CategoryTotal(
    val category: CategoryEntity,
    val totalAmount: Long,
    val transactionCount: Int,
    val percentageInt: Int
)

data class DailyBarPoint(
    val date: LocalDate,
    val labelAr: String,
    val expenseAmount: Long,
    val incomeAmount: Long
)

data class DashboardSummary(
    val balance: Long,
    val totalIncome: Long,
    val totalExpense: Long,
    val monthIncome: Long,
    val monthExpense: Long,
    val todayExpense: Long,
    val monthExpenseByCategory: List<CategoryTotal>,
    val last7Days: List<DailyBarPoint>
)

data class TransactionFilter(
    val searchQuery: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod? = null,
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            type != null ||
            categoryId != null ||
            paymentMethod != null ||
            dateRangePreset != DateRangePreset.ALL
}

data class DailyTransactionGroup(
    val date: LocalDate,
    val headerLabel: String,
    val transactions: List<TransactionEntity>,
    val totalExpense: Long,
    val totalIncome: Long,
    val netAmount: Long
)

data class PeriodReport(
    val periodLabel: String,
    val previousPeriodLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalExpense: Long,
    val totalIncome: Long,
    val netAmount: Long,
    val previousExpense: Long,
    val previousIncome: Long,
    val expenseChangePercent: Long?,
    val incomeChangePercent: Long?,
    val expenseByCategory: List<CategoryTotal>,
    val incomeByCategory: List<CategoryTotal>,
    val biggestExpense: TransactionEntity?
)

data class BudgetProgress(
    val budget: BudgetEntity,
    val category: CategoryEntity,
    val spentAmount: Long,
    val remainingAmount: Long,
    val percentageInt: Int
)

object FinanceCalculator {

    fun computeDashboardSummary(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DashboardSummary {
        val today = ArabicDateUtils.toLocalDate(nowMillis, zoneId)
        val currentMonth = YearMonth.from(today)
        val monthStart = ArabicDateUtils.startOfDayMillis(currentMonth.atDay(1), zoneId)
        val monthEnd = ArabicDateUtils.endOfDayMillis(currentMonth.atEndOfMonth(), zoneId)
        val todayStart = ArabicDateUtils.startOfDayMillis(today, zoneId)
        val todayEnd = ArabicDateUtils.endOfDayMillis(today, zoneId)

        var totalIncome = 0L
        var totalExpense = 0L
        var monthIncome = 0L
        var monthExpense = 0L
        var todayExpense = 0L

        val monthCategoryTotals = mutableMapOf<Long, Pair<Long, Int>>()
        val dailyExpenseMap = mutableMapOf<LocalDate, Long>()
        val dailyIncomeMap = mutableMapOf<LocalDate, Long>()

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amount
                if (tx.dateTime in monthStart..monthEnd) {
                    monthIncome += tx.amount
                }
            } else {
                totalExpense += tx.amount
                if (tx.dateTime in monthStart..monthEnd) {
                    monthExpense += tx.amount
                    val prev = monthCategoryTotals[tx.categoryId] ?: (0L to 0)
                    monthCategoryTotals[tx.categoryId] = (prev.first + tx.amount) to (prev.second + 1)
                }
                if (tx.dateTime in todayStart..todayEnd) {
                    todayExpense += tx.amount
                }
            }
            val txDate = ArabicDateUtils.toLocalDate(tx.dateTime, zoneId)
            if (tx.type == TransactionType.EXPENSE) {
                dailyExpenseMap[txDate] = (dailyExpenseMap[txDate] ?: 0L) + tx.amount
            } else {
                dailyIncomeMap[txDate] = (dailyIncomeMap[txDate] ?: 0L) + tx.amount
            }
        }

        val categoryBreakdown = monthCategoryTotals.entries.mapNotNull { (catId, pair) ->
            val category = categoriesById[catId] ?: return@mapNotNull null
            val pct = if (monthExpense > 0L) ((pair.first * 100L) / monthExpense).toInt() else 0
            CategoryTotal(
                category = category,
                totalAmount = pair.first,
                transactionCount = pair.second,
                percentageInt = pct
            )
        }.sortedByDescending { it.totalAmount }

        val last7Days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DailyBarPoint(
                date = date,
                labelAr = ArabicDateUtils.formatShortDayName(date),
                expenseAmount = dailyExpenseMap[date] ?: 0L,
                incomeAmount = dailyIncomeMap[date] ?: 0L
            )
        }

        return DashboardSummary(
            balance = totalIncome - totalExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            todayExpense = todayExpense,
            monthExpenseByCategory = categoryBreakdown,
            last7Days = last7Days
        )
    }

    fun filterTransactions(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        filter: TransactionFilter,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<TransactionEntity> {
        val today = ArabicDateUtils.toLocalDate(nowMillis, zoneId)
        val query = filter.searchQuery.trim().lowercase()

        val (rangeStart, rangeEnd) = when (filter.dateRangePreset) {
            DateRangePreset.ALL -> null to null
            DateRangePreset.TODAY -> {
                ArabicDateUtils.startOfDayMillis(today, zoneId) to ArabicDateUtils.endOfDayMillis(today, zoneId)
            }
            DateRangePreset.THIS_WEEK -> {
                val weekStart = ArabicDateUtils.startOfWeek(today)
                val weekEnd = weekStart.plusDays(6)
                ArabicDateUtils.startOfDayMillis(weekStart, zoneId) to ArabicDateUtils.endOfDayMillis(weekEnd, zoneId)
            }
            DateRangePreset.THIS_MONTH -> {
                val ym = YearMonth.from(today)
                ArabicDateUtils.startOfDayMillis(ym.atDay(1), zoneId) to ArabicDateUtils.endOfDayMillis(ym.atEndOfMonth(), zoneId)
            }
            DateRangePreset.CUSTOM -> {
                val start = filter.customStartMillis?.let {
                    ArabicDateUtils.startOfDayMillis(ArabicDateUtils.toLocalDate(it, zoneId), zoneId)
                }
                val end = filter.customEndMillis?.let {
                    ArabicDateUtils.endOfDayMillis(ArabicDateUtils.toLocalDate(it, zoneId), zoneId)
                }
                start to end
            }
        }

        return transactions.filter { tx ->
            if (filter.type != null && tx.type != filter.type) return@filter false
            if (filter.categoryId != null && tx.categoryId != filter.categoryId) return@filter false
            if (filter.paymentMethod != null && tx.paymentMethod != filter.paymentMethod) return@filter false
            if (rangeStart != null && tx.dateTime < rangeStart) return@filter false
            if (rangeEnd != null && tx.dateTime > rangeEnd) return@filter false
            if (query.isNotEmpty()) {
                val categoryName = categoriesById[tx.categoryId]?.nameAr.orEmpty().lowercase()
                val note = tx.note.lowercase()
                if (!note.contains(query) && !categoryName.contains(query)) return@filter false
            }
            true
        }
    }

    fun groupTransactionsByDay(
        transactions: List<TransactionEntity>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyTransactionGroup> {
        val grouped = transactions.groupBy { ArabicDateUtils.toLocalDate(it.dateTime, zoneId) }
        return grouped.entries
            .sortedByDescending { it.key }
            .map { (date, items) ->
                var exp = 0L
                var inc = 0L
                for (tx in items) {
                    if (tx.type == TransactionType.EXPENSE) exp += tx.amount else inc += tx.amount
                }
                DailyTransactionGroup(
                    date = date,
                    headerLabel = ArabicDateUtils.formatDayHeader(date, today),
                    transactions = items.sortedByDescending { it.dateTime },
                    totalExpense = exp,
                    totalIncome = inc,
                    netAmount = inc - exp
                )
            }
    }

    fun computePeriodReport(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        isMonthly: Boolean,
        periodOffset: Int = 0,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PeriodReport {
        val today = ArabicDateUtils.toLocalDate(nowMillis, zoneId)
        val startDate: LocalDate
        val endDate: LocalDate
        val prevStartDate: LocalDate
        val prevEndDate: LocalDate
        val periodLabel: String
        val prevPeriodLabel: String

        if (isMonthly) {
            val targetYm = YearMonth.from(today).plusMonths(periodOffset.toLong())
            val prevYm = targetYm.minusMonths(1)
            startDate = targetYm.atDay(1)
            endDate = targetYm.atEndOfMonth()
            prevStartDate = prevYm.atDay(1)
            prevEndDate = prevYm.atEndOfMonth()
            periodLabel = ArabicDateUtils.formatMonthYearLabel(targetYm)
            prevPeriodLabel = ArabicDateUtils.formatMonthYearLabel(prevYm)
        } else {
            val currentWeekStart = ArabicDateUtils.startOfWeek(today).plusWeeks(periodOffset.toLong())
            startDate = currentWeekStart
            endDate = currentWeekStart.plusDays(6)
            prevStartDate = currentWeekStart.minusWeeks(1)
            prevEndDate = prevStartDate.plusDays(6)
            periodLabel = "${ArabicDateUtils.formatShortDate(ArabicDateUtils.startOfDayMillis(startDate, zoneId), zoneId)} - ${ArabicDateUtils.formatShortDate(ArabicDateUtils.startOfDayMillis(endDate, zoneId), zoneId)}"
            prevPeriodLabel = "الأسبوع السابق"
        }

        val startMs = ArabicDateUtils.startOfDayMillis(startDate, zoneId)
        val endMs = ArabicDateUtils.endOfDayMillis(endDate, zoneId)
        val prevStartMs = ArabicDateUtils.startOfDayMillis(prevStartDate, zoneId)
        val prevEndMs = ArabicDateUtils.endOfDayMillis(prevEndDate, zoneId)

        var totalExpense = 0L
        var totalIncome = 0L
        var prevExpense = 0L
        var prevIncome = 0L
        var biggestExpense: TransactionEntity? = null

        val expenseMap = mutableMapOf<Long, Pair<Long, Int>>()
        val incomeMap = mutableMapOf<Long, Pair<Long, Int>>()

        for (tx in transactions) {
            if (tx.dateTime in startMs..endMs) {
                if (tx.type == TransactionType.EXPENSE) {
                    totalExpense += tx.amount
                    val prev = expenseMap[tx.categoryId] ?: (0L to 0)
                    expenseMap[tx.categoryId] = (prev.first + tx.amount) to (prev.second + 1)
                    if (biggestExpense == null || tx.amount > biggestExpense.amount) {
                        biggestExpense = tx
                    }
                } else {
                    totalIncome += tx.amount
                    val prev = incomeMap[tx.categoryId] ?: (0L to 0)
                    incomeMap[tx.categoryId] = (prev.first + tx.amount) to (prev.second + 1)
                }
            } else if (tx.dateTime in prevStartMs..prevEndMs) {
                if (tx.type == TransactionType.EXPENSE) {
                    prevExpense += tx.amount
                } else {
                    prevIncome += tx.amount
                }
            }
        }

        val expenseCats = expenseMap.entries.mapNotNull { (catId, pair) ->
            val cat = categoriesById[catId] ?: return@mapNotNull null
            val pct = if (totalExpense > 0L) ((pair.first * 100L) / totalExpense).toInt() else 0
            CategoryTotal(cat, pair.first, pair.second, pct)
        }.sortedByDescending { it.totalAmount }

        val incomeCats = incomeMap.entries.mapNotNull { (catId, pair) ->
            val cat = categoriesById[catId] ?: return@mapNotNull null
            val pct = if (totalIncome > 0L) ((pair.first * 100L) / totalIncome).toInt() else 0
            CategoryTotal(cat, pair.first, pair.second, pct)
        }.sortedByDescending { it.totalAmount }

        val expChange = if (prevExpense > 0L) ((totalExpense - prevExpense) * 100L) / prevExpense else null
        val incChange = if (prevIncome > 0L) ((totalIncome - prevIncome) * 100L) / prevIncome else null

        return PeriodReport(
            periodLabel = periodLabel,
            previousPeriodLabel = prevPeriodLabel,
            startDate = startDate,
            endDate = endDate,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netAmount = totalIncome - totalExpense,
            previousExpense = prevExpense,
            previousIncome = prevIncome,
            expenseChangePercent = expChange,
            incomeChangePercent = incChange,
            expenseByCategory = expenseCats,
            incomeByCategory = incomeCats,
            biggestExpense = biggestExpense
        )
    }

    fun computeBudgetProgress(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        monthYear: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<BudgetProgress> {
        val ym = ArabicDateUtils.parseYearMonthKey(monthYear)
        val startMs = ArabicDateUtils.startOfDayMillis(ym.atDay(1), zoneId)
        val endMs = ArabicDateUtils.endOfDayMillis(ym.atEndOfMonth(), zoneId)

        val spentByCategory = mutableMapOf<Long, Long>()
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE && tx.dateTime in startMs..endMs) {
                spentByCategory[tx.categoryId] = (spentByCategory[tx.categoryId] ?: 0L) + tx.amount
            }
        }

        return budgets
            .filter { it.monthYear == monthYear }
            .mapNotNull { budget ->
                val cat = categoriesById[budget.categoryId] ?: return@mapNotNull null
                val spent = spentByCategory[budget.categoryId] ?: 0L
                val pct = if (budget.limitAmount > 0L) ((spent * 100L) / budget.limitAmount).toInt() else 0
                BudgetProgress(
                    budget = budget,
                    category = cat,
                    spentAmount = spent,
                    remainingAmount = budget.limitAmount - spent,
                    percentageInt = pct
                )
            }
            .sortedByDescending { it.percentageInt }
    }
}
