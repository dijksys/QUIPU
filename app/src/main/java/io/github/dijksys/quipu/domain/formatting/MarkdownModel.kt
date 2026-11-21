package io.github.dijksys.quipu.domain.formatting

sealed interface MarkdownBlock {
    data class Paragraph(val content: List<MarkdownInline>) : MarkdownBlock

    data class Heading(
        val level: Int,
        val content: List<MarkdownInline>,
    ) : MarkdownBlock

    data class UnorderedList(val items: List<MarkdownListItem>) : MarkdownBlock

    data class OrderedList(
        val start: Int,
        val items: List<MarkdownListItem>,
    ) : MarkdownBlock

    data class Checklist(val items: List<MarkdownChecklistItem>) : MarkdownBlock

    data class CodeBlock(
        val language: String?,
        val code: String,
    ) : MarkdownBlock
}

data class MarkdownListItem(
    val content: List<MarkdownInline>,
)

data class MarkdownChecklistItem(
    val content: List<MarkdownInline>,
    val checked: Boolean,
    val lineIndex: Int,
)

sealed interface MarkdownInline {
    data class Text(val value: String) : MarkdownInline
    data class Bold(val content: List<MarkdownInline>) : MarkdownInline
    data class Italic(val content: List<MarkdownInline>) : MarkdownInline
    data class Code(val value: String) : MarkdownInline

    data class Link(
        val text: String,
        val url: String,
    ) : MarkdownInline
}
