package io.github.dijksys.quipu.domain.formatting

/** Extrae texto legible del Markdown para extractos y listados. */
object MarkdownTextExtractor {

    private val whitespace = Regex("\\s+")

    fun plainText(markdown: String): String =
        normalize(blocksText(MarkdownParser.parse(markdown)))

    fun blocksText(blocks: List<MarkdownBlock>): String =
        blocks.joinToString(" ") { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> inlineText(block.content)
                is MarkdownBlock.Heading -> inlineText(block.content)
                is MarkdownBlock.UnorderedList -> block.items.joinToString(" ") {
                    inlineText(it.content)
                }

                is MarkdownBlock.OrderedList -> block.items.joinToString(" ") {
                    inlineText(it.content)
                }

                is MarkdownBlock.Checklist -> block.items.joinToString(" ") {
                    inlineText(it.content)
                }

                is MarkdownBlock.CodeBlock -> block.code
            }
        }

    fun preview(markdown: String, maxLength: Int = 180): String =
        plainText(markdown).take(maxLength)

    fun inlineText(inlines: List<MarkdownInline>): String =
        inlines.joinToString("") { inline ->
            when (inline) {
                is MarkdownInline.Text -> inline.value
                is MarkdownInline.Bold -> inlineText(inline.content)
                is MarkdownInline.Italic -> inlineText(inline.content)
                is MarkdownInline.Code -> inline.value
                is MarkdownInline.Link -> inline.text
            }
        }

    private fun normalize(value: String): String =
        value.replace(whitespace, " ").trim()
}
