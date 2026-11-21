package io.github.dijksys.quipu.domain.formatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownVisualPlannerTest {

    @Test
    fun `bold delimiters are hidden and content is styled`() {
        val plan = MarkdownVisualPlanner.plan("Hola **mundo**")

        assertEquals("Hola mundo", plan.text)
        assertTrue(
            plan.styles.any {
                it.style == MarkdownVisualStyle.BOLD && it.start == 5 && it.end == 10
            },
        )
        assertEquals(5, plan.originalToTransformed[5])
        assertEquals(10, plan.originalToTransformed[14])
        assertEquals(7, plan.transformedToOriginal[5])
    }

    @Test
    fun `incomplete syntax stays literal`() {
        val plan = MarkdownVisualPlanner.plan("Hola **mundo y *otro")

        assertEquals("Hola **mundo y *otro", plan.text)
        assertTrue(plan.styles.isEmpty())
    }

    @Test
    fun `headings hide the marker and keep hierarchy`() {
        val plan = MarkdownVisualPlanner.plan("## Título")

        assertEquals("Título", plan.text)
        assertTrue(plan.styles.any { it.style == MarkdownVisualStyle.HEADING_2 })
    }

    @Test
    fun `lists use a visible marker`() {
        val unordered = MarkdownVisualPlanner.plan("- Uno\n- Dos")
        assertEquals("• Uno\n• Dos", unordered.text)
        assertEquals(
            2,
            unordered.styles.count { it.style == MarkdownVisualStyle.LIST_MARKER },
        )

        val ordered = MarkdownVisualPlanner.plan("1. Uno")
        assertEquals("1. Uno", ordered.text)
    }

    @Test
    fun `checklists replace brackets with checkbox glyphs`() {
        val pending = MarkdownVisualPlanner.plan("- [ ] Comprar")
        assertEquals("• ☐ Comprar", pending.text)
        assertTrue(pending.styles.any { it.style == MarkdownVisualStyle.CHECKLIST_PENDING })

        val done = MarkdownVisualPlanner.plan("- [x] Comprar")
        assertEquals("• ☑ Comprar", done.text)
        assertTrue(done.styles.any { it.style == MarkdownVisualStyle.CHECKLIST_DONE })
    }

    @Test
    fun `links hide the destination and keep the label`() {
        val plan = MarkdownVisualPlanner.plan("[QUIPU](https://github.com/dijksys/QUIPU)")

        assertEquals("QUIPU", plan.text)
        assertTrue(
            plan.styles.any {
                it.style == MarkdownVisualStyle.LINK && it.start == 0 && it.end == 5
            },
        )
    }

    @Test
    fun `code blocks and inline code keep their content`() {
        val block = MarkdownVisualPlanner.plan("```kotlin\nfun main() {}\n```")
        assertTrue(block.text.contains("fun main() {}"))
        assertTrue(block.styles.any { it.style == MarkdownVisualStyle.CODE_BLOCK })

        val inline = MarkdownVisualPlanner.plan("Usa `flow` aquí")
        assertEquals("Usa flow aquí", inline.text)
        assertTrue(inline.styles.any { it.style == MarkdownVisualStyle.INLINE_CODE })
    }

    @Test
    fun `autolinks are styled and exposed with punctuation excluded`() {
        val source = "Consulta platform.deepseek.com."
        val plan = MarkdownVisualPlanner.plan(source)
        val link = plan.links.single()

        assertEquals(source, plan.text)
        assertEquals("https://platform.deepseek.com", link.url)
        assertEquals(source.indexOf("platform"), link.start)
        assertEquals(source.length - 1, link.end)
        assertTrue(
            plan.styles.any {
                it.style == MarkdownVisualStyle.LINK &&
                    it.start == link.start &&
                    it.end == link.end
            },
        )
    }

    @Test
    fun `explicit urls and multiple links are detected without changing text`() {
        val source = "Uno https://one.com y dos example.org"
        val plan = MarkdownVisualPlanner.plan(source)

        assertEquals(source, plan.text)
        assertEquals(2, plan.links.size)
        assertEquals("https://one.com", plan.links[0].url)
        assertEquals("https://example.org", plan.links[1].url)
    }

    @Test
    fun `markdown links with url labels keep one visual link`() {
        val source = "[https://platform.deepseek.com/usage](https://platform.deepseek.com/usage)"
        val plan = MarkdownVisualPlanner.plan(source)

        assertEquals("https://platform.deepseek.com/usage", plan.text)
        val link = plan.links.single()
        assertEquals(0, link.start)
        assertEquals(plan.text.length, link.end)
        assertEquals("https://platform.deepseek.com/usage", link.url)
    }

    @Test
    fun `urls inside code are not autolinks`() {
        val inline = MarkdownVisualPlanner.plan("Usa `https://example.com` aquí")
        assertTrue(inline.links.isEmpty())
        assertTrue(inline.styles.none { it.style == MarkdownVisualStyle.LINK })

        val block = MarkdownVisualPlanner.plan(
            "```\nhttps://example.com\nexample.org\n```",
        )
        assertTrue(block.links.isEmpty())
        assertTrue(block.text.contains("https://example.com"))
    }

    @Test
    fun `text with dots and emails is not linked`() {
        assertTrue(MarkdownVisualPlanner.plan("palabra...otra").links.isEmpty())
        assertTrue(MarkdownVisualPlanner.plan("usuario@example.com").links.isEmpty())
        assertTrue(MarkdownVisualPlanner.plan("version.1.2.3").links.isEmpty())
    }

    @Test
    fun `offset mapping is always inside bounds and monotonic`() {
        val plan = MarkdownVisualPlanner.plan(
            "# Título\n\n**Negrita** y [enlace](https://example.com)\n- [x] Hecho",
        )

        assertTrue(plan.originalToTransformed.size >= plan.text.length + 1)
        for (offset in 0 until plan.originalToTransformed.size - 1) {
            assertTrue(plan.originalToTransformed[offset] <= plan.originalToTransformed[offset + 1])
        }
        for (offset in 0..plan.text.length) {
            val original = plan.transformedToOriginal[offset]
            assertTrue(original in 0..plan.originalToTransformed.lastIndex)
        }
        plan.links.forEach { link ->
            assertTrue(link.start in 0 until plan.text.length)
            assertTrue(link.end in (link.start + 1)..plan.text.length)
        }
        assertFalse(plan.text.contains("https://example.com"))
    }
}
