package com.example.data.local

import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val habits: List<HabitEntity>,
    val logs: List<HabitLogEntity>,
    val settings: UserSettings?
)

object BackupJsonCodec {
    private const val FORMAT_VERSION = 1

    fun encode(habits: List<HabitEntity>, logs: List<HabitLogEntity>, settings: UserSettings): String {
        val root = JSONObject()
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val habitsArray = JSONArray()
        habits.forEach { habit ->
            habitsArray.put(
                JSONObject().apply {
                    put("id", habit.id)
                    put("name", habit.name)
                    put("description", habit.description)
                    put("iconKey", habit.iconKey)
                    put("colorKey", habit.colorKey)
                    put("targetParts", habit.targetParts)
                    put("sortOrder", habit.sortOrder)
                    put("isArchived", habit.isArchived)
                    put("reminderEnabled", habit.reminderEnabled)
                    put("reminderHour", habit.reminderHour)
                    put("reminderMinute", habit.reminderMinute)
                    put("createdAtEpochDay", habit.createdAtEpochDay)
                }
            )
        }
        root.put("habits", habitsArray)

        val logsArray = JSONArray()
        logs.forEach { log ->
            logsArray.put(
                JSONObject().apply {
                    put("habitId", log.habitId)
                    put("epochDay", log.epochDay)
                    put("completedParts", log.completedParts)
                    put("updatedAtMillis", log.updatedAtMillis)
                }
            )
        }
        root.put("logs", logsArray)

        root.put(
            "settings",
            JSONObject().apply {
                put("themeMode", settings.themeMode.name)
                put("language", settings.language.name)
                put("globalRemindersEnabled", settings.globalRemindersEnabled)
                put("endOfDayReminderEnabled", settings.endOfDayReminderEnabled)
                put("endOfDayHour", settings.endOfDayHour)
                put("endOfDayMinute", settings.endOfDayMinute)
            }
        )
        return root.toString(2)
    }

    fun decode(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val habitsArray = root.getJSONArray("habits")
        val habits = buildList {
            for (i in 0 until habitsArray.length()) {
                val obj = habitsArray.getJSONObject(i)
                add(
                    HabitEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        iconKey = obj.optString("iconKey", "check"),
                        colorKey = obj.optString("colorKey", "emerald"),
                        targetParts = obj.optInt("targetParts", 1).coerceIn(1, 20),
                        sortOrder = obj.optInt("sortOrder", i),
                        isArchived = obj.optBoolean("isArchived", false),
                        reminderEnabled = obj.optBoolean("reminderEnabled", false),
                        reminderHour = obj.optInt("reminderHour", 9).coerceIn(0, 23),
                        reminderMinute = obj.optInt("reminderMinute", 0).coerceIn(0, 59),
                        createdAtEpochDay = obj.optLong("createdAtEpochDay", 0L)
                    )
                )
            }
        }

        val validHabitIds = habits.map { it.id }.toSet()
        val logsArray = root.getJSONArray("logs")
        val logs = buildList {
            for (i in 0 until logsArray.length()) {
                val obj = logsArray.getJSONObject(i)
                val habitId = obj.getLong("habitId")
                if (habitId in validHabitIds) {
                    add(
                        HabitLogEntity(
                            habitId = habitId,
                            epochDay = obj.getLong("epochDay"),
                            completedParts = obj.optInt("completedParts", 0).coerceAtLeast(0),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val settingsObj = root.optJSONObject("settings")
        val settings = settingsObj?.let { s ->
            UserSettings(
                themeMode = s.optString("themeMode").let {
                    runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
                },
                language = s.optString("language").let {
                    runCatching { AppLanguage.valueOf(it) }.getOrDefault(AppLanguage.AR)
                },
                globalRemindersEnabled = s.optBoolean("globalRemindersEnabled", true),
                endOfDayReminderEnabled = s.optBoolean("endOfDayReminderEnabled", false),
                endOfDayHour = s.optInt("endOfDayHour", 21).coerceIn(0, 23),
                endOfDayMinute = s.optInt("endOfDayMinute", 0).coerceIn(0, 59)
            )
        }

        return BackupPayload(habits = habits, logs = logs, settings = settings)
    }
}
