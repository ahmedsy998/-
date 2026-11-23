package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.DateRangePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.LocalMasroofyColors
import com.example.ui.viewmodel.MainUiState
import com.example.util.SypFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onPaymentMethodFilterChange: (PaymentMethod?) -> Unit,
    onDateRangePresetChange: (DateRangePreset) -> Unit,
    onCustomDateRangeSelected: (Long?, Long?) -> Unit,
    onClearFilters: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onAddTransactionClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        LoadingStateView(modifier = modifier)
        return
    }
    if (uiState.errorMessage != null && uiState.allTransactions.isEmpty()) {
        ErrorStateView(
            message = uiState.errorMessage,
            onRetry = onRetry,
            modifier = modifier
        )
        return
    }

    val filter = uiState.transactionFilter
    val semanticColors = LocalMasroofyColors.current

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showPaymentPicker by remember { mutableStateOf(false) }
    var showCustomRangePicker by remember { mutableStateOf(false) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Screen Header + Search
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الحركات",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (filter.hasActiveFilters) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("clear_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterListOff,
                            contentDescription = "مسح الفلاتر",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح الفلاتر")
                    }
                }
            }

            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input"),
                placeholder = { Text("ابحث بالملاحظة أو اسم التصنيف...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "بحث"
                    )
                },
                trailingIcon = {
                    if (filter.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row 1: Type, Category, Payment Method
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.type == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text("الكل") },
                modifier = Modifier.testTag("filter_type_all")
            )
            FilterChip(
                selected = filter.type == TransactionType.EXPENSE,
                onClick = {
                    onTypeFilterChange(if (filter.type == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                },
                label = { Text("مصاريف") },
                modifier = Modifier.testTag("filter_type_expense")
            )
            FilterChip(
                selected = filter.type == TransactionType.INCOME,
                onClick = {
                    onTypeFilterChange(if (filter.type == TransactionType.INCOME) null else TransactionType.INCOME)
                },
                label = { Text("دخل") },
                modifier = Modifier.testTag("filter_type_income")
            )

            val selectedCatName = filter.categoryId?.let { uiState.categoriesById[it]?.nameAr }
            FilterChip(
                selected = filter.categoryId != null,
                onClick = { showCategoryPicker = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(selectedCatName ?: "التصنيف") },
                modifier = Modifier.testTag("filter_category_button")
            )

            FilterChip(
                selected = filter.paymentMethod != null,
                onClick = { showPaymentPicker = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(filter.paymentMethod?.labelAr ?: "طريقة الدفع") },
                modifier = Modifier.testTag("filter_payment_button")
            )
        }

        // Filter Chips Row 2: Date Range Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateRangePreset.entries.forEach { preset ->
                FilterChip(
                    selected = filter.dateRangePreset == preset,
                    onClick = {
                        if (preset == DateRangePreset.CUSTOM) {
                            showCustomRangePicker = true
                        } else {
                            onDateRangePresetChange(preset)
                        }
                    },
                    label = { Text(preset.labelAr) },
                    modifier = Modifier.testTag("filter_date_${preset.name}")
                )
            }
        }

        // Totals Summary Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "النتائج: ${uiState.filteredTransactions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "دخل: ${SypFormatter.format(uiState.filteredTotalIncome)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = semanticColors.income
                    )
                    Text(
                        text = "مصروف: ${SypFormatter.format(uiState.filteredTotalExpense)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = semanticColors.expense
                    )
                }
            }
        }

        // Grouped Transactions List
        if (uiState.groupedTransactions.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Rounded.ReceiptLong,
                    title = if (uiState.allTransactions.isEmpty()) "لا توجد حركات مسجلة" else "لا توجد نتائج مطابقة",
                    subtitle = if (uiState.allTransactions.isEmpty()) {
                        "أضف أول حركة لتبدأ بتتبع مصاريفك ودخلك."
                    } else {
                        "جرّب تعديل كلمات البحث أو إزالة بعض الفلاتر."
                    },
                    actionLabel = if (uiState.allTransactions.isEmpty()) "إضافة حركة" else "مسح الفلاتر",
                    onAction = if (uiState.allTransactions.isEmpty()) onAddTransactionClick else onClearFilters
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transactions_grouped_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.groupedTransactions.forEach { group ->
                    item(key = "header_${group.date}") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.headerLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val dailyColor = if (group.netAmount >= 0L) semanticColors.income else semanticColors.expense
                                Text(
                                    text = SypFormatter.format(group.netAmount, showPlusSign = true),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = dailyColor
                                )
                            }
                        }
                    }

                    items(
                        items = group.transactions,
                        key = { "tx_${it.id}" }
                    ) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            category = uiState.categoriesById[tx.categoryId],
                            onClick = { onEditTransaction(tx.id) },
                            modifier = Modifier.testTag("tx_item_${tx.id}"),
                            trailingExtra = {
                                IconButton(
                                    onClick = { pendingDeleteTx = tx },
                                    modifier = Modifier.testTag("tx_delete_${tx.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "حذف الحركة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Category Filter Dialog
    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("تصفية حسب التصنيف") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filter.categoryId == null,
                                onClick = {
                                    onCategoryFilterChange(null)
                                    showCategoryPicker = false
                                }
                            )
                            TextButton(
                                onClick = {
                                    onCategoryFilterChange(null)
                                    showCategoryPicker = false
                                }
                            ) {
                                Text("جميع التصنيفات")
                            }
                        }
                    }
                    items(uiState.categories) { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filter.categoryId == cat.id,
                                onClick = {
                                    onCategoryFilterChange(cat.id)
                                    showCategoryPicker = false
                                }
                            )
                            TextButton(
                                onClick = {
                                    onCategoryFilterChange(cat.id)
                                    showCategoryPicker = false
                                }
                            ) {
                                Text("${cat.nameAr} (${cat.type.labelAr})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Payment Method Filter Dialog
    if (showPaymentPicker) {
        AlertDialog(
            onDismissRequest = { showPaymentPicker = false },
            title = { Text("تصفية حسب طريقة الدفع") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = filter.paymentMethod == null,
                            onClick = {
                                onPaymentMethodFilterChange(null)
                                showPaymentPicker = false
                            }
                        )
                        TextButton(
                            onClick = {
                                onPaymentMethodFilterChange(null)
                                showPaymentPicker = false
                            }
                        ) {
                            Text("الكل")
                        }
                    }
                    PaymentMethod.entries.forEach { method ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = filter.paymentMethod == method,
                                onClick = {
                                    onPaymentMethodFilterChange(method)
                                    showPaymentPicker = false
                                }
                            )
                            TextButton(
                                onClick = {
                                    onPaymentMethodFilterChange(method)
                                    showPaymentPicker = false
                                }
                            ) {
                                Text(method.labelAr)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Custom Date Range Picker Dialog
    if (showCustomRangePicker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startUtc = state.selectedStartDateMillis
                        val endUtc = state.selectedEndDateMillis ?: startUtc
                        val startLocal = startUtc?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        val endLocal = endUtc?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        if (startLocal != null) {
                            onCustomDateRangeSelected(startLocal, endLocal)
                        }
                        showCustomRangePicker = false
                    }
                ) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DateRangePicker(
                state = state,
                modifier = Modifier.height(420.dp)
            )
        }
    }

    // Delete Confirmation Dialog
    val txToDelete = pendingDeleteTx
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteTx = null },
            title = { Text("تأكيد الحذف") },
            text = {
                Text(
                    "هل تريد حذف حركة بقيمة ${SypFormatter.format(txToDelete.amount)}؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(txToDelete)
                        pendingDeleteTx = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semanticColors.expense,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTx = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
