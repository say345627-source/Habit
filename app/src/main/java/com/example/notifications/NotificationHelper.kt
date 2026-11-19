package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_HABIT_REMINDERS = "habit_reminders"
    const val CHANNEL_EOD_SUMMARY = "eod_summary"
    const val EOD_NOTIFICATION_ID = 900_001

    fun ensureChannels(context: Context, isArabic: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val habitChannel = NotificationChannel(
                CHANNEL_HABIT_REMINDERS,
                if (isArabic) "تذكيرات العادات" else "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = if (isArabic) "تذكيرات يومية لكل عادة في وقتها المحدد" else "Scheduled daily reminders for habits"
            }

            val eodChannel = NotificationChannel(
                CHANNEL_EOD_SUMMARY,
                if (isArabic) "تذكير نهاية اليوم" else "End-of-Day Nudge",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = if (isArabic) "تنبيه لطيف عند وجود عادات غير مكتملة بنهاية اليوم" else "Gentle reminder for remaining habits"
            }
            manager.createNotificationChannel(habitChannel)
            manager.createNotificationChannel(eodChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showHabitReminder(context: Context, habitId: Long, habitName: String, isArabic: Boolean) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context, isArabic)

        val title = if (isArabic) "حان وقت: $habitName" else "Time for: $habitName"
        val body = if (isArabic) "خطوة صغيرة الآن تصنع فارقًا في سلسلتك اليومية." else "A small step now keeps your daily streak alive."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_HABIT_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(habitId.toInt(), notification)
    }

    fun showEndOfDayReminder(context: Context, remainingCount: Int, isArabic: Boolean) {
        if (!hasNotificationPermission(context) || remainingCount <= 0) return
        ensureChannels(context, isArabic)

        val title = if (isArabic) "وتيرة اليوم لم تنتهِ بعد" else "Gentle end-of-day check-in"
        val body = if (isArabic) {
            "لديك $remainingCount عادات بانتظارك قبل نهاية اليوم."
        } else {
            "You still have $remainingCount habits left to finish today."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            EOD_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EOD_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(EOD_NOTIFICATION_ID, notification)
    }
}
