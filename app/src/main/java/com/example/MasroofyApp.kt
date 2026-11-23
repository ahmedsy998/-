package com.example

import android.app.Application
import com.example.data.local.MasroofyDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MasroofyRepository
import com.example.notification.BudgetNotificationHelper

class MasroofyApp : Application() {
    lateinit var database: MasroofyDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var notificationHelper: BudgetNotificationHelper
        private set
    lateinit var repository: MasroofyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = MasroofyDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        notificationHelper = BudgetNotificationHelper(this).apply {
            ensureChannel()
        }
        repository = MasroofyRepository(
            database = database,
            settingsDataStore = settingsDataStore,
            budgetNotifier = notificationHelper
        )
    }
}
