package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.domain.HabitCatalog
import com.example.ui.components.HabitSegmentedRing
import com.example.ui.components.SegmentedPartsBar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.state.DayStripItem
import com.example.ui.state.HabitDayCheck
import com.example.ui.state.TodayHabitItem
import com.example.ui.state.TodayUiState
import com.example.ui.theme.ThemeMode
import com.example.util.DayKey
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    themeMode: ThemeMode,
    onSelectDay: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onLoadMoreHistory: () -> Unit,
    onIncrementPart: (TodayHabitItem) -> Unit,
    onIncrementPartForDay: (TodayHabitItem, Long) -> Unit,
    onDecrementPart: (TodayHabitItem) -> Unit,
    onCompleteAllParts: (TodayHabitItem) -> Unit,
    onResetHabitDay: (Long) -> Unit,
    onOpenAddHabit: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit,
    onArchiveHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val stripState = rememberLazyListState()
    var actionTargetHabit by remember { mutableStateOf<TodayHabitItem?>(null) }

    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val shouldLoadMoreFromStrip by remember(uiState.historyDaysCount) {
        derivedStateOf {
            val lastVisible = stripState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.historyDaysCount - 15
        }
    }

    LaunchedEffect(shouldLoadMoreFromStrip) {
        if (shouldLoadMoreFromStrip) {
            onLoadMoreHistory()
        }
    }

    LaunchedEffect(uiState.selectedDay.epochDay, uiState.today.epochDay) {
        val diff = (uiState.today.epochDay - uiState.selectedDay.epochDay).toInt()
        if (diff in 0 until uiState.historyDaysCount) {
            val visibleIndices = stripState.layoutInfo.visibleItemsInfo.map { it.index }
            if (diff !in visibleIndices) {
                stripState.animateScrollToItem(diff)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val isToday = uiState.selectedDay.epochDay == uiState.today.epochDay
                        val isYesterday = uiState.selectedDay.epochDay == uiState.today.epochDay - 1L
                        val dayTitle = when {
                            isToday -> strings.todayLabel
                            isYesterday -> strings.yesterdayLabel
                            strings.isArabic -> DayKey.arabicWeekdayShort(uiState.selectedDay.dayOfWeekIso)
                            else -> DayKey.englishWeekdayShort(uiState.selectedDay.dayOfWeekIso)
                        }
                        val monthLabel = if (strings.isArabic) {
                            DayKey.arabicMonthName(uiState.selectedDay.month)
                        } else {
                            DayKey.englishMonthName(uiState.selectedDay.month)
                        }
                        Text(
                            text = dayTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${uiState.selectedDay.dayOfMonth} $monthLabel ${uiState.selectedDay.year}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(
                                imageVector = Icons.Rounded.AllInclusive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = strings.continuousNoEndHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.selectedDay.epochDay != uiState.today.epochDay,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FilledTonalButton(
                            onClick = onJumpToToday,
                            modifier = Modifier.testTag("jump_to_today_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.jumpToToday)
                        }
                    }
                }
            }

            // Hero Summary Card
            item {
                TodaySummaryHeroCard(uiState = uiState)
            }

            // Endless Horizontal Day Strip (Today at index 0, scrolling infinitely into past days)
            item {
                LazyRow(
                    state = stripState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_strip_row")
                ) {
                    items(
                        count = uiState.historyDaysCount,
                        key = { offset -> uiState.today.epochDay - offset.toLong() }
                    ) { offset ->
                        val day = uiState.today.minusDays(offset.toLong())
                        val item = DayStripItem(
                            day = day,
                            completionRatio = uiState.dayCompletionRatios[day.epochDay] ?: 0f,
                            isToday = day.epochDay == uiState.today.epochDay,
                            isSelected = day.epochDay == uiState.selectedDay.epochDay
                        )
                        DayStripPill(
                            item = item,
                            isArabic = strings.isArabic,
                            onClick = { onSelectDay(day.epochDay) }
                        )
                    }
                }
            }

            // Habits List or Empty State
            if (uiState.habits.isEmpty()) {
                item {
                    TodayEmptyStateCard(onAddHabit = onOpenAddHabit)
                }
            } else {
                items(uiState.habits, key = { it.habit.id }) { item ->
                    TodayHabitCard(
                        item = item,
                        today = uiState.today,
                        selectedDay = uiState.selectedDay,
                        historyDaysCount = uiState.historyDaysCount,
                        isDark = isDark,
                        onLoadMoreHistory = onLoadMoreHistory,
                        onTapIncrement = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIncrementPart(item)
                        },
                        onTapDayCheck = { dayEpoch ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIncrementPartForDay(item, dayEpoch)
                        },
                        onDecrement = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDecrementPart(item)
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            actionTargetHabit = item
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenAddHabit,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_habit_fab")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = strings.addHabit
            )
        }
    }

    actionTargetHabit?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { actionTargetHabit = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = target.habit.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text(strings.completeAllParts) },
                    leadingContent = {
                        Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .clickable {
                            onCompleteAllParts(target)
                            actionTargetHabit = null
                        }
                        .testTag("sheet_complete_all")
                )
                if (target.completedParts > 0) {
                    ListItem(
                        headlineContent = { Text(strings.resetToday) },
                        leadingContent = {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable {
                                onResetHabitDay(target.habit.id)
                                actionTargetHabit = null
                            }
                            .testTag("sheet_reset_day")
                    )
                }
                ListItem(
                    headlineContent = { Text(strings.editHabit) },
                    leadingContent = {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                    },
                    modifier = Modifier
                        .clickable {
                            val h = target.habit
                            actionTargetHabit = null
                            onEditHabit(h)
                        }
                        .testTag("sheet_edit_habit")
                )
                ListItem(
                    headlineContent = { Text(strings.archiveHabit) },
                    leadingContent = {
                        Icon(Icons.Rounded.Archive, contentDescription = null)
                    },
                    modifier = Modifier
                        .clickable {
                            val id = target.habit.id
                            actionTargetHabit = null
                            onArchiveHabit(id)
                        }
                        .testTag("sheet_archive_habit")
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryHeroCard(uiState: TodayUiState) {
    val strings = LocalAppStrings.current
    val pct = (uiState.overallProgress * 100f).roundToInt().coerceIn(0, 100)
    val animatedPct by animateFloatAsState(
        targetValue = uiState.overallProgress,
        animationSpec = tween(350),
        label = "hero_progress"
    )
    val headline = when {
        uiState.totalHabitsCount > 0 && uiState.completedHabitsCount == uiState.totalHabitsCount -> strings.allDoneHeadline
        uiState.overallProgress > 0f -> strings.keepGoingHeadline
        else -> strings.startDayHeadline
    }

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = strings.completedOfTotal(uiState.completedHabitsCount, uiState.totalHabitsCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            HabitSegmentedRing(
                completedParts = (animatedPct * 100f).roundToInt(),
                targetParts = 100,
                accentColor = MaterialTheme.colorScheme.primary,
                isCompleted = uiState.totalHabitsCount > 0 && uiState.completedHabitsCount == uiState.totalHabitsCount,
                size = 74.dp,
                strokeWidth = 7.dp
            ) {
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun DayStripPill(
    item: DayStripItem,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val weekday = if (isArabic) {
        DayKey.arabicWeekdayInitial(item.day.dayOfWeekIso)
    } else {
        DayKey.englishWeekdayShort(item.day.dayOfWeekIso).take(2)
    }

    val containerColor = when {
        item.isSelected -> MaterialTheme.colorScheme.primary
        item.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        item.isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = when {
        item.completionRatio >= 1f -> if (item.isSelected) Color.White else MaterialTheme.colorScheme.primary
        item.completionRatio > 0f -> if (item.isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = if (item.isSelected) 4.dp else 1.dp,
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("day_pill_${item.day.epochDay}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = weekday,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
            Text(
                text = item.day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodayHabitCard(
    item: TodayHabitItem,
    today: DayKey,
    selectedDay: DayKey,
    historyDaysCount: Int,
    isDark: Boolean,
    onLoadMoreHistory: () -> Unit,
    onTapIncrement: () -> Unit,
    onTapDayCheck: (Long) -> Unit,
    onDecrement: () -> Unit,
    onLongPress: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colorOption = HabitCatalog.colorForKey(item.habit.colorKey)
    val accent = colorOption.primary
    val softTint = if (isDark) colorOption.softDark else colorOption.softLight

    val cardColor = if (item.isCompleted) {
        softTint.copy(alpha = if (isDark) 0.45f else 0.58f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.isCompleted) {
                    Modifier.border(1.5.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                }
            )
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = onTapIncrement,
                onLongClick = onLongPress
            )
            .testTag("habit_card_${item.habit.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(softTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitCatalog.iconForKey(item.habit.iconKey),
                        contentDescription = item.habit.name,
                        tint = accent,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.habit.description.isNotBlank()) {
                        Text(
                            text = item.habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.currentStreak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = strings.streakDays(item.currentStreak),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        if (item.habit.targetParts > 1) {
                            Text(
                                text = strings.partsProgress(item.completedParts, item.habit.targetParts),
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.habit.targetParts > 1 && item.completedParts > 0) {
                        FilledTonalIconButton(
                            onClick = onDecrement,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("habit_undo_${item.habit.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = strings.undoPartDesc,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HabitSegmentedRing(
                        completedParts = item.completedParts,
                        targetParts = item.habit.targetParts,
                        accentColor = accent,
                        isCompleted = item.isCompleted,
                        size = 46.dp,
                        strokeWidth = 4.5.dp,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(onClick = onTapIncrement)
                            .testTag("habit_ring_${item.habit.id}")
                    ) {
                        if (item.habit.targetParts > 1) {
                            Text(
                                text = "+1",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = strings.addPartDesc,
                                tint = accent.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (item.habit.targetParts > 1) {
                SegmentedPartsBar(
                    completedParts = item.completedParts,
                    targetParts = item.habit.targetParts,
                    accentColor = accent
                )
            }

            // Endless continuous horizontal row of daily ✔ checkmarks (Today -> infinite past)
            EndlessSideBySideDayChecksRow(
                habitId = item.habit.id,
                targetParts = item.habit.targetParts,
                logsByEpochDay = item.logsByEpochDay,
                today = today,
                selectedDay = selectedDay,
                historyDaysCount = historyDaysCount,
                accentColor = accent,
                softTint = softTint,
                isArabic = strings.isArabic,
                onLoadMoreHistory = onLoadMoreHistory,
                onTapDay = onTapDayCheck
            )
        }
    }
}

@Composable
fun EndlessSideBySideDayChecksRow(
    habitId: Long,
    targetParts: Int,
    logsByEpochDay: Map<Long, Int>,
    today: DayKey,
    selectedDay: DayKey,
    historyDaysCount: Int,
    accentColor: Color,
    softTint: Color,
    isArabic: Boolean,
    onLoadMoreHistory: () -> Unit,
    onTapDay: (Long) -> Unit
) {
    val rowState = rememberLazyListState()

    val shouldLoadMore by remember(historyDaysCount) {
        derivedStateOf {
            val lastVisible = rowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= historyDaysCount - 15
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMoreHistory()
        }
    }

    LazyRow(
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("endless_checks_row_$habitId")
    ) {
        items(
            count = historyDaysCount,
            key = { offset -> today.epochDay - offset.toLong() }
        ) { offset ->
            val day = today.minusDays(offset.toLong())
            val completedParts = (logsByEpochDay[day.epochDay] ?: 0).coerceIn(0, targetParts)
            val dayCheck = HabitDayCheck(
                day = day,
                completedParts = completedParts,
                targetParts = targetParts,
                isSelected = day.epochDay == selectedDay.epochDay,
                isToday = day.epochDay == today.epochDay
            )
            DayCheckTile(
                dayCheck = dayCheck,
                habitId = habitId,
                accentColor = accentColor,
                softTint = softTint,
                isArabic = isArabic,
                showMonthTag = day.dayOfMonth == 1 || dayCheck.isToday,
                onTap = { onTapDay(day.epochDay) },
                modifier = Modifier.width(44.dp)
            )
        }
    }
}

@Composable
fun SideBySideDayChecksRow(
    days: List<HabitDayCheck>,
    accentColor: Color,
    softTint: Color,
    isArabic: Boolean,
    habitId: Long,
    onTapDay: ((Long) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { dayCheck ->
            DayCheckTile(
                dayCheck = dayCheck,
                habitId = habitId,
                accentColor = accentColor,
                softTint = softTint,
                isArabic = isArabic,
                showMonthTag = false,
                onTap = if (onTapDay != null) {
                    { onTapDay(dayCheck.day.epochDay) }
                } else {
                    null
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCheckTile(
    dayCheck: HabitDayCheck,
    habitId: Long,
    accentColor: Color,
    softTint: Color,
    isArabic: Boolean,
    showMonthTag: Boolean,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val dayInitial = if (isArabic) {
        DayKey.arabicWeekdayInitial(dayCheck.day.dayOfWeekIso)
    } else {
        DayKey.englishWeekdayShort(dayCheck.day.dayOfWeekIso).take(2)
    }

    val boxBackground = when {
        dayCheck.isCompleted -> accentColor
        dayCheck.isPartial -> softTint
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val boxBorder = when {
        dayCheck.isSelected && !dayCheck.isCompleted -> BorderStrokeColor(accentColor, 2.dp)
        dayCheck.isSelected && dayCheck.isCompleted -> BorderStrokeColor(MaterialTheme.colorScheme.onSurface, 2.dp)
        dayCheck.isPartial -> BorderStrokeColor(accentColor.copy(alpha = 0.65f), 1.5.dp)
        !dayCheck.isCompleted -> BorderStrokeColor(MaterialTheme.colorScheme.outlineVariant, 1.dp)
        else -> null
    }

    val topLabel = when {
        dayCheck.isToday && showMonthTag -> if (isArabic) "اليوم" else "Now"
        showMonthTag -> if (isArabic) {
            DayKey.arabicMonthName(dayCheck.day.month).take(5)
        } else {
            DayKey.englishMonthName(dayCheck.day.month).take(3)
        }
        else -> dayInitial
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onTap != null) {
                    Modifier.clickable(onClick = onTap)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 2.dp)
            .testTag("day_check_${habitId}_${dayCheck.day.epochDay}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = topLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (dayCheck.isSelected || dayCheck.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (dayCheck.isSelected || dayCheck.isToday) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(boxBackground)
                .then(
                    if (boxBorder != null) {
                        Modifier.border(boxBorder.width, boxBorder.color, RoundedCornerShape(11.dp))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                dayCheck.isCompleted -> {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "${dayCheck.day.dayOfMonth} ✓",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                dayCheck.isPartial -> {
                    Text(
                        text = "${dayCheck.completedParts}/${dayCheck.targetParts}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                else -> {
                    Text(
                        text = dayCheck.day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

private data class BorderStrokeColor(
    val color: Color,
    val width: androidx.compose.ui.unit.Dp
)

@Composable
private fun TodayEmptyStateCard(onAddHabit: () -> Unit) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = strings.emptyTodayTitle,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.emptyTodaySubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAddHabit,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_add_habit_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addFirstHabit)
            }
        }
    }
}
