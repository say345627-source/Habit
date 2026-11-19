package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.HabitEntity
import com.example.data.local.UserSettings
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun syncAll(habits: List<HabitEntity>, settings: UserSettings) {
        habits.forEach { habit ->
            if (settings.globalRemindersEnabled && !habit.isArchived && habit.reminderEnabled) {
                scheduleHabitReminder(habit)
            } else {
                cancelHabitReminder(habit.id)
            }
        }
        if (settings.globalRemindersEnabled && settings.endOfDayReminderEnabled) {
            scheduleEndOfDayReminder(settings.endOfDayHour, settings.endOfDayMinute)
        } else {
            cancelEndOfDayReminder()
        }
    }

    fun scheduleHabitReminder(habit: HabitEntity) {
        val manager = alarmManager ?: return
        val pendingIntent = habitPendingIntent(habit.id, habit.name)
        val triggerMillis = nextTriggerMillis(habit.reminderHour, habit.reminderMinute)
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancelHabitReminder(habitId: Long) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
        }
        val pi = PendingIntent.getBroadcast(
            context,
            habitRequestCode(habitId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            manager.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleEndOfDayReminder(hour: Int, minute: Int) {
        val manager = alarmManager ?: return
        val pendingIntent = eodPendingIntent()
        val triggerMillis = nextTriggerMillis(hour, minute)
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancelEndOfDayReminder() {
        val manager = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_EOD_REMINDER
        }
        val pi = PendingIntent.getBroadcast(
            context,
            EOD_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            manager.cancel(pi)
            pi.cancel()
        }
    }

    private fun habitPendingIntent(habitId: Long, habitName: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
        }
        return PendingIntent.getBroadcast(
            context,
            habitRequestCode(habitId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun eodPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_EOD_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            EOD_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun habitRequestCode(habitId: Long): Int = (10_000L + (habitId % 100_000L)).toInt()

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return target.timeInMillis
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.example.ACTION_HABIT_REMINDER"
        const val ACTION_EOD_REMINDER = "com.example.ACTION_EOD_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        private const val EOD_REQUEST_CODE = 900_001
    }
}
