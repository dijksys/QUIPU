package io.github.dijksys.quipu.domain.formatting

/**
 * Plan de presentaci��n del editor: qu�� caracteres se ocultan, qu�� rangos se
 * estilizan y c��mo se relacionan las posiciones originales con las visibles.
 * Nunca modifica el Markdown almacenado.
 */
data class MarkdownVisualPlan(
    val text: String,
    val styles: List<MarkdownVisualRange>,
    val links: List<MarkdownVisualLink>,
    val checklists: List<MarkdownVisualChecklist>,
    val originalToTransformed: IntArray,
    val transformedToOriginal: IntArray,
)

data class MarkdownVisualLink(
    val start: Int,
    val end: Int,
    val url: String,
)

data class MarkdownVisualChecklist(
    val start: Int,
    val end: Int,
    val lineIndex: Int,
    val checked: Boolean,
)

data class MarkdownVisualRange(
    val start: Int,
    val end: Int,
    val style: MarkdownVisualStyle,
)

enum class MarkdownVisualStyle {
    BOLD,
    ITALIC,
    HEADING_1,
    HEADING_2,
    HEADING_3,
    LINK,
    INLINE_CODE,
    CODE_BLOCK,
    LIST_MARKER,
    CHECKLIST_DONE,
    CHECKLIST_PENDING,
}

object MarkdownVisualPlanner {

    private val headingPattern = Regex("""^(#{1,3})\s+(.*)$""")
    private val checklistPattern = Regex("""^(\s*[-*+]\s+)\[([ xX])\]\s+(.*)$""")
    private val unorderedPattern = Regex("""^(\s*[-*+]\s+)(.*)$""")
    private val orderedPattern = Regex("""^(\s*\d+\.\s+)(.*)$""")

    private data class Replacement(
        val range: IntRange,
        val text: String,
        val style: MarkdownVisualStyle?,
    )

    private data class SourceStyle(
        val range: IntRange,
        val style: MarkdownVisualStyle,
    )

    private data class SourceLink(
        val range: IntRange,
        val url: String,
    )

    private data class SourceChecklist(
        val range: IntRange,
        val lineIndex: Int,
        val checked: Boolean,
    )

    fun plan(source: String): MarkdownVisualPlan {
        val hidden = BooleanArray(source.length)
        val replacements = arrayOfNulls<Replacement>(source.length)
        val styles = mutableListOf<SourceStyle>()
        val sourceLinks = mutableListOf<SourceLink>()
        val sourceChecklists = mutableListOf<SourceChecklist>()

        fun hide(range: IntRange) {
            for (index in range) {
                if (index in source.indices) hidden[index] = true
            }
        }

        fun replace(range: IntRange, text: String, style: MarkdownVisualStyle?) {
            replacements[range.first] = Replacement(range, text, style)
            hide(range)
        }

        fun style(range: IntRange, value: MarkdownVisualStyle) {
            if (range.first <= range.last) styles += SourceStyle(range, value)
        }

        fun link(range: IntRange, url: String) {
            if (range.first <= range.last) sourceLinks += SourceLink(range, url)
        }

        var lineStart = 0
        var lineIndex = 0
        while (lineStart <= source.length) {
            val lineBreak = source.indexOf('\n', lineStart)
            val lineEnd = if (lineBreak < 0) source.length else lineBreak
            val line = source.substring(lineStart, lineEnd)

            if (line.trimStart().startsWith("```")) {
                val closing = findClosingFence(source, lineEnd)
                if (closing != null) {
                    style(lineStart until closing, MarkdownVisualStyle.CODE_BLOCK)
                    lineIndex += source.substring(lineStart, closing).count { it == '\n' } + 1
                    lineStart = closing + 1
                    continue
                }
            }

            val heading = headingPattern.matchEntire(line)
            if (heading != null) {
                val contentStart = lineStart + heading.groups[2]!!.range.first
                hide(lineStart until contentStart)
                style(
                    contentStart until lineEnd,
                    when (heading.groupValues[1].length) {
                        1 -> MarkdownVisualStyle.HEADING_1
                        2 -> MarkdownVisualStyle.HEADING_2
                        else -> MarkdownVisualStyle.HEADING_3
                    },
                )
                scanInline(source, contentStart, lineEnd, ::hide, ::style, ::link)
                lineIndex++
                lineStart = lineEnd + 1
                continue
            }

            checklistPattern.matchEntire(line)?.let { match ->
                val marker = lineStart + match.groups[1]!!.range.first +
                    match.groupValues[1].indexOfFirst { it == '-' || it == '*' || it == '+' }
                replace(marker..marker, "•", MarkdownVisualStyle.LIST_MARKER)

                val checkboxStart = lineStart + match.groups[2]!!.range.first - 1
                val checked = match.groupValues[2].equals("x", ignoreCase = true)
                val checkboxRange = checkboxStart until checkboxStart + 3
                replace(
                    checkboxRange,
                    if (checked) "☑" else "☐",
                    if (checked) {
                        MarkdownVisualStyle.CHECKLIST_DONE
                    } else {
                        MarkdownVisualStyle.CHECKLIST_PENDING
                    },
                )
                sourceChecklists += SourceChecklist(
                    range = checkboxRange,
                    lineIndex = lineIndex,
                    checked = checked,
                )

                val contentStart = lineStart + match.groups[3]!!.range.first
                scanInline(source, contentStart, lineEnd, ::hide, ::style, ::link)
                lineIndex++
                lineStart = lineEnd + 1
                continue
            }

            unorderedPattern.matchEntire(line)?.let { match ->
                val marker = lineStart + match.groups[1]!!.range.first +
                    match.groupValues[1].indexOfFirst { it == '-' || it == '*' || it == '+' }
                replace(marker..marker, "•", MarkdownVisualStyle.LIST_MARKER)
                val contentStart = lineStart + match.groups[2]!!.range.first
                scanInline(source, contentStart, lineEnd, ::hide, ::style, ::link)
                lineIndex++
                lineStart = lineEnd + 1
                continue
            }

            orderedPattern.matchEntire(line)?.let { match ->
                val prefix = match.groupValues[1]
                val markerEnd = lineStart + match.groups[1]!!.range.first +
                    prefix.indexOfFirst { it == '.' }
                style(lineStart until markerEnd + 1, MarkdownVisualStyle.LIST_MARKER)
                val contentStart = lineStart + match.groups[2]!!.range.first
                scanInline(source, contentStart, lineEnd, ::hide, ::style, ::link)
                lineIndex++
                lineStart = lineEnd + 1
                continue
            }

            scanInline(source, lineStart, lineEnd, ::hide, ::style, ::link)
            lineIndex++
            lineStart = lineEnd + 1
        }

        val output = StringBuilder(source.length)
        val originalToTransformed = IntArray(source.length + 1)
        val transformedToOriginal = IntArray(source.length + 1)
        val outputStyles = mutableListOf<MarkdownVisualRange>()

        var index = 0
        while (index < source.length) {
            val replacement = replacements[index]
            if (replacement != null) {
                val outStart = output.length
                for (original in replacement.range) originalToTransformed[original] = outStart
                replacement.text.forEach { char ->
                    transformedToOriginal[output.length] = replacement.range.first
                    output.append(char)
                }
                replacement.style?.let { style ->
                    outputStyles += MarkdownVisualRange(
                        start = outStart,
                        end = outStart + replacement.text.length,
                        style = style,
                    )
                }
                index = replacement.range.last + 1
                continue
            }

            if (hidden[index]) {
                originalToTransformed[index] = output.length
                index++
                continue
            }

            originalToTransformed[index] = output.length
            transformedToOriginal[output.length] = index
            output.append(source[index])
            index++
        }
        originalToTransformed[source.length] = output.length
        transformedToOriginal[output.length] = source.length

        styles.forEach { style ->
            val start = originalToTransformed[style.range.first]
            val end = originalToTransformed[style.range.last + 1]
            if (end > start) {
                outputStyles += MarkdownVisualRange(start, end, style.style)
            }
        }

        val outputLinks = sourceLinks.mapNotNull { link ->
            val start = originalToTransformed[link.range.first]
            val end = originalToTransformed[link.range.last + 1]
            if (end > start) MarkdownVisualLink(start, end, link.url) else null
        }.distinctBy { it.start to it.end }.sortedBy { it.start }

        val outputChecklists = sourceChecklists.mapNotNull { checklist ->
            val start = originalToTransformed[checklist.range.first]
            val end = originalToTransformed[checklist.range.last + 1]
            if (end > start) {
                MarkdownVisualChecklist(
                    start = start,
                    end = end,
                    lineIndex = checklist.lineIndex,
                    checked = checklist.checked,
                )
            } else {
                null
            }
        }.distinctBy { it.start to it.end }.sortedBy { it.start }

        return MarkdownVisualPlan(
            text = output.toString(),
            styles = outputStyles.sortedBy { it.start },
            links = outputLinks,
            checklists = outputChecklists,
            originalToTransformed = originalToTransformed,
            transformedToOriginal = transformedToOriginal.copyOf(output.length + 1),
        )
    }

    private fun findClosingFence(source: String, from: Int): Int? {
        var lineStart = if (from < source.length) from + 1 else source.length
        while (lineStart <= source.length) {
            val lineBreak = source.indexOf('\n', lineStart)
            val lineEnd = if (lineBreak < 0) source.length else lineBreak
            if (source.substring(lineStart, lineEnd).trim() == "```") return lineEnd
            if (lineBreak < 0) return null
            lineStart = lineBreak + 1
        }
        return null
    }

    private fun scanInline(
        source: String,
        from: Int,
        to: Int,
        hide: (IntRange) -> Unit,
        style: (IntRange, MarkdownVisualStyle) -> Unit,
        link: (IntRange, String) -> Unit,
    ) {
        var index = from
        while (index < to) {
            when {
                source.startsWith("**", index) -> {
                    val close = source.indexOf("**", index + 2)
                    if (close in (index + 2) until to) {
                        hide(index until index + 2)
                        hide(close until close + 2)
                        style(index + 2 until close, MarkdownVisualStyle.BOLD)
                        scanInline(source, index + 2, close, hide, style, link)
                        index = close + 2
                    } else {
                        index += 2
                    }
                }

                source[index] == '*' -> {
                    val close = findItalicClose(source, index + 1, to)
                    if (close >= 0) {
                        hide(index..index)
                        hide(close..close)
                        style(index + 1 until close, MarkdownVisualStyle.ITALIC)
                        scanInline(source, index + 1, close, hide, style, link)
                        index = close + 1
                    } else {
                        index++
                    }
                }

                source[index] == '`' -> {
                    val close = source.indexOf('`', index + 1)
                    if (close in (index + 1) until to) {
                        hide(index..index)
                        hide(close..close)
                        style(index + 1 until close, MarkdownVisualStyle.INLINE_CODE)
                        index = close + 1
                    } else {
                        index++
                    }
                }

                source[index] == '!' && index + 1 < to && source[index + 1] == '[' -> {
                    val labelEnd = source.indexOf("](", index + 2)
                    val urlEnd = if (labelEnd >= 0) source.indexOf(')', labelEnd + 2) else -1
                    if (labelEnd > index + 2 && urlEnd > labelEnd + 2) {
                        index = urlEnd + 1
                    } else {
                        index++
                    }
                }

                source[index] == '[' && (index == from || source[index - 1] != '!') -> {
                    val markdownLink = AutoLinkDetector.markdownLinkAt(source, index, to)
                    if (markdownLink != null) {
                        hide(index until markdownLink.visibleStart)
                        hide(markdownLink.visibleEnd until markdownLink.end)
                        style(
                            markdownLink.visibleStart until markdownLink.visibleEnd,
                            MarkdownVisualStyle.LINK,
                        )
                        link(
                            markdownLink.visibleStart until markdownLink.visibleEnd,
                            markdownLink.normalizedUrl,
                        )
                        scanInline(
                            source,
                            markdownLink.visibleStart,
                            markdownLink.visibleEnd,
                            hide,
                            style,
                            link,
                        )
                        index = markdownLink.end
                    } else {
                        val labelEnd = source.indexOf("](", index + 1)
                        val urlEnd = if (labelEnd >= 0) source.indexOf(')', labelEnd + 2) else -1
                        index = if (labelEnd > index + 1 && urlEnd > labelEnd + 2) {
                            urlEnd + 1
                        } else {
                            index + 1
                        }
                    }
                }

                AutoLinkDetector.isPotentialStart(source, index) -> {
                    val autoLink = AutoLinkDetector.findAt(source, index, to)
                    if (autoLink != null && autoLink.end <= to) {
                        style(
                            autoLink.visibleStart until autoLink.visibleEnd,
                            MarkdownVisualStyle.LINK,
                        )
                        link(
                            autoLink.visibleStart until autoLink.visibleEnd,
                            autoLink.normalizedUrl,
                        )
                        index = autoLink.end
                    } else {
                        index++
                    }
                }

                else -> index++
            }
        }
    }

    private fun findItalicClose(source: String, start: Int, end: Int): Int {
        var index = start
        while (index < end) {
            if (source[index] == '*') {
                val previousIsStar = index > 0 && source[index - 1] == '*'
                val nextIsStar = index + 1 < source.length && source[index + 1] == '*'
                if (!previousIsStar && !nextIsStar) return index
            }
            index++
        }
        return -1
    }
}
