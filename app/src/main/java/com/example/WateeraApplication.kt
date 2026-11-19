package com.example

import android.app.Application
import com.example.data.HabitRepository
import com.example.data.local.SettingsDataStore
import com.example.data.local.WateeraDatabase
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler

class WateeraApplication : Application() {
    val database: WateeraDatabase by lazy { WateeraDatabase.getInstance(this) }
    val settingsStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }
    val habitRepository: HabitRepository by lazy {
        HabitRepository(
            context = this,
            habitDao = database.habitDao(),
            settingsStore = settingsStore,
            reminderScheduler = reminderScheduler
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
