package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.LocalMasroofyColors
import com.example.ui.viewmodel.MainUiState
import com.example.util.ArabicDateUtils
import com.example.util.SypFormatter

@Composable
fun ReportsScreen(
    uiState: MainUiState,
    onSetMonthly: (Boolean) -> Unit,
    onShiftPeriod: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        LoadingStateView(modifier = modifier)
        return
    }
    if (uiState.errorMessage != null && uiState.periodReport == null) {
        ErrorStateView(
            message = uiState.errorMessage,
            onRetry = onRetry,
            modifier = modifier
        )
        return
    }

    val report = uiState.periodReport ?: return
    val semanticColors = LocalMasroofyColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "التقارير المالية",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Weekly / Monthly Toggle
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !uiState.reportIsMonthly,
                    onClick = { onSetMonthly(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("report_weekly_tab")
                ) {
                    Text("تقرير أسبوعي")
                }
                SegmentedButton(
                    selected = uiState.reportIsMonthly,
                    onClick = { onSetMonthly(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("report_monthly_tab")
                ) {
                    Text("تقرير شهري")
                }
            }
        }

        // Period Navigator
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
                    IconButton(onClick = { onShiftPeriod(-1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "الفترة السابقة"
                        )
                    }
                    Text(
                        text = report.periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { onShiftPeriod(1) },
                        enabled = uiState.reportPeriodOffset < 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "الفترة التالية"
                        )
                    }
                }
            }
        }

        // Period Summary & Comparison
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = semanticColors.expenseContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "إجمالي المصاريف",
                            style = MaterialTheme.typography.labelLarge,
                            color = semanticColors.onExpenseContainer
                        )
                        Text(
                            text = SypFormatter.format(report.totalExpense),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = semanticColors.onExpenseContainer
                        )
                        ComparisonBadge(
                            changePercent = report.expenseChangePercent,
                            positiveIsBad = true
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = semanticColors.incomeContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "إجمالي الدخل",
                            style = MaterialTheme.typography.labelLarge,
                            color = semanticColors.onIncomeContainer
                        )
                        Text(
                            text = SypFormatter.format(report.totalIncome),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = semanticColors.onIncomeContainer
                        )
                        ComparisonBadge(
                            changePercent = report.incomeChangePercent,
                            positiveIsBad = false
                        )
                    }
                }
            }
        }

        // Net Period Card
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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "صافي الفترة",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "مقارنة بالفترة السابقة (${SypFormatter.format(report.previousExpense)} مصاريف)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = SypFormatter.format(report.netAmount, showPlusSign = true),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (report.netAmount >= 0L) semanticColors.income else semanticColors.expense
                    )
                }
            }
        }

        // Biggest Single Expense
        val biggest = report.biggestExpense
        if (biggest != null) {
            val cat = uiState.categoriesById[biggest.categoryId]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "أكبر مصروف",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أكبر مصروف منفرد",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "${cat?.nameAr ?: "مصروف"}${if (biggest.note.isNotBlank()) " — ${biggest.note}" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = ArabicDateUtils.formatDateTimeDisplay(biggest.dateTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = SypFormatter.format(biggest.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Category Breakdown
        if (report.expenseByCategory.isEmpty() && report.incomeByCategory.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Assessment,
                    title = "لا توجد حركات في هذه الفترة",
                    subtitle = "جرّب الانتقال إلى فترة سابقة أو إضافة حركات جديدة."
                )
            }
        } else {
            if (report.expenseByCategory.isNotEmpty()) {
                item {
                    Text(
                        text = "تفاصيل المصاريف حسب التصنيف",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(report.expenseByCategory, key = { "rep_exp_${it.category.id}" }) { item ->
                    CategoryBreakdownRow(
                        category = item.category,
                        amount = item.totalAmount,
                        percentageInt = item.percentageInt,
                        count = item.transactionCount
                    )
                }
            }

            if (report.incomeByCategory.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تفاصيل الدخل حسب التصنيف",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(report.incomeByCategory, key = { "rep_inc_${it.category.id}" }) { item ->
                    CategoryBreakdownRow(
                        category = item.category,
                        amount = item.totalAmount,
                        percentageInt = item.percentageInt,
                        count = item.transactionCount
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonBadge(
    changePercent: Long?,
    positiveIsBad: Boolean
) {
    if (changePercent == null) {
        Text(
            text = "لا مقارنة سابقة",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val icon = if (changePercent >= 0L) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    val sign = if (changePercent > 0L) "+" else ""
    Surface(
        color = Color.Black.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "$sign$changePercent% عن السابق",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    category: com.example.data.local.entity.CategoryEntity,
    amount: Long,
    percentageInt: Int,
    count: Int
) {
    val catColor = Color(category.color)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadgeIcon(category = category, size = 38.dp, iconSize = 20.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.nameAr,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$count حركات • $percentageInt%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = SypFormatter.format(amount),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LinearProgressIndicator(
                progress = { (percentageInt / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = catColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
