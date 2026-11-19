package io.github.dijksys.quipu.feature.reminders

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.components.QuipuButton
import io.github.dijksys.quipu.core.designsystem.components.QuipuCard
import io.github.dijksys.quipu.core.designsystem.components.QuipuConfirmDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuEmptyState
import io.github.dijksys.quipu.core.designsystem.components.QuipuFilterChip
import io.github.dijksys.quipu.core.designsystem.components.QuipuTopAppBar
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.core.permissions.rememberExactAlarmPermissionController
import io.github.dijksys.quipu.core.permissions.rememberNotificationPermissionController
import io.github.dijksys.quipu.domain.model.Reminder
import io.github.dijksys.quipu.domain.model.ReminderFilter
import io.github.dijksys.quipu.domain.model.ReminderStatus

@Composable
fun RemindersScreen(
    onCreateReminder: () -> Unit,
    onOpenReminder: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermission = rememberNotificationPermissionController()
    val exactAlarmPermission = rememberExactAlarmPermissionController()
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }

    val messageCompleted = stringResource(R.string.reminder_completed_message)
    val messageCancelled = stringResource(R.string.reminder_cancelled_message)
    val messageDeleted = stringResource(R.string.reminder_deleted_message)
    val messageError = stringResource(R.string.error_generic)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RemindersEvent.Completed -> snackbarHostState.showSnackbar(messageCompleted)

                RemindersEvent.Cancelled -> snackbarHostState.showSnackbar(messageCancelled)

                RemindersEvent.Deleted -> snackbarHostState.showSnackbar(messageDeleted)

                RemindersEvent.Failure -> snackbarHostState.showSnackbar(messageError)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { QuipuTopAppBar(title = stringResource(R.string.reminders_title)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateReminder,
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.reminders_new_reminder)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = QuipuSpacing.screenHorizontal,
                end = QuipuSpacing.screenHorizontal,
                top = QuipuSpacing.xs,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(QuipuSpacing.md),
        ) {
            if (!state.notificationsEnabled) {
                item {
                    PermissionBanner(
                        icon = Icons.Rounded.NotificationsOff,
                        title = stringResource(R.string.reminders_notifications_off_title),
                        message = stringResource(R.string.reminders_notifications_off_message),
                        primaryLabel = stringResource(R.string.reminders_open_settings),
                        onPrimary = onOpenSettings,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            } else if (!notificationPermission.granted) {
                item {
                    PermissionBanner(
                        icon = Icons.Rounded.WarningAmber,
                        title = stringResource(R.string.reminders_notification_permission_title),
                        message = stringResource(R.string.reminders_notification_permission_message),
                        primaryLabel = stringResource(R.string.reminders_allow_notifications),
                        onPrimary = notificationPermission.request,
                        secondaryLabel = stringResource(R.string.reminders_open_settings),
                        onSecondary = notificationPermission.openSettings,
                    )
                }
            }

            if (state.notificationsEnabled && exactAlarmPermission.isSupported && !exactAlarmPermission.granted) {
                item {
                    PermissionBanner(
                        icon = Icons.Rounded.ErrorOutline,
                        title = stringResource(R.string.reminders_exact_alarm_title),
                        message = stringResource(R.string.reminders_exact_alarm_message),
                        primaryLabel = stringResource(R.string.reminders_allow_exact_alarms),
                        onPrimary = exactAlarmPermission.request,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.sm),
                ) {
                    ReminderFilter.entries.forEach { filter ->
                        val count = state.countFor(filter)
                        val label = stringResource(filter.labelRes()) +
                            if (count > 0) " · $count" else ""
                        QuipuFilterChip(
                            text = label,
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                        )
                    }
                }
            }

            when {
                state.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = QuipuSpacing.xxxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.isEmpty -> item {
                    ReminderEmptyState(
                        filter = state.filter,
                        onCreateReminder = onCreateReminder,
                    )
                }

                else -> items(state.visibleReminders, key = { it.id }) { reminder ->
                    ReminderListItem(
                        reminder = reminder,
                        nowMillis = state.nowMillis,
                        onClick = { onOpenReminder(reminder.id) },
                        onComplete = {
                            if (reminder.status == ReminderStatus.PENDING) {
                                viewModel.complete(reminder)
                            }
                        },
                        actions = {
                            ReminderActionsMenu(
                                reminder = reminder,
                                onEdit = { onOpenReminder(reminder.id) },
                                onComplete = { viewModel.complete(reminder) },
                                onCancel = { viewModel.cancel(reminder) },
                                onDelete = { reminderToDelete = reminder },
                            )
                        },
                    )
                }
            }
        }
    }

    reminderToDelete?.let { reminder ->
        QuipuConfirmDialog(
            title = stringResource(R.string.reminder_delete_dialog_title),
            message = stringResource(R.string.reminder_delete_dialog_message, reminder.title),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(reminder)
                reminderToDelete = null
            },
            onDismiss = { reminderToDelete = null },
            destructive = true,
        )
    }
}

@Composable
private fun PermissionBanner(
    icon: ImageVector,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(modifier = Modifier.padding(QuipuSpacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(QuipuSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(QuipuSpacing.xxs))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(QuipuSpacing.sm))
            Row(
                horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuipuButton(text = primaryLabel, onClick = onPrimary)
                if (secondaryLabel != null && onSecondary != null) {
                    io.github.dijksys.quipu.core.designsystem.components.QuipuTextButton(
                        text = secondaryLabel,
                        onClick = onSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderActionsMenu(
    reminder: Reminder,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.action_more_options),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = {
                expanded = false
                onEdit()
            },
        )
        if (reminder.status == ReminderStatus.PENDING) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reminder_complete)) },
                leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                onClick = {
                    expanded = false
                    onComplete()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reminder_cancel)) },
                leadingIcon = { Icon(Icons.Rounded.Cancel, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCancel()
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                expanded = false
                onDelete()
            },
        )
    }
}

@Composable
internal fun ReminderEmptyState(
    filter: ReminderFilter,
    onCreateReminder: () -> Unit,
) {
    val icon = when (filter) {
        ReminderFilter.PENDING -> Icons.Rounded.NotificationsNone
        ReminderFilter.OVERDUE -> Icons.Rounded.AccessTime
        ReminderFilter.COMPLETED -> Icons.Rounded.CheckCircle
        ReminderFilter.CANCELLED -> Icons.Rounded.Cancel
    }
    val title = when (filter) {
        ReminderFilter.PENDING -> stringResource(R.string.reminders_empty_pending_title)
        ReminderFilter.OVERDUE -> stringResource(R.string.reminders_empty_overdue_title)
        ReminderFilter.COMPLETED -> stringResource(R.string.reminders_empty_completed_title)
        ReminderFilter.CANCELLED -> stringResource(R.string.reminders_empty_cancelled_title)
    }
    val message = when (filter) {
        ReminderFilter.PENDING -> stringResource(R.string.reminders_empty_pending_message)
        ReminderFilter.OVERDUE -> stringResource(R.string.reminders_empty_overdue_message)
        ReminderFilter.COMPLETED -> stringResource(R.string.reminders_empty_completed_message)
        ReminderFilter.CANCELLED -> stringResource(R.string.reminders_empty_cancelled_message)
    }

    QuipuEmptyState(
        icon = icon,
        title = title,
        message = message,
        actionLabel = if (filter == ReminderFilter.PENDING) {
            stringResource(R.string.reminders_empty_action)
        } else {
            null
        },
        onAction = if (filter == ReminderFilter.PENDING) onCreateReminder else null,
    )
}

private fun ReminderFilter.labelRes(): Int = when (this) {
    ReminderFilter.PENDING -> R.string.reminder_filter_pending
    ReminderFilter.OVERDUE -> R.string.reminder_filter_overdue
    ReminderFilter.COMPLETED -> R.string.reminder_filter_completed
    ReminderFilter.CANCELLED -> R.string.reminder_filter_cancelled
}
