package io.github.dijksys.quipu.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.common.DateTimeUtils
import io.github.dijksys.quipu.core.designsystem.components.QuipuCard
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.domain.model.Note
import io.github.dijksys.quipu.domain.model.NoteFilter

@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    QuipuCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = QuipuSpacing.lg,
            vertical = QuipuSpacing.md,
        ),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (note.isPinned) {
                        Spacer(Modifier.width(QuipuSpacing.xs + 2.dp))
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.note_pinned_label),
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (note.preview.isNotBlank()) {
                    Spacer(Modifier.height(QuipuSpacing.xs))
                    Text(
                        text = note.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(QuipuSpacing.sm))
                Text(
                    text = stringResource(
                        R.string.note_last_modified,
                        DateTimeUtils.formatDateTime(note.updatedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actions != null) {
                Spacer(Modifier.width(QuipuSpacing.xs))
                actions()
            }
        }
    }
}

@Composable
fun NoteActionsMenu(
    note: Note,
    filter: NoteFilter,
    onTogglePin: () -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.action_more_options),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        when (filter) {
            NoteFilter.ACTIVE -> {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (note.isPinned) R.string.note_unpin else R.string.note_pin,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.PushPin, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onTogglePin()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_archive)) },
                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSetArchived(true)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_move_to_trash)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onMoveToTrash()
                    },
                )
            }

            NoteFilter.ARCHIVED -> {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_unarchive)) },
                    leadingIcon = { Icon(Icons.Rounded.Unarchive, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSetArchived(false)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_move_to_trash)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onMoveToTrash()
                    },
                )
            }

            NoteFilter.TRASH -> {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_restore)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onRestore()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_delete_permanently)) },
                    leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onDeletePermanently()
                    },
                )
            }
        }
    }
}
