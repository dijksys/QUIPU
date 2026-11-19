package com.example.quipu.feature.reminders

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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.quipu.R
import com.example.quipu.core.common.DateTimeUtils
import com.example.quipu.core.designsystem.components.QuipuCard
import com.example.quipu.core.designsystem.components.QuipuInfoPill
import com.example.quipu.core.designsystem.theme.QuipuSpacing
import com.example.quipu.domain.model.Reminder
import com.example.quipu.domain.model.ReminderStatus
import androidx.compose.ui.res.stringResource

@Composable
fun ReminderListItem(
    reminder: Reminder,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onComplete: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val isOverdue = reminder.isOverdue(nowMillis)
    val isDone = reminder.status == ReminderStatus.COMPLETED
    val isCancelled = reminder.status == ReminderStatus.CANCELLED

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
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.width(QuipuSpacing.xs + 2.dp))
                    Text(
                        text = "${DateTimeUtils.formatShortDate(reminder.scheduledAt)} · ${
                            DateTimeUtils.formatTime(reminder.scheduledAt)
                        }",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                Spacer(Modifier.height(QuipuSpacing.xs))
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isDone || isCancelled) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                    color = if (isDone || isCancelled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val description = reminder.description?.trim().orEmpty()
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(QuipuSpacing.xs))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(QuipuSpacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        isOverdue -> QuipuInfoPill(
                            text = stringResource(R.string.reminder_status_overdue),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )

                        isDone -> QuipuInfoPill(
                            text = stringResource(R.string.reminder_status_completed),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        isCancelled -> QuipuInfoPill(
                            text = stringResource(R.string.reminder_status_cancelled),
                        )
                    }
                    if (reminder.noteId != null) {
                        QuipuInfoPill(
                            text = stringResource(R.string.reminder_linked_note),
                            icon = Icons.Rounded.Link,
                        )
                    }
                }
            }
            if (onComplete != null && reminder.status == ReminderStatus.PENDING) {
                IconButton(onClick = onComplete) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.reminder_complete),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (actions != null) {
                actions()
            }
        }
    }
}
