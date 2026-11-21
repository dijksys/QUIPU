package io.github.dijksys.quipu.domain.formatting

import java.net.IDN
import java.net.URI
import java.util.Locale

enum class AutoLinkType {
    EXPLICIT_URL,
    BARE_DOMAIN,
    MARKDOWN_LINK,
}

/**
 * Enlace detectado en el texto fuente. [start, end) cubre todo el fragmento
 * reconocido; [visibleStart, visibleEnd) cubre el texto que se muestra y se
 * puede pulsar.
 */
data class AutoLinkRange(
    val start: Int,
    val end: Int,
    val visibleStart: Int,
    val visibleEnd: Int,
    val rawText: String,
    val visibleText: String,
    val normalizedUrl: String,
    val type: AutoLinkType,
)

/**
 * Detector determinista y sin dependencias de Android. Es la única fuente de
 * validación de URLs para el parser, el planner visual y el editor.
 */
object AutoLinkDetector {

    private val allowedSchemes = setOf("http", "https")
    private val allowedTokenCharacters = buildSet {
        addAll(('a'..'z'))
        addAll(('A'..'Z'))
        addAll(('0'..'9'))
        addAll("._~:/?#[]@!$&'()*+,;=%-".toList())
    }

    private val validCountryTlds = """
        ac ad ae af ag ai al am ao aq ar as at au aw ax az ba bb bd be bf bg bh bi bj bm bn bo br bs bt bw by bz
        ca cc cd cf cg ch ci ck cl cm cn co cr cu cv cw cx cy cz de dj dk dm do dz ec ee eg er es et eu fi fj fk
        fm fo fr ga gb gd ge gf gg gh gi gl gm gn gp gq gr gs gt gu gw gy hk hm hn hr ht hu id ie il im in io iq
        ir is it je jm jo jp ke kg kh ki km kn kp kr kw ky kz la lb lc li lk lr ls lt lu lv ly ma mc md me mg mh
        mk ml mm mn mo mp mq mr ms mt mu mv mw mx my mz na nc ne nf ng ni nl no np nr nu nz om pa pe pf pg ph pk
        pl pm pn pr ps pt pw py qa re ro rs ru rw sa sb sc sd se sg sh si sk sl sm sn so sr ss st su sv sx sy sz
        tc td tf tg th tj tk tl tm tn to tr tt tv tw tz ua ug uk us uy uz va vc ve vg vi vn vu wf ws ye yt za zm zw
    """.trimIndent().split(Regex("\\s+")).toSet()

    private val validGenericTlds = """
        aero app art asia bar best bet biz blog cafe cloud club code com coop dance design dev digital email energy
        engineering enterprise events exchange expert express finance financial fm fun global gov group guru health
        info institute int international io jobs land law life link live ltd media mobi museum name net network news
        ninja one online org page pro pub review rocks school science services shop site software solutions space
        store studio support systems team tech tel today tools top travel university uno video vip wiki work works
        world xyz zone
    """.trimIndent().split(Regex("\\s+")).toSet()

    fun detect(text: String): List<AutoLinkRange> {
        if (text.isEmpty()) return emptyList()

        val protected = BooleanArray(text.length)
        markFencedCode(text, protected)
        markInlineCode(text, protected)

        val links = mutableListOf<AutoLinkRange>()
        var index = 0
        while (index < text.length) {
            if (protected[index]) {
                index++
                continue
            }
            val markdown = markdownLinkAt(text, index, text.length)
            if (markdown != null) {
                links += markdown
                for (position in markdown.start until markdown.end) {
                    if (position in protected.indices) protected[position] = true
                }
                index = markdown.end
                continue
            }
            val auto = findAt(text, index, text.length)
            if (auto != null) {
                links += auto
                index = auto.end
            } else {
                index++
            }
        }
        return links.sortedBy { it.start }
    }

    /** Busca un autolink que empiece exactamente en [index]. */
    fun findAt(text: String, index: Int, endExclusive: Int = text.length): AutoLinkRange? {        if (index !in text.indices || index >= endExclusive) return null
        if (!isStartBoundary(text, index)) return null

        val explicit = if (
            text.regionMatches(index, "http://", 0, 7, ignoreCase = true) ||
            text.regionMatches(index, "https://", 0, 8, ignoreCase = true)
        ) {
            readUrl(index, endExclusive, text)?.let { candidate ->
                val normalized = normalizeExplicit(candidate.raw) ?: return@let null
                AutoLinkRange(
                    start = index,
                    end = candidate.end,
                    visibleStart = index,
                    visibleEnd = candidate.end,
                    rawText = candidate.raw,
                    visibleText = candidate.raw,
                    normalizedUrl = normalized,
                    type = AutoLinkType.EXPLICIT_URL,
                )
            }
        } else {
            null
        }
        if (explicit != null) return explicit

        if (!isDomainStart(text, index, endExclusive)) return null
        if (hasMalformedSchemePrefix(text, index)) return null
        val candidate = readUrl(index, endExclusive, text) ?: return null
        if (candidate.raw.count { it == '.' } == 0) return null
        val normalized = normalizeBareDomain(candidate.raw) ?: return null
        return AutoLinkRange(
            start = index,
            end = candidate.end,
            visibleStart = index,
            visibleEnd = candidate.end,
            rawText = candidate.raw,
            visibleText = candidate.raw,
            normalizedUrl = normalized,
            type = AutoLinkType.BARE_DOMAIN,
        )
    }

    /** Reconoce [texto](https://destino) con un destino HTTP/HTTPS válido. */
    fun markdownLinkAt(
        text: String,
        index: Int,
        endExclusive: Int = text.length,
    ): AutoLinkRange? {
        if (index !in text.indices || text[index] != '[') return null
        if (index > 0 && text[index - 1] == '!') return null

        val labelEnd = text.indexOf("](", index + 1)
        if (labelEnd !in (index + 1) until endExclusive) return null
        val urlEnd = text.indexOf(')', labelEnd + 2)
        if (urlEnd !in (labelEnd + 2) until endExclusive) return null

        val label = text.substring(index + 1, labelEnd)
        val url = text.substring(labelEnd + 2, urlEnd).trim()
        if (label.isEmpty() || url.isEmpty()) return null
        val normalized = normalizeExplicit(url) ?: return null

        return AutoLinkRange(
            start = index,
            end = urlEnd + 1,
            visibleStart = index + 1,
            visibleEnd = labelEnd,
            rawText = text.substring(index, urlEnd + 1),
            visibleText = label,
            normalizedUrl = normalized,
            type = AutoLinkType.MARKDOWN_LINK,
        )
    }

    /** Evita intentar detectar en mitad de una palabra o correo. */
    fun isPotentialStart(text: String, index: Int): Boolean {
        if (index !in text.indices || !isStartBoundary(text, index)) return false
        val char = text[index]
        if (!char.isLetterOrDigit()) return false
        if (
            text.regionMatches(index, "http://", 0, 7, ignoreCase = true) ||
            text.regionMatches(index, "https://", 0, 8, ignoreCase = true)
        ) {
            return true
        }
        return isDomainStart(text, index, text.length)
    }

    fun normalizedHttpUrl(raw: String): String? = normalizeExplicit(raw.trim())

    fun isValidHttpUrl(raw: String): Boolean = normalizedHttpUrl(raw) != null

    private data class Candidate(val raw: String, val end: Int)

    private fun readUrl(index: Int, endExclusive: Int, text: String): Candidate? {
        var cursor = index
        while (cursor < endExclusive && text[cursor] in allowedTokenCharacters) {
            cursor++
        }
        if (cursor == index) return null

        var end = cursor
        while (end > index) {
            val char = text[end - 1]
            val trimmed = when (char) {
                '.', ',', ';', ':', '!', '?', '\'', '"' -> true
                ')' -> text.substring(index, end).count { it == ')' } >
                    text.substring(index, end).count { it == '(' }

                ']' -> text.substring(index, end).count { it == ']' } >
                    text.substring(index, end).count { it == '[' }

                '}' -> text.substring(index, end).count { it == '}' } >
                    text.substring(index, end).count { it == '{' }

                else -> false
            }
            if (!trimmed) break
            end--
        }
        if (end <= index) return null
        return Candidate(text.substring(index, end), end)
    }

    private fun normalizeExplicit(raw: String): String? {
        if (raw.isEmpty() || raw.any { it.isWhitespace() || it.code < 0x20 }) return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme !in allowedSchemes) return null
        if (!uri.isAbsolute || uri.isOpaque) return null
        if (uri.port !in -1..65_535) return null

        val host = uri.host ?: extractHost(uri.rawAuthority) ?: return null
        if (!isValidHost(host, requireDot = true, allowIp = true)) return null
        return when {
            raw.startsWith("http://", ignoreCase = true) -> "http://" + raw.substring(7)
            raw.startsWith("https://", ignoreCase = true) -> "https://" + raw.substring(8)
            else -> null
        }
    }

    private fun normalizeBareDomain(raw: String): String? {
        if (raw.isEmpty() || raw.any { it.isWhitespace() || it.code < 0x20 }) return null
        if (raw.contains('@')) return null

        val authorityEnd = raw.indexOfAny(charArrayOf('/', '?', '#')).let {
            if (it < 0) raw.length else it
        }
        val authority = raw.substring(0, authorityEnd)
        val host = extractHost(authority) ?: return null
        if (!isValidHost(host, requireDot = true, allowIp = false)) return null

        val asciiHost = toAsciiHost(host) ?: return null
        val tld = asciiHost.substringAfterLast('.').lowercase(Locale.ROOT)
        if (!isKnownTld(tld)) return null
        return "https://$raw"
    }

    private fun extractHost(authority: String?): String? {
        if (authority.isNullOrBlank()) return null
        val withoutUser = authority.substringAfterLast('@')
        if (withoutUser.isBlank()) return null
        if (withoutUser.startsWith("[")) return null
        val hostPort = withoutUser
        val colon = hostPort.indexOf(':')
        if (colon >= 0) {
            if (hostPort.indexOf(':', colon + 1) >= 0) return null
            val port = hostPort.substring(colon + 1)
            if (port.isEmpty() || port.length > 5 || port.any { !it.isDigit() }) return null
            val value = port.toIntOrNull() ?: return null
            if (value !in 1..65_535) return null
            return hostPort.substring(0, colon)
        }
        return hostPort
    }

    private fun isValidHost(host: String, requireDot: Boolean, allowIp: Boolean): Boolean {
        if (host.isBlank() || host.length > 253) return false
        if (allowIp && isIpv4(host)) return true

        val ascii = toAsciiHost(host) ?: return false
        val labels = ascii.split('.')
        if (requireDot && labels.size < 2) return false
        if (labels.any { it.isEmpty() || it.length > 63 }) return false

        val labelPattern = Regex("""^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$""")
        return labels.all { label ->
            labelPattern.matches(label) && !label.startsWith('-') && !label.endsWith('-')
        }
    }

    private fun toAsciiHost(host: String): String? = runCatching {
        IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).lowercase(Locale.ROOT)
    }.getOrNull()

    private fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all { it.isDigit() } &&
                part.toIntOrNull() in 0..255
        }
    }

    private fun hasMalformedSchemePrefix(text: String, index: Int): Boolean {
        var cursor = index - 1
        while (cursor >= 0 && text[cursor].isWhitespace()) cursor--
        if (cursor < 0) return false
        val tokenEnd = cursor + 1
        while (cursor >= 0 && !text[cursor].isWhitespace()) cursor--
        val token = text.substring(cursor + 1, tokenEnd)
        if (!token.startsWith("http://", ignoreCase = true) &&
            !token.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        return normalizeExplicit(token) == null
    }

    private fun isKnownTld(tld: String): Boolean =
        tld in validCountryTlds || tld in validGenericTlds || tld.startsWith("xn--")

    private fun isStartBoundary(text: String, index: Int): Boolean {
        if (index == 0) return true
        val previous = text[index - 1]
        return !(previous.isLetterOrDigit() || previous == '@' || previous == '.' ||
            previous == '_' || previous == '-' || previous == '/')
    }

    private fun isDomainStart(text: String, index: Int, endExclusive: Int): Boolean {        val first = text[index]
        if (!first.isLetterOrDigit()) return false
        var cursor = index
        while (cursor < endExclusive && text[cursor] in allowedTokenCharacters) {
            if (text[cursor] == '.') return true
            cursor++
        }
        return false
    }

    private fun markFencedCode(text: String, protected: BooleanArray) {
        var lineStart = 0
        while (lineStart <= text.length) {
            val lineBreak = text.indexOf('\n', lineStart)
            val lineEnd = if (lineBreak < 0) text.length else lineBreak
            val line = text.substring(lineStart, lineEnd)
            if (line.trimStart().startsWith("```")) {
                val closingEnd = findClosingFenceEnd(text, lineEnd)
                if (closingEnd != null) {
                    for (position in lineStart until closingEnd) {
                        if (position in protected.indices) protected[position] = true
                    }
                    lineStart = closingEnd + 1
                    continue
                }
            }
            if (lineBreak < 0) break
            lineStart = lineBreak + 1
        }
    }

    private fun findClosingFenceEnd(text: String, fromLineEnd: Int): Int? {
        var lineStart = if (fromLineEnd < text.length) fromLineEnd + 1 else text.length
        while (lineStart <= text.length) {
            val lineBreak = text.indexOf('\n', lineStart)
            val lineEnd = if (lineBreak < 0) text.length else lineBreak
            if (text.substring(lineStart, lineEnd).trim() == "```") return lineEnd
            if (lineBreak < 0) return null
            lineStart = lineBreak + 1
        }
        return null
    }

    private fun markInlineCode(text: String, protected: BooleanArray) {
        var index = 0
        while (index < text.length) {
            if (protected[index] || text[index] != '`') {
                index++
                continue
            }
            val close = text.indexOf('`', index + 1)
            if (close < 0) break
            for (position in index..close) {
                if (position in protected.indices) protected[position] = true
            }
            index = close + 1
        }
    }
}
