package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.HabitEntity
import com.example.domain.HabitCatalog
import com.example.ui.components.formatTime
import com.example.ui.i18n.LocalAppStrings

@Composable
fun HabitsScreen(
    activeHabits: List<HabitEntity>,
    archivedHabits: List<HabitEntity>,
    onOpenAddHabit: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit,
    onArchiveHabit: (Long, Boolean) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onPersistOrder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 88.dp.toPx() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Archived
    var habitPendingDelete by remember { mutableStateOf<HabitEntity?>(null) }

    val workingActive = remember { mutableStateListOf<HabitEntity>() }
    var draggedHabitId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(activeHabits) {
        if (draggedHabitId == null) {
            workingActive.clear()
            workingActive.addAll(activeHabits)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = strings.habitsTitle,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("habits_tab_active")
                    ) {
                        Text("${strings.activeTab} (${activeHabits.size})")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("habits_tab_archived")
                    ) {
                        Text("${strings.archivedTab} (${archivedHabits.size})")
                    }
                }
            }

            if (selectedTab == 0) {
                if (workingActive.size > 1) {
                    item {
                        Text(
                            text = strings.dragHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (workingActive.isEmpty()) {
                    item {
                        EmptyHabitsPlaceholder(
                            title = strings.emptyTodayTitle,
                            subtitle = strings.emptyTodaySubtitle,
                            actionLabel = strings.addFirstHabit,
                            onAction = onOpenAddHabit
                        )
                    }
                } else {
                    itemsIndexed(workingActive, key = { _, h -> h.id }) { index, habit ->
                        val isDragging = draggedHabitId == habit.id
                        val elevation by animateDpAsState(if (isDragging) 10.dp else 1.dp, label = "drag_elev")
                        var menuExpanded by remember { mutableStateOf(false) }
                        val accent = HabitCatalog.colorForKey(habit.colorKey).primary

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onEditHabit(habit) }
                                .testTag("manage_habit_card_${habit.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Drag Handle
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .pointerInput(habit.id, workingActive.size) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedHabitId = habit.id
                                                    dragOffsetY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    val currentIndex = workingActive.indexOfFirst { it.id == habit.id }
                                                    if (currentIndex != -1) {
                                                        val step = (dragOffsetY / rowHeightPx).toInt()
                                                        if (step != 0) {
                                                            val targetIndex = (currentIndex + step)
                                                                .coerceIn(0, workingActive.lastIndex)
                                                            if (targetIndex != currentIndex) {
                                                                val moved = workingActive.removeAt(currentIndex)
                                                                workingActive.add(targetIndex, moved)
                                                                dragOffsetY -= (targetIndex - currentIndex) * rowHeightPx
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedHabitId = null
                                                    dragOffsetY = 0f
                                                    onPersistOrder(workingActive.map { it.id })
                                                },
                                                onDragCancel = {
                                                    draggedHabitId = null
                                                    dragOffsetY = 0f
                                                    onPersistOrder(workingActive.map { it.id })
                                                }
                                            )
                                        }
                                        .testTag("drag_handle_${habit.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragIndicator,
                                        contentDescription = strings.dragHint,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HabitCatalog.iconForKey(habit.iconKey),
                                        contentDescription = habit.name,
                                        tint = accent
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.partsUnitLabel(habit.targetParts),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = accent
                                        )
                                        if (habit.reminderEnabled) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = formatTime(habit.reminderHour, habit.reminderMinute),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.testTag("habit_menu_${habit.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = strings.editHabit
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(strings.editHabit) },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                            onClick = {
                                                menuExpanded = false
                                                onEditHabit(habit)
                                            }
                                        )
                                        if (index > 0) {
                                            DropdownMenuItem(
                                                text = { Text(strings.moveUp) },
                                                leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val moved = workingActive.removeAt(index)
                                                    workingActive.add(index - 1, moved)
                                                    onPersistOrder(workingActive.map { it.id })
                                                }
                                            )
                                        }
                                        if (index < workingActive.lastIndex) {
                                            DropdownMenuItem(
                                                text = { Text(strings.moveDown) },
                                                leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val moved = workingActive.removeAt(index)
                                                    workingActive.add(index + 1, moved)
                                                    onPersistOrder(workingActive.map { it.id })
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(strings.archiveHabit) },
                                            leadingIcon = { Icon(Icons.Rounded.Archive, null) },
                                            onClick = {
                                                menuExpanded = false
                                                onArchiveHabit(habit.id, true)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.delete) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.DeleteOutline,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                habitPendingDelete = habit
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (archivedHabits.isEmpty()) {
                    item {
                        EmptyHabitsPlaceholder(
                            title = strings.emptyArchivedTitle,
                            subtitle = strings.emptyArchivedSubtitle,
                            actionLabel = null,
                            onAction = null
                        )
                    }
                } else {
                    itemsIndexed(archivedHabits, key = { _, h -> h.id }) { _, habit ->
                        val accent = HabitCatalog.colorForKey(habit.colorKey).primary
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HabitCatalog.iconForKey(habit.iconKey),
                                        contentDescription = habit.name,
                                        tint = accent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = strings.partsUnitLabel(habit.targetParts),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onArchiveHabit(habit.id, false) },
                                    modifier = Modifier.testTag("unarchive_habit_${habit.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Unarchive,
                                        contentDescription = strings.unarchiveHabit,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { habitPendingDelete = habit },
                                    modifier = Modifier.testTag("delete_archived_${habit.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
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
                .testTag("habits_add_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = strings.addHabit)
        }
    }

    habitPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { habitPendingDelete = null },
            title = { Text(strings.confirmDeleteTitle) },
            text = { Text(strings.confirmDeleteMessage(target.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteHabit(target.id)
                        habitPendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitPendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun EmptyHabitsPlaceholder(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
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
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}
