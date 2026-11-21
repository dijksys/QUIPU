package io.github.dijksys.quipu.domain.formatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLinkDetectorTest {

    @Test
    fun `detects explicit http and https urls`() {
        val https = detectOne("https://platform.deepseek.com/usage")!!
        assertEquals(AutoLinkType.EXPLICIT_URL, https.type)
        assertEquals("https://platform.deepseek.com/usage", https.normalizedUrl)

        val http = detectOne("http://example.com")!!
        assertEquals(AutoLinkType.EXPLICIT_URL, http.type)
        assertEquals("http://example.com", http.normalizedUrl)
    }

    @Test
    fun `detects bare domains and normalizes to https`() {
        assertEquals(
            "https://platform.deepseek.com",
            detectOne("platform.deepseek.com")!!.normalizedUrl,
        )
        assertEquals("https://www.example.com", detectOne("www.example.com")!!.normalizedUrl)
        assertEquals("https://docs.example.com", detectOne("docs.example.com")!!.normalizedUrl)
    }

    @Test
    fun `detects bare domains with path query fragment and port`() {
        assertEquals(
            "https://example.com/path",
            detectOne("example.com/path")!!.normalizedUrl,
        )
        assertEquals(
            "https://example.com/search?q=quipu#results",
            detectOne("example.com/search?q=quipu#results")!!.normalizedUrl,
        )
        assertEquals(
            "https://example.com:8080/path",
            detectOne("example.com:8080/path")!!.normalizedUrl,
        )
    }

    @Test
    fun `detects markdown links and their visible label`() {
        val text = "[QUIPU](https://github.com/dijksys/QUIPU)"
        val link = detectOne(text)!!
        assertEquals(AutoLinkType.MARKDOWN_LINK, link.type)
        assertEquals("QUIPU", link.visibleText)
        assertEquals("https://github.com/dijksys/QUIPU", link.normalizedUrl)
        assertEquals(1, link.visibleStart)
        assertEquals(6, link.visibleEnd)
    }

    @Test
    fun `detects markdown links whose label is the url`() {
        val text = "[https://platform.deepseek.com/usage](https://platform.deepseek.com/usage)"
        val link = detectOne(text)!!
        assertEquals(AutoLinkType.MARKDOWN_LINK, link.type)
        assertEquals("https://platform.deepseek.com/usage", link.visibleText)
        assertEquals("https://platform.deepseek.com/usage", link.normalizedUrl)
    }

    @Test
    fun `excludes trailing sentence punctuation`() {
        val text = "Consulta platform.deepseek.com."
        val link = detectOne(text)!!
        assertEquals("platform.deepseek.com", link.rawText)
        assertEquals(text.length - 1, link.end)

        val comma = detectOne("example.com, luego")!!
        assertEquals("example.com", comma.rawText)

        val parenthesis = detectOne("(https://example.com/path)")!!
        assertEquals("https://example.com/path", parenthesis.rawText)
    }

    @Test
    fun `keeps balanced parentheses in paths`() {
        val link = detectOne("https://example.com/a(b)")!!
        assertEquals("https://example.com/a(b)", link.rawText)
    }

    @Test
    fun `detects multiple urls in one paragraph`() {
        val links = AutoLinkDetector.detect("Uno https://one.com y dos example.org.")
        assertEquals(2, links.size)
        assertEquals("https://one.com", links[0].normalizedUrl)
        assertEquals("https://example.org", links[1].normalizedUrl)
    }

    @Test
    fun `rejects text that is not a domain`() {
        listOf(
            "palabra...otra",
            "hola.mundo",
            "texto..com",
            ".example.com",
            "example.",
            "example.1",
            "example.c",
            "usuario@example.com",
            "version.1.2.3",
            "texto.con.muchos..puntos",
            "exa_mple.com",
            "-example.com",
            "example-.com",
            "example..com",
            "www.",
        ).forEach { text ->
            assertTrue("No debería detectar: $text", AutoLinkDetector.detect(text).isEmpty())
        }
    }

    @Test
    fun `rejects unsafe schemes and malformed urls`() {
        listOf(
            "javascript:alert(1)",
            "file:///storage/file",
            "intent://example.com",
            "content://example.com",
            "data:text/plain,hola",
            "https://",
            "http://",
            "https://example.com:99999",
            "https://example.com:abc",
            "https://exa mple.com",
            "example.com@evil.com",
        ).forEach { text ->
            assertTrue("No debería detectar: $text", AutoLinkDetector.detect(text).isEmpty())
        }
    }

    @Test
    fun `does not detect inside inline code`() {
        assertTrue(AutoLinkDetector.detect("`https://example.com`").isEmpty())
        assertTrue(AutoLinkDetector.detect("Usa `example.com` aquí").isEmpty())
    }

    @Test
    fun `does not detect inside fenced code blocks`() {
        val text = """
            Antes
            ```
            https://example.com
            example.org
            ```
            example.net
        """.trimIndent()

        val links = AutoLinkDetector.detect(text)
        assertEquals(1, links.size)
        assertEquals("https://example.net", links.single().normalizedUrl)
    }

    @Test
    fun `unclosed fence keeps the rest literal`() {
        val text = "Antes\n```\nexample.com\nsigue"
        val links = AutoLinkDetector.detect(text)
        assertEquals(1, links.size)
        assertEquals("https://example.com", links.single().normalizedUrl)
    }

    @Test
    fun `validates explicit urls through the shared api`() {
        assertEquals(
            "https://example.com/path",
            AutoLinkDetector.normalizedHttpUrl("https://example.com/path"),
        )
        assertEquals(
            "https://EXAMPLE.COM/Path",
            AutoLinkDetector.normalizedHttpUrl("HTTPS://EXAMPLE.COM/Path"),
        )
        assertNull(AutoLinkDetector.normalizedHttpUrl("javascript:alert(1)"))
        assertNull(AutoLinkDetector.normalizedHttpUrl("https://exa mple.com"))
    }

    private fun detectOne(text: String): AutoLinkRange? =
        AutoLinkDetector.detect(text).singleOrNull()
}
