package com.example.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.max

data class HabitColorOption(
    val key: String,
    val primary: Color,
    val softLight: Color,
    val softDark: Color
)

data class HabitIconOption(
    val key: String,
    val icon: ImageVector,
    val labelAr: String,
    val labelEn: String
)

data class HabitStarterTemplate(
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val iconKey: String,
    val colorKey: String,
    val targetParts: Int
)

enum class BadgeType(val thresholdDays: Int) {
    STREAK_7(7),
    STREAK_30(30),
    STREAK_100(100)
}

object HabitCatalog {
    val colors: List<HabitColorOption> = listOf(
        HabitColorOption("emerald", Color(0xFF1B7F6B), Color(0xFFE0F3EE), Color(0xFF15362F)),
        HabitColorOption("ocean", Color(0xFF2563EB), Color(0xFFE3ECFD), Color(0xFF182A4A)),
        HabitColorOption("coral", Color(0xFFDC583B), Color(0xFFFDE8E3), Color(0xFF451F17)),
        HabitColorOption("amber", Color(0xFFD97706), Color(0xFFFEF0D9), Color(0xFF43290E)),
        HabitColorOption("violet", Color(0xFF7C3AED), Color(0xFFEDE6FD), Color(0xFF2A1B47)),
        HabitColorOption("rose", Color(0xFFDB2777), Color(0xFFFCE3EF), Color(0xFF44182D)),
        HabitColorOption("teal", Color(0xFF0D9488), Color(0xFFDDF5F2), Color(0xFF133633)),
        HabitColorOption("olive", Color(0xFF658B38), Color(0xFFEAF3DF), Color(0xFF243216)),
        HabitColorOption("indigo", Color(0xFF4F46E5), Color(0xFFE6E6FD), Color(0xFF1E1C47)),
        HabitColorOption("plum", Color(0xFF9333EA), Color(0xFFF2E5FD), Color(0xFF33184A))
    )

    val icons: List<HabitIconOption> = listOf(
        HabitIconOption("water", Icons.Rounded.WaterDrop, "ماء", "Water"),
        HabitIconOption("book", Icons.Rounded.AutoStories, "قراءة", "Reading"),
        HabitIconOption("fitness", Icons.Rounded.FitnessCenter, "رياضة", "Workout"),
        HabitIconOption("meditation", Icons.Rounded.SelfImprovement, "تأمل", "Mindfulness"),
        HabitIconOption("run", Icons.Rounded.DirectionsRun, "مشي", "Walk / Run"),
        HabitIconOption("sleep", Icons.Rounded.Bedtime, "نوم", "Sleep"),
        HabitIconOption("sun", Icons.Rounded.WbSunny, "صباح", "Morning"),
        HabitIconOption("code", Icons.Rounded.Code, "برمجة", "Coding"),
        HabitIconOption("journal", Icons.Rounded.EditNote, "تدوين", "Journal"),
        HabitIconOption("food", Icons.Rounded.Restaurant, "غذاء", "Nutrition"),
        HabitIconOption("heart", Icons.Rounded.Favorite, "صحة", "Health"),
        HabitIconOption("brain", Icons.Rounded.Psychology, "تعلّم", "Learning"),
        HabitIconOption("art", Icons.Rounded.Brush, "إبداع", "Art"),
        HabitIconOption("music", Icons.Rounded.MusicNote, "موسيقى", "Music"),
        HabitIconOption("coffee", Icons.Rounded.LocalCafe, "استراحة", "Break"),
        HabitIconOption("check", Icons.Rounded.CheckCircle, "إنجاز", "Focus")
    )

    val quickTemplates: List<HabitStarterTemplate> = listOf(
        HabitStarterTemplate(
            nameAr = "شرب الماء",
            nameEn = "Drink Water",
            descriptionAr = "8 أكواب على مدار اليوم",
            descriptionEn = "8 glasses throughout the day",
            iconKey = "water",
            colorKey = "ocean",
            targetParts = 8
        ),
        HabitStarterTemplate(
            nameAr = "قراءة يومية",
            nameEn = "Daily Reading",
            descriptionAr = "3 جلسات قراءة مركّزة",
            descriptionEn = "3 focused reading sessions",
            iconKey = "book",
            colorKey = "emerald",
            targetParts = 3
        ),
        HabitStarterTemplate(
            nameAr = "مشي 30 دقيقة",
            nameEn = "30-min Walk",
            descriptionAr = "حركة يومية لتجديد النشاط",
            descriptionEn = "Daily movement to recharge",
            iconKey = "run",
            colorKey = "coral",
            targetParts = 1
        ),
        HabitStarterTemplate(
            nameAr = "أذكار وهدوء",
            nameEn = "Mindful Pause",
            descriptionAr = "صباحًا ومساءً",
            descriptionEn = "Morning & evening",
            iconKey = "meditation",
            colorKey = "violet",
            targetParts = 2
        )
    )

    fun colorForKey(key: String): HabitColorOption =
        colors.firstOrNull { it.key == key } ?: colors.first()

    fun iconForKey(key: String): ImageVector =
        icons.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.CheckCircle
}

object StreakCalculator {
    /**
     * Calculates current continuous daily streak ending on [todayEpochDay] (or yesterday if today
     * has not been completed yet, so active streaks are preserved during the day).
     */
    fun currentStreak(completedEpochDays: Set<Long>, todayEpochDay: Long): Int {
        if (completedEpochDays.isEmpty()) return 0
        var cursor = when {
            completedEpochDays.contains(todayEpochDay) -> todayEpochDay
            completedEpochDays.contains(todayEpochDay - 1L) -> todayEpochDay - 1L
            else -> return 0
        }
        var streak = 0
        while (completedEpochDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    fun longestStreak(completedEpochDays: Set<Long>): Int {
        if (completedEpochDays.isEmpty()) return 0
        val sorted = completedEpochDays.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1L) {
                current++
                best = max(best, current)
            } else {
                current = 1
            }
        }
        return best
    }
}
