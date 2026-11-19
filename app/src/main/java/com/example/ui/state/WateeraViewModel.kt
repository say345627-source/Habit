package com.example.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BackupStats
import com.example.data.HabitRepository
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.UserSettings
import com.example.domain.BadgeType
import com.example.domain.StreakCalculator
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.util.DayKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DayStripItem(
    val day: DayKey,
    val completionRatio: Float,
    val isToday: Boolean,
    val isSelected: Boolean
)

data class HabitDayCheck(
    val day: DayKey,
    val completedParts: Int,
    val targetParts: Int,
    val isSelected: Boolean,
    val isToday: Boolean
) {
    val isCompleted: Boolean get() = completedParts >= targetParts
    val isPartial: Boolean get() = completedParts in 1 until targetParts
}

data class TodayHabitItem(
    val habit: HabitEntity,
    val completedParts: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val logsByEpochDay: Map<Long, Int>
) {
    val isCompleted: Boolean get() = completedParts >= habit.targetParts
    val progress: Float get() = (completedParts.toFloat() / habit.targetParts.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class TodayUiState(
    val today: DayKey = DayKey.today(),
    val selectedDay: DayKey = DayKey.today(),
    val historyDaysCount: Int = 90,
    val dayCompletionRatios: Map<Long, Float> = emptyMap(),
    val habits: List<TodayHabitItem> = emptyList(),
    val completedHabitsCount: Int = 0,
    val totalHabitsCount: Int = 0,
    val overallProgress: Float = 0f
)

data class HeatmapCell(
    val day: DayKey,
    val intensity: Float,
    val completedHabits: Int,
    val totalHabits: Int,
    val isFuture: Boolean
)

data class ChartBarPoint(
    val label: String,
    val percentage: Int
)

data class HabitOverviewItem(
    val habit: HabitEntity,
    val currentStreak: Int,
    val longestStreak: Int,
    val rate30Days: Int,
    val completedDaysThisMonth: Int,
    val elapsedDaysThisMonth: Int,
    val monthlyRatePct: Int,
    val todayCompletedParts: Int,
    val last7Days: List<HabitDayCheck>
)

data class BadgeStatus(
    val type: BadgeType,
    val unlocked: Boolean,
    val bestProgressDays: Int
)

data class StatsUiState(
    val hasHabits: Boolean = false,
    val allHabits: List<HabitEntity> = emptyList(),
    val selectedHabitFilterId: Long? = null,
    val dailyContinuityPct: Int = 0,
    val dailyCompletedCount: Int = 0,
    val dailyTotalCount: Int = 0,
    val monthlyContinuityPct: Int = 0,
    val monthlyCompletedDays: Int = 0,
    val monthlyElapsedDays: Int = 0,
    val currentStreakForFilter: Int = 0,
    val overallCommitmentRate: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val badges: List<BadgeStatus> = emptyList(),
    val heatmapYear: Int = DayKey.today().year,
    val heatmapMonth: Int = DayKey.today().month,
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val selectedHeatmapCell: HeatmapCell? = null,
    val daily14Bars: List<ChartBarPoint> = emptyList(),
    val monthly6Bars: List<ChartBarPoint> = emptyList(),
    val habitOverviews: List<HabitOverviewItem> = emptyList()
)

sealed interface UiFeedbackEvent {
    data class HabitCompleted(val habitName: String, val unlockedBadge: BadgeType?) : UiFeedbackEvent
    data class BackupExported(val stats: BackupStats) : UiFeedbackEvent
    data class BackupImported(val stats: BackupStats) : UiFeedbackEvent
    data object BackupFailed : UiFeedbackEvent
}

class WateeraViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val todayKey: DayKey = DayKey.today()
    private val selectedEpochDay = MutableStateFlow(todayKey.epochDay)
    private val historyDaysCountState = MutableStateFlow(90)
    private val heatmapMonthState = MutableStateFlow(todayKey.year to todayKey.month)
    private val selectedHeatmapEpochDay = MutableStateFlow<Long?>(todayKey.epochDay)
    private val selectedHabitFilterId = MutableStateFlow<Long?>(null)

    private val _feedbackEvents = MutableSharedFlow<UiFeedbackEvent>(extraBufferCapacity = 8)
    val feedbackEvents = _feedbackEvents.asSharedFlow()

    val settingsState: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val activeHabitsState: StateFlow<List<HabitEntity>> = repository.activeHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedHabitsState: StateFlow<List<HabitEntity>> = repository.archivedHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayUiState: StateFlow<TodayUiState> = combine(
        repository.activeHabits,
        repository.allLogs,
        selectedEpochDay,
        historyDaysCountState
    ) { habits, logs, selectedDayEpoch, historyCount ->
        buildTodayUiState(habits, logs, selectedDayEpoch, historyCount)
    }
        // Keep history grouping, streak calculations and progress aggregation off the UI thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    private val statsInputs = combine(
        heatmapMonthState,
        selectedHeatmapEpochDay,
        selectedHabitFilterId
    ) { ym, heatmapDay, filterId ->
        Triple(ym, heatmapDay, filterId)
    }

    val statsUiState: StateFlow<StatsUiState> = combine(
        repository.activeHabits,
        repository.allLogs,
        statsInputs,
        repository.settings
    ) { habits, logs, inputs, settings ->
        buildStatsUiState(
            habits = habits,
            logs = logs,
            heatmapYear = inputs.first.first,
            heatmapMonth = inputs.first.second,
            selectedHeatmapEpoch = inputs.second,
            habitFilterId = inputs.third,
            language = settings.language
        )
    }
        // Statistics scan the full log history; never perform this work on the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun selectDay(epochDay: Long) {
        if (epochDay <= todayKey.epochDay) {
            selectedEpochDay.value = epochDay
        }
    }

    fun jumpToToday() {
        selectedEpochDay.value = todayKey.epochDay
    }

    fun loadMoreHistoryDays() {
        historyDaysCountState.value += 90
    }

    fun previousHeatmapMonth() {
        val (y, m) = heatmapMonthState.value
        heatmapMonthState.value = DayKey.previousMonth(y, m)
    }

    fun nextHeatmapMonth() {
        val (y, m) = heatmapMonthState.value
        val (ny, nm) = DayKey.nextMonth(y, m)
        if (ny < todayKey.year || (ny == todayKey.year && nm <= todayKey.month)) {
            heatmapMonthState.value = ny to nm
        }
    }

    fun selectHeatmapDay(epochDay: Long) {
        selectedHeatmapEpochDay.value = epochDay
    }

    fun selectHabitFilter(habitId: Long?) {
        selectedHabitFilterId.value = habitId
    }

    fun incrementPart(habitItem: TodayHabitItem) {
        incrementPartForDay(habitItem, selectedEpochDay.value)
    }

    fun incrementPartForDay(habitItem: TodayHabitItem, epochDay: Long) {
        if (epochDay > todayKey.epochDay) return
        viewModelScope.launch {
            val prevBest = habitItem.longestStreak
            val becameCompleted = repository.incrementPart(habitItem.habit, epochDay)
            if (becameCompleted) {
                val newStreak = if (epochDay == todayKey.epochDay) habitItem.currentStreak + 1 else habitItem.currentStreak
                val unlocked = when {
                    newStreak >= 100 && prevBest < 100 -> BadgeType.STREAK_100
                    newStreak >= 30 && prevBest < 30 -> BadgeType.STREAK_30
                    newStreak >= 7 && prevBest < 7 -> BadgeType.STREAK_7
                    else -> null
                }
                _feedbackEvents.tryEmit(UiFeedbackEvent.HabitCompleted(habitItem.habit.name, unlocked))
            }
        }
    }

    fun decrementPart(habitItem: TodayHabitItem) {
        viewModelScope.launch {
            repository.decrementPart(habitItem.habit, selectedEpochDay.value)
        }
    }

    fun completeAllParts(habitItem: TodayHabitItem) {
        viewModelScope.launch {
            val becameCompleted = repository.completeAllParts(habitItem.habit, selectedEpochDay.value)
            if (becameCompleted) {
                _feedbackEvents.tryEmit(UiFeedbackEvent.HabitCompleted(habitItem.habit.name, null))
            }
        }
    }

    fun resetHabitDay(habitId: Long) {
        viewModelScope.launch {
            repository.resetDay(habitId, selectedEpochDay.value)
        }
    }

    fun saveHabit(
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
        viewModelScope.launch {
            repository.saveHabit(
                id = id,
                name = name,
                description = description,
                iconKey = iconKey,
                colorKey = colorKey,
                targetParts = targetParts,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
        }
    }

    fun setArchived(habitId: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(habitId, archived)
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun persistHabitOrder(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.updateSortOrders(orderedIds)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setGlobalReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setGlobalReminders(enabled) }
    }

    fun setEndOfDayReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { repository.setEndOfDayReminder(enabled, hour, minute) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            repository.exportBackup(uri)
                .onSuccess { _feedbackEvents.tryEmit(UiFeedbackEvent.BackupExported(it)) }
                .onFailure { _feedbackEvents.tryEmit(UiFeedbackEvent.BackupFailed) }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            repository.importBackup(uri)
                .onSuccess { _feedbackEvents.tryEmit(UiFeedbackEvent.BackupImported(it)) }
                .onFailure { _feedbackEvents.tryEmit(UiFeedbackEvent.BackupFailed) }
        }
    }

    private fun buildTodayUiState(
        habits: List<HabitEntity>,
        logs: List<HabitLogEntity>,
        selectedDayEpoch: Long,
        historyDaysCount: Int
    ): TodayUiState {
        val today = DayKey.today()
        val logsByHabit = logs.groupBy { it.habitId }
        val logsByDay = logs.groupBy { it.epochDay }

        val dayRatios = if (habits.isEmpty()) {
            emptyMap()
        } else {
            logsByDay.mapValues { (_, dayLogs) ->
                val dayLogMap = dayLogs.associateBy { it.habitId }
                habits.sumOf { habit ->
                    val done = dayLogMap[habit.id]?.completedParts ?: 0
                    (done.toFloat() / habit.targetParts.coerceAtLeast(1)).coerceIn(0f, 1f).toDouble()
                }.toFloat() / habits.size
            }
        }

        val selectedLogs = logsByDay[selectedDayEpoch].orEmpty().associateBy { it.habitId }
        val selectedDayKey = DayKey.fromEpochDay(selectedDayEpoch)

        val items = habits.map { habit ->
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val logMap = habitLogs.associate { it.epochDay to it.completedParts.coerceIn(0, habit.targetParts) }
            val completedDays = logMap.filterValues { it >= habit.targetParts }.keys
            val doneParts = (selectedLogs[habit.id]?.completedParts ?: 0).coerceIn(0, habit.targetParts)

            TodayHabitItem(
                habit = habit,
                completedParts = doneParts,
                currentStreak = StreakCalculator.currentStreak(completedDays, today.epochDay),
                longestStreak = StreakCalculator.longestStreak(completedDays),
                logsByEpochDay = logMap
            )
        }

        val completedCount = items.count { it.isCompleted }
        val overallProgress = if (items.isEmpty()) {
            0f
        } else {
            items.map { it.progress }.average().toFloat()
        }

        return TodayUiState(
            today = today,
            selectedDay = selectedDayKey,
            historyDaysCount = historyDaysCount,
            dayCompletionRatios = dayRatios,
            habits = items,
            completedHabitsCount = completedCount,
            totalHabitsCount = items.size,
            overallProgress = overallProgress
        )
    }

    private fun buildStatsUiState(
        habits: List<HabitEntity>,
        logs: List<HabitLogEntity>,
        heatmapYear: Int,
        heatmapMonth: Int,
        selectedHeatmapEpoch: Long?,
        habitFilterId: Long?,
        language: AppLanguage
    ): StatsUiState {
        if (habits.isEmpty()) {
            return StatsUiState(
                hasHabits = false,
                badges = BadgeType.entries.map { BadgeStatus(it, unlocked = false, bestProgressDays = 0) },
                heatmapYear = heatmapYear,
                heatmapMonth = heatmapMonth
            )
        }

        val today = DayKey.today()
        val logsByHabit = logs.groupBy { it.habitId }
        val logsByDay = logs.groupBy { it.epochDay }
        val effectiveFilterId = habitFilterId?.takeIf { id -> habits.any { it.id == id } }
        val targetHabits = if (effectiveFilterId != null) {
            habits.filter { it.id == effectiveFilterId }
        } else {
            habits
        }

        fun dayCompletionRatio(epochDay: Long, subset: List<HabitEntity> = targetHabits): Pair<Float, Int> {
            if (subset.isEmpty()) return 0f to 0
            val dayLogs = logsByDay[epochDay].orEmpty().associateBy { it.habitId }
            var completedCount = 0
            var progressSum = 0f
            subset.forEach { habit ->
                val done = dayLogs[habit.id]?.completedParts ?: 0
                if (done >= habit.targetParts) completedCount++
                progressSum += (done.toFloat() / habit.targetParts.coerceAtLeast(1)).coerceIn(0f, 1f)
            }
            return (progressSum / subset.size) to completedCount
        }

        val monthLength = DayKey.lengthOfMonth(heatmapYear, heatmapMonth)
        val elapsedInSelectedMonth = when {
            heatmapYear < today.year || (heatmapYear == today.year && heatmapMonth < today.month) -> monthLength
            heatmapYear == today.year && heatmapMonth == today.month -> today.dayOfMonth
            else -> 0
        }

        val habitOverviews = habits.map { habit ->
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val completedDays = habitLogs.filter { it.completedParts >= habit.targetParts }
                .map { it.epochDay }
                .toSet()
            val logMap = habitLogs.associate { it.epochDay to it.completedParts }
            val current = StreakCalculator.currentStreak(completedDays, today.epochDay)
            val longest = StreakCalculator.longestStreak(completedDays)

            val last30Done = (29L downTo 0L).sumOf { offset ->
                val d = today.epochDay - offset
                val parts = logMap[d] ?: 0
                (parts.toFloat() / habit.targetParts.coerceAtLeast(1)).coerceIn(0f, 1f).toDouble()
            }
            val rate30 = ((last30Done / 30.0) * 100.0).roundToInt().coerceIn(0, 100)

            val completedThisMonth = (1..elapsedInSelectedMonth).count { dom ->
                val d = DayKey.fromYearMonthDay(heatmapYear, heatmapMonth, dom).epochDay
                (logMap[d] ?: 0) >= habit.targetParts
            }
            val monthlyRate = if (elapsedInSelectedMonth > 0) {
                ((completedThisMonth.toFloat() / elapsedInSelectedMonth) * 100f).roundToInt().coerceIn(0, 100)
            } else {
                0
            }

            val last7Days = (6L downTo 0L).map { offset ->
                val day = today.minusDays(offset)
                val parts = (logMap[day.epochDay] ?: 0).coerceIn(0, habit.targetParts)
                HabitDayCheck(
                    day = day,
                    completedParts = parts,
                    targetParts = habit.targetParts,
                    isSelected = day.epochDay == today.epochDay,
                    isToday = day.epochDay == today.epochDay
                )
            }

            HabitOverviewItem(
                habit = habit,
                currentStreak = current,
                longestStreak = longest,
                rate30Days = rate30,
                completedDaysThisMonth = completedThisMonth,
                elapsedDaysThisMonth = elapsedInSelectedMonth,
                monthlyRatePct = monthlyRate,
                todayCompletedParts = (logMap[today.epochDay] ?: 0).coerceIn(0, habit.targetParts),
                last7Days = last7Days
            )
        }

        val filteredOverviews = if (effectiveFilterId != null) {
            habitOverviews.filter { it.habit.id == effectiveFilterId }
        } else {
            habitOverviews
        }

        val bestStreak = filteredOverviews.maxOfOrNull { it.longestStreak } ?: 0
        val currentStreakFilter = filteredOverviews.maxOfOrNull { it.currentStreak } ?: 0
        val totalCompletions = targetHabits.sumOf { habit ->
            logsByHabit[habit.id].orEmpty().count { it.completedParts >= habit.targetParts }
        }
        val overallCommitment = filteredOverviews.map { it.rate30Days }.average().roundToInt().coerceIn(0, 100)

        // Daily continuity stat (today)
        val (todayRatio, todayCompletedCount) = dayCompletionRatio(today.epochDay, targetHabits)
        val dailyContinuityPct = (todayRatio * 100f).roundToInt().coerceIn(0, 100)

        // Monthly continuity stat (selected heatmap month)
        val heatmapCells = (1..monthLength).map { dom ->
            val day = DayKey.fromYearMonthDay(heatmapYear, heatmapMonth, dom)
            val isFuture = day.epochDay > today.epochDay
            if (isFuture) {
                HeatmapCell(day, 0f, 0, targetHabits.size, isFuture = true)
            } else {
                val (ratio, completed) = dayCompletionRatio(day.epochDay, targetHabits)
                HeatmapCell(day, ratio, completed, targetHabits.size, isFuture = false)
            }
        }

        val nonFutureMonthCells = heatmapCells.filter { !it.isFuture }
        val monthlyCompletedDays = nonFutureMonthCells.count {
            it.totalHabits > 0 && it.completedHabits == it.totalHabits
        }
        val monthlyContinuityPct = if (nonFutureMonthCells.isNotEmpty()) {
            (nonFutureMonthCells.map { it.intensity }.average().toFloat() * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val badges = BadgeType.entries.map { badge ->
            val maxGlobalBest = habitOverviews.maxOfOrNull { it.longestStreak } ?: 0
            BadgeStatus(
                type = badge,
                unlocked = maxGlobalBest >= badge.thresholdDays,
                bestProgressDays = maxGlobalBest.coerceAtMost(badge.thresholdDays)
            )
        }

        val selectedCell = heatmapCells.firstOrNull { it.day.epochDay == selectedHeatmapEpoch }
            ?: heatmapCells.lastOrNull { !it.isFuture }

        val daily14Bars = (13L downTo 0L).map { offset ->
            val day = today.minusDays(offset)
            val (ratio, _) = dayCompletionRatio(day.epochDay, targetHabits)
            ChartBarPoint(
                label = day.dayOfMonth.toString(),
                percentage = (ratio * 100f).roundToInt().coerceIn(0, 100)
            )
        }

        val monthly6Bars = buildMonthly6Trend(today, ::dayCompletionRatio, targetHabits, language)

        return StatsUiState(
            hasHabits = true,
            allHabits = habits,
            selectedHabitFilterId = effectiveFilterId,
            dailyContinuityPct = dailyContinuityPct,
            dailyCompletedCount = todayCompletedCount,
            dailyTotalCount = targetHabits.size,
            monthlyContinuityPct = monthlyContinuityPct,
            monthlyCompletedDays = monthlyCompletedDays,
            monthlyElapsedDays = nonFutureMonthCells.size,
            currentStreakForFilter = currentStreakFilter,
            overallCommitmentRate = overallCommitment,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            badges = badges,
            heatmapYear = heatmapYear,
            heatmapMonth = heatmapMonth,
            heatmapCells = heatmapCells,
            selectedHeatmapCell = selectedCell,
            daily14Bars = daily14Bars,
            monthly6Bars = monthly6Bars,
            habitOverviews = habitOverviews
        )
    }

    private fun buildMonthly6Trend(
        today: DayKey,
        dayRatioFn: (Long, List<HabitEntity>) -> Pair<Float, Int>,
        targetHabits: List<HabitEntity>,
        language: AppLanguage
    ): List<ChartBarPoint> {
        val months = mutableListOf<Pair<Int, Int>>()
        var y = today.year
        var m = today.month
        repeat(6) {
            months.add(0, y to m)
            val prev = DayKey.previousMonth(y, m)
            y = prev.first
            m = prev.second
        }
        return months.map { (yr, mo) ->
            val len = DayKey.lengthOfMonth(yr, mo)
            val limitDay = if (yr == today.year && mo == today.month) today.dayOfMonth else len
            var sum = 0f
            for (d in 1..limitDay) {
                val epoch = DayKey.fromYearMonthDay(yr, mo, d).epochDay
                sum += dayRatioFn(epoch, targetHabits).first
            }
            val pct = if (limitDay > 0) ((sum / limitDay) * 100f).roundToInt().coerceIn(0, 100) else 0
            val label = if (language == AppLanguage.AR) {
                DayKey.arabicMonthName(mo).take(5)
            } else {
                DayKey.englishMonthName(mo).take(3)
            }
            ChartBarPoint(label = label, percentage = pct)
        }
    }

    class Factory(private val repository: HabitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WateeraViewModel(repository) as T
        }
    }
}
