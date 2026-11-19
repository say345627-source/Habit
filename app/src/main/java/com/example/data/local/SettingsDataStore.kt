package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "wateera_settings")

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.AR,
    val globalRemindersEnabled: Boolean = true,
    val endOfDayReminderEnabled: Boolean = false,
    val endOfDayHour: Int = 21,
    val endOfDayMinute: Int = 0
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val GLOBAL_REMINDERS = booleanPreferencesKey("global_reminders")
        val EOD_ENABLED = booleanPreferencesKey("eod_reminder_enabled")
        val EOD_HOUR = intPreferencesKey("eod_reminder_hour")
        val EOD_MINUTE = intPreferencesKey("eod_reminder_minute")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.AR,
            globalRemindersEnabled = prefs[Keys.GLOBAL_REMINDERS] ?: true,
            endOfDayReminderEnabled = prefs[Keys.EOD_ENABLED] ?: false,
            endOfDayHour = prefs[Keys.EOD_HOUR] ?: 21,
            endOfDayMinute = prefs[Keys.EOD_MINUTE] ?: 0
        )
    }

    suspend fun getSettingsOnce(): UserSettings = settingsFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setGlobalRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GLOBAL_REMINDERS] = enabled }
    }

    suspend fun setEndOfDayReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[Keys.EOD_ENABLED] = enabled
            it[Keys.EOD_HOUR] = hour
            it[Keys.EOD_MINUTE] = minute
        }
    }

    suspend fun restoreSettings(settings: UserSettings) {
        context.settingsDataStore.edit {
            it[Keys.THEME_MODE] = settings.themeMode.name
            it[Keys.LANGUAGE] = settings.language.name
            it[Keys.GLOBAL_REMINDERS] = settings.globalRemindersEnabled
            it[Keys.EOD_ENABLED] = settings.endOfDayReminderEnabled
            it[Keys.EOD_HOUR] = settings.endOfDayHour
            it[Keys.EOD_MINUTE] = settings.endOfDayMinute
        }
    }
}
