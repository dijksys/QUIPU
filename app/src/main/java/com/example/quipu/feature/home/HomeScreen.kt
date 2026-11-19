package com.example.quipu.feature.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.quipu.R
import com.example.quipu.core.common.DateTimeUtils
import com.example.quipu.core.designsystem.components.QuipuButton
import com.example.quipu.core.designsystem.components.QuipuCard
import com.example.quipu.core.designsystem.components.QuipuSectionHeader
import com.example.quipu.core.designsystem.components.QuipuTopAppBar
import com.example.quipu.core.designsystem.theme.QuipuSpacing
import com.example.quipu.domain.model.Note
import com.example.quipu.domain.model.Reminder
import com.example.quipu.feature.notes.NoteListItem
import com.example.quipu.feature.reminders.ReminderListItem

@Composable
fun HomeScreen(
    onOpenNotes: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenReminder: (String) -> Unit,
    onCreateNote: () -> Unit,
    onCreateReminder: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { QuipuTopAppBar(title = stringResource(R.string.home_title)) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = QuipuSpacing.screenHorizontal,
                end = QuipuSpacing.screenHorizontal,
                top = QuipuSpacing.xs,
                bottom = QuipuSpacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(QuipuSpacing.md),
        ) {
            item {
                Text(
                    text = DateTimeUtils.formatLongDate(state.nowMillis)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(QuipuSpacing.xs))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                SummaryRow(
                    notesCount = state.notesCount,
                    pendingReminders = state.pendingRemindersCount,
                    onOpenNotes = onOpenNotes,
                    onOpenReminders = onOpenReminders,
                )
            }

            if (state.isCompletelyEmpty) {
                item {
                    QuipuCard {
                        Text(
                            text = stringResource(R.string.home_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(QuipuSpacing.xs))
                        Text(
                            text = stringResource(R.string.home_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(QuipuSpacing.lg))
                        Column(verticalArrangement = Arrangement.spacedBy(QuipuSpacing.sm)) {
                            QuipuButton(
                                text = stringResource(R.string.home_new_note),
                                onClick = onCreateNote,
                                icon = Icons.Rounded.Add,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_new_note"),
                            )
                            com.example.quipu.core.designsystem.components.QuipuOutlinedButton(
                                text = stringResource(R.string.home_new_reminder),
                                onClick = onCreateReminder,
                                icon = Icons.Rounded.NotificationsActive,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_new_reminder"),
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.md)) {
                        QuickActionCard(
                            title = stringResource(R.string.home_new_note),
                            icon = Icons.Rounded.Add,
                            onClick = onCreateNote,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_new_note"),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        QuickActionCard(
                            title = stringResource(R.string.home_new_reminder),
                            icon = Icons.Rounded.NotificationsActive,
                            onClick = onCreateReminder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_new_reminder"),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            if (state.pinnedNotes.isNotEmpty()) {
                item {
                    QuipuSectionHeader(
                        title = stringResource(R.string.home_pinned_title),
                        actionLabel = stringResource(R.string.action_see_all),
                        onAction = onOpenNotes,
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.md),
                        contentPadding = PaddingValues(vertical = QuipuSpacing.xxs),
                    ) {
                        items(state.pinnedNotes, key = { it.id }) { note ->
                            PinnedNoteCard(
                                note = note,
                                onClick = { onOpenNote(note.id) },
                                modifier = Modifier.width(252.dp),
                            )
                        }
                    }
                }
            }

            if (state.recentNotes.isNotEmpty()) {
                item {
                    QuipuSectionHeader(
                        title = stringResource(R.string.home_recent_title),
                        actionLabel = stringResource(R.string.action_see_all),
                        onAction = onOpenNotes,
                    )
                }
                items(state.recentNotes, key = { it.id }) { note ->
                    NoteListItem(note = note, onClick = { onOpenNote(note.id) })
                }
            }

            if (state.upcomingReminders.isNotEmpty()) {
                item {
                    QuipuSectionHeader(
                        title = stringResource(R.string.home_upcoming_title),
                        actionLabel = stringResource(R.string.action_see_all),
                        onAction = onOpenReminders,
                    )
                }
                items(state.upcomingReminders, key = { it.id }) { reminder ->
                    ReminderListItem(
                        reminder = reminder,
                        nowMillis = state.nowMillis,
                        onClick = { onOpenReminder(reminder.id) },
                    )
                }
            }

            if (!state.isCompletelyEmpty && state.upcomingReminders.isEmpty()) {
                item {
                    QuipuSectionHeader(title = stringResource(R.string.home_upcoming_title))
                    QuipuCard {
                        Text(
                            text = stringResource(R.string.home_no_upcoming_reminders),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    notesCount: Int,
    pendingReminders: Int,
    onOpenNotes: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.md)) {
        SummaryCard(
            count = notesCount,
            label = stringResource(R.string.home_notes_count_label),
            icon = Icons.Rounded.Description,
            onClick = onOpenNotes,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            count = pendingReminders,
            label = stringResource(R.string.home_pending_count_label),
            icon = Icons.Rounded.NotificationsNone,
            onClick = onOpenReminders,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuipuCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(QuipuSpacing.md))
            Column {
                Text(text = count.toString(), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QuipuSpacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.sm),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PinnedNoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuipuCard(onClick = onClick, modifier = modifier) {
        Text(
            text = note.displayTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (note.preview.isNotBlank()) {
            Spacer(Modifier.height(QuipuSpacing.xs))
            Text(
                text = note.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(QuipuSpacing.sm))
        Text(
            text = DateTimeUtils.formatDateTime(note.updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
