package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.domain.HabitCatalog
import com.example.ui.i18n.LocalAppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorSheet(
    editingHabit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long?,
        name: String,
        description: String,
        iconKey: String,
        colorKey: String,
        targetParts: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editingHabit) { mutableStateOf(editingHabit?.name ?: "") }
    var description by remember(editingHabit) { mutableStateOf(editingHabit?.description ?: "") }
    var iconKey by remember(editingHabit) { mutableStateOf(editingHabit?.iconKey ?: "water") }
    var colorKey by remember(editingHabit) { mutableStateOf(editingHabit?.colorKey ?: "emerald") }
    var targetParts by remember(editingHabit) { mutableIntStateOf(editingHabit?.targetParts ?: 1) }
    var reminderEnabled by remember(editingHabit) { mutableStateOf(editingHabit?.reminderEnabled ?: false) }
    var reminderHour by remember(editingHabit) { mutableIntStateOf(editingHabit?.reminderHour ?: 9) }
    var reminderMinute by remember(editingHabit) { mutableIntStateOf(editingHabit?.reminderMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (editingHabit == null) strings.addHabit else strings.editHabit,
                style = MaterialTheme.typography.headlineSmall
            )

            if (editingHabit == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.quickTemplatesTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitCatalog.quickTemplates.forEach { template ->
                            AssistChip(
                                onClick = {
                                    name = if (strings.isArabic) template.nameAr else template.nameEn
                                    description = if (strings.isArabic) template.descriptionAr else template.descriptionEn
                                    iconKey = template.iconKey
                                    colorKey = template.colorKey
                                    targetParts = template.targetParts
                                },
                                label = { Text(if (strings.isArabic) template.nameAr else template.nameEn) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = HabitCatalog.iconForKey(template.iconKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = HabitCatalog.colorForKey(template.colorKey).primary
                                    )
                                },
                                modifier = Modifier.testTag("template_${template.iconKey}")
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.habitNameLabel) },
                placeholder = { Text(strings.habitNamePlaceholder) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_name_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(strings.habitDescLabel) },
                placeholder = { Text(strings.habitDescPlaceholder) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_description_input")
            )

            // Daily Parts Split Stepper
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dailyPartsLabel,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (targetParts == 1) strings.singleTapLabel else strings.partsUnitLabel(targetParts),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (targetParts > 1) targetParts-- },
                                enabled = targetParts > 1,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("parts_decrement_button")
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = strings.undoPartDesc)
                            }
                            Text(
                                text = targetParts.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalIconButton(
                                onClick = { if (targetParts < 20) targetParts++ },
                                enabled = targetParts < 20,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("parts_increment_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = strings.addPartDesc)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 5, 8).forEach { preset ->
                            FilterChip(
                                selected = targetParts == preset,
                                onClick = { targetParts = preset },
                                label = { Text("$preset") },
                                modifier = Modifier.testTag("parts_preset_$preset")
                            )
                        }
                    }
                }
            }

            // Color Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.chooseColorLabel,
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HabitCatalog.colors.forEach { option ->
                        val selected = option.key == colorKey
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(option.primary)
                                .then(
                                    if (selected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { colorKey = option.key }
                                .testTag("color_option_${option.key}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = option.key,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Icon Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.chooseIconLabel,
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val accent = HabitCatalog.colorForKey(colorKey).primary
                    HabitCatalog.icons.forEach { option ->
                        val selected = option.key == iconKey
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) accent.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .then(
                                    if (selected) Modifier.border(2.dp, accent, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable { iconKey = option.key }
                                .testTag("icon_option_${option.key}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = if (strings.isArabic) option.labelAr else option.labelEn,
                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Per-Habit Reminder
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(text = strings.reminderLabel, style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                                if (enabled) onRequestNotificationPermission()
                            },
                            modifier = Modifier.testTag("habit_reminder_switch")
                        )
                    }

                    if (reminderEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showTimePicker = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("habit_reminder_time_button"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.reminderTimeLabel,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = formatTime(reminderHour, reminderMinute),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_habit_button")
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        onSave(
                            editingHabit?.id,
                            name,
                            description,
                            iconKey,
                            colorKey,
                            targetParts,
                            reminderEnabled,
                            reminderHour,
                            reminderMinute
                        )
                        onDismiss()
                    },
                    enabled = name.trim().isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .testTag("save_habit_button")
                ) {
                    Text(strings.save)
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerModalDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModalDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.chooseTimeTitle) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
