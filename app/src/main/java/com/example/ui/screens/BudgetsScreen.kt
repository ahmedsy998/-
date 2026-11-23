package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.BudgetEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LocalMasroofyColors
import com.example.ui.viewmodel.MainUiState
import com.example.util.ArabicDateUtils
import com.example.util.BudgetProgress
import com.example.util.SypFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    uiState: MainUiState,
    onShiftMonth: (Int) -> Unit,
    onSaveBudget: (categoryId: Long, limitAmount: Long) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val semanticColors = LocalMasroofyColors.current
    val expenseCategories = remember(uiState.categories) {
        uiState.categories.filter { it.type == TransactionType.EXPENSE }
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingProgress by remember { mutableStateOf<BudgetProgress?>(null) }
    var pendingDeleteBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الميزانيات الشهرية") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProgress = null
                    showBudgetDialog = true
                },
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "إضافة ميزانية"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "تفعيل الإشعارات",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تفعيل تنبيهات الميزانية",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "اسمح بالإشعارات ليصلك تنبيه عند 80% و100% من الميزانية.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(
                                onClick = {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            ) {
                                Text("تفعيل")
                            }
                        }
                    }
                }
            }

            // Month Navigator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onShiftMonth(-1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "الشهر السابق"
                            )
                        }
                        Text(
                            text = ArabicDateUtils.formatMonthYearLabel(uiState.selectedBudgetMonth),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { onShiftMonth(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "الشهر التالي"
                            )
                        }
                    }
                }
            }

            if (uiState.budgetProgressList.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "لا توجد ميزانيات لهذا الشهر",
                        subtitle = "حدد سقفاً شهرياً لأي تصنيف مصاريف وسيقوم مصروفي بتنبيهك عند 80% و100%.",
                        actionLabel = "إضافة ميزانية",
                        onAction = {
                            editingProgress = null
                            showBudgetDialog = true
                        }
                    )
                }
            } else {
                items(
                    items = uiState.budgetProgressList,
                    key = { "budget_${it.budget.id}" }
                ) { progress ->
                    val statusColor = when {
                        progress.percentageInt >= 100 -> semanticColors.expense
                        progress.percentageInt >= 80 -> semanticColors.warning
                        else -> semanticColors.income
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_card_${progress.budget.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryBadgeIcon(category = progress.category, size = 40.dp, iconSize = 20.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = progress.category.nameAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "المصروف ${SypFormatter.format(progress.spentAmount)} من ${SypFormatter.format(progress.budget.limitAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingProgress = progress
                                        showBudgetDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "تعديل الميزانية"
                                    )
                                }
                                IconButton(
                                    onClick = { pendingDeleteBudget = progress.budget }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "حذف الميزانية",
                                        tint = semanticColors.expense
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { (progress.percentageInt / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${progress.percentageInt}% مستهلك",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor
                                )
                                val remText = if (progress.remainingAmount >= 0L) {
                                    "المتبقي: ${SypFormatter.format(progress.remainingAmount)}"
                                } else {
                                    "تجاوز بـ: ${SypFormatter.format(-progress.remainingAmount)}"
                                }
                                Text(
                                    text = remText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        val initialCatId = editingProgress?.category?.id ?: expenseCategories.firstOrNull()?.id
        var selectedCatId by remember(editingProgress) { mutableStateOf(initialCatId) }
        var limitText by remember(editingProgress) {
            mutableStateOf(editingProgress?.budget?.limitAmount?.toString() ?: "")
        }
        var amountError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(if (editingProgress != null) "تعديل الميزانية" else "إضافة ميزانية شهرية")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (editingProgress == null && expenseCategories.isNotEmpty()) {
                        Text(
                            text = "اختر التصنيف",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            items(expenseCategories) { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCatId = cat.id }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedCatId == cat.id,
                                        onClick = { selectedCatId = cat.id }
                                    )
                                    Text(
                                        text = cat.nameAr,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else if (editingProgress != null) {
                        Text(
                            text = "التصنيف: ${editingProgress!!.category.nameAr}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    OutlinedTextField(
                        value = limitText,
                        onValueChange = {
                            limitText = SypFormatter.normalizeDigits(it).take(13)
                            amountError = null
                        },
                        label = { Text("الحد الشهري (ل.س)") },
                        singleLine = true,
                        isError = amountError != null,
                        supportingText = amountError?.let { msg -> { Text(msg) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_limit_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = SypFormatter.parsePositiveWholeAmount(limitText)
                        val catId = selectedCatId
                        if (parsed == null) {
                            amountError = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                            return@Button
                        }
                        if (catId != null) {
                            onSaveBudget(catId, parsed)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showBudgetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_budget_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val toDelete = pendingDeleteBudget
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteBudget = null },
            title = { Text("حذف الميزانية") },
            text = { Text("هل أنت متأكد من حذف هذه الميزانية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(toDelete)
                        pendingDeleteBudget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semanticColors.expense,
                        contentColor = Color.White
                    )
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBudget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
