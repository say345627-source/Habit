package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.HabitEntity
import com.example.domain.BadgeType
import com.example.notifications.NotificationHelper
import com.example.ui.components.CelebrationConfettiOverlay
import com.example.ui.components.HabitEditorSheet
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.state.UiFeedbackEvent
import com.example.ui.state.WateeraViewModel
import com.example.ui.theme.WateeraTheme
import com.example.util.DayKey

const val ROUTE_TODAY = "today"
const val ROUTE_STATS = "stats"
const val ROUTE_HABITS = "habits"
const val ROUTE_SETTINGS = "settings"

private data class NavTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun WateeraApp(viewModel: WateeraViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val todayUiState by viewModel.todayUiState.collectAsStateWithLifecycle()
    val statsUiState by viewModel.statsUiState.collectAsStateWithLifecycle()
    val activeHabits by viewModel.activeHabitsState.collectAsStateWithLifecycle()
    val archivedHabits by viewModel.archivedHabitsState.collectAsStateWithLifecycle()

    val appStrings = if (settings.language == AppLanguage.AR) ArabicStrings else EnglishStrings
    val layoutDirection = if (settings.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var confettiCounter by remember { mutableIntStateOf(0) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var hasNotifPerm by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { event ->
            when (event) {
                is UiFeedbackEvent.HabitCompleted -> {
                    confettiCounter++
                    val message = if (event.unlockedBadge != null) {
                        val badgeTitle = when (event.unlockedBadge) {
                            BadgeType.STREAK_7 -> appStrings.badge7Title
                            BadgeType.STREAK_30 -> appStrings.badge30Title
                            BadgeType.STREAK_100 -> appStrings.badge100Title
                        }
                        appStrings.badgeUnlockedBanner(badgeTitle)
                    } else {
                        appStrings.habitCompletedBanner(event.habitName)
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }

                is UiFeedbackEvent.BackupExported -> {
                    snackbarHostState.showSnackbar(
                        appStrings.exportSuccess(event.stats.habitsCount, event.stats.logsCount)
                    )
                }

                is UiFeedbackEvent.BackupImported -> {
                    snackbarHostState.showSnackbar(
                        appStrings.importSuccess(event.stats.habitsCount, event.stats.logsCount)
                    )
                }

                UiFeedbackEvent.BackupFailed -> {
                    snackbarHostState.showSnackbar(appStrings.backupError)
                }
            }
        }
    }

    val tabs = remember {
        listOf(
            NavTab(ROUTE_TODAY, Icons.Rounded.Today, Icons.Outlined.Today, "nav_today"),
            NavTab(ROUTE_STATS, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_stats"),
            NavTab(ROUTE_HABITS, Icons.Rounded.Checklist, Icons.Outlined.Checklist, "nav_habits"),
            NavTab(ROUTE_SETTINGS, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    fun labelForRoute(route: String): String = when (route) {
        ROUTE_TODAY -> appStrings.navToday
        ROUTE_STATS -> appStrings.navStats
        ROUTE_HABITS -> appStrings.navHabits
        ROUTE_SETTINGS -> appStrings.navSettings
        else -> ""
    }

    WateeraTheme(themeMode = settings.themeMode) {
        CompositionLocalProvider(
            LocalAppStrings provides appStrings,
            LocalLayoutDirection provides layoutDirection
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            fun navigateToTab(route: String) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                tabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = labelForRoute(tab.route)
                                            )
                                        },
                                        label = { Text(labelForRoute(tab.route)) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                tabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = labelForRoute(tab.route)
                                            )
                                        },
                                        label = { Text(labelForRoute(tab.route)) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_TODAY,
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxSize()
                            ) {
                                composable(ROUTE_TODAY) {
                                    TodayScreen(
                                        uiState = todayUiState,
                                        themeMode = settings.themeMode,
                                        onSelectDay = viewModel::selectDay,
                                        onJumpToToday = viewModel::jumpToToday,
                                        onLoadMoreHistory = viewModel::loadMoreHistoryDays,
                                        onIncrementPart = viewModel::incrementPart,
                                        onIncrementPartForDay = viewModel::incrementPartForDay,
                                        onDecrementPart = viewModel::decrementPart,
                                        onCompleteAllParts = viewModel::completeAllParts,
                                        onResetHabitDay = viewModel::resetHabitDay,
                                        onOpenAddHabit = {
                                            editingHabit = null
                                            editorOpen = true
                                        },
                                        onEditHabit = { habit ->
                                            editingHabit = habit
                                            editorOpen = true
                                        },
                                        onArchiveHabit = { id ->
                                            viewModel.setArchived(id, true)
                                        }
                                    )
                                }

                                composable(ROUTE_STATS) {
                                    StatsScreen(
                                        uiState = statsUiState,
                                        themeMode = settings.themeMode,
                                        onSelectHabitFilter = viewModel::selectHabitFilter,
                                        onPreviousMonth = viewModel::previousHeatmapMonth,
                                        onNextMonth = viewModel::nextHeatmapMonth,
                                        onSelectHeatmapDay = viewModel::selectHeatmapDay
                                    )
                                }

                                composable(ROUTE_HABITS) {
                                    HabitsScreen(
                                        activeHabits = activeHabits,
                                        archivedHabits = archivedHabits,
                                        onOpenAddHabit = {
                                            editingHabit = null
                                            editorOpen = true
                                        },
                                        onEditHabit = { habit ->
                                            editingHabit = habit
                                            editorOpen = true
                                        },
                                        onArchiveHabit = viewModel::setArchived,
                                        onDeleteHabit = viewModel::deleteHabit,
                                        onPersistOrder = viewModel::persistHabitOrder
                                    )
                                }

                                composable(ROUTE_SETTINGS) {
                                    SettingsScreen(
                                        settings = settings,
                                        hasNotificationPermission = hasNotifPerm,
                                        onSetThemeMode = viewModel::setThemeMode,
                                        onSetLanguage = viewModel::setLanguage,
                                        onSetGlobalReminders = viewModel::setGlobalReminders,
                                        onSetEndOfDayReminder = viewModel::setEndOfDayReminder,
                                        onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded,
                                        onExportBackupClick = {
                                            exportBackupLauncher.launch("wateera-backup-${DayKey.today().isoString}.json")
                                        },
                                        onImportBackupClick = {
                                            importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                        }
                                    )
                                }
                            }

                            CelebrationConfettiOverlay(triggerKey = confettiCounter)
                        }
                    }
                }

                if (editorOpen) {
                    HabitEditorSheet(
                        editingHabit = editingHabit,
                        onDismiss = { editorOpen = false },
                        onSave = viewModel::saveHabit,
                        onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded
                    )
                }
            }
        }
    }
}
