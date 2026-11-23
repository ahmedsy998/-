package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.util.ArabicDateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.ZoneId

data class BackupPayload(
    val version: Int,
    val exportedAt: Long,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>
)

object ExportBackupManager {

    private const val BACKUP_VERSION = 1

    fun buildCsvContent(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM for proper Arabic rendering in Excel and mobile spreadsheet apps
        sb.append('\uFEFF')
        sb.append("المعرف,النوع,المبلغ (ل.س),التصنيف,التاريخ والوقت,طريقة الدفع,التكرار,ملاحظة\n")
        for (tx in transactions.sortedByDescending { it.dateTime }) {
            val catName = categoriesById[tx.categoryId]?.nameAr ?: "غير معروف"
            val row = listOf(
                tx.id.toString(),
                tx.type.labelAr,
                tx.amount.toString(),
                catName,
                ArabicDateUtils.formatIsoDateTime(tx.dateTime, zoneId),
                tx.paymentMethod.labelAr,
                tx.recurringRule.labelAr,
                tx.note
            ).joinToString(",") { escapeCsv(it) }
            sb.append(row).append('\n')
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun writeCsvToCacheAndGetUri(
        context: Context,
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>
    ): Uri {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = ArabicDateUtils.formatIsoDateTime(System.currentTimeMillis())
            .replace(":", "-")
            .replace(" ", "_")
        val file = File(exportsDir, "masroofy_transactions_$timestamp.csv")
        val content = buildCsvContent(transactions, categoriesById)
        file.writeText(content, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildBackupJson(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("app", "Masroofy")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", exportedAt)

        val catsJson = JSONArray()
        for (cat in categories) {
            catsJson.put(
                JSONObject().apply {
                    put("id", cat.id)
                    put("nameAr", cat.nameAr)
                    put("type", cat.type.name)
                    put("icon", cat.icon)
                    put("color", cat.color)
                    put("isCustom", cat.isCustom)
                }
            )
        }
        root.put("categories", catsJson)

        val txsJson = JSONArray()
        for (tx in transactions) {
            txsJson.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type.name)
                    put("amount", tx.amount)
                    put("categoryId", tx.categoryId)
                    put("dateTime", tx.dateTime)
                    put("paymentMethod", tx.paymentMethod.name)
                    put("note", tx.note)
                    put("recurringRule", tx.recurringRule.name)
                    put("recurringProcessed", tx.recurringProcessed)
                    put("createdAt", tx.createdAt)
                }
            )
        }
        root.put("transactions", txsJson)

        val budgetsJson = JSONArray()
        for (budget in budgets) {
            budgetsJson.put(
                JSONObject().apply {
                    put("id", budget.id)
                    put("categoryId", budget.categoryId)
                    put("monthYear", budget.monthYear)
                    put("limitAmount", budget.limitAmount)
                    put("notified80", budget.notified80)
                    put("notified100", budget.notified100)
                }
            )
        }
        root.put("budgets", budgetsJson)

        return root.toString(2)
    }

    fun parseBackupJson(rawJson: String): BackupPayload {
        val root = try {
            JSONObject(rawJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح", e)
        }

        if (root.optString("app") != "Masroofy") {
            throw IllegalArgumentException("هذا الملف لا يخص تطبيق مصروفي")
        }

        val version = root.optInt("version", 0)
        if (version < 1) {
            throw IllegalArgumentException("إصدار النسخة الاحتياطية غير مدعوم")
        }

        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

        val catsArr = root.optJSONArray("categories")
            ?: throw IllegalArgumentException("النسخة الاحتياطية لا تحتوي على التصنيفات")
        val categories = buildList {
            for (i in 0 until catsArr.length()) {
                val obj = catsArr.getJSONObject(i)
                add(
                    CategoryEntity(
                        id = obj.getLong("id"),
                        nameAr = obj.getString("nameAr"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        icon = obj.getString("icon"),
                        color = obj.getLong("color"),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
        }

        val txsArr = root.optJSONArray("transactions")
            ?: throw IllegalArgumentException("النسخة الاحتياطية لا تحتوي على الحركات")
        val transactions = buildList {
            for (i in 0 until txsArr.length()) {
                val obj = txsArr.getJSONObject(i)
                add(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        amount = obj.getLong("amount"),
                        categoryId = obj.getLong("categoryId"),
                        dateTime = obj.getLong("dateTime"),
                        paymentMethod = PaymentMethod.valueOf(obj.getString("paymentMethod")),
                        note = obj.optString("note", ""),
                        recurringRule = RecurringRule.valueOf(obj.optString("recurringRule", RecurringRule.NONE.name)),
                        recurringProcessed = obj.optBoolean("recurringProcessed", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val budgetsArr = root.optJSONArray("budgets") ?: JSONArray()
        val budgets = buildList {
            for (i in 0 until budgetsArr.length()) {
                val obj = budgetsArr.getJSONObject(i)
                add(
                    BudgetEntity(
                        id = obj.getLong("id"),
                        categoryId = obj.getLong("categoryId"),
                        monthYear = obj.getString("monthYear"),
                        limitAmount = obj.getLong("limitAmount"),
                        notified80 = obj.optBoolean("notified80", false),
                        notified100 = obj.optBoolean("notified100", false)
                    )
                )
            }
        }

        if (categories.isEmpty()) {
            throw IllegalArgumentException("قائمة التصنيفات فارغة في الملف")
        }

        return BackupPayload(
            version = version,
            exportedAt = exportedAt,
            categories = categories,
            transactions = transactions,
            budgets = budgets
        )
    }
}
