package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.BackupPayload
import com.example.data.export.ExportBackupManager
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.DateRangePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import com.example.data.repository.MasroofyRepository
import com.example.util.ArabicDateUtils
import com.example.util.BudgetProgress
import com.example.util.DailyTransactionGroup
import com.example.util.DashboardSummary
import com.example.util.FinanceCalculator
import com.example.util.PeriodReport
import com.example.util.TransactionFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class MainUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val categories: List<CategoryEntity> = emptyList(),
    val categoriesById: Map<Long, CategoryEntity> = emptyMap(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val dashboardSummary: DashboardSummary = DashboardSummary(0L, 0L, 0L, 0L, 0L, 0L, emptyList(), emptyList()),
    val transactionFilter: TransactionFilter = TransactionFilter(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: List<DailyTransactionGroup> = emptyList(),
    val filteredTotalExpense: Long = 0L,
    val filteredTotalIncome: Long = 0L,
    val reportIsMonthly: Boolean = true,
    val reportPeriodOffset: Int = 0,
    val periodReport: PeriodReport? = null,
    val selectedBudgetMonth: String = ArabicDateUtils.currentMonthYear(),
    val budgetProgressList: List<BudgetProgress> = emptyList(),
    val currentMonthAlerts: List<BudgetProgress> = emptyList()
)

class MainViewModel(
    private val repository: MasroofyRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _reportIsMonthly = MutableStateFlow(true)
    private val _reportOffset = MutableStateFlow(0)
    private val _budgetMonth = MutableStateFlow(ArabicDateUtils.currentMonthYear())
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureDefaultCategories()
                repository.processDueRecurringTransactions()
                repository.evaluateBudgetsForMonth(ArabicDateUtils.currentMonthYear())
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "حدث خطأ أثناء تحميل البيانات"
            }
        }
    }

    private val dataFlow = combine(
        repository.transactions,
        repository.categories,
        repository.budgets,
        repository.themeMode
    ) { txs, cats, budgets, theme ->
        Quad(txs, cats, budgets, theme)
    }

    private val uiControlsFlow = combine(
        _filter,
        _reportIsMonthly,
        _reportOffset,
        _budgetMonth,
        _errorMessage
    ) { filter, isMonthly, offset, budgetMonth, err ->
        UiControls(filter, isMonthly, offset, budgetMonth, err)
    }

    val uiState: StateFlow<MainUiState> = combine(
        dataFlow,
        uiControlsFlow
    ) { data, controls ->
        val (txs, cats, budgets, theme) = data
        val catsById = cats.associateBy { it.id }
        val now = System.currentTimeMillis()
        val currentMonth = ArabicDateUtils.currentMonthYear(now)

        val dashboard = FinanceCalculator.computeDashboardSummary(txs, catsById, now)
        val filtered = FinanceCalculator.filterTransactions(txs, catsById, controls.filter, now)
        val grouped = FinanceCalculator.groupTransactionsByDay(filtered)
        var filtExp = 0L
        var filtInc = 0L
        for (tx in filtered) {
            if (tx.type == TransactionType.EXPENSE) filtExp += tx.amount else filtInc += tx.amount
        }

        val report = FinanceCalculator.computePeriodReport(
            transactions = txs,
            categoriesById = catsById,
            isMonthly = controls.reportIsMonthly,
            periodOffset = controls.reportOffset,
            nowMillis = now
        )

        val budgetProgress = FinanceCalculator.computeBudgetProgress(
            budgets = budgets,
            transactions = txs,
            categoriesById = catsById,
            monthYear = controls.budgetMonth
        )

        val currentAlerts = FinanceCalculator.computeBudgetProgress(
            budgets = budgets,
            transactions = txs,
            categoriesById = catsById,
            monthYear = currentMonth
        ).filter { it.percentageInt >= 80 }

        MainUiState(
            isLoading = false,
            errorMessage = controls.errorMessage,
            themeMode = theme,
            categories = cats,
            categoriesById = catsById,
            allTransactions = txs,
            dashboardSummary = dashboard,
            transactionFilter = controls.filter,
            filteredTransactions = filtered,
            groupedTransactions = grouped,
            filteredTotalExpense = filtExp,
            filteredTotalIncome = filtInc,
            reportIsMonthly = controls.reportIsMonthly,
            reportPeriodOffset = controls.reportOffset,
            periodReport = report,
            selectedBudgetMonth = controls.budgetMonth,
            budgetProgressList = budgetProgress,
            currentMonthAlerts = currentAlerts
        )
    }
        .catch { err ->
            emit(
                MainUiState(
                    isLoading = false,
                    errorMessage = err.message ?: "تعذر قراءة قاعدة البيانات"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(isLoading = true)
        )

    fun refreshRecurringOnResume() {
        viewModelScope.launch {
            runCatching {
                val created = repository.processDueRecurringTransactions()
                if (created > 0) {
                    _statusMessage.value = "تمت إضافة $created حركات متكررة تلقائياً"
                }
            }
        }
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun updateSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filter.value = _filter.value.copy(type = type)
    }

    fun updateCategoryFilter(categoryId: Long?) {
        _filter.value = _filter.value.copy(categoryId = categoryId)
    }

    fun updatePaymentMethodFilter(method: PaymentMethod?) {
        _filter.value = _filter.value.copy(paymentMethod = method)
    }

    fun updateDateRangePreset(preset: DateRangePreset) {
        _filter.value = _filter.value.copy(
            dateRangePreset = preset,
            customStartMillis = if (preset == DateRangePreset.CUSTOM) _filter.value.customStartMillis else null,
            customEndMillis = if (preset == DateRangePreset.CUSTOM) _filter.value.customEndMillis else null
        )
    }

    fun updateCustomDateRange(startMillis: Long?, endMillis: Long?) {
        _filter.value = _filter.value.copy(
            dateRangePreset = DateRangePreset.CUSTOM,
            customStartMillis = startMillis,
            customEndMillis = endMillis
        )
    }

    fun applyChartCategoryFilter(categoryId: Long) {
        _filter.value = TransactionFilter(
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            dateRangePreset = DateRangePreset.THIS_MONTH
        )
    }

    fun applyChartDayFilter(date: LocalDate) {
        val millis = ArabicDateUtils.startOfDayMillis(date)
        _filter.value = TransactionFilter(
            dateRangePreset = DateRangePreset.CUSTOM,
            customStartMillis = millis,
            customEndMillis = millis
        )
    }

    fun clearAllFilters() {
        _filter.value = TransactionFilter()
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteTransaction(transaction)
                _statusMessage.value = "تم حذف الحركة"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "تعذر حذف الحركة"
            }
        }
    }

    fun setReportMonthly(isMonthly: Boolean) {
        _reportIsMonthly.value = isMonthly
        _reportOffset.value = 0
    }

    fun shiftReportPeriod(delta: Int) {
        val next = _reportOffset.value + delta
        if (next <= 0) {
            _reportOffset.value = next
        }
    }

    fun shiftBudgetMonth(delta: Int) {
        val current = ArabicDateUtils.parseYearMonthKey(_budgetMonth.value)
        _budgetMonth.value = ArabicDateUtils.formatYearMonthKey(current.plusMonths(delta.toLong()))
    }

    fun saveBudget(categoryId: Long, limitAmount: Long) {
        viewModelScope.launch {
            runCatching {
                repository.saveBudget(categoryId, _budgetMonth.value, limitAmount)
                _statusMessage.value = "تم حفظ الميزانية"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "تعذر حفظ الميزانية"
            }
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteBudget(budget)
                _statusMessage.value = "تم حذف الميزانية"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "تعذر حذف الميزانية"
            }
        }
    }

    fun saveCustomCategory(
        existingId: Long? = null,
        nameAr: String,
        type: TransactionType,
        icon: String,
        color: Long
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveCustomCategory(existingId, nameAr, type, icon, color)
                _statusMessage.value = if (existingId == null) "تمت إضافة التصنيف" else "تم تحديث التصنيف"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "تعذر حفظ التصنيف"
            }
        }
    }

    fun deleteCustomCategory(category: CategoryEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteCustomCategory(category)
                _statusMessage.value = "تم حذف التصنيف"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "تعذر حذف التصنيف"
            }
        }
    }

    suspend fun generateBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun parseBackup(rawJson: String): Result<BackupPayload> {
        return runCatching { ExportBackupManager.parseBackupJson(rawJson) }
    }

    fun restoreBackup(payload: BackupPayload) {
        viewModelScope.launch {
            runCatching {
                repository.restoreFromBackup(payload)
                repository.evaluateBudgetsForMonth(ArabicDateUtils.currentMonthYear())
                _statusMessage.value = "تمت استعادة النسخة الاحتياطية بنجاح (${payload.transactions.size} حركة)"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "فشلت استعادة النسخة الاحتياطية"
            }
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class UiControls(
        val filter: TransactionFilter,
        val reportIsMonthly: Boolean,
        val reportOffset: Int,
        val budgetMonth: String,
        val errorMessage: String?
    )
}
