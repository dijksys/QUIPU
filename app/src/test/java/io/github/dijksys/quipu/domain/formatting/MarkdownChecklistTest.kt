package io.github.dijksys.quipu.domain.formatting

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownChecklistTest {

    @Test
    fun `toggles pending item to checked`() {
        assertEquals(
            "- [x] Comprar pan",
            MarkdownChecklist.toggleItem("- [ ] Comprar pan", 0),
        )
    }

    @Test
    fun `toggles checked item to pending`() {
        assertEquals(
            "- [ ] Comprar pan",
            MarkdownChecklist.toggleItem("- [x] Comprar pan", 0),
        )
    }

    @Test
    fun `preserves other lines and indentation`() {
        val markdown = "# Lista\n\n  - [ ] Uno\n- [x] Dos"
        assertEquals(
            "# Lista\n\n  - [x] Uno\n- [x] Dos",
            MarkdownChecklist.toggleItem(markdown, 2),
        )
    }

    @Test
    fun `does not change a non checklist line`() {
        val markdown = "- Elemento normal"
        assertEquals(markdown, MarkdownChecklist.toggleItem(markdown, 0))
        assertEquals(markdown, MarkdownChecklist.toggleItem(markdown, 20))
    }
}
