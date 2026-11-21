package io.github.dijksys.quipu.feature.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import io.github.dijksys.quipu.domain.search.SearchText

/**
 * Texto con las coincidencias resaltadas. La normalizaci��n es la misma que la
 * de la consulta FTS, por lo que las tildes y mayǧsculas no alteran el resultado.
 */
@Composable
fun NoteSearchHighlight(
    text: String,
    terms: List<String>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val background = MaterialTheme.colorScheme.primaryContainer
    val foreground = MaterialTheme.colorScheme.onPrimaryContainer
    val annotated = remember(text, terms, background, foreground) {
        highlightMatches(text, terms, background, foreground)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

fun highlightMatches(
    text: String,
    terms: List<String>,
    background: Color,
    foreground: Color,
): AnnotatedString {
    if (terms.isEmpty() || text.isEmpty()) return AnnotatedString(text)

    val ranges = matchRanges(text, terms)
    if (ranges.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(
                SpanStyle(background = background, color = foreground),
                range.first,
                range.last + 1,
            )
        }
    }
}

private fun matchRanges(text: String, terms: List<String>): List<IntRange> {
    val normalized = StringBuilder(text.length)
    val originalStart = mutableListOf<Int>()
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val chars = String(Character.toChars(codePoint))
        SearchText.normalize(chars).forEach { normalizedChar ->
            normalized.append(normalizedChar)
            originalStart += index
        }
        index += Character.charCount(codePoint)
    }

    val ranges = mutableListOf<IntRange>()
    terms.forEach { term ->
        if (term.isEmpty()) return@forEach
        var from = 0
        while (true) {
            val found = normalized.indexOf(term, from)
            if (found < 0) break
            val startsToken = found == 0 || !normalized[found - 1].isLetterOrDigit()
            if (startsToken) {
                val originalFrom = originalStart[found]
                val originalTo = if (found + term.length < originalStart.size) {
                    originalStart[found + term.length]
                } else {
                    text.length
                }
                if (originalTo > originalFrom) ranges += originalFrom until originalTo
            }
            from = found + 1
        }
    }

    if (ranges.isEmpty()) return emptyList()
    val sorted = ranges.sortedBy { it.first }
    val merged = mutableListOf<IntRange>()
    sorted.forEach { range ->
        val last = merged.lastOrNull()
        if (last != null && range.first <= last.last + 1) {
            merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
        } else {
            merged += range
        }
    }
    return merged
}
