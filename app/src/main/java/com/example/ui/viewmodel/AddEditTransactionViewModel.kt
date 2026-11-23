package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.data.repository.MasroofyRepository
import com.example.util.SypFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddEditTransactionUiState(
    val transactionId: Long? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val selectedCategoryId: Long? = null,
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val recurringRule: RecurringRule = RecurringRule.NONE,
    val categoriesForType: List<CategoryEntity> = emptyList(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val generalError: String? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

class AddEditTransactionViewModel(
    private val repository: MasroofyRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AddEditFormState())

    val uiState: StateFlow<AddEditTransactionUiState> = combine(
        _form,
        repository.categories
    ) { form, allCategories ->
        val filteredCats = allCategories.filter { it.type == form.type }
        val resolvedCategoryId = if (filteredCats.any { it.id == form.selectedCategoryId }) {
            form.selectedCategoryId
        } else {
            filteredCats.firstOrNull()?.id
        }

        AddEditTransactionUiState(
            transactionId = form.transactionId,
            isEditing = form.transactionId != null && form.transactionId > 0L,
            isLoading = form.isLoading,
            isSaving = form.isSaving,
            type = form.type,
            amountText = form.amountText,
            selectedCategoryId = resolvedCategoryId,
            dateTimeMillis = form.dateTimeMillis,
            paymentMethod = form.paymentMethod,
            note = form.note,
            recurringRule = form.recurringRule,
            categoriesForType = filteredCats,
            amountError = form.amountError,
            categoryError = form.categoryError,
            generalError = form.generalError,
            isSaved = form.isSaved,
            isDeleted = form.isDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditTransactionUiState()
    )

    fun loadTransaction(transactionId: Long?) {
        if (transactionId == null || transactionId <= 0L) {
            _form.value = AddEditFormState(dateTimeMillis = System.currentTimeMillis())
            viewModelScope.launch { repository.ensureDefaultCategories() }
            return
        }
        _form.value = AddEditFormState(transactionId = transactionId, isLoading = true)
        viewModelScope.launch {
            runCatching {
                repository.ensureDefaultCategories()
                repository.getTransactionById(transactionId)
            }.onSuccess { tx ->
                if (tx != null) {
                    _form.value = AddEditFormState(
                        transactionId = tx.id,
                        isLoading = false,
                        type = tx.type,
                        amountText = tx.amount.toString(),
                        selectedCategoryId = tx.categoryId,
                        dateTimeMillis = tx.dateTime,
                        paymentMethod = tx.paymentMethod,
                        note = tx.note,
                        recurringRule = tx.recurringRule
                    )
                } else {
                    _form.value = AddEditFormState(
                        isLoading = false,
                        generalError = "تعذر العثور على الحركة المطلوبة"
                    )
                }
            }.onFailure { err ->
                _form.value = AddEditFormState(
                    isLoading = false,
                    generalError = err.message ?: "حدث خطأ أثناء تحميل الحركة"
                )
            }
        }
    }

    fun setType(type: TransactionType) {
        if (_form.value.type != type) {
            _form.value = _form.value.copy(
                type = type,
                selectedCategoryId = null,
                categoryError = null
            )
        }
    }

    fun setAmountText(raw: String) {
        val normalized = SypFormatter.normalizeDigits(raw).take(13)
        _form.value = _form.value.copy(
            amountText = normalized,
            amountError = null
        )
    }

    fun appendKeypadDigit(digit: String) {
        val current = _form.value.amountText
        if (current.length >= 13) return
        val next = if (current == "0") digit else current + digit
        setAmountText(next)
    }

    fun appendDoubleZero() {
        val current = _form.value.amountText
        if (current.isEmpty() || current == "0" || current.length >= 12) return
        setAmountText(current + "00")
    }

    fun appendTripleZero() {
        val current = _form.value.amountText
        if (current.isEmpty() || current == "0" || current.length >= 11) return
        setAmountText(current + "000")
    }

    fun backspaceKeypad() {
        val current = _form.value.amountText
        if (current.isNotEmpty()) {
            setAmountText(current.dropLast(1))
        }
    }

    fun setCategory(categoryId: Long) {
        _form.value = _form.value.copy(
            selectedCategoryId = categoryId,
            categoryError = null
        )
    }

    fun setDateTime(epochMillis: Long) {
        _form.value = _form.value.copy(dateTimeMillis = epochMillis)
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _form.value = _form.value.copy(paymentMethod = method)
    }

    fun setNote(note: String) {
        _form.value = _form.value.copy(note = note)
    }

    fun setRecurringRule(rule: RecurringRule) {
        _form.value = _form.value.copy(recurringRule = rule)
    }

    fun save() {
        val snapshot = uiState.value
        val parsedAmount = SypFormatter.parsePositiveWholeAmount(snapshot.amountText)
        val catId = snapshot.selectedCategoryId

        var hasError = false
        var amountError: String? = null
        var categoryError: String? = null

        if (parsedAmount == null) {
            amountError = if (snapshot.amountText.isBlank()) {
                "يرجى إدخال المبلغ بالليرة السورية"
            } else {
                "المبلغ يجب أن يكون رقماً صحيحاً أكبر من صفر"
            }
            hasError = true
        }
        if (catId == null || catId <= 0L) {
            categoryError = "يرجى اختيار التصنيف"
            hasError = true
        }

        if (hasError) {
            _form.value = _form.value.copy(
                amountError = amountError,
                categoryError = categoryError
            )
            return
        }

        _form.value = _form.value.copy(isSaving = true, generalError = null)
        viewModelScope.launch {
            runCatching {
                repository.saveTransaction(
                    existingId = snapshot.transactionId,
                    type = snapshot.type,
                    amount = parsedAmount!!,
                    categoryId = catId!!,
                    dateTime = snapshot.dateTimeMillis,
                    paymentMethod = snapshot.paymentMethod,
                    note = snapshot.note,
                    recurringRule = snapshot.recurringRule
                )
            }.onSuccess {
                _form.value = _form.value.copy(isSaving = false, isSaved = true)
            }.onFailure { err ->
                _form.value = _form.value.copy(
                    isSaving = false,
                    generalError = err.message ?: "تعذر حفظ الحركة"
                )
            }
        }
    }

    fun deleteCurrentTransaction() {
        val id = _form.value.transactionId ?: return
        _form.value = _form.value.copy(isSaving = true)
        viewModelScope.launch {
            runCatching {
                repository.deleteTransactionById(id)
            }.onSuccess {
                _form.value = _form.value.copy(isSaving = false, isDeleted = true)
            }.onFailure { err ->
                _form.value = _form.value.copy(
                    isSaving = false,
                    generalError = err.message ?: "تعذر حذف الحركة"
                )
            }
        }
    }

    private data class AddEditFormState(
        val transactionId: Long? = null,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val type: TransactionType = TransactionType.EXPENSE,
        val amountText: String = "",
        val selectedCategoryId: Long? = null,
        val dateTimeMillis: Long = System.currentTimeMillis(),
        val paymentMethod: PaymentMethod = PaymentMethod.CASH,
        val note: String = "",
        val recurringRule: RecurringRule = RecurringRule.NONE,
        val amountError: String? = null,
        val categoryError: String? = null,
        val generalError: String? = null,
        val isSaved: Boolean = false,
        val isDeleted: Boolean = false
    )
}
