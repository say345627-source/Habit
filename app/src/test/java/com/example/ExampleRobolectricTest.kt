package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.HabitRepository
import com.example.data.local.BackupJsonCodec
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.UserSettings
import com.example.data.local.WateeraDatabase
import com.example.domain.StreakCalculator
import com.example.notifications.ReminderScheduler
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.util.DayKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: WateeraDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WateeraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HabitRepository(
            context = context,
            habitDao = db.habitDao(),
            settingsStore = SettingsDataStore(context),
            reminderScheduler = ReminderScheduler(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app_name matches Wateera Arabic title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("وتيرة", context.getString(R.string.app_name))
    }

    @Test
    fun `DayKey roundtrips dates and leap years accurately`() {
        val leap = DayKey.fromYearMonthDay(2024, 2, 29)
        assertEquals("2024-02-29", leap.isoString)
        assertEquals(leap, DayKey.fromEpochDay(leap.epochDay))
        assertEquals(29, DayKey.lengthOfMonth(2024, 2))
        assertEquals(28, DayKey.lengthOfMonth(2025, 2))
        assertEquals(DayKey.fromYearMonthDay(2024, 3, 1), leap.plusDays(1))
    }

    @Test
    fun `StreakCalculator handles active yesterday and longest streaks`() {
        val today = 20_000L
        val days = setOf(today - 1, today - 2, today - 3, today - 10, today - 11)
        assertEquals(3, StreakCalculator.currentStreak(days, today))
        assertEquals(3, StreakCalculator.longestStreak(days))

        val withToday = days + today
        assertEquals(4, StreakCalculator.currentStreak(withToday, today))
        assertEquals(4, StreakCalculator.longestStreak(withToday))

        val broken = setOf(today - 2, today - 3, today - 4, today - 5)
        assertEquals(0, StreakCalculator.currentStreak(broken, today))
        assertEquals(4, StreakCalculator.longestStreak(broken))
    }

    @Test
    fun `BackupJsonCodec encodes and decodes habits logs and settings`() {
        val habits = listOf(
            HabitEntity(
                id = 1L,
                name = "شرب الماء",
                description = "8 أكواب",
                iconKey = "water",
                colorKey = "ocean",
                targetParts = 8,
                sortOrder = 0,
                isArchived = false,
                reminderEnabled = true,
                reminderHour = 10,
                reminderMinute = 30,
                createdAtEpochDay = 20_000L
            )
        )
        val logs = listOf(HabitLogEntity(habitId = 1L, epochDay = 20_000L, completedParts = 5))
        val settings = UserSettings(ThemeMode.DARK, AppLanguage.AR, true, true, 21, 15)

        val json = BackupJsonCodec.encode(habits, logs, settings)
        val decoded = BackupJsonCodec.decode(json)

        assertEquals(1, decoded.habits.size)
        assertEquals("شرب الماء", decoded.habits.first().name)
        assertEquals(8, decoded.habits.first().targetParts)
        assertEquals(1, decoded.logs.size)
        assertEquals(5, decoded.logs.first().completedParts)
        assertEquals(ThemeMode.DARK, decoded.settings?.themeMode)
        assertEquals(21, decoded.settings?.endOfDayHour)
    }

    @Test
    fun `repository increments multi-part habits and persists sort order`() = runBlocking {
        repository.saveHabit(
            id = null,
            name = "قراءة",
            description = "3 أجزاء",
            iconKey = "book",
            colorKey = "emerald",
            targetParts = 3,
            reminderEnabled = false,
            reminderHour = 9,
            reminderMinute = 0
        )
        repository.saveHabit(
            id = null,
            name = "مشي",
            description = "",
            iconKey = "run",
            colorKey = "coral",
            targetParts = 1,
            reminderEnabled = false,
            reminderHour = 9,
            reminderMinute = 0
        )

        val habits = repository.activeHabits.first()
        assertEquals(2, habits.size)
        val reading = habits[0]
        val walk = habits[1]
        val day = DayKey.today().epochDay

        assertFalse(repository.incrementPart(reading, day)) // 1/3
        assertFalse(repository.incrementPart(reading, day)) // 2/3
        assertTrue(repository.incrementPart(reading, day))  // 3/3 completed!

        repository.decrementPart(reading, day) // back to 2/3
        val logAfterUndo = db.habitDao().getLogForDay(reading.id, day)
        assertEquals(2, logAfterUndo?.completedParts)

        // Single-part habit toggles on/off
        assertTrue(repository.incrementPart(walk, day))
        assertFalse(repository.incrementPart(walk, day))

        // Drag reorder persistence
        repository.updateSortOrders(listOf(walk.id, reading.id))
        val reordered = repository.activeHabits.first()
        assertEquals(walk.id, reordered[0].id)
        assertEquals(reading.id, reordered[1].id)
    }
}
