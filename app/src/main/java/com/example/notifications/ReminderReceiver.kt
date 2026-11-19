package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SettingsDataStore
import com.example.data.local.WateeraDatabase
import com.example.ui.i18n.AppLanguage
import com.example.util.DayKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val action = intent.action

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = WateeraDatabase.getInstance(appContext)
                val dao = db.habitDao()
                val settings = SettingsDataStore(appContext).getSettingsOnce()
                val isArabic = settings.language == AppLanguage.AR
                val scheduler = ReminderScheduler(appContext)
                val todayEpoch = DayKey.today().epochDay

                when (action) {
                    ReminderScheduler.ACTION_HABIT_REMINDER -> {
                        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, -1L)
                        if (habitId != -1L) {
                            val habit = dao.getHabitById(habitId)
                            if (habit != null && !habit.isArchived && habit.reminderEnabled && settings.globalRemindersEnabled) {
                                val log = dao.getLogForDay(habitId, todayEpoch)
                                val completed = (log?.completedParts ?: 0) >= habit.targetParts
                                if (!completed) {
                                    NotificationHelper.showHabitReminder(appContext, habit.id, habit.name, isArabic)
                                }
                                scheduler.scheduleHabitReminder(habit)
                            }
                        }
                    }

                    ReminderScheduler.ACTION_EOD_REMINDER -> {
                        if (settings.globalRemindersEnabled && settings.endOfDayReminderEnabled) {
                            val activeHabits = dao.getActiveHabitsOnce()
                            val todayLogs = dao.getLogsForDayOnce(todayEpoch).associateBy { it.habitId }
                            val incompleteCount = activeHabits.count { habit ->
                                val done = todayLogs[habit.id]?.completedParts ?: 0
                                done < habit.targetParts
                            }
                            if (incompleteCount > 0) {
                                NotificationHelper.showEndOfDayReminder(appContext, incompleteCount, isArabic)
                            }
                            scheduler.scheduleEndOfDayReminder(settings.endOfDayHour, settings.endOfDayMinute)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
