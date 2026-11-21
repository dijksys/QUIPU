package io.github.dijksys.quipu.domain.formatting

/**
 * Parser Markdown deliberadamente pequeño para el subconjunto de QUIPU.
 * No interpreta HTML, imágenes ni extensiones fuera de las documentadas.
 */
object MarkdownParser {

    private val headingPattern = Regex("""^(#{1,3})\s+(.+)$""")
    private val checklistPattern = Regex("""^\s*[-*+]\s+\[([ xX])\]\s+(.*)$""")
    private val unorderedPattern = Regex("""^\s*[-*+]\s+(.+)$""")
    private val orderedPattern = Regex("""^\s*(\d+)\.\s+(.+)$""")

    fun parse(markdown: String): List<MarkdownBlock> {
        if (markdown.isEmpty()) return emptyList()

        val lines = markdown.split('\n')
        val blocks = mutableListOf<MarkdownBlock>()
        var index = 0

        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) {
                index++
                continue
            }

            if (line.trimStart().startsWith("```")) {
                val closing = findClosingFence(lines, index + 1)
                if (closing != null) {
                    val language = line.trim().removePrefix("```").trim().ifBlank { null }
                    val code = lines.subList(index + 1, closing).joinToString("\n")
                    blocks += MarkdownBlock.CodeBlock(language = language, code = code)
                    index = closing + 1
                    continue
                }
                // Delimitador incompleto: se conserva como texto literal.
                blocks += MarkdownBlock.Paragraph(parseInline(line))
                index++
                continue
            }

            headingPattern.matchEntire(line)?.let { match ->
                blocks += MarkdownBlock.Heading(
                    level = match.groupValues[1].length,
                    content = parseInline(match.groupValues[2]),
                )
                index++
                continue
            }

            if (checklistPattern.containsMatchIn(line)) {
                val items = mutableListOf<MarkdownChecklistItem>()
                while (index < lines.size) {
                    val match = checklistPattern.matchEntire(lines[index]) ?: break
                    items += MarkdownChecklistItem(
                        content = parseInline(match.groupValues[2]),
                        checked = match.groupValues[1].equals("x", ignoreCase = true),
                        lineIndex = index,
                    )
                    index++
                }
                blocks += MarkdownBlock.Checklist(items)
                continue
            }

            if (unorderedPattern.matches(line)) {
                val items = mutableListOf<MarkdownListItem>()
                while (index < lines.size) {
                    val match = unorderedPattern.matchEntire(lines[index]) ?: break
                    items += MarkdownListItem(parseInline(match.groupValues[1]))
                    index++
                }
                blocks += MarkdownBlock.UnorderedList(items)
                continue
            }

            orderedPattern.matchEntire(line)?.let { first ->
                val items = mutableListOf<MarkdownListItem>()
                while (index < lines.size) {
                    val match = orderedPattern.matchEntire(lines[index]) ?: break
                    items += MarkdownListItem(parseInline(match.groupValues[2]))
                    index++
                }
                blocks += MarkdownBlock.OrderedList(
                    start = first.groupValues[1].toIntOrNull() ?: 1,
                    items = items,
                )
                continue
            }

            val paragraph = mutableListOf<String>()
            while (index < lines.size) {
                val current = lines[index]
                if (current.isBlank() || isBlockStart(current)) break
                paragraph += current
                index++
            }
            if (paragraph.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(parseInline(paragraph.joinToString("\n")))
            } else {
                // Salvaguarda para no perder una línea desconocida.
                blocks += MarkdownBlock.Paragraph(parseInline(lines[index]))
                index++
            }
        }

        return blocks
    }

    fun parseInline(text: String): List<MarkdownInline> = parseInlineRange(text)

    internal fun isValidHttpUrl(url: String): Boolean = AutoLinkDetector.isValidHttpUrl(url)

    fun isPotentialAutoLinkStart(text: String, index: Int): Boolean =
        AutoLinkDetector.isPotentialStart(text, index)

    private fun findClosingFence(lines: List<String>, start: Int): Int? {
        for (index in start until lines.size) {
            if (lines[index].trim() == "```") return index
        }
        return null
    }

    private fun isBlockStart(line: String): Boolean =
        line.trimStart().startsWith("```") ||
            headingPattern.containsMatchIn(line) ||
            checklistPattern.containsMatchIn(line) ||
            unorderedPattern.matches(line) ||
            orderedPattern.containsMatchIn(line)

    private fun parseInlineRange(text: String): List<MarkdownInline> {
        val result = mutableListOf<MarkdownInline>()
        val plain = StringBuilder()
        var index = 0

        fun flushText() {
            if (plain.isNotEmpty()) {
                result += MarkdownInline.Text(plain.toString())
                plain.clear()
            }
        }

        while (index < text.length) {
            when {
                text.startsWith("**", index) -> {
                    val close = text.indexOf("**", index + 2)
                    if (close >= 0) {
                        flushText()
                        result += MarkdownInline.Bold(
                            parseInlineRange(text.substring(index + 2, close)),
                        )
                        index = close + 2
                    } else {
                        plain.append("**")
                        index += 2
                    }
                }

                text[index] == '*' -> {
                    val close = findItalicClose(text, index + 1)
                    if (close >= 0) {
                        flushText()
                        result += MarkdownInline.Italic(
                            parseInlineRange(text.substring(index + 1, close)),
                        )
                        index = close + 1
                    } else {
                        plain.append('*')
                        index++
                    }
                }

                text[index] == '`' -> {
                    val close = text.indexOf('`', index + 1)
                    if (close >= 0) {
                        flushText()
                        result += MarkdownInline.Code(text.substring(index + 1, close))
                        index = close + 1
                    } else {
                        plain.append('`')
                        index++
                    }
                }

                text[index] == '!' && index + 1 < text.length && text[index + 1] == '[' -> {
                    val labelEnd = text.indexOf("](", index + 2)
                    val urlEnd = if (labelEnd >= 0) text.indexOf(')', labelEnd + 2) else -1
                    if (labelEnd > index + 2 && urlEnd > labelEnd + 2) {
                        plain.append(text.substring(index, urlEnd + 1))
                        index = urlEnd + 1
                    } else {
                        plain.append('!')
                        index++
                    }
                }

                text[index] == '[' && (index == 0 || text[index - 1] != '!') -> {
                    val link = AutoLinkDetector.markdownLinkAt(text, index)
                    if (link != null) {
                        flushText()
                        result += MarkdownInline.Link(
                            text = link.visibleText,
                            url = link.normalizedUrl,
                        )
                        index = link.end
                    } else {
                        val labelEnd = text.indexOf("](", index + 1)
                        val urlEnd = if (labelEnd >= 0) text.indexOf(')', labelEnd + 2) else -1
                        if (labelEnd > index + 1 && urlEnd > labelEnd + 2) {
                            plain.append(text.substring(index, urlEnd + 1))
                            index = urlEnd + 1
                        } else {
                            plain.append('[')
                            index++
                        }
                    }
                }

                AutoLinkDetector.isPotentialStart(text, index) -> {
                    val link = AutoLinkDetector.findAt(text, index)
                    if (link != null) {
                        flushText()
                        result += MarkdownInline.Link(
                            text = link.visibleText,
                            url = link.normalizedUrl,
                        )
                        index = link.end
                    } else {
                        plain.append(text[index])
                        index++
                    }
                }

                else -> {
                    plain.append(text[index])
                    index++
                }
            }
        }
        flushText()
        return result
    }

    private fun findItalicClose(text: String, start: Int): Int {
        var index = start
        while (index < text.length) {
            if (text[index] == '*') {
                val previousIsStar = index > 0 && text[index - 1] == '*'
                val nextIsStar = index + 1 < text.length && text[index + 1] == '*'
                if (!previousIsStar && !nextIsStar) return index
            }
            index++
        }
        return -1
    }
}
