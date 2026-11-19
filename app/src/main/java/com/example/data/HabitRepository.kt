package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.BackupJsonCodec
import com.example.data.local.HabitDao
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.UserSettings
import com.example.notifications.ReminderScheduler
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.util.DayKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class BackupStats(val habitsCount: Int, val logsCount: Int)

class HabitRepository(
    private val context: Context,
    private val habitDao: HabitDao,
    private val settingsStore: SettingsDataStore,
    private val reminderScheduler: ReminderScheduler
) {
    val activeHabits: Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
    val archivedHabits: Flow<List<HabitEntity>> = habitDao.observeArchivedHabits()
    val allHabits: Flow<List<HabitEntity>> = habitDao.observeAllHabits()
    val allLogs: Flow<List<HabitLogEntity>> = habitDao.observeAllLogs()
    val settings: Flow<UserSettings> = settingsStore.settingsFlow

    suspend fun saveHabit(
        id: Long?,
        name: String,
        description: String,
        iconKey: String,
        colorKey: String,
        targetParts: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        val currentSettings = settingsStore.getSettingsOnce()
        val existing = if (id != null && id > 0) habitDao.getHabitById(id) else null
        val savedHabit = if (existing == null) {
            val nextOrder = habitDao.getMaxSortOrder() + 1
            val newHabit = HabitEntity(
                name = trimmedName,
                description = description.trim(),
                iconKey = iconKey,
                colorKey = colorKey,
                targetParts = targetParts.coerceIn(1, 20),
                sortOrder = nextOrder,
                isArchived = false,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                createdAtEpochDay = DayKey.today().epochDay
            )
            val insertedId = habitDao.insertHabit(newHabit)
            newHabit.copy(id = insertedId)
        } else {
            val updated = existing.copy(
                name = trimmedName,
                description = description.trim(),
                iconKey = iconKey,
                colorKey = colorKey,
                targetParts = targetParts.coerceIn(1, 20),
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            habitDao.updateHabit(updated)
            updated
        }

        if (currentSettings.globalRemindersEnabled && savedHabit.reminderEnabled && !savedHabit.isArchived) {
            reminderScheduler.scheduleHabitReminder(savedHabit)
        } else {
            reminderScheduler.cancelHabitReminder(savedHabit.id)
        }
    }

    /**
     * Increments completed parts for [epochDay]. Returns true if this tap completed the habit.
     */
    suspend fun incrementPart(habit: HabitEntity, epochDay: Long): Boolean {
        val current = habitDao.getLogForDay(habit.id, epochDay)?.completedParts ?: 0
        if (habit.targetParts == 1) {
            return if (current >= 1) {
                habitDao.deleteLog(habit.id, epochDay)
                false
            } else {
                habitDao.upsertLog(HabitLogEntity(habit.id, epochDay, 1))
                true
            }
        }
        val next = (current + 1).coerceAtMost(habit.targetParts)
        if (next != current) {
            habitDao.upsertLog(HabitLogEntity(habit.id, epochDay, next))
        }
        return current < habit.targetParts && next >= habit.targetParts
    }

    suspend fun decrementPart(habit: HabitEntity, epochDay: Long) {
        val current = habitDao.getLogForDay(habit.id, epochDay)?.completedParts ?: 0
        val next = (current - 1).coerceAtLeast(0)
        if (next == 0) {
            habitDao.deleteLog(habit.id, epochDay)
        } else {
            habitDao.upsertLog(HabitLogEntity(habit.id, epochDay, next))
        }
    }

    suspend fun completeAllParts(habit: HabitEntity, epochDay: Long): Boolean {
        val current = habitDao.getLogForDay(habit.id, epochDay)?.completedParts ?: 0
        habitDao.upsertLog(HabitLogEntity(habit.id, epochDay, habit.targetParts))
        return current < habit.targetParts
    }

    suspend fun resetDay(habitId: Long, epochDay: Long) {
        habitDao.deleteLog(habitId, epochDay)
    }

    suspend fun setArchived(habitId: Long, archived: Boolean) {
        habitDao.setArchived(habitId, archived)
        val habit = habitDao.getHabitById(habitId)
        val currentSettings = settingsStore.getSettingsOnce()
        if (habit != null && !archived && habit.reminderEnabled && currentSettings.globalRemindersEnabled) {
            reminderScheduler.scheduleHabitReminder(habit)
        } else {
            reminderScheduler.cancelHabitReminder(habitId)
        }
    }

    suspend fun deleteHabit(habitId: Long) {
        reminderScheduler.cancelHabitReminder(habitId)
        habitDao.deleteHabitById(habitId)
    }

    suspend fun updateSortOrders(orderedIds: List<Long>) {
        habitDao.updateSortOrders(orderedIds)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        settingsStore.setThemeMode(mode)
    }

    suspend fun setLanguage(language: AppLanguage) {
        settingsStore.setLanguage(language)
    }

    suspend fun setGlobalReminders(enabled: Boolean) {
        settingsStore.setGlobalRemindersEnabled(enabled)
        syncReminders()
    }

    suspend fun setEndOfDayReminder(enabled: Boolean, hour: Int, minute: Int) {
        settingsStore.setEndOfDayReminder(enabled, hour, minute)
        syncReminders()
    }

    suspend fun syncReminders() {
        val habits = habitDao.getAllHabitsOnce()
        val currentSettings = settingsStore.getSettingsOnce()
        reminderScheduler.syncAll(habits, currentSettings)
    }

    suspend fun exportBackup(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val habits = habitDao.getAllHabitsOnce()
            val logs = habitDao.getAllLogsOnce()
            val settings = settingsStore.getSettingsOnce()
            val json = BackupJsonCodec.encode(habits, logs, settings)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Cannot open output stream")
            BackupStats(habits.size, logs.size)
        }
    }

    suspend fun importBackup(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Cannot open input stream")

            val payload = BackupJsonCodec.decode(json)
            habitDao.replaceAllData(payload.habits, payload.logs)
            if (payload.settings != null) {
                settingsStore.restoreSettings(payload.settings)
            }
            syncReminders()
            BackupStats(payload.habits.size, payload.logs.size)
        }
    }
}
