package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.ExportBackupManager
import com.example.data.local.MasroofyDatabase
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MasroofyRepository
import com.example.notification.BudgetNotifier
import com.example.ui.navigation.MasroofyRoot
import com.example.ui.theme.MasroofyTheme
import com.example.ui.viewmodel.AddEditTransactionViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ArabicDateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: MasroofyDatabase
    private val firedNotifications = mutableListOf<Triple<String, Int, Long>>()

    private val fakeNotifier = object : BudgetNotifier {
        override fun notifyThreshold(
            budgetId: Long,
            categoryNameAr: String,
            thresholdPercent: Int,
            spentAmount: Long,
            limitAmount: Long,
            actualPercent: Int
        ) {
            firedNotifications.add(Triple(categoryNameAr, thresholdPercent, spentAmount))
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        firedNotifications.clear()
        db = Room.inMemoryDatabaseBuilder(context, MasroofyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appNameAndPermissions_complyWithOfflineNoMicNoAccountPolicy() {
        assertEquals("مصروفي", context.getString(R.string.app_name))
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
        assertFalse(perms.contains(Manifest.permission.INTERNET))
        assertFalse(perms.contains(Manifest.permission.RECORD_AUDIO))
        assertFalse(perms.contains(Manifest.permission.GET_ACCOUNTS))
        assertTrue(perms.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun budgetNotifications_fireAt80And100PercentOffline() {
        runBlocking {
            val repo = MasroofyRepository(db, SettingsDataStore(context), fakeNotifier)
            repo.ensureDefaultCategories()
            val foodCat = db.categoryDao().getAllOnce().first { it.nameAr == "طعام وشراب" }
            val now = System.currentTimeMillis()
            val monthYear = ArabicDateUtils.currentMonthYear(now)

            repo.saveBudget(foodCat.id, monthYear, 100_000L)
            // 70% -> no notification
            repo.saveTransaction(
                type = TransactionType.EXPENSE,
                amount = 70_000L,
                categoryId = foodCat.id,
                dateTime = now,
                paymentMethod = PaymentMethod.CASH,
                note = "تسوّق",
                recurringRule = RecurringRule.NONE,
                nowMillis = now
            )
            assertEquals(0, firedNotifications.size)

            // +15% = 85% -> fires 80% notification
            repo.saveTransaction(
                type = TransactionType.EXPENSE,
                amount = 15_000L,
                categoryId = foodCat.id,
                dateTime = now,
                paymentMethod = PaymentMethod.CASH,
                note = "غداء",
                recurringRule = RecurringRule.NONE,
                nowMillis = now
            )
            assertEquals(1, firedNotifications.size)
            assertEquals(80, firedNotifications[0].second)

            // +20% = 105% -> fires 100% notification
            repo.saveTransaction(
                type = TransactionType.EXPENSE,
                amount = 20_000L,
                categoryId = foodCat.id,
                dateTime = now,
                paymentMethod = PaymentMethod.CASH,
                note = "عشاء",
                recurringRule = RecurringRule.NONE,
                nowMillis = now
            )
            assertEquals(2, firedNotifications.size)
            assertEquals(100, firedNotifications[1].second)
        }
    }

    @Test
    fun recurringTransaction_autoCreatesDueOccurrences() {
        runBlocking {
            val zone = ZoneId.systemDefault()
            val repo = MasroofyRepository(db, SettingsDataStore(context), fakeNotifier, zone)
            repo.ensureDefaultCategories()
            val netCat = db.categoryDao().getAllOnce().first { it.nameAr == "إنترنت وهاتف" }

            val now = System.currentTimeMillis()
            val twoWeeksAgo = now - (15L * 24L * 60L * 60L * 1000L)

            repo.saveTransaction(
                type = TransactionType.EXPENSE,
                amount = 45_000L,
                categoryId = netCat.id,
                dateTime = twoWeeksAgo,
                paymentMethod = PaymentMethod.CASH,
                note = "اشتراك أسبوعي",
                recurringRule = RecurringRule.WEEKLY,
                nowMillis = now
            )

            val allTxs = db.transactionDao().getAllOnce()
            // Original (day -15) + week 1 (day -8) + week 2 (day -1) = 3 occurrences
            assertEquals(3, allTxs.size)
        }
    }

    @Test
    fun jsonBackupAndRestore_roundTripsAllDataAtomically() {
        runBlocking {
            val repo = MasroofyRepository(db, SettingsDataStore(context), fakeNotifier)
            repo.ensureDefaultCategories()
            val foodCat = db.categoryDao().getAllOnce().first { it.nameAr == "طعام وشراب" }
            val now = System.currentTimeMillis()
            val monthYear = ArabicDateUtils.currentMonthYear(now)

            repo.saveCustomCategory(nameAr = "تصنيف تجريبي", type = TransactionType.EXPENSE, icon = "coffee", color = 0xFF6D4C41)
            repo.saveBudget(foodCat.id, monthYear, 250_000L)
            repo.saveTransaction(
                type = TransactionType.EXPENSE,
                amount = 95_000L,
                categoryId = foodCat.id,
                dateTime = now,
                paymentMethod = PaymentMethod.CARD,
                note = "نسخة احتياطية",
                recurringRule = RecurringRule.MONTHLY,
                nowMillis = now
            )

            val json = repo.exportBackupJson()
            val payload = ExportBackupManager.parseBackupJson(json)
            assertTrue(payload.categories.any { it.nameAr == "تصنيف تجريبي" })
            assertEquals(1, payload.transactions.size)
            assertEquals(1, payload.budgets.size)

            // Wipe and restore
            db.budgetDao().deleteAll()
            db.transactionDao().deleteAll()
            db.categoryDao().deleteAll()
            assertEquals(0, db.transactionDao().getAllOnce().size)

            repo.restoreFromBackup(payload)
            val restoredTxs = db.transactionDao().getAllOnce()
            assertEquals(1, restoredTxs.size)
            assertEquals(95_000L, restoredTxs.first().amount)
            assertEquals("نسخة احتياطية", restoredTxs.first().note)
            assertEquals(RecurringRule.MONTHLY, restoredTxs.first().recurringRule)
            assertEquals(1, db.budgetDao().getAllOnce().size)
        }
    }

    @Test
    fun dataPersistsAcrossDatabaseCloseAndReopen() {
        runBlocking {
            val dbName = "persist_test_masroofy.db"
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) dbFile.delete()

            val diskDb1 = Room.databaseBuilder(context, MasroofyDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val repo1 = MasroofyRepository(diskDb1, SettingsDataStore(context), fakeNotifier)
            repo1.ensureDefaultCategories()
            val foodCat = diskDb1.categoryDao().getAllOnce().first { it.nameAr == "طعام وشراب" }
            repo1.saveTransaction(
                type = TransactionType.EXPENSE,
                amount = 55_000L,
                categoryId = foodCat.id,
                dateTime = System.currentTimeMillis(),
                paymentMethod = PaymentMethod.CASH,
                note = "حفظ دائم",
                recurringRule = RecurringRule.NONE
            )
            diskDb1.close()

            val diskDb2 = Room.databaseBuilder(context, MasroofyDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val reopenedTxs = diskDb2.transactionDao().getAllOnce()
            assertEquals(1, reopenedTxs.size)
            assertEquals(55_000L, reopenedTxs.first().amount)
            assertEquals("حفظ دائم", reopenedTxs.first().note)
            diskDb2.close()
            dbFile.delete()
        }
    }

    @Test
    fun composeAddTransaction_updatesDashboardTotalsImmediately() {
        val repo = MasroofyRepository(db, SettingsDataStore(context), fakeNotifier)
        val mainVm = MainViewModel(repo)
        val addVm = AddEditTransactionViewModel(repo)

        composeRule.setContent {
            MasroofyTheme {
                MasroofyRoot(
                    mainViewModel = mainVm,
                    addEditViewModel = addVm
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate to Add Transaction via center FAB
        composeRule.onNodeWithTag("center_add_fab").performClick()
        composeRule.waitForIdle()

        // Enter 125,000 SYP and note, scroll to Save button, then save
        composeRule.onNodeWithTag("amount_input").performScrollTo().performTextInput("125000")
        composeRule.onNodeWithTag("note_input").performScrollTo().performTextInput("غداء اختبار")
        composeRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("dashboard_balance_value").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Verify dashboard shows updated totals immediately
        composeRule.onNodeWithTag("dashboard_balance_value")
            .assertIsDisplayed()
            .assertTextEquals("-125,000 ل.س")
        composeRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("dashboard_today_expense"))
        composeRule.onNodeWithTag("dashboard_today_expense")
            .assertIsDisplayed()
            .assertTextEquals("125,000 ل.س")
        runBlocking {
            val saved = repo.transactions.first()
            assertEquals(1, saved.size)
            assertEquals(125_000L, saved.first().amount)
        }
    }
}
