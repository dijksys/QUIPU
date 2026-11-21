package io.github.dijksys.quipu.domain.formatting

/** Cambia únicamente el estado de una casilla Markdown en la línea indicada. */
object MarkdownChecklist {

    private val checklistLine = Regex("""^(\s*[-*+]\s+\[)([ xX])(\].*)$""")

    fun toggleItem(markdown: String, lineIndex: Int): String {
        val lines = markdown.split('\n').toMutableList()
        if (lineIndex !in lines.indices) return markdown

        val match = checklistLine.matchEntire(lines[lineIndex]) ?: return markdown
        val checked = match.groupValues[2].equals("x", ignoreCase = true)
        val markerRange = match.groups[2]?.range ?: return markdown
        lines[lineIndex] = lines[lineIndex].replaceRange(
            markerRange,
            if (checked) " " else "x",
        )
        return lines.joinToString("\n")
    }
}
