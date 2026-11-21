package io.github.dijksys.quipu.domain.formatting

import io.github.dijksys.quipu.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTextExtractorTest {

    @Test
    fun `removes inline syntax and keeps visible text`() {
        assertEquals(
            "Importante: revisar documentación",
            MarkdownTextExtractor.plainText("**Importante:** revisar *documentación*"),
        )
    }

    @Test
    fun `keeps link text and removes the destination`() {
        assertEquals(
            "Repositorio QUIPU",
            MarkdownTextExtractor.plainText(
                "[Repositorio QUIPU](https://github.com/dijksys/QUIPU)",
            ),
        )
    }

    @Test
    fun `removes list and checkbox prefixes`() {
        assertEquals(
            "Comprar componentes",
            MarkdownTextExtractor.plainText("- [ ] Comprar componentes"),
        )
        assertEquals(
            "Uno Dos",
            MarkdownTextExtractor.plainText("- Uno\n- Dos"),
        )
        assertEquals(
            "Uno Dos",
            MarkdownTextExtractor.plainText("1. Uno\n2. Dos"),
        )
    }

    @Test
    fun `headings and paragraphs are joined as readable text`() {
        assertEquals(
            "Objetivos Implementar. Probar.",
            MarkdownTextExtractor.plainText(
                """
                ## Objetivos

                - Implementar.
                - Probar.
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `keeps code block content`() {
        assertEquals(
            "fun main() {}",
            MarkdownTextExtractor.plainText("```kotlin\nfun main() {}\n```"),
        )
    }

    @Test
    fun `collapses whitespace and limits preview`() {
        assertEquals(
            "Texto con espacios",
            MarkdownTextExtractor.plainText("  Texto   con\n\nespacios  "),
        )
        assertEquals(
            "12345",
            MarkdownTextExtractor.preview("1234567890", maxLength = 5),
        )
    }

    @Test
    fun `note preview uses markdown plain text`() {
        val note = Note(
            id = "n1",
            title = "Nota",
            content = "# Importante\n\n**Revisar** la [documentación](https://ejemplo.com).",
            createdAt = 1,
            updatedAt = 1,
        )

        assertEquals("Importante Revisar la documentación.", note.preview)
    }
}
