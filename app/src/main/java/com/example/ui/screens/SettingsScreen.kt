package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.export.BackupPayload
import com.example.data.export.ExportBackupManager
import com.example.data.model.ThemeMode
import com.example.ui.viewmodel.MainUiState
import com.example.util.ArabicDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenCategories: () -> Unit,
    onGenerateBackupJson: suspend () -> String,
    onParseBackup: (String) -> Result<BackupPayload>,
    onRestoreBackup: (BackupPayload) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = onGenerateBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("تعذر فتح الملف للحفظ")
                    }
                }.onSuccess {
                    onShowMessage("تم حفظ النسخة الاحتياطية بنجاح")
                }.onFailure { err ->
                    onShowMessage(err.message ?: "فشل إنشاء النسخة الاحتياطية")
                }
            }
        }
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: error("تعذر قراءة الملف المحدد")
                    }
                    onParseBackup(content).getOrThrow()
                }.onSuccess { payload ->
                    pendingRestorePayload = payload
                }.onFailure { err ->
                    onShowMessage(err.message ?: "ملف النسخة الاحتياطية غير صالح")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "الإعدادات",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Theme Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = "المظهر",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مظهر التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size
                                ),
                                modifier = Modifier.testTag("theme_button_${mode.name}")
                            ) {
                                Text(mode.labelAr)
                            }
                        }
                    }
                }
            }
        }

        // Budgets & Categories Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsActionRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "الميزانيات الشهرية",
                        subtitle = if (uiState.budgetProgressList.isEmpty()) {
                            "تحديد حد شهري وتنبيهات لكل تصنيف"
                        } else {
                            "${uiState.budgetProgressList.size} ميزانية نشطة • ${uiState.currentMonthAlerts.size} تنبيهات"
                        },
                        onClick = onOpenBudgets,
                        testTag = "settings_budgets_row"
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Category,
                        title = "إدارة التصنيفات",
                        subtitle = "${uiState.categories.size} تصنيف للمصاريف والدخل",
                        onClick = onOpenCategories,
                        testTag = "settings_categories_row"
                    )
                }
            }
        }

        // Export & Backup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsActionRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "تصدير الحركات (CSV)",
                        subtitle = "مشاركة جدول الحركات بصيغة متوافقة مع Excel",
                        onClick = {
                            if (uiState.allTransactions.isEmpty()) {
                                onShowMessage("لا توجد حركات لتصديرها")
                                return@SettingsActionRow
                            }
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        ExportBackupManager.writeCsvToCacheAndGetUri(
                                            context = context,
                                            transactions = uiState.allTransactions,
                                            categoriesById = uiState.categoriesById
                                        )
                                    }
                                }.onSuccess { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "حركات مصروفي CSV")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "مشاركة ملف CSV")
                                    )
                                }.onFailure { err ->
                                    onShowMessage(err.message ?: "تعذر تصدير ملف CSV")
                                }
                            }
                        },
                        testTag = "settings_export_csv_row"
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Backup,
                        title = "نسخ احتياطي كامل (JSON)",
                        subtitle = "حفظ جميع الحركات والتصنيفات والميزانيات في ملف واحد",
                        onClick = {
                            val dateStamp = ArabicDateUtils.formatShortDate(System.currentTimeMillis())
                                .replace("/", "-")
                            createBackupLauncher.launch("masroofy_backup_$dateStamp.json")
                        },
                        testTag = "settings_backup_json_row"
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Restore,
                        title = "استعادة من نسخة احتياطية",
                        subtitle = "استيراد ملف JSON واستبدال البيانات الحالية",
                        onClick = {
                            openRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        testTag = "settings_restore_json_row"
                    )
                }
            }
        }

        // About Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "حول التطبيق",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "حول تطبيق مصروفي",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "الإصدار ${BuildConfig.VERSION_NAME} — دفتر حسابات شخصي بالليرة السورية (ل.س).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "يعمل التطبيق بدون إنترنت بالكامل، وتُحفظ بياناتك على جهازك فقط دون أي حسابات أو خوادم خارجية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val restoreCandidate = pendingRestorePayload
    if (restoreCandidate != null) {
        AlertDialog(
            onDismissRequest = { pendingRestorePayload = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية") },
            text = {
                Text(
                    "سيتم استبدال جميع البيانات الحالية بالنسخة الاحتياطية التي تحتوي على:\n" +
                        "• ${restoreCandidate.transactions.size} حركة\n" +
                        "• ${restoreCandidate.categories.size} تصنيف\n" +
                        "• ${restoreCandidate.budgets.size} ميزانية\n\n" +
                        "هل تريد المتابعة؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(restoreCandidate)
                        pendingRestorePayload = null
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("استعادة واستبدال")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestorePayload = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
