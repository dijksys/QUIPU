package io.github.dijksys.quipu.feature.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.common.DateTimeUtils
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing

/**
 * Footer discreto del editor: comunica el estado de persistencia y los
 * metadatos confirmados por el repositorio.
 */
@Composable
fun NoteSaveStatusFooter(
    saveStatus: NoteSaveStatus,
    createdAt: Long?,
    updatedAt: Long?,
    justCreated: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (saveStatus) {
        NoteSaveStatus.EMPTY -> stringResource(R.string.note_save_empty)
        NoteSaveStatus.UNSAVED -> stringResource(R.string.note_save_unsaved)
        NoteSaveStatus.SAVING -> stringResource(R.string.note_save_saving)
        NoteSaveStatus.SAVED -> stringResource(
            if (justCreated) R.string.note_save_created else R.string.note_save_saved,
        )

        NoteSaveStatus.ERROR -> stringResource(R.string.note_save_error)
    }
    val iconTint = when (saveStatus) {
        NoteSaveStatus.SAVED, NoteSaveStatus.SAVING -> MaterialTheme.colorScheme.primary
        NoteSaveStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val messageColor = when (saveStatus) {
        NoteSaveStatus.EMPTY, NoteSaveStatus.UNSAVED ->
            MaterialTheme.colorScheme.onSurfaceVariant

        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = QuipuSpacing.sm, bottom = QuipuSpacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (saveStatus == NoteSaveStatus.SAVING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 1.5.dp,
                )
            } else {
                Icon(
                    imageVector = saveStatus.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconTint,
                )
            }
            Spacer(Modifier.width(QuipuSpacing.sm))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = messageColor,
            )
            if (saveStatus == NoteSaveStatus.ERROR) {
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = QuipuSpacing.sm, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.note_save_retry),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (updatedAt != null) {
            Spacer(Modifier.height(QuipuSpacing.xxs))
            Text(
                text = stringResource(
                    R.string.note_editor_updated,
                    DateTimeUtils.formatMetadataDateTime(updatedAt),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (createdAt != null) {
            Text(
                text = stringResource(
                    R.string.note_editor_created,
                    DateTimeUtils.formatMetadataDate(createdAt),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private fun NoteSaveStatus.icon(): ImageVector = when (this) {
    NoteSaveStatus.EMPTY -> Icons.Rounded.Edit
    NoteSaveStatus.UNSAVED -> Icons.Rounded.Edit
    NoteSaveStatus.SAVING -> Icons.Rounded.Edit
    NoteSaveStatus.SAVED -> Icons.Rounded.CloudDone
    NoteSaveStatus.ERROR -> Icons.Rounded.CloudOff
}
