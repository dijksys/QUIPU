package io.github.dijksys.quipu.domain.formatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFormattingTest {

    @Test
    fun `bold wraps the selection and keeps it selected`() {
        val result = MarkdownFormatting.toggleBold(
            "hola mundo",
            MarkdownSelection(0, 4),
        )

        assertEquals("**hola** mundo", result.text)
        assertEquals(MarkdownSelection(2, 6), result.selection)
    }

    @Test
    fun `bold without selection inserts delimiters and places the cursor inside`() {
        val result = MarkdownFormatting.toggleBold(
            "hola",
            MarkdownSelection(2, 2),
        )

        assertEquals("ho****la", result.text)
        assertEquals(MarkdownSelection(4, 4), result.selection)
    }

    @Test
    fun `italic wraps the selection and keeps it selected`() {
        val result = MarkdownFormatting.toggleItalic(
            "hola",
            MarkdownSelection(1, 3),
        )

        assertEquals("h*ol*a", result.text)
        assertEquals(MarkdownSelection(2, 4), result.selection)
    }

    @Test
    fun `italic without selection inserts delimiters and places the cursor inside`() {
        val result = MarkdownFormatting.toggleItalic(
            "hola",
            MarkdownSelection(2, 2),
        )

        assertEquals("ho**la", result.text)
        assertEquals(MarkdownSelection(3, 3), result.selection)
    }

    @Test
    fun `heading applies to the current line without duplicating the prefix`() {
        val first = MarkdownFormatting.applyHeading(
            "titulo",
            MarkdownSelection(3, 3),
            2,
        )
        val second = MarkdownFormatting.applyHeading(
            first.text,
            MarkdownSelection(5, 5),
            2,
        )

        assertEquals("## titulo", first.text)
        assertEquals("## titulo", second.text)
    }

    @Test
    fun `unordered list applies to every selected line`() {
        val result = MarkdownFormatting.applyUnorderedList(
            "uno\ndos",
            MarkdownSelection(0, 7),
        )

        assertEquals("- uno\n- dos", result.text)
    }

    @Test
    fun `ordered list numbers every selected line`() {
        val result = MarkdownFormatting.applyOrderedList(
            "uno\ndos",
            MarkdownSelection(0, 7),
        )

        assertEquals("1. uno\n2. dos", result.text)
    }

    @Test
    fun `checklist applies once and does not duplicate the prefix`() {
        val first = MarkdownFormatting.applyChecklist(
            "uno\ndos",
            MarkdownSelection(0, 7),
        )
        val second = MarkdownFormatting.applyChecklist(
            first.text,
            MarkdownSelection(0, first.text.length),
        )

        assertEquals("- [ ] uno\n- [ ] dos", first.text)
        assertEquals(first.text, second.text)
    }

    @Test
    fun `applying the same unordered list repeatedly does not duplicate it`() {
        val result = MarkdownFormatting.applyUnorderedList(
            "- uno",
            MarkdownSelection(0, 5),
        )

        assertEquals("- uno", result.text)
    }

    @Test
    fun `list replaces another list prefix`() {
        val result = MarkdownFormatting.applyOrderedList(
            "- uno",
            MarkdownSelection(0, 5),
        )

        assertEquals("1. uno", result.text)
    }

    @Test
    fun `code block wraps a selection`() {
        val result = MarkdownFormatting.insertCodeBlock(
            "codigo",
            MarkdownSelection(0, 6),
            language = "kotlin",
        )

        assertEquals("```kotlin\ncodigo\n```", result.text)
        assertEquals(MarkdownSelection(10, 16), result.selection)
    }

    @Test
    fun `code block without selection inserts an empty block`() {
        val result = MarkdownFormatting.insertCodeBlock(
            "abc",
            MarkdownSelection(1, 1),
        )

        assertEquals("a```\n\n```bc", result.text)
        assertEquals(MarkdownSelection(5, 5), result.selection)
    }

    @Test
    fun `link replaces the selection with a markdown link`() {
        val result = MarkdownFormatting.insertLink(
            "texto",
            MarkdownSelection(0, 5),
            label = "Repositorio",
            url = "https://github.com/dijksys/QUIPU",
        )

        assertEquals("[Repositorio](https://github.com/dijksys/QUIPU)", result.text)
        assertEquals(MarkdownSelection(1, 12), result.selection)
    }

    @Test
    fun `invalid link returns the original content`() {
        val result = MarkdownFormatting.insertLink(
            "texto",
            MarkdownSelection(1, 3),
            label = "Enlace",
            url = "javascript:alert(1)",
        )

        assertEquals("texto", result.text)
        assertEquals(MarkdownSelection(1, 3), result.selection)
        assertFalse(MarkdownFormatting.isValidHttpUrl("file:///tmp/a"))
        assertFalse(MarkdownFormatting.isValidHttpUrl("intent://x"))
        assertTrue(MarkdownFormatting.isValidHttpUrl("https://ejemplo.com"))
        assertTrue(MarkdownFormatting.isValidHttpUrl("http://ejemplo.com/ruta"))
    }

    @Test
    fun `empty line with list action receives the prefix and cursor`() {
        val result = MarkdownFormatting.applyUnorderedList(
            "",
            MarkdownSelection(0, 0),
        )

        assertEquals("- ", result.text)
        assertEquals(MarkdownSelection(2, 2), result.selection)
    }
}
