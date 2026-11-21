package io.github.dijksys.quipu.feature.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.theme.QuipuSpacing
import io.github.dijksys.quipu.domain.formatting.MarkdownBlock
import io.github.dijksys.quipu.domain.formatting.MarkdownChecklistItem
import io.github.dijksys.quipu.domain.formatting.MarkdownInline
import io.github.dijksys.quipu.domain.formatting.MarkdownParser

@Composable
fun NoteMarkdownPreview(
    markdown: String,
    onToggleChecklist: (Int) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (markdown.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("note_markdown_preview"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.note_markdown_preview_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = QuipuSpacing.sm)
            .testTag("note_markdown_preview"),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> InlineMarkdown(
                    inlines = block.content,
                    style = MaterialTheme.typography.bodyLarge,
                    onOpenLink = onOpenLink,
                )

                is MarkdownBlock.Heading -> InlineMarkdown(
                    inlines = block.content,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    onOpenLink = onOpenLink,
                )

                is MarkdownBlock.UnorderedList -> Column {
                    block.items.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "•",
                                modifier = Modifier.width(20.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            InlineMarkdown(
                                inlines = item.content,
                                style = MaterialTheme.typography.bodyLarge,
                                onOpenLink = onOpenLink,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                is MarkdownBlock.OrderedList -> Column {
                    block.items.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "${block.start + index}.",
                                modifier = Modifier.width(28.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            InlineMarkdown(
                                inlines = item.content,
                                style = MaterialTheme.typography.bodyLarge,
                                onOpenLink = onOpenLink,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                is MarkdownBlock.Checklist -> Column {
                    block.items.forEach { item ->
                        ChecklistRow(
                            item = item,
                            onToggle = { onToggleChecklist(item.lineIndex) },
                            onOpenLink = onOpenLink,
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = block.code,
                        modifier = Modifier.padding(QuipuSpacing.md),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(QuipuSpacing.sm))
        }
    }
}

@Composable
private fun ChecklistRow(
    item: MarkdownChecklistItem,
    onToggle: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val checkboxDescription = stringResource(R.string.note_markdown_checkbox)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier
                .testTag("markdown_checkbox_${item.lineIndex}")
                .semantics { contentDescription = checkboxDescription },
        )
        Spacer(Modifier.width(QuipuSpacing.xs))
        InlineMarkdown(
            inlines = item.content,
            style = MaterialTheme.typography.bodyLarge,
            onOpenLink = onOpenLink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InlineMarkdown(
    inlines: List<MarkdownInline>,
    style: TextStyle,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val presentation = remember(inlines, linkColor, codeBackground) {
        buildInlinePresentation(inlines, linkColor, codeBackground)
    }
    ClickableText(
        text = presentation.text,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            presentation.links.firstOrNull { offset in it.start until it.end }
                ?.let { onOpenLink(it.url) }
        },
    )
}

private data class LinkSpan(
    val start: Int,
    val end: Int,
    val url: String,
)

private data class InlinePresentation(
    val text: AnnotatedString,
    val links: List<LinkSpan>,
)

private fun buildInlinePresentation(
    inlines: List<MarkdownInline>,
    linkColor: Color,
    codeBackground: Color,
): InlinePresentation {
    val links = mutableListOf<LinkSpan>()
    val text = buildAnnotatedString {
        appendInlines(inlines, SpanStyle(), links, linkColor, codeBackground)
    }
    return InlinePresentation(text, links)
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<MarkdownInline>,
    baseStyle: SpanStyle,
    links: MutableList<LinkSpan>,
    linkColor: Color,
    codeBackground: Color,
) {
    inlines.forEach { inline ->
        when (inline) {
            is MarkdownInline.Text -> withStyle(baseStyle) { append(inline.value) }
            is MarkdownInline.Bold -> appendInlines(
                inline.content,
                baseStyle.merge(SpanStyle(fontWeight = FontWeight.Bold)),
                links,
                linkColor,
                codeBackground,
            )

            is MarkdownInline.Italic -> appendInlines(
                inline.content,
                baseStyle.merge(SpanStyle(fontStyle = FontStyle.Italic)),
                links,
                linkColor,
                codeBackground,
            )

            is MarkdownInline.Code -> withStyle(
                baseStyle.merge(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                    ),
                ),
            ) {
                append(inline.value)
            }

            is MarkdownInline.Link -> {
                val start = length
                withStyle(
                    baseStyle.merge(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                ) {
                    append(inline.text)
                }
                links += LinkSpan(start, length, inline.url)
            }
        }
    }
}
