package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.BadgeType
import com.example.domain.HabitCatalog
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.state.BadgeStatus
import com.example.ui.state.ChartBarPoint
import com.example.ui.state.HabitOverviewItem
import com.example.ui.state.HeatmapCell
import com.example.ui.state.StatsUiState
import com.example.ui.theme.ThemeMode
import com.example.util.DayKey

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    themeMode: ThemeMode,
    onSelectHabitFilter: (Long?) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectHeatmapDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var chartTab by remember { mutableIntStateOf(0) } // 0 = Daily (14d), 1 = Monthly (6m)
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val filterAccent = uiState.selectedHabitFilterId?.let { id ->
        uiState.allHabits.firstOrNull { it.id == id }?.let { HabitCatalog.colorForKey(it.colorKey).primary }
    } ?: MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.statsTitle,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (!uiState.hasHabits) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = strings.emptyStatsMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Habit Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedHabitFilterId == null,
                        onClick = { onSelectHabitFilter(null) },
                        label = { Text(strings.allHabitsFilter) },
                        modifier = Modifier.testTag("stats_filter_all")
                    )
                    uiState.allHabits.forEach { habit ->
                        val accent = HabitCatalog.colorForKey(habit.colorKey).primary
                        FilterChip(
                            selected = uiState.selectedHabitFilterId == habit.id,
                            onClick = { onSelectHabitFilter(habit.id) },
                            label = { Text(habit.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = HabitCatalog.iconForKey(habit.iconKey),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("stats_filter_${habit.id}")
                        )
                    }
                }
            }

            // Dedicated Daily & Monthly Continuity Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContinuitySummaryCard(
                        title = strings.dailyConsistencyTitle,
                        percentage = uiState.dailyContinuityPct,
                        subtitle = strings.completedOfTotal(uiState.dailyCompletedCount, uiState.dailyTotalCount),
                        extraLine = strings.streakDays(uiState.currentStreakForFilter),
                        icon = Icons.Rounded.Today,
                        accentColor = filterAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("daily_continuity_card")
                    )

                    ContinuitySummaryCard(
                        title = strings.monthlyConsistencyTitle,
                        percentage = uiState.monthlyContinuityPct,
                        subtitle = strings.monthDaysRatio(
                            uiState.monthlyCompletedDays,
                            uiState.monthlyElapsedDays,
                            uiState.monthlyContinuityPct
                        ),
                        extraLine = strings.streakBest(uiState.bestStreak),
                        icon = Icons.Rounded.CalendarMonth,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monthly_continuity_card")
                    )
                }
            }

            // Monthly Heatmap Calendar with ✔ indicators
            item {
                MonthlyHeatmapCard(
                    uiState = uiState,
                    accentColor = filterAccent,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDay = onSelectHeatmapDay
                )
            }

            // Daily (14d) & Monthly (6m) Continuity Bar Charts
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.chartsTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = chartTab == 0,
                                    onClick = { chartTab = 0 },
                                    label = { Text(strings.daily14ChartTab) },
                                    modifier = Modifier.testTag("chart_tab_daily")
                                )
                                FilterChip(
                                    selected = chartTab == 1,
                                    onClick = { chartTab = 1 },
                                    label = { Text(strings.monthly6ChartTab) },
                                    modifier = Modifier.testTag("chart_tab_monthly")
                                )
                            }
                        }

                        val points = if (chartTab == 0) uiState.daily14Bars else uiState.monthly6Bars
                        CommitmentBarChart(points = points, barColor = filterAccent)
                    }
                }
            }

            // Achievement Badges
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.badgesTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.badges, key = { it.type.name }) { badge ->
                            BadgeMilestoneCard(badge = badge)
                        }
                    }
                }
            }

            // All Habits Daily & Monthly Overview with side-by-side ✔ checkmarks
            item {
                Text(
                    text = strings.allHabitsOverviewTitle,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(uiState.habitOverviews, key = { it.habit.id }) { overview ->
                HabitOverviewRowCard(
                    item = overview,
                    isDark = isDark,
                    isArabic = strings.isArabic
                )
            }
        }
    }
}

@Composable
private fun ContinuitySummaryCard(
    title: String,
    percentage: Int,
    subtitle: String,
    extraLine: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = extraLine,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun BadgeMilestoneCard(badge: BadgeStatus) {
    val strings = LocalAppStrings.current
    val (title, icon) = when (badge.type) {
        BadgeType.STREAK_7 -> strings.badge7Title to Icons.Rounded.MilitaryTech
        BadgeType.STREAK_30 -> strings.badge30Title to Icons.Rounded.WorkspacePremium
        BadgeType.STREAK_100 -> strings.badge100Title to Icons.Rounded.EmojiEvents
    }
    val container = if (badge.unlocked) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val iconTint = if (badge.unlocked) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .width(148.dp)
            .testTag("badge_card_${badge.type.thresholdDays}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (badge.unlocked) strings.badgeUnlocked else strings.badgeLocked(badge.type.thresholdDays),
                style = MaterialTheme.typography.labelSmall,
                color = if (badge.unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = {
                    (badge.bestProgressDays.toFloat() / badge.type.thresholdDays).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (badge.unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MonthlyHeatmapCard(
    uiState: StatsUiState,
    accentColor: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val monthTitle = if (strings.isArabic) {
        "${DayKey.arabicMonthName(uiState.heatmapMonth)} ${uiState.heatmapYear}"
    } else {
        "${DayKey.englishMonthName(uiState.heatmapMonth)} ${uiState.heatmapYear}"
    }
    val today = DayKey.today()
    val canGoNext = uiState.heatmapYear < today.year ||
        (uiState.heatmapYear == today.year && uiState.heatmapMonth < today.month)

    val weekOrder = if (strings.isArabic) listOf(6, 7, 1, 2, 3, 4, 5) else listOf(1, 2, 3, 4, 5, 6, 7)
    val firstDay = DayKey.fromYearMonthDay(uiState.heatmapYear, uiState.heatmapMonth, 1)
    val leadingBlanks = weekOrder.indexOf(firstDay.dayOfWeekIso).coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = strings.heatmapTitle, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.testTag("heatmap_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = onNextMonth,
                        enabled = canGoNext,
                        modifier = Modifier.testTag("heatmap_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }
            }

            // Weekday Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekOrder.forEach { dow ->
                    val lbl = if (strings.isArabic) {
                        DayKey.arabicWeekdayInitial(dow)
                    } else {
                        DayKey.englishWeekdayShort(dow).take(1)
                    }
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val paddedCells: List<HeatmapCell?> = List(leadingBlanks) { null } + uiState.heatmapCells
            val rows = paddedCells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cell = row.getOrNull(col)
                            if (cell == null) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val selected = uiState.selectedHeatmapCell?.day?.epochDay == cell.day.epochDay
                                val bg = heatmapColorForIntensity(accentColor, cell.intensity, cell.isFuture)
                                val allDone = !cell.isFuture && cell.totalHabits > 0 && cell.completedHabits == cell.totalHabits
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .then(
                                            if (selected) {
                                                Modifier.border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    RoundedCornerShape(8.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable(enabled = !cell.isFuture) {
                                            onSelectDay(cell.day.epochDay)
                                        }
                                        .testTag("heatmap_cell_${cell.day.dayOfMonth}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (allDone) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "${cell.day.dayOfMonth} ✓",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = cell.day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (cell.intensity >= 0.6f && !cell.isFuture) {
                                                Color.White
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = if (cell.isFuture) 0.3f else 0.78f
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.selectedHeatmapCell?.let { sel ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = strings.selectedDaySummary(
                                "${sel.day.dayOfMonth}/${sel.day.month}",
                                sel.completedHabits,
                                sel.totalHabits
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } ?: Spacer(modifier = Modifier.width(1.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.heatmapLegendLow,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(0f, 0.25f, 0.55f, 0.8f, 1f).forEach { lvl ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(heatmapColorForIntensity(accentColor, lvl, isFuture = false))
                        )
                    }
                    Text(
                        text = strings.heatmapLegendHigh,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun heatmapColorForIntensity(base: Color, intensity: Float, isFuture: Boolean): Color {
    if (isFuture) return MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.45f)
    return when {
        intensity <= 0.01f -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        intensity < 0.35f -> base.copy(alpha = 0.25f)
        intensity < 0.65f -> base.copy(alpha = 0.52f)
        intensity < 0.99f -> base.copy(alpha = 0.78f)
        else -> base
    }
}

@Composable
private fun CommitmentBarChart(points: List<ChartBarPoint>, barColor: Color) {
    if (points.isEmpty()) return
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val compactLabels = points.size > 10

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactLabels) 4.dp else 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { point ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!compactLabels || point.percentage > 0) {
                        Text(
                            text = if (compactLabels) "${point.percentage}" else "${point.percentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val barWidth = (size.width * 0.66f).coerceAtMost(26.dp.toPx())
                        val left = (size.width - barWidth) / 2f
                        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                        val fillHeight = (size.height * (point.percentage / 100f)).coerceAtLeast(
                            if (point.percentage > 0) 6.dp.toPx() else 0f
                        )
                        if (fillHeight > 0f) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, size.height - fillHeight),
                                size = Size(barWidth, fillHeight),
                                cornerRadius = radius
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compactLabels) 4.dp else 10.dp)
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HabitOverviewRowCard(
    item: HabitOverviewItem,
    isDark: Boolean,
    isArabic: Boolean
) {
    val strings = LocalAppStrings.current
    val colorOption = HabitCatalog.colorForKey(item.habit.colorKey)
    val accent = colorOption.primary
    val softTint = if (isDark) colorOption.softDark else colorOption.softLight

    val todayDone = item.todayCompletedParts >= item.habit.targetParts
    val todayStatusText = when {
        todayDone -> strings.doneTodayBadge
        item.todayCompletedParts > 0 -> strings.remainingTodayBadge(item.habit.targetParts - item.todayCompletedParts)
        else -> strings.notDoneTodayBadge
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(softTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitCatalog.iconForKey(item.habit.iconKey),
                            contentDescription = item.habit.name,
                            tint = accent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = todayStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (todayDone) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = strings.streakDays(item.currentStreak),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = strings.streakBest(item.longestStreak),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Side-by-side 7-day ✔ checkmarks for this habit
            SideBySideDayChecksRow(
                days = item.last7Days,
                accentColor = accent,
                softTint = softTint,
                isArabic = isArabic,
                habitId = item.habit.id,
                onTapDay = null
            )

            // Monthly continuity bar & exact days ratio
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.monthlyConsistencyTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.monthDaysRatio(
                            item.completedDaysThisMonth,
                            item.elapsedDaysThisMonth,
                            item.monthlyRatePct
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
                LinearProgressIndicator(
                    progress = { (item.monthlyRatePct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
