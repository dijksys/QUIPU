package io.github.dijksys.quipu.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.feature.home.HomeScreen
import io.github.dijksys.quipu.feature.notes.NoteEditorScreen
import io.github.dijksys.quipu.feature.notes.NotesScreen
import io.github.dijksys.quipu.feature.reminders.ReminderEditorScreen
import io.github.dijksys.quipu.feature.reminders.RemindersScreen
import io.github.dijksys.quipu.feature.settings.SettingsScreen

@Composable
fun AppNavHost(
    pendingReminderId: String?,
    onPendingReminderConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in AppDestination.topLevelRoutes

    LaunchedEffect(pendingReminderId) {
        val reminderId = pendingReminderId ?: return@LaunchedEffect
        navController.navigate(AppDestination.REMINDERS) {
            launchSingleTop = true
        }
        navController.navigate(AppDestination.reminderEditor(reminderId))
        onPendingReminderConsumed()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(AppDestination.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.HOME,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .consumeWindowInsets(
                    if (showBottomBar) WindowInsets.navigationBars else WindowInsets(0, 0, 0, 0),
                ),
        ) {
            composable(AppDestination.HOME) {
                HomeScreen(
                    onOpenNotes = { navController.navigate(AppDestination.notes()) },
                    onOpenReminders = { navController.navigate(AppDestination.REMINDERS) },
                    onOpenNote = { noteId ->
                        navController.navigate(AppDestination.noteEditor(noteId))
                    },
                    onOpenReminder = { reminderId ->
                        navController.navigate(AppDestination.reminderEditor(reminderId))
                    },
                    onCreateNote = { navController.navigate(AppDestination.noteEditor()) },
                    onCreateReminder = { navController.navigate(AppDestination.reminderEditor()) },
                )
            }

            composable(
                route = AppDestination.NOTES,
                arguments = listOf(
                    navArgument(AppDestination.ARG_FILTER) {
                        type = NavType.StringType
                        defaultValue = NoteFilter.ACTIVE.name
                    },
                ),
            ) {
                NotesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { noteId ->
                        navController.navigate(AppDestination.noteEditor(noteId))
                    },
                    onCreateNote = { navController.navigate(AppDestination.noteEditor()) },
                    onOpenArchive = {
                        navController.navigate(AppDestination.notes(NoteFilter.ARCHIVED))
                    },
                    onOpenTrash = {
                        navController.navigate(AppDestination.notes(NoteFilter.TRASH))
                    },
                )
            }

            composable(
                route = AppDestination.NOTE_EDITOR,
                arguments = listOf(navArgument(AppDestination.ARG_NOTE_ID) {
                    type = NavType.StringType
                }),
            ) {
                NoteEditorScreen(
                    onBack = { navController.popBackStack() },
                    onCreateReminder = { noteId ->
                        navController.navigate(AppDestination.reminderEditor(noteId = noteId))
                    },
                )
            }

            composable(AppDestination.REMINDERS) {
                RemindersScreen(
                    onCreateReminder = { navController.navigate(AppDestination.reminderEditor()) },
                    onOpenReminder = { reminderId ->
                        navController.navigate(AppDestination.reminderEditor(reminderId))
                    },
                    onOpenSettings = { navController.navigate(AppDestination.SETTINGS) },
                )
            }

            composable(
                route = AppDestination.REMINDER_EDITOR,
                arguments = listOf(
                    navArgument(AppDestination.ARG_REMINDER_ID) {
                        type = NavType.StringType
                    },
                    navArgument(AppDestination.ARG_NOTE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                ReminderEditorScreen(onBack = { navController.popBackStack() })
            }

            composable(AppDestination.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
