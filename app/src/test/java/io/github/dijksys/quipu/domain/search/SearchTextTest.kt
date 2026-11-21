package io.github.dijksys.quipu.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {

    @Test
    fun `normalization removes diacritics and case`() {
        assertEquals("organizacion", SearchText.normalize("Organización"))
        assertEquals("nino", SearchText.normalize("Niño"))
    }

    @Test
    fun `terms are split safely`() {
        assertEquals(listOf("plan", "reuniones"), SearchText.terms("Plan, reuniones!"))
        assertTrue(SearchText.terms("¡¿?!").isEmpty())
    }

    @Test
    fun `match expression uses AND semantics and prefixes`() {
        assertEquals("plan* reunion*", SearchText.matchExpression("Plan reunión"))
        assertNull(SearchText.matchExpression("***"))
    }

    @Test
    fun `operators and quotes never reach the match expression`() {
        val expression = SearchText.matchExpression("\"AND\" OR NOT (record*)")
        assertEquals("and* or* not* record*", expression)
        assertTrue(expression!!.none { it == '"' || it == '(' || it == ')' })
    }

    @Test
    fun `tag normalization collapses spaces`() {
        assertEquals("mi etiqueta", SearchText.normalizeTag("  Mi   ETIQUETA "))
    }
}
