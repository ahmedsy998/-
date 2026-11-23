package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.export.BackupPayload
import com.example.data.export.ExportBackupManager
import com.example.data.local.DefaultCategories
import com.example.data.local.MasroofyDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.RecurringRule
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import com.example.data.preferences.SettingsDataStore
import com.example.notification.BudgetNotifier
import com.example.util.ArabicDateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

class MasroofyRepository(
    private val database: MasroofyDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val budgetNotifier: BudgetNotifier,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val recurringMutex = Mutex()
    private val seedMutex = Mutex()

    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val budgets: Flow<List<BudgetEntity>> = budgetDao.observeAll()
    val themeMode: Flow<ThemeMode> = settingsDataStore.themeMode

    suspend fun setThemeMode(mode: ThemeMode) {
        settingsDataStore.setThemeMode(mode)
    }

    suspend fun ensureDefaultCategories() {
        seedMutex.withLock {
            if (categoryDao.count() == 0) {
                categoryDao.insertAll(DefaultCategories.all)
            }
        }
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun saveTransaction(
        existingId: Long? = null,
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        dateTime: Long,
        paymentMethod: com.example.data.model.PaymentMethod,
        note: String,
        recurringRule: RecurringRule,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        ensureDefaultCategories()
        val id = if (existingId != null && existingId > 0L) {
            val old = transactionDao.getById(existingId)
            val updated = TransactionEntity(
                id = existingId,
                type = type,
                amount = amount,
                categoryId = categoryId,
                dateTime = dateTime,
                paymentMethod = paymentMethod,
                note = note.trim(),
                recurringRule = recurringRule,
                recurringProcessed = if (old?.recurringRule == recurringRule && old.dateTime == dateTime) {
                    old.recurringProcessed
                } else {
                    false
                },
                createdAt = old?.createdAt ?: nowMillis
            )
            transactionDao.update(updated)
            existingId
        } else {
            transactionDao.insert(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    dateTime = dateTime,
                    paymentMethod = paymentMethod,
                    note = note.trim(),
                    recurringRule = recurringRule,
                    recurringProcessed = false,
                    createdAt = nowMillis
                )
            )
        }

        processDueRecurringTransactions(nowMillis)
        evaluateBudgetsForMonth(ArabicDateUtils.monthYearFromMillis(dateTime, zoneId))
        return id
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun processDueRecurringTransactions(nowMillis: Long = System.currentTimeMillis()): Int {
        return recurringMutex.withLock {
            ensureDefaultCategories()
            var createdCount = 0
            val active = transactionDao.getActiveRecurringTransactions()
            for (tx in active) {
                var current = tx
                while (current.recurringRule != RecurringRule.NONE && !current.recurringProcessed) {
                    val nextTime = ArabicDateUtils.nextOccurrenceMillis(
                        current.dateTime,
                        current.recurringRule,
                        zoneId
                    ) ?: break
                    if (nextTime > nowMillis) break

                    // Mark previous occurrence processed and insert next occurrence carrying the rule forward
                    database.withTransaction {
                        transactionDao.update(current.copy(recurringProcessed = true))
                        val newId = transactionDao.insert(
                            TransactionEntity(
                                type = current.type,
                                amount = current.amount,
                                categoryId = current.categoryId,
                                dateTime = nextTime,
                                paymentMethod = current.paymentMethod,
                                note = current.note,
                                recurringRule = current.recurringRule,
                                recurringProcessed = false,
                                createdAt = nowMillis
                            )
                        )
                        current = current.copy(
                            id = newId,
                            dateTime = nextTime,
                            recurringProcessed = false
                        )
                    }
                    createdCount++
                    evaluateBudgetsForMonth(ArabicDateUtils.monthYearFromMillis(nextTime, zoneId))
                }
            }
            createdCount
        }
    }

    suspend fun saveCustomCategory(
        existingId: Long? = null,
        nameAr: String,
        type: TransactionType,
        icon: String,
        color: Long
    ): Long {
        ensureDefaultCategories()
        val trimmed = nameAr.trim()
        return if (existingId != null && existingId > 0L) {
            val old = categoryDao.getById(existingId)
            categoryDao.update(
                CategoryEntity(
                    id = existingId,
                    nameAr = trimmed,
                    type = type,
                    icon = icon,
                    color = color,
                    isCustom = old?.isCustom ?: true
                )
            )
            existingId
        } else {
            categoryDao.insert(
                CategoryEntity(
                    nameAr = trimmed,
                    type = type,
                    icon = icon,
                    color = color,
                    isCustom = true
                )
            )
        }
    }

    suspend fun deleteCustomCategory(category: CategoryEntity) {
        if (!category.isCustom) return
        database.withTransaction {
            val fallback = categoryDao.getFallbackCategory(category.type)
                ?: categoryDao.getAllOnce().firstOrNull { it.type == category.type && it.id != category.id }
            if (fallback != null) {
                transactionDao.reassignCategory(category.id, fallback.id)
            }
            categoryDao.delete(category)
        }
    }

    suspend fun saveBudget(
        categoryId: Long,
        monthYear: String,
        limitAmount: Long
    ) {
        val existing = budgetDao.getByCategoryAndMonth(categoryId, monthYear)
        if (existing != null) {
            // Reset notification flags if limit increased so thresholds re-evaluate accurately
            val resetFlags = limitAmount > existing.limitAmount
            budgetDao.update(
                existing.copy(
                    limitAmount = limitAmount,
                    notified80 = if (resetFlags) false else existing.notified80,
                    notified100 = if (resetFlags) false else existing.notified100
                )
            )
        } else {
            budgetDao.insert(
                BudgetEntity(
                    categoryId = categoryId,
                    monthYear = monthYear,
                    limitAmount = limitAmount,
                    notified80 = false,
                    notified100 = false
                )
            )
        }
        evaluateBudgetsForMonth(monthYear)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.delete(budget)
    }

    suspend fun evaluateBudgetsForMonth(monthYear: String) {
        val budgets = budgetDao.getForMonthOnce(monthYear)
        if (budgets.isEmpty()) return

        val ym = ArabicDateUtils.parseYearMonthKey(monthYear)
        val startMs = ArabicDateUtils.startOfDayMillis(ym.atDay(1), zoneId)
        val endMs = ArabicDateUtils.endOfDayMillis(ym.atEndOfMonth(), zoneId)

        val allTransactions = transactionDao.getAllOnce()
        val categoriesById = categoryDao.getAllOnce().associateBy { it.id }

        val spentByCategory = mutableMapOf<Long, Long>()
        for (tx in allTransactions) {
            if (tx.type == TransactionType.EXPENSE && tx.dateTime in startMs..endMs) {
                spentByCategory[tx.categoryId] = (spentByCategory[tx.categoryId] ?: 0L) + tx.amount
            }
        }

        for (budget in budgets) {
            if (budget.limitAmount <= 0L) continue
            val spent = spentByCategory[budget.categoryId] ?: 0L
            val actualPercent = ((spent * 100L) / budget.limitAmount).toInt()
            val categoryName = categoriesById[budget.categoryId]?.nameAr ?: "تصنيف"

            if (spent * 100L >= budget.limitAmount * 100L && !budget.notified100) {
                budgetNotifier.notifyThreshold(
                    budgetId = budget.id,
                    categoryNameAr = categoryName,
                    thresholdPercent = 100,
                    spentAmount = spent,
                    limitAmount = budget.limitAmount,
                    actualPercent = actualPercent
                )
                budgetDao.update(budget.copy(notified80 = true, notified100 = true))
            } else if (spent * 100L >= budget.limitAmount * 80L && !budget.notified80) {
                budgetNotifier.notifyThreshold(
                    budgetId = budget.id,
                    categoryNameAr = categoryName,
                    thresholdPercent = 80,
                    spentAmount = spent,
                    limitAmount = budget.limitAmount,
                    actualPercent = actualPercent
                )
                budgetDao.update(budget.copy(notified80 = true))
            }
        }
    }

    suspend fun exportBackupJson(): String {
        ensureDefaultCategories()
        val categories = categoryDao.getAllOnce()
        val transactions = transactionDao.getAllOnce()
        val budgets = budgetDao.getAllOnce()
        return ExportBackupManager.buildBackupJson(categories, transactions, budgets)
    }

    suspend fun restoreFromBackup(payload: BackupPayload) {
        database.withTransaction {
            budgetDao.deleteAll()
            transactionDao.deleteAll()
            categoryDao.deleteAll()
            categoryDao.insertAll(payload.categories)
            transactionDao.insertAll(payload.transactions)
            budgetDao.insertAll(payload.budgets)
        }
    }

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> = transactionDao.getAllOnce()
    suspend fun getAllCategoriesMapOnce(): Map<Long, CategoryEntity> = categoryDao.getAllOnce().associateBy { it.id }
}
