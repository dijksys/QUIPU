package io.github.dijksys.quipu.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.components.QuipuConfirmDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuEmptyState
import io.github.dijksys.quipu.core.designsystem.components.QuipuTopAppBar
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(
    onBack: () -> Unit,
    onCreateReminder: (String) -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val messagePinned = stringResource(R.string.note_pinned)
    val messageUnpinned = stringResource(R.string.note_unpinned)
    val messageError = stringResource(R.string.error_generic)
    val messageWriteBeforeReminder = stringResource(R.string.note_write_before_reminder)

    val leaveEditor: () -> Unit = {
        scope.launch(Dispatchers.Main.immediate) {
            viewModel.saveNow()
            if (viewModel.uiState.value.saveStatus != NoteSaveStatus.ERROR) {
                onBack()
            }
        }
    }

    BackHandler(onBack = leaveEditor)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.flush()
    }

    LaunchedEffect(state.isLoading, state.isNewSession) {
        if (!state.isLoading && state.isNewSession) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteEditorEvent.PinChanged -> snackbarHostState.showSnackbar(
                    if (event.pinned) messagePinned else messageUnpinned,
                )

                is NoteEditorEvent.ArchiveChanged -> {
                    onBack()
                }

                NoteEditorEvent.Deleted -> onBack()
                NoteEditorEvent.Failure -> snackbarHostState.showSnackbar(messageError)
            }
        }
    }

    val editorTitle = stringResource(
        if (state.isNewSession) R.string.note_editor_new else R.string.note_editor_edit,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            QuipuTopAppBar(
                title = editorTitle,
                onBack = leaveEditor,
                actions = {
                    IconButton(
                        onClick = viewModel::togglePin,
                        enabled = state.hasText,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(
                                if (state.isPinned) R.string.note_unpin else R.string.note_pin,
                            ),
                            tint = if (state.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setArchived(!state.isArchived) },
                        enabled = state.hasText,
                    ) {
                        Icon(
                            imageVector = if (state.isArchived) {
                                Icons.Rounded.Unarchive
                            } else {
                                Icons.Rounded.Archive
                            },
                            contentDescription = stringResource(
                                if (state.isArchived) R.string.note_unarchive else R.string.note_archive,
                            ),
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.note_create_reminder)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                                },
                                enabled = state.hasText,
                                onClick = {
                                    showOverflowMenu = false
                                    scope.launch(Dispatchers.Main.immediate) {
                                        val noteId = viewModel.saveNow()
                                        if (noteId != null) {
                                            onCreateReminder(noteId)
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                messageWriteBeforeReminder,
                                            )
                                        }
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.note_move_to_trash)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog = true
                                },
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
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.note_not_found_title),
                message = stringResource(R.string.note_not_found_message),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                actionLabel = stringResource(R.string.action_back),
                onAction = leaveEditor,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(horizontal = QuipuSpacing.screenHorizontal),
            ) {
                TextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("note_title"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.note_editor_title_placeholder),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = transparentTextFieldColors(),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextField(
                    value = state.content,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("note_content"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.note_editor_content_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = transparentTextFieldColors(),
                )
                NoteSaveStatusFooter(
                    saveStatus = state.saveStatus,
                    createdAt = state.createdAt,
                    updatedAt = state.updatedAt,
                    justCreated = state.justCreated,
                    onRetry = viewModel::retrySave,
                )
            }
        }
    }

    if (showDeleteDialog) {
        QuipuConfirmDialog(
            title = stringResource(R.string.note_trash_dialog_title),
            message = stringResource(R.string.note_trash_dialog_message),
            confirmLabel = stringResource(R.string.note_move_to_trash),
            onConfirm = {
                showDeleteDialog = false
                viewModel.moveToTrash()
            },
            onDismiss = { showDeleteDialog = false },
            destructive = true,
        )
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
)
