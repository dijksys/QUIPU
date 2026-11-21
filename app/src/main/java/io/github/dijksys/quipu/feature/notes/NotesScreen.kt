package io.github.dijksys.quipu.feature.notes

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.common.DateTimeUtils
import io.github.dijksys.quipu.core.designsystem.components.QuipuConfirmDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuDatePickerDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuEmptyState
import io.github.dijksys.quipu.core.designsystem.components.QuipuFilterChip
import io.github.dijksys.quipu.core.designsystem.components.QuipuSearchField
import io.github.dijksys.quipu.core.designsystem.components.QuipuSectionHeader
import io.github.dijksys.quipu.core.designsystem.components.QuipuTextButton
import io.github.dijksys.quipu.core.designsystem.components.QuipuTopAppBar
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteDateFilter
import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import io.github.dijksys.quipu.domain.search.SearchText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var showCustomRangeDialog by remember { mutableStateOf(false) }

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
    val highlightTerms = if (state.isSearching) {
        SearchText.terms(state.query)
    } else {
        emptyList()
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
                modifier = Modifier
                    .padding(
                        start = QuipuSpacing.screenHorizontal,
                        end = QuipuSpacing.screenHorizontal,
                        top = QuipuSpacing.xs,
                        bottom = QuipuSpacing.sm,
                    )
                    .testTag("notes_search"),
            )

            if (state.isSearching) {
                SearchSummary(
                    query = state.query,
                    resultCount = state.notes.size,
                    isLoading = state.isLoading,
                    dateFilter = state.dateFilter,
                    sortOrder = state.sortOrder,
                    hasActiveFilters = state.hasActiveFilters,
                    onDateFilterChange = viewModel::onDateFilterChange,
                    onCustomRange = { showCustomRangeDialog = true },
                    onSortOrderChange = viewModel::onSortOrderChange,
                    onClear = viewModel::clearSearch,
                )
            }

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
                                    highlightTerms = highlightTerms,
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
                                    highlightTerms = highlightTerms,
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
                                highlightTerms = highlightTerms,
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

    if (showCustomRangeDialog) {
        CustomRangeDialog(
            current = state.customDateRange,
            onApply = { range ->
                viewModel.onCustomRangeChange(range)
                showCustomRangeDialog = false
            },
            onDismiss = { showCustomRangeDialog = false },
        )
    }
}

@Composable
private fun SearchSummary(
    query: String,
    resultCount: Int,
    isLoading: Boolean,
    dateFilter: NoteDateFilter,
    sortOrder: NoteSortOrder,
    hasActiveFilters: Boolean,
    onDateFilterChange: (NoteDateFilter) -> Unit,
    onCustomRange: () -> Unit,
    onSortOrderChange: (NoteSortOrder) -> Unit,
    onClear: () -> Unit,
) {
    var showDateMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = QuipuSpacing.screenHorizontal, vertical = QuipuSpacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notes_search_query_label, query),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                )
                Text(
                    text = if (isLoading) {
                        stringResource(R.string.note_save_saving)
                    } else {
                        pluralStringResource(
                            R.plurals.notes_search_count,
                            resultCount,
                            resultCount,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasActiveFilters || query.isNotBlank()) {
                QuipuTextButton(
                    text = stringResource(R.string.notes_filter_clear),
                    onClick = onClear,
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                QuipuFilterChip(
                    text = dateFilterLabel(dateFilter),
                    selected = dateFilter != NoteDateFilter.ALL,
                    onClick = { showDateMenu = true },
                    modifier = Modifier.testTag("notes_date_filter"),
                )
                DropdownMenu(
                    expanded = showDateMenu,
                    onDismissRequest = { showDateMenu = false },
                ) {
                    NoteDateFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(dateFilterLabel(option)) },
                            onClick = {
                                showDateMenu = false
                                if (option == NoteDateFilter.CUSTOM) {
                                    onCustomRange()
                                } else {
                                    onDateFilterChange(option)
                                }
                            },
                        )
                    }
                }
            }
            Box {
                QuipuFilterChip(
                    text = sortLabel(sortOrder),
                    selected = sortOrder != NoteSortOrder.RELEVANCE,
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("notes_sort"),
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    NoteSortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(option)) },
                            onClick = {
                                showSortMenu = false
                                onSortOrderChange(option)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun dateFilterLabel(filter: NoteDateFilter): String = stringResource(
    when (filter) {
        NoteDateFilter.ALL -> R.string.notes_filter_date_all
        NoteDateFilter.TODAY -> R.string.notes_filter_date_today
        NoteDateFilter.LAST_7_DAYS -> R.string.notes_filter_date_week
        NoteDateFilter.LAST_30_DAYS -> R.string.notes_filter_date_month
        NoteDateFilter.CUSTOM -> R.string.notes_filter_date_custom
    },
)

@Composable
private fun sortLabel(order: NoteSortOrder): String = stringResource(
    when (order) {
        NoteSortOrder.RELEVANCE -> R.string.notes_sort_relevance
        NoteSortOrder.RECENT -> R.string.notes_sort_recent
        NoteSortOrder.TITLE -> R.string.notes_sort_title
    },
)

@Composable
private fun CustomRangeDialog(
    current: NoteDateRange?,
    onApply: (NoteDateRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es")) }

    fun toDate(millis: Long?): LocalDate =
        millis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: today

    var start by remember { mutableStateOf(toDate(current?.startMillis).takeIf { current != null } ?: today.minusDays(6)) }
    var end by remember {
        mutableStateOf(
            toDate(current?.endMillisExclusive?.minus(1)).takeIf { current != null } ?: today,
        )
    }
    var pickingStart by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes_custom_range_title)) },
        text = {
            Column {
                RangeField(
                    label = stringResource(R.string.notes_custom_range_start),
                    value = start.format(formatter),
                    onClick = { pickingStart = true },
                )
                Spacer(Modifier.height(QuipuSpacing.sm))
                RangeField(
                    label = stringResource(R.string.notes_custom_range_end),
                    value = end.format(formatter),
                    onClick = { pickingStart = false },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !end.isBefore(start),
                onClick = { onApply(NoteDateRange(start, end, zone)) },
            ) {
                Text(stringResource(R.string.notes_custom_range_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )

    when (pickingStart) {
        true -> QuipuDatePickerDialog(
            initialDate = start,
            onDateSelected = { start = it },
            onDismiss = { pickingStart = null },
        )

        false -> QuipuDatePickerDialog(
            initialDate = end,
            onDateSelected = { end = it },
            onDismiss = { pickingStart = null },
        )

        null -> Unit
    }
}

@Composable
private fun RangeField(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = QuipuSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClick) {
            Text(value)
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    filter: NoteFilter,
    viewModel: NotesViewModel,
    highlightTerms: List<String>,
    onOpenNote: (String) -> Unit,
    onRequestDeletePermanently: (Note) -> Unit,
) {
    NoteListItem(
        note = note,
        onClick = {
            if (filter != NoteFilter.TRASH) onOpenNote(note.id)
        },
        highlightTerms = highlightTerms,
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
