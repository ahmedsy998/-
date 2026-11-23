package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalMasroofyColors
import com.example.util.CategoryTotal
import com.example.util.DailyBarPoint
import com.example.util.SypFormatter
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun ExpenseDonutChart(
    items: List<CategoryTotal>,
    totalExpense: Long,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || totalExpense <= 0L) return

    val slices = items.map { item ->
        val sweep = (item.totalAmount.toFloat() / totalExpense.toFloat()) * 360f
        item to sweep
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .testTag("dashboard_donut_chart"),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier
                    .size(196.dp)
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = hypot(dx, dy)
                            val outerRadius = minOf(size.width, size.height) / 2f
                            val innerRadius = outerRadius * 0.48f
                            if (dist in innerRadius..outerRadius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                angle = (angle + 90f + 360f) % 360f
                                var acc = 0f
                                for ((item, sweep) in slices) {
                                    if (angle >= acc && angle <= acc + sweep) {
                                        onCategorySelected(item.category.id)
                                        break
                                    }
                                    acc += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = 34.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                var startAngle = -90f
                for ((item, sweep) in slices) {
                    val gap = if (slices.size > 1) 2f else 0f
                    val drawSweep = (sweep - gap).coerceAtLeast(1f)
                    drawArc(
                        color = Color(item.category.color),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "مصاريف الشهر",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SypFormatter.formatNumberOnly(totalExpense),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ل.س",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCategorySelected(item.category.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("donut_legend_item_${item.category.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(item.category.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category.nameAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.percentageInt}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = SypFormatter.format(item.totalAmount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun Last7DaysBarChart(
    days: List<DailyBarPoint>,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val maxAmount = days.maxOf { it.expenseAmount }.coerceAtLeast(1L)
    val today = LocalDate.now()
    val expenseColor = LocalMasroofyColors.current.expense
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_7day_bar_chart")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { point ->
                val isToday = point.date == today
                val fraction = if (point.expenseAmount <= 0L) 0f else {
                    (point.expenseAmount.toFloat() / maxAmount.toFloat()).coerceIn(0.06f, 1f)
                }
                val barColor = if (isToday) primaryColor else expenseColor.copy(alpha = 0.78f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDaySelected(point.date) }
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                        .testTag("bar_day_${point.date}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (point.expenseAmount > 0L) {
                        Text(
                            text = compactAmountLabel(point.expenseAmount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxHeight(fraction.coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(
                                    if (point.expenseAmount > 0L) barColor
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isToday) "اليوم" else point.labelAr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isToday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun compactAmountLabel(amount: Long): String {
    return when {
        amount >= 1_000_000L -> "${amount / 1_000_000L}M"
        amount >= 1_000L -> "${amount / 1_000L}k"
        else -> amount.toString()
    }
}
