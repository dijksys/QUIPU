package io.github.dijksys.quipu.domain.formatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `parses headings paragraphs and lists`() {
        val blocks = MarkdownParser.parse(
            """
            # Plan

            Texto con **negrita** y *cursiva*.

            ## Objetivos

            - Uno
            - Dos

            1. Primero
            2. Segundo
            """.trimIndent(),
        )

        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals(1, (blocks[0] as MarkdownBlock.Heading).level)
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)
        assertTrue(blocks[2] is MarkdownBlock.Heading)
        assertEquals(2, (blocks[2] as MarkdownBlock.Heading).level)
        assertTrue(blocks[3] is MarkdownBlock.UnorderedList)
        assertEquals(2, (blocks[3] as MarkdownBlock.UnorderedList).items.size)
        assertTrue(blocks[4] is MarkdownBlock.OrderedList)
        assertEquals(2, (blocks[4] as MarkdownBlock.OrderedList).items.size)
    }

    @Test
    fun `parses inline styles and links`() {
        val inlines = MarkdownParser.parseInline(
            "Hola **mundo** *cursiva* `code` [repo](https://github.com/dijksys/QUIPU)",
        )

        assertTrue(inlines[1] is MarkdownInline.Bold)
        assertTrue(inlines[3] is MarkdownInline.Italic)
        assertTrue(inlines[5] is MarkdownInline.Code)
        assertTrue(inlines[7] is MarkdownInline.Link)
        val link = inlines[7] as MarkdownInline.Link
        assertEquals("repo", link.text)
        assertEquals("https://github.com/dijksys/QUIPU", link.url)
    }

    @Test
    fun `parses checklist state and line index`() {
        val blocks = MarkdownParser.parse(
            """
            Intro

            - [ ] Pendiente
            - [x] Hecho
            """.trimIndent(),
        )

        val checklist = blocks[1] as MarkdownBlock.Checklist
        assertEquals(2, checklist.items.size)
        assertEquals(false, checklist.items[0].checked)
        assertEquals(true, checklist.items[1].checked)
        assertEquals(2, checklist.items[0].lineIndex)
        assertEquals(3, checklist.items[1].lineIndex)
    }

    @Test
    fun `parses fenced code with language`() {
        val block = MarkdownParser.parse(
            """
            ```kotlin
            fun main() {
                println("QUIPU")
            }
            ```
            """.trimIndent(),
        ).single() as MarkdownBlock.CodeBlock

        assertEquals("kotlin", block.language)
        assertEquals("fun main() {\n    println(\"QUIPU\")\n}", block.code)
    }

    @Test
    fun `incomplete syntax is preserved as literal text`() {
        assertEquals(
            listOf(MarkdownInline.Text("**sin cerrar")),
            MarkdownParser.parseInline("**sin cerrar"),
        )
        assertEquals(
            listOf(MarkdownInline.Text("[x](javascript:alert(1))")),
            MarkdownParser.parseInline("[x](javascript:alert(1))"),
        )
        assertEquals(
            listOf(MarkdownInline.Text("![alt](https://x.com/a.png)")),
            MarkdownParser.parseInline("![alt](https://x.com/a.png)"),
        )
        assertEquals(
            listOf(MarkdownInline.Text("<b>html</b>")),
            MarkdownParser.parseInline("<b>html</b>"),
        )
    }

    @Test
    fun `unclosed code fence remains visible as text`() {
        val blocks = MarkdownParser.parse("```kotlin\nfun main()")
        assertEquals(2, blocks.size)
        assertTrue(blocks.none { it is MarkdownBlock.CodeBlock })
    }

    @Test
    fun `empty markdown has no blocks`() {
        assertTrue(MarkdownParser.parse("").isEmpty())
        assertTrue(MarkdownParser.parse("\n\n").isEmpty())
    }

    @Test
    fun `large document parses without losing content`() {
        val markdown = (1..2_000).joinToString("\n\n") { "## Título $it\n\nTexto en español." }
        val blocks = MarkdownParser.parse(markdown)
        assertEquals(4_000, blocks.size)
    }
}
