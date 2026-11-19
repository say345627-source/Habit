package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSettings
import com.example.notifications.NotificationHelper
import com.example.ui.components.TimePickerModalDialog
import com.example.ui.components.formatTime
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    settings: UserSettings,
    hasNotificationPermission: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetGlobalReminders: (Boolean) -> Unit,
    onSetEndOfDayReminder: (Boolean, Int, Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val permGranted = hasNotificationPermission || NotificationHelper.hasNotificationPermission(context)
    var showEodTimePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Appearance Section
        item {
            SettingsCardSection(
                title = strings.appearanceSection,
                icon = Icons.Rounded.Brightness6
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to strings.themeSystem,
                        ThemeMode.LIGHT to strings.themeLight,
                        ThemeMode.DARK to strings.themeDark
                    )
                    modes.forEachIndexed { idx, (mode, label) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                            modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        // Language Section
        item {
            SettingsCardSection(
                title = strings.languageSection,
                icon = Icons.Rounded.Language
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val langs = listOf(
                        AppLanguage.AR to strings.languageArabic,
                        AppLanguage.EN to strings.languageEnglish
                    )
                    langs.forEachIndexed { idx, (lang, label) ->
                        SegmentedButton(
                            selected = settings.language == lang,
                            onClick = { onSetLanguage(lang) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = langs.size),
                            modifier = Modifier.testTag("language_option_${lang.name.lowercase()}")
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        // Reminders Section
        item {
            SettingsCardSection(
                title = strings.remindersSection,
                icon = Icons.Rounded.NotificationsActive
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permGranted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsPaused,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(strings.notificationPermTitle, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        strings.notificationPermMissing,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.testTag("grant_notification_permission")
                            ) {
                                Text(strings.grantPermission)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.globalRemindersTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                strings.globalRemindersSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.globalRemindersEnabled,
                            onCheckedChange = { enabled ->
                                onSetGlobalReminders(enabled)
                                if (enabled) onRequestNotificationPermission()
                            },
                            modifier = Modifier.testTag("global_reminders_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.endOfDayTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                strings.endOfDaySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.endOfDayReminderEnabled,
                            enabled = settings.globalRemindersEnabled,
                            onCheckedChange = { enabled ->
                                onSetEndOfDayReminder(enabled, settings.endOfDayHour, settings.endOfDayMinute)
                                if (enabled) onRequestNotificationPermission()
                            },
                            modifier = Modifier.testTag("eod_reminder_switch")
                        )
                    }

                    if (settings.globalRemindersEnabled && settings.endOfDayReminderEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showEodTimePicker = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("eod_time_button"),
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
                                Text(strings.reminderTimeLabel, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = formatTime(settings.endOfDayHour, settings.endOfDayMinute),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Backup & Restore Section
        item {
            SettingsCardSection(
                title = strings.backupSection,
                icon = Icons.Rounded.CloudDownload
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onExportBackupClick)
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("export_backup_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.exportBackupTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                strings.exportBackupSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onImportBackupClick)
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("import_backup_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.importBackupTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                strings.importBackupSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // About Section
        item {
            SettingsCardSection(
                title = strings.aboutSection,
                icon = Icons.Rounded.Info
            ) {
                Text(
                    text = strings.aboutSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showEodTimePicker) {
        TimePickerModalDialog(
            initialHour = settings.endOfDayHour,
            initialMinute = settings.endOfDayMinute,
            onDismiss = { showEodTimePicker = false },
            onConfirm = { h, m ->
                onSetEndOfDayReminder(true, h, m)
                showEodTimePicker = false
            }
        )
    }
}

@Composable
private fun SettingsCardSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            content()
        }
    }
}
