package io.github.dijksys.quipu.feature.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.common.DateTimeUtils
import io.github.dijksys.quipu.core.designsystem.components.QuipuButton
import io.github.dijksys.quipu.core.designsystem.components.QuipuCard
import io.github.dijksys.quipu.core.designsystem.components.QuipuConfirmDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuDatePickerDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuDateTimeField
import io.github.dijksys.quipu.core.designsystem.components.QuipuEmptyState
import io.github.dijksys.quipu.core.designsystem.components.QuipuInfoPill
import io.github.dijksys.quipu.core.designsystem.components.QuipuSelectableRow
import io.github.dijksys.quipu.core.designsystem.components.QuipuTextButton
import io.github.dijksys.quipu.core.designsystem.components.QuipuTimePickerDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuTopAppBar
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.core.permissions.rememberExactAlarmPermissionController
import io.github.dijksys.quipu.core.permissions.rememberNotificationPermissionController
import io.github.dijksys.quipu.domain.model.ReminderStatus

@Composable
fun ReminderEditorScreen(
    onBack: () -> Unit,
    viewModel: ReminderEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermission = rememberNotificationPermissionController()
    val exactAlarmPermission = rememberExactAlarmPermissionController()
    val messageError = stringResource(R.string.error_generic)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNotePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReminderEditorEvent.Saved,
                ReminderEditorEvent.Deleted,
                ReminderEditorEvent.Completed,
                ReminderEditorEvent.Cancelled,
                -> onBack()

                ReminderEditorEvent.Failure -> snackbarHostState.showSnackbar(messageError)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            QuipuTopAppBar(
                title = stringResource(
                    if (state.isNew) {
                        R.string.reminder_editor_new_title
                    } else {
                        R.string.reminder_editor_edit_title
                    },
                ),
                onBack = onBack,
                actions = {
                    if (!state.isNew && !state.isMissing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.isMissing -> QuipuEmptyState(
                icon = Icons.Rounded.ErrorOutline,
                title = stringResource(R.string.reminder_not_found_title),
                message = stringResource(R.string.reminder_not_found_message),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = QuipuSpacing.screenHorizontal,
                        end = QuipuSpacing.screenHorizontal,
                        top = QuipuSpacing.xs,
                        bottom = QuipuSpacing.xxxl,
                    ),
                verticalArrangement = Arrangement.spacedBy(QuipuSpacing.md),
            ) {
                state.error?.let { error ->
                    NoticeCard(
                        icon = Icons.Rounded.ErrorOutline,
                        title = stringResource(R.string.reminder_error_title),
                        message = stringResource(error.messageRes()),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    )
                }

                if (!state.notificationsEnabled) {
                    NoticeCard(
                        icon = Icons.Rounded.NotificationsOff,
                        title = stringResource(R.string.reminder_editor_notifications_off_title),
                        message = stringResource(R.string.reminder_editor_notifications_off_message),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title"),
                    label = { Text(stringResource(R.string.reminder_editor_title_label)) },
                    placeholder = { Text(stringResource(R.string.reminder_editor_title_placeholder)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = editorFieldColors(),
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.reminder_editor_description_label)) },
                    placeholder = {
                        Text(stringResource(R.string.reminder_editor_description_placeholder))
                    },
                    minLines = 2,
                    maxLines = 5,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = editorFieldColors(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.sm)) {
                    QuipuDateTimeField(
                        label = stringResource(R.string.reminder_editor_date_label),
                        value = DateTimeUtils.formatDate(state.scheduledAt),
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.CalendarMonth,
                    )
                    QuipuDateTimeField(
                        label = stringResource(R.string.reminder_editor_time_label),
                        value = DateTimeUtils.formatTime(state.scheduledAt),
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                QuipuCard(contentPadding = PaddingValues(0.dp)) {
                    QuipuSelectableRow(
                        title = state.linkedNoteTitle
                            ?: stringResource(R.string.reminder_editor_no_note),
                        subtitle = stringResource(R.string.reminder_editor_linked_note),
                        selected = false,
                        icon = Icons.Rounded.Description,
                        onClick = { showNotePicker = true },
                        trailing = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }

                if (state.notificationsEnabled && !notificationPermission.granted) {
                    NoticeCard(
                        icon = Icons.Rounded.WarningAmber,
                        title = stringResource(R.string.reminder_editor_notification_permission_title),
                        message = stringResource(R.string.reminder_editor_notification_permission_message),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        actionLabel = stringResource(R.string.reminders_allow_notifications),
                        onAction = notificationPermission.request,
                    )
                }

                if (
                    state.notificationsEnabled &&
                    exactAlarmPermission.isSupported &&
                    !exactAlarmPermission.granted
                ) {
                    NoticeCard(
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.reminder_editor_exact_alarm_title),
                        message = stringResource(R.string.reminder_editor_exact_alarm_message),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        actionLabel = stringResource(R.string.reminders_allow_exact_alarms),
                        onAction = exactAlarmPermission.request,
                    )
                }

                if (!state.isNew && state.status != ReminderStatus.PENDING) {
                    QuipuCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            QuipuInfoPill(
                                text = stringResource(
                                    if (state.status == ReminderStatus.COMPLETED) {
                                        R.string.reminder_status_completed
                                    } else {
                                        R.string.reminder_status_cancelled
                                    },
                                ),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(QuipuSpacing.sm))
                        Text(
                            text = stringResource(R.string.reminder_editor_reactivate_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                QuipuButton(
                    text = if (state.isSaving) {
                        stringResource(R.string.reminder_editor_saving)
                    } else {
                        stringResource(
                            if (state.isNew) {
                                R.string.reminder_editor_save
                            } else {
                                R.string.reminder_editor_save_changes
                            },
                        )
                    },
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.CheckCircle,
                )

                if (!state.isNew && state.status == ReminderStatus.PENDING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        QuipuTextButton(
                            text = stringResource(R.string.reminder_complete),
                            onClick = viewModel::complete,
                        )
                        QuipuTextButton(
                            text = stringResource(R.string.reminder_cancel),
                            onClick = viewModel::cancelReminder,
                        )
                    }
                }

                if (!state.isNew) {
                    QuipuTextButton(
                        text = stringResource(R.string.action_delete),
                        onClick = { showDeleteDialog = true },
                        destructive = true,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        QuipuDatePickerDialog(
            initialDate = DateTimeUtils.datePart(state.scheduledAt),
            onDateSelected = viewModel::setDate,
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        QuipuTimePickerDialog(
            initialHour = DateTimeUtils.hour(state.scheduledAt),
            initialMinute = DateTimeUtils.minute(state.scheduledAt),
            onTimeSelected = { hour, minute -> viewModel.setTime(hour, minute) },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showNotePicker) {
        AlertDialog(
            onDismissRequest = { showNotePicker = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.reminder_editor_choose_note)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    item {
                        QuipuSelectableRow(
                            title = stringResource(R.string.reminder_editor_no_note),
                            selected = state.noteId == null,
                            onClick = {
                                viewModel.selectNote(null)
                                showNotePicker = false
                            },
                        )
                    }
                    items(state.availableNotes, key = { it.id }) { note ->
                        QuipuSelectableRow(
                            title = note.displayTitle,
                            subtitle = DateTimeUtils.formatDateTime(note.updatedAt),
                            selected = state.noteId == note.id,
                            onClick = {
                                viewModel.selectNote(note.id)
                                showNotePicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotePicker = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }

    if (showDeleteDialog) {
        QuipuConfirmDialog(
            title = stringResource(R.string.reminder_delete_dialog_title),
            message = stringResource(R.string.reminder_delete_dialog_message, state.title),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
            destructive = true,
        )
    }
}

@Composable
private fun NoticeCard(
    icon: ImageVector,
    title: String,
    message: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
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
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(QuipuSpacing.sm))
                QuipuButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.primary,
)

private fun ReminderEditorError.messageRes(): Int = when (this) {
    ReminderEditorError.TITLE_REQUIRED -> R.string.reminder_error_title_required
    ReminderEditorError.TIME_IN_PAST -> R.string.reminder_error_time_in_past
    ReminderEditorError.SAVE_FAILED -> R.string.reminder_error_save_failed
    ReminderEditorError.NOT_FOUND -> R.string.reminder_error_not_found
}
