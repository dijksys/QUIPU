package io.github.dijksys.quipu.feature.notes

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSearchHighlightTest {

    @Test
    fun `highlights accents and prefixes`() {
        val highlighted = highlightMatches(
            text = "Organización semanal",
            terms = listOf("organizacion"),
            background = Color.Yellow,
            foreground = Color.Black,
        )

        assertEquals(1, highlighted.spanStyles.size)
        assertEquals(0, highlighted.spanStyles.first().start)
        assertEquals(12, highlighted.spanStyles.first().end)
    }

    @Test
    fun `prefix matches inside a word are not highlighted`() {
        val highlighted = highlightMatches(
            text = "misrecordatorios",
            terms = listOf("record"),
            background = Color.Yellow,
            foreground = Color.Black,
        )

        assertEquals(0, highlighted.spanStyles.size)
    }

    @Test
    fun `tag matches are highlighted`() {
        val highlighted = highlightMatches(
            text = "#Trabajo #Ideas",
            terms = listOf("trabajo"),
            background = Color.Yellow,
            foreground = Color.Black,
        )

        assertEquals(1, highlighted.spanStyles.size)
        assertEquals(1, highlighted.spanStyles.first().start)
    }

    @Test
    fun `empty terms leave the text untouched`() {
        val highlighted = highlightMatches(
            text = "Sin búsqueda",
            terms = emptyList(),
            background = Color.Yellow,
            foreground = Color.Black,
        )

        assertEquals(0, highlighted.spanStyles.size)
        assertEquals("Sin búsqueda", highlighted.text)
    }
}
