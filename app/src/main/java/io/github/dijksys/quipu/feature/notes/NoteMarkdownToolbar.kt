package io.github.dijksys.quipu.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing

enum class MarkdownToolbarAction {
    BOLD,
    ITALIC,
    UNORDERED_LIST,
    ORDERED_LIST,
    CHECKLIST,
    LINK,
    CODE_BLOCK,
}

@Composable
fun NoteMarkdownToolbar(
    onAction: (MarkdownToolbarAction) -> Unit,
    onHeading: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHeadingMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_markdown_toolbar"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = QuipuSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarButton(
                icon = Icons.Rounded.FormatBold,
                contentDescription = stringResource(R.string.note_markdown_bold),
                testTag = "markdown_bold",
                onClick = { onAction(MarkdownToolbarAction.BOLD) },
            )
            ToolbarButton(
                icon = Icons.Rounded.FormatItalic,
                contentDescription = stringResource(R.string.note_markdown_italic),
                testTag = "markdown_italic",
                onClick = { onAction(MarkdownToolbarAction.ITALIC) },
            )
            Box {
                ToolbarButton(
                    icon = Icons.Rounded.Title,
                    contentDescription = stringResource(R.string.note_markdown_heading),
                    testTag = "markdown_heading",
                    onClick = { showHeadingMenu = true },
                )
                DropdownMenu(
                    expanded = showHeadingMenu,
                    onDismissRequest = { showHeadingMenu = false },
                ) {
                    HeadingMenuItem(1, onHeading) { showHeadingMenu = false }
                    HeadingMenuItem(2, onHeading) { showHeadingMenu = false }
                    HeadingMenuItem(3, onHeading) { showHeadingMenu = false }
                }
            }
            ToolbarButton(
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                contentDescription = stringResource(R.string.note_markdown_unordered),
                testTag = "markdown_unordered",
                onClick = { onAction(MarkdownToolbarAction.UNORDERED_LIST) },
            )
            ToolbarButton(
                icon = Icons.Rounded.FormatListNumbered,
                contentDescription = stringResource(R.string.note_markdown_ordered),
                testTag = "markdown_ordered",
                onClick = { onAction(MarkdownToolbarAction.ORDERED_LIST) },
            )
            ToolbarButton(
                icon = Icons.Rounded.Checklist,
                contentDescription = stringResource(R.string.note_markdown_checklist),
                testTag = "markdown_checklist",
                onClick = { onAction(MarkdownToolbarAction.CHECKLIST) },
            )
            ToolbarButton(
                icon = Icons.Rounded.Link,
                contentDescription = stringResource(R.string.note_markdown_link),
                testTag = "markdown_link",
                onClick = { onAction(MarkdownToolbarAction.LINK) },
            )
            ToolbarButton(
                icon = Icons.Rounded.Code,
                contentDescription = stringResource(R.string.note_markdown_code),
                testTag = "markdown_code",
                onClick = { onAction(MarkdownToolbarAction.CODE_BLOCK) },
            )
        }
    }
}

@Composable
private fun HeadingMenuItem(
    level: Int,
    onHeading: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val label = when (level) {
        1 -> stringResource(R.string.note_markdown_h1)
        2 -> stringResource(R.string.note_markdown_h2)
        else -> stringResource(R.string.note_markdown_h3)
    }
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onHeading(level)
            onDismiss()
        },
    )
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
fun NoteEditorModeToggle(
    isPreview: Boolean,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(QuipuSpacing.sm),
    ) {
        ModePill(
            label = stringResource(R.string.note_markdown_edit),
            selected = !isPreview,
            onClick = onEdit,
            modifier = Modifier
                .weight(1f)
                .testTag("note_mode_edit"),
        )
        ModePill(
            label = stringResource(R.string.note_markdown_preview),
            selected = isPreview,
            onClick = onPreview,
            modifier = Modifier
                .weight(1f)
                .testTag("note_mode_preview"),
        )
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = QuipuSpacing.sm, vertical = QuipuSpacing.sm),
            )
        }
    }
}
