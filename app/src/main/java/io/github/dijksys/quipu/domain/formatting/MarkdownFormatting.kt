package io.github.dijksys.quipu.domain.formatting

data class MarkdownSelection(
    val start: Int,
    val end: Int,
)

data class MarkdownEditResult(
    val text: String,
    val selection: MarkdownSelection,
)

/** Transformaciones puras de selección y cursor para el editor. */
object MarkdownFormatting {

    private val headingPrefix = Regex("""^#{1,3}\s+""")
    private val checklistPrefix = Regex("""^[-*+]\s+\[[ xX]\]\s+""")
    private val unorderedPrefix = Regex("""^[-*+]\s+""")
    private val orderedPrefix = Regex("""^\d+\.\s+""")

    fun toggleBold(text: String, selection: MarkdownSelection): MarkdownEditResult =
        wrapInline(text, selection, "**")

    fun toggleItalic(text: String, selection: MarkdownSelection): MarkdownEditResult =
        wrapInline(text, selection, "*")

    fun applyHeading(
        text: String,
        selection: MarkdownSelection,
        level: Int,
    ): MarkdownEditResult {
        val safeLevel = level.coerceIn(1, 3)
        return transformLines(text, selection) { line, _, _ ->
            if (line.isBlank()) line
            else "#".repeat(safeLevel) + " " + stripBlockPrefixes(line)
        }
    }

    fun applyUnorderedList(
        text: String,
        selection: MarkdownSelection,
    ): MarkdownEditResult = transformLines(text, selection) { line, _, allowBlank ->
        when {
            line.isBlank() && !allowBlank -> line
            else -> "- " + stripBlockPrefixes(line)
        }
    }

    fun applyOrderedList(
        text: String,
        selection: MarkdownSelection,
    ): MarkdownEditResult = transformLines(text, selection) { line, number, allowBlank ->
        when {
            line.isBlank() && !allowBlank -> line
            else -> "$number. " + stripBlockPrefixes(line)
        }
    }

    fun applyChecklist(
        text: String,
        selection: MarkdownSelection,
    ): MarkdownEditResult = transformLines(text, selection) { line, _, allowBlank ->
        when {
            line.isBlank() && !allowBlank -> line
            checklistPrefix.containsMatchIn(line) -> line
            else -> "- [ ] " + stripBlockPrefixes(line)
        }
    }

    fun insertCodeBlock(
        text: String,
        selection: MarkdownSelection,
        language: String? = null,
    ): MarkdownEditResult {
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(start, text.length)
        val fence = "```" + (language?.trim().orEmpty()) 
        if (start == end) {
            val insertion = "$fence\n\n```"
            val result = text.substring(0, start) + insertion + text.substring(start)
            val caret = start + fence.length + 1
            return MarkdownEditResult(result, MarkdownSelection(caret, caret))
        }

        val selected = text.substring(start, end)
        val replacement = "$fence\n$selected\n```"
        val result = text.substring(0, start) + replacement + text.substring(end)
        val contentStart = start + fence.length + 1
        return MarkdownEditResult(
            text = result,
            selection = MarkdownSelection(contentStart, contentStart + selected.length),
        )
    }

    fun insertLink(
        text: String,
        selection: MarkdownSelection,
        label: String,
        url: String,
    ): MarkdownEditResult {
        if (!MarkdownParser.isValidHttpUrl(url)) {
            return MarkdownEditResult(text, selection)
        }
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(start, text.length)
        val visibleText = label.trim().ifBlank { url }
        val replacement = "[$visibleText]($url)"
        val result = text.substring(0, start) + replacement + text.substring(end)
        val labelStart = start + 1
        return if (start == end) {
            MarkdownEditResult(
                text = result,
                selection = MarkdownSelection(
                    labelStart + visibleText.length,
                    labelStart + visibleText.length,
                ),
            )
        } else {
            MarkdownEditResult(
                text = result,
                selection = MarkdownSelection(
                    labelStart,
                    labelStart + visibleText.length,
                ),
            )
        }
    }

    fun isValidHttpUrl(url: String): Boolean = MarkdownParser.isValidHttpUrl(url)

    private fun wrapInline(
        text: String,
        selection: MarkdownSelection,
        delimiter: String,
    ): MarkdownEditResult {
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(start, text.length)

        if (start == end) {
            val insertion = delimiter + delimiter
            val result = text.substring(0, start) + insertion + text.substring(start)
            val caret = start + delimiter.length
            return MarkdownEditResult(result, MarkdownSelection(caret, caret))
        }

        val delimiterLength = delimiter.length
        val alreadyWrapped = start >= delimiterLength &&
            end + delimiterLength <= text.length &&
            text.substring(start - delimiterLength, start) == delimiter &&
            text.substring(end, end + delimiterLength) == delimiter &&
            !isPartOfDoubleAsterisk(text, start, end, delimiterLength)

        if (alreadyWrapped) {
            val result = text.removeRange(end, end + delimiterLength)
                .removeRange(start - delimiterLength, start)
            return MarkdownEditResult(
                text = result,
                selection = MarkdownSelection(
                    start - delimiterLength,
                    end - delimiterLength,
                ),
            )
        }

        val result = text.substring(0, start) +
            delimiter +
            text.substring(start, end) +
            delimiter +
            text.substring(end)
        return MarkdownEditResult(
            text = result,
            selection = MarkdownSelection(
                start + delimiterLength,
                end + delimiterLength,
            ),
        )
    }

    private fun isPartOfDoubleAsterisk(
        text: String,
        start: Int,
        end: Int,
        delimiterLength: Int,
    ): Boolean {
        if (delimiterLength != 1) return false
        val before = if (start - delimiterLength - 1 >= 0) {
            text[start - delimiterLength - 1]
        } else {
            null
        }
        val after = if (end + delimiterLength < text.length) {
            text[end + delimiterLength]
        } else {
            null
        }
        return before == '*' || after == '*'
    }

    /**
     * Aplica una transformación a todas las líneas intersectadas por la
     * selección. Para una selección colapsada que cae en una línea vacía,
     * permite que la herramienta inserte el prefijo correspondiente.
     */
    private fun transformLines(
        text: String,
        selection: MarkdownSelection,
        transform: (line: String, number: Int, allowBlank: Boolean) -> String,
    ): MarkdownEditResult {
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(start, text.length)
        val firstLineStart = lineStart(text, start)
        val lastProbe = if (end > start && end > 0 && text[end - 1] == '\n') {
            end - 1
        } else {
            end
        }
        val lastLineEnd = lineEnd(text, lastProbe.coerceIn(firstLineStart, text.length))
        val region = text.substring(firstLineStart, lastLineEnd)
        val oldLines = region.split('\n')
        val allowBlank = start == end && oldLines.size == 1

        var number = 1
        val newLines = oldLines.map { line ->
            val transformed = transform(line, number, allowBlank)
            if (line.isNotBlank()) number++
            transformed
        }
        val newRegion = newLines.joinToString("\n")
        val result = text.replaceRange(firstLineStart, lastLineEnd, newRegion)

        val oldPrefixLength = prefixLength(oldLines.firstOrNull().orEmpty())
        val newPrefixLength = prefixLength(newLines.firstOrNull().orEmpty())
        val newSelection = if (start == end) {
            val caret = (start + newPrefixLength - oldPrefixLength)
                .coerceIn(firstLineStart, result.length)
            MarkdownSelection(caret, caret)
        } else {
            val oldLineStarts = mutableListOf<Int>()
            var oldCursor = firstLineStart
            oldLines.forEach { line ->
                oldLineStarts += oldCursor
                oldCursor += line.length + 1
            }
            val newLineStarts = mutableListOf<Int>()
            var newCursor = firstLineStart
            newLines.forEach { line ->
                newLineStarts += newCursor
                newCursor += line.length + 1
            }

            fun mapOffset(offset: Int, lineIndex: Int): Int {
                val oldLineStart = oldLineStarts[lineIndex]
                val newLineStart = newLineStarts[lineIndex]
                val oldPrefix = prefixLength(oldLines[lineIndex])
                val newPrefix = prefixLength(newLines[lineIndex])
                val offsetInLine = (offset - oldLineStart).coerceAtLeast(0)
                val contentOffset = (offsetInLine - oldPrefix).coerceAtLeast(0)
                return (newLineStart + newPrefix + contentOffset)
                    .coerceIn(newLineStart, newLineStart + newLines[lineIndex].length)
            }

            val selectionStart = mapOffset(start, 0)
            val selectionEnd = if (end == lastLineEnd) {
                firstLineStart + newRegion.length
            } else {
                mapOffset(end, oldLines.lastIndex)
            }
            MarkdownSelection(
                selectionStart,
                maxOf(selectionStart, selectionEnd),
            )
        }
        return MarkdownEditResult(result, newSelection)
    }

    private fun stripBlockPrefixes(line: String): String {
        var current = line
        while (true) {
            val next = when {
                headingPrefix.containsMatchIn(current) ->
                    headingPrefix.replaceFirst(current, "")

                checklistPrefix.containsMatchIn(current) ->
                    checklistPrefix.replaceFirst(current, "")

                unorderedPrefix.containsMatchIn(current) ->
                    unorderedPrefix.replaceFirst(current, "")

                orderedPrefix.containsMatchIn(current) ->
                    orderedPrefix.replaceFirst(current, "")

                else -> null
            }
            if (next == null || next == current) return current
            current = next
        }
    }

    private fun prefixLength(line: String): Int = line.length - stripBlockPrefixes(line).length

    private fun lineStart(text: String, index: Int): Int {
        if (text.isEmpty()) return 0
        val safeIndex = index.coerceIn(0, text.length)
        if (safeIndex == 0) return 0
        val previousNewline = text.lastIndexOf('\n', safeIndex - 1)
        return if (previousNewline < 0) 0 else previousNewline + 1
    }

    private fun lineEnd(text: String, index: Int): Int {
        val safeIndex = index.coerceIn(0, text.length)
        val newline = text.indexOf('\n', safeIndex)
        return if (newline < 0) text.length else newline
    }
}
