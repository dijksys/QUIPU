package io.github.dijksys.quipu.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.components.QuipuConfirmDialog
import io.github.dijksys.quipu.core.designsystem.components.QuipuEmptyState
import io.github.dijksys.quipu.core.designsystem.components.QuipuFilterChip
import io.github.dijksys.quipu.core.designsystem.components.QuipuTopAppBar
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.domain.formatting.MarkdownChecklist
import io.github.dijksys.quipu.domain.formatting.MarkdownEditResult
import io.github.dijksys.quipu.domain.formatting.MarkdownFormatting
import io.github.dijksys.quipu.domain.formatting.MarkdownSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
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
    val contentFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current
    var editorValue by remember(state.noteId) {
        mutableStateOf(TextFieldValue(state.content))
    }
    var isPreview by rememberSaveable(state.noteId) { mutableStateOf(false) }
    var contentFocused by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkLabel by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var linkSelection by remember { mutableStateOf(MarkdownSelection(0, 0)) }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val imeVisible = WindowInsets.isImeVisible
    val showToolbar = !isPreview && contentFocused && imeVisible

    val linkColor = MaterialTheme.colorScheme.primary
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant
    val codeBlockBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val markerColor = MaterialTheme.colorScheme.primary
    val visualTransformation = remember(
        linkColor,
        inlineCodeBackground,
        codeBlockBackground,
        markerColor,
    ) {
        MarkdownVisualTransformation(
            linkColor = linkColor,
            inlineCodeBackground = inlineCodeBackground,
            codeBlockBackground = codeBlockBackground,
            markerColor = markerColor,
        )
    }

    val messagePinned = stringResource(R.string.note_pinned)
    val messageUnpinned = stringResource(R.string.note_unpinned)
    val messageError = stringResource(R.string.error_generic)
    val messageWriteBeforeReminder = stringResource(R.string.note_write_before_reminder)
    val messageLinkError = stringResource(R.string.note_markdown_link_error)

    LaunchedEffect(state.content, state.isLoading) {
        if (!state.isLoading && editorValue.text != state.content) {
            val start = editorValue.selection.start.coerceIn(0, state.content.length)
            val end = editorValue.selection.end.coerceIn(0, state.content.length)
            editorValue = TextFieldValue(
                text = state.content,
                selection = TextRange(start, end),
            )
        }
    }

    val currentSelection = {
        val start = minOf(editorValue.selection.start, editorValue.selection.end)
        val end = maxOf(editorValue.selection.start, editorValue.selection.end)
        MarkdownSelection(start, end)
    }

    val applyContent: (MarkdownEditResult) -> Unit = { result ->
        editorValue = TextFieldValue(
            text = result.text,
            selection = TextRange(result.selection.start, result.selection.end),
        )
        if (result.text != state.content) {
            viewModel.onContentChange(result.text)
        }
        if (!isPreview) {
            contentFocusRequester.requestFocus()
        }
    }

    val openLinkDialog: () -> Unit = {
        val selection = currentSelection()
        linkSelection = selection
        linkLabel = if (selection.start != selection.end) {
            editorValue.text.substring(selection.start, selection.end)
        } else {
            ""
        }
        linkUrl = ""
        showLinkDialog = true
    }

    val applyToolbarAction: (MarkdownToolbarAction) -> Unit = { action ->
        when (action) {
            MarkdownToolbarAction.BOLD -> applyContent(
                MarkdownFormatting.toggleBold(editorValue.text, currentSelection()),
            )

            MarkdownToolbarAction.ITALIC -> applyContent(
                MarkdownFormatting.toggleItalic(editorValue.text, currentSelection()),
            )

            MarkdownToolbarAction.UNORDERED_LIST -> applyContent(
                MarkdownFormatting.applyUnorderedList(editorValue.text, currentSelection()),
            )

            MarkdownToolbarAction.ORDERED_LIST -> applyContent(
                MarkdownFormatting.applyOrderedList(editorValue.text, currentSelection()),
            )

            MarkdownToolbarAction.CHECKLIST -> applyContent(
                MarkdownFormatting.applyChecklist(editorValue.text, currentSelection()),
            )

            MarkdownToolbarAction.CODE_BLOCK -> applyContent(
                MarkdownFormatting.insertCodeBlock(editorValue.text, currentSelection()),
            )

            MarkdownToolbarAction.LINK -> openLinkDialog()
        }
    }

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
                                text = { Text(stringResource(R.string.note_tags_action)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null)
                                },
                                enabled = state.hasText,
                                onClick = {
                                    showOverflowMenu = false
                                    tagInput = ""
                                    showTagDialog = true
                                },
                            )
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

                if (state.tags.isNotEmpty()) {
                    TagChips(
                        tags = state.tags,
                        onRemove = { tag ->
                            viewModel.setTags(state.tags - tag)
                        },
                    )
                }

                if (!imeVisible) {
                    Spacer(Modifier.height(QuipuSpacing.xs))
                    NoteEditorModeToggle(
                        isPreview = isPreview,
                        onEdit = {
                            if (isPreview) {
                                isPreview = false
                                contentFocusRequester.requestFocus()
                            }
                        },
                        onPreview = {
                            if (!isPreview) {
                                isPreview = true
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        },
                    )
                }

                Spacer(Modifier.height(QuipuSpacing.xs))
                if (isPreview) {
                    NoteMarkdownPreview(
                        markdown = state.content,
                        onToggleChecklist = { lineIndex ->
                            val updated = MarkdownChecklist.toggleItem(state.content, lineIndex)
                            if (updated != state.content) {
                                val caret = editorValue.selection.start.coerceIn(0, updated.length)
                                editorValue = TextFieldValue(
                                    text = updated,
                                    selection = TextRange(caret, caret),
                                )
                                viewModel.onContentChange(updated)
                            }
                        },
                        onOpenLink = { url ->
                            runCatching { uriHandler.openUri(url) }
                                .onFailure {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(messageLinkError)
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                } else {
                    TextField(
                        value = editorValue,
                        onValueChange = { value ->
                            editorValue = value
                            if (value.text != state.content) {
                                viewModel.onContentChange(value.text)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(contentFocusRequester)
                            .onFocusChanged { contentFocused = it.isFocused }
                            .testTag("note_content"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.note_editor_content_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        colors = transparentTextFieldColors(),
                    )
                }

                if (!imeVisible) {
                    NoteSaveStatusFooter(
                        saveStatus = state.saveStatus,
                        createdAt = state.createdAt,
                        updatedAt = state.updatedAt,
                        justCreated = state.justCreated,
                        onRetry = viewModel::retrySave,
                    )
                }

                if (showToolbar) {
                    Spacer(Modifier.height(QuipuSpacing.xs))
                    NoteMarkdownToolbar(
                        onAction = applyToolbarAction,
                        onHeading = { level ->
                            applyContent(
                                MarkdownFormatting.applyHeading(
                                    editorValue.text,
                                    currentSelection(),
                                    level,
                                ),
                            )
                        },
                    )
                }
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

    if (showTagDialog) {
        TagDialog(
            tags = state.tags,
            availableTags = state.availableTags,
            input = tagInput,
            onInputChange = { tagInput = it },
            onAdd = { values ->
                viewModel.setTags(state.tags + values)
                tagInput = ""
            },
            onRemove = { tag -> viewModel.setTags(state.tags - tag) },
            onDismiss = {
                showTagDialog = false
                tagInput = ""
            },
        )
    }

    if (showLinkDialog) {
        val linkIsValid = MarkdownFormatting.isValidHttpUrl(linkUrl)
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(stringResource(R.string.note_markdown_link_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = linkLabel,
                        onValueChange = { linkLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.note_markdown_link_label)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(QuipuSpacing.sm))
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.note_markdown_link_url)) },
                        singleLine = true,
                        isError = linkUrl.isNotBlank() && !linkIsValid,
                        supportingText = if (linkUrl.isNotBlank() && !linkIsValid) {
                            {
                                Text(stringResource(R.string.note_markdown_link_invalid))
                            }
                        } else {
                            null
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = linkIsValid,
                    onClick = {
                        val result = MarkdownFormatting.insertLink(
                            editorValue.text,
                            linkSelection,
                            linkLabel,
                            linkUrl,
                        )
                        showLinkDialog = false
                        applyContent(result)
                    },
                ) {
                    Text(stringResource(R.string.note_markdown_link_insert))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TagChips(
    tags: List<String>,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = QuipuSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = true,
                onClick = { onRemove(tag) },
                label = { Text(tag) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.note_tags_remove, tag),
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun TagDialog(
    tags: List<String>,
    availableTags: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val suggestions = availableTags.filterNot { candidate ->
        tags.any { it.equals(candidate, ignoreCase = true) }
    }
    val submit = {
        val values = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (values.isNotEmpty()) onAdd(values)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_tags_title)) },
        text = {
            Column {
                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.note_tags_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.xs),
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { onRemove(tag) },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(
                                            R.string.note_tags_remove,
                                            tag,
                                        ),
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(QuipuSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.note_tags_add_hint)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.size(QuipuSpacing.sm))
                    TextButton(
                        enabled = input.isNotBlank(),
                        onClick = submit,
                    ) {
                        Text(stringResource(R.string.note_tags_add))
                    }
                }
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(QuipuSpacing.sm))
                    Text(
                        text = stringResource(R.string.note_tags_suggestions),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(QuipuSpacing.xxs))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.xs),
                    ) {
                        suggestions.forEach { tag ->
                            QuipuFilterChip(
                                text = tag,
                                selected = false,
                                onClick = { onAdd(listOf(tag)) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_accept))
            }
        },
    )
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
