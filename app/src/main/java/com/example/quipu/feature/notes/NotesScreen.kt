package com.example.quipu.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.quipu.R
import com.example.quipu.core.designsystem.components.QuipuConfirmDialog
import com.example.quipu.core.designsystem.components.QuipuEmptyState
import com.example.quipu.core.designsystem.components.QuipuSearchField
import com.example.quipu.core.designsystem.components.QuipuSectionHeader
import com.example.quipu.core.designsystem.components.QuipuTextButton
import com.example.quipu.core.designsystem.components.QuipuTopAppBar
import com.example.quipu.core.designsystem.theme.QuipuSpacing
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.NoteFilter

@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onCreateNote: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    val messagePinned = stringResource(R.string.note_pinned)
    val messageUnpinned = stringResource(R.string.note_unpinned)
    val messageArchived = stringResource(R.string.note_archived)
    val messageUnarchived = stringResource(R.string.note_unarchived)
    val messageTrashed = stringResource(R.string.note_trashed)
    val messageUndo = stringResource(R.string.action_undo)
    val messageRestored = stringResource(R.string.note_restored)
    val messageDeleted = stringResource(R.string.note_deleted_permanently)
    val messageTrashEmptied = stringResource(R.string.trash_emptied)
    val messageError = stringResource(R.string.error_generic)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesEvent.PinChanged -> snackbarHostState.showSnackbar(
                    if (event.pinned) messagePinned else messageUnpinned,
                )

                is NotesEvent.ArchiveChanged -> snackbarHostState.showSnackbar(
                    if (event.archived) messageArchived else messageUnarchived,
                )

                is NotesEvent.Trashed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = messageTrashed,
                        actionLabel = messageUndo,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restore(event.noteId)
                    }
                }

                NotesEvent.Restored -> snackbarHostState.showSnackbar(messageRestored)

                NotesEvent.PermanentlyDeleted -> snackbarHostState.showSnackbar(messageDeleted)

                NotesEvent.TrashEmptied -> snackbarHostState.showSnackbar(messageTrashEmptied)

                NotesEvent.Failure -> snackbarHostState.showSnackbar(messageError)
            }
        }
    }

    val title = when (state.filter) {
        NoteFilter.ACTIVE -> stringResource(R.string.notes_title)
        NoteFilter.ARCHIVED -> stringResource(R.string.notes_archived_title)
        NoteFilter.TRASH -> stringResource(R.string.notes_trash_title)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            QuipuTopAppBar(
                title = title,
                onBack = if (state.filter == NoteFilter.ACTIVE) null else onBack,
                actions = {
                    when (state.filter) {
                        NoteFilter.ACTIVE -> {
                            IconButton(onClick = onOpenArchive) {
                                Icon(
                                    imageVector = Icons.Rounded.Archive,
                                    contentDescription = stringResource(R.string.notes_open_archive),
                                )
                            }
                            IconButton(onClick = onOpenTrash) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.notes_open_trash),
                                )
                            }
                        }

                        NoteFilter.ARCHIVED -> {
                            IconButton(onClick = onOpenTrash) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.notes_open_trash),
                                )
                            }
                        }

                        NoteFilter.TRASH -> {
                            if (state.notes.isNotEmpty()) {
                                QuipuTextButton(
                                    text = stringResource(R.string.trash_empty_action),
                                    onClick = { showEmptyTrashDialog = true },
                                    destructive = true,
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.filter == NoteFilter.ACTIVE) {
                ExtendedFloatingActionButton(
                    onClick = onCreateNote,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.notes_new_note)) },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            QuipuSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.notes_search_placeholder),
                modifier = Modifier.padding(
                    start = QuipuSpacing.screenHorizontal,
                    end = QuipuSpacing.screenHorizontal,
                    top = QuipuSpacing.xs,
                    bottom = QuipuSpacing.md,
                ),
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                state.isEmpty -> EmptyNotesState(
                    filter = state.filter,
                    hasQuery = state.query.isNotBlank(),
                    onBack = onBack,
                    onCreateNote = onCreateNote,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = QuipuSpacing.screenHorizontal,
                        end = QuipuSpacing.screenHorizontal,
                        bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(QuipuSpacing.sm),
                ) {
                    if (state.filter == NoteFilter.ACTIVE && state.query.isBlank()) {
                        val pinned = state.notes.filter { it.isPinned }
                        val others = state.notes.filterNot { it.isPinned }
                        if (pinned.isNotEmpty()) {
                            item {
                                QuipuSectionHeader(
                                    title = stringResource(R.string.notes_pinned_section),
                                )
                            }
                            items(pinned, key = { it.id }) { note ->
                                NoteRow(
                                    note = note,
                                    filter = state.filter,
                                    viewModel = viewModel,
                                    onOpenNote = onOpenNote,
                                    onRequestDeletePermanently = { noteToDelete = it },
                                )
                            }
                        }
                        if (others.isNotEmpty()) {
                            if (pinned.isNotEmpty()) {
                                item {
                                    QuipuSectionHeader(
                                        title = stringResource(R.string.notes_others_section),
                                    )
                                }
                            }
                            items(others, key = { it.id }) { note ->
                                NoteRow(
                                    note = note,
                                    filter = state.filter,
                                    viewModel = viewModel,
                                    onOpenNote = onOpenNote,
                                    onRequestDeletePermanently = { noteToDelete = it },
                                )
                            }
                        }
                    } else {
                        items(state.notes, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                filter = state.filter,
                                viewModel = viewModel,
                                onOpenNote = onOpenNote,
                                onRequestDeletePermanently = { noteToDelete = it },
                            )
                        }
                    }
                }
            }
        }
    }

    noteToDelete?.let { note ->
        QuipuConfirmDialog(
            title = stringResource(R.string.note_delete_dialog_title),
            message = stringResource(R.string.note_delete_dialog_message, note.displayTitle),
            confirmLabel = stringResource(R.string.note_delete_permanently),
            onConfirm = {
                viewModel.deletePermanently(note.id)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null },
            destructive = true,
        )
    }

    if (showEmptyTrashDialog) {
        QuipuConfirmDialog(
            title = stringResource(R.string.trash_empty_dialog_title),
            message = stringResource(R.string.trash_empty_dialog_message),
            confirmLabel = stringResource(R.string.trash_empty_action),
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            },
            onDismiss = { showEmptyTrashDialog = false },
            destructive = true,
        )
    }
}

@Composable
private fun NoteRow(
    note: Note,
    filter: NoteFilter,
    viewModel: NotesViewModel,
    onOpenNote: (String) -> Unit,
    onRequestDeletePermanently: (Note) -> Unit,
) {
    NoteListItem(
        note = note,
        onClick = {
            if (filter != NoteFilter.TRASH) onOpenNote(note.id)
        },
        actions = {
            NoteActionsMenu(
                note = note,
                filter = filter,
                onTogglePin = { viewModel.togglePin(note) },
                onSetArchived = { archived -> viewModel.setArchived(note, archived) },
                onMoveToTrash = { viewModel.moveToTrash(note) },
                onRestore = { viewModel.restore(note.id) },
                onDeletePermanently = { onRequestDeletePermanently(note) },
            )
        },
    )
}

@Composable
internal fun EmptyNotesState(
    filter: NoteFilter,
    hasQuery: Boolean,
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
) {
    val icon = when {
        hasQuery -> Icons.Rounded.SearchOff
        filter == NoteFilter.ARCHIVED -> Icons.Rounded.Archive
        filter == NoteFilter.TRASH -> Icons.Rounded.DeleteOutline
        else -> Icons.Rounded.Description
    }
    val title = when {
        hasQuery -> stringResource(R.string.notes_search_empty_title)
        filter == NoteFilter.ARCHIVED -> stringResource(R.string.notes_archived_empty_title)
        filter == NoteFilter.TRASH -> stringResource(R.string.notes_trash_empty_title)
        else -> stringResource(R.string.notes_empty_title)
    }
    val message = when {
        hasQuery -> stringResource(R.string.notes_search_empty_message)
        filter == NoteFilter.ARCHIVED -> stringResource(R.string.notes_archived_empty_message)
        filter == NoteFilter.TRASH -> stringResource(R.string.notes_trash_empty_message)
        else -> stringResource(R.string.notes_empty_message)
    }

    QuipuEmptyState(
        icon = icon,
        title = title,
        message = message,
        modifier = Modifier.fillMaxSize(),
        actionLabel = when {
            hasQuery -> null
            filter == NoteFilter.ACTIVE -> stringResource(R.string.notes_empty_action)
            else -> stringResource(R.string.action_back_to_notes)
        },
        onAction = when {
            hasQuery -> null
            filter == NoteFilter.ACTIVE -> onCreateNote
            else -> onBack
        },
    )
}
