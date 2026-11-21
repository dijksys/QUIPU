package io.github.dijksys.quipu.feature.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import io.github.dijksys.quipu.domain.formatting.MarkdownVisualPlan
import io.github.dijksys.quipu.domain.formatting.MarkdownVisualStyle

/**
 * Representación visual del Markdown mientras se edita. Solo transforma la
 * presentación: el TextFieldValue y el contenido persistido no cambian.
 */
class MarkdownVisualTransformation(
    private val plan: MarkdownVisualPlan,
    private val linkColor: Color,
    private val inlineCodeBackground: Color,
    private val codeBlockBackground: Color,
    private val markerColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val annotated = buildAnnotatedString {
            append(plan.text)
            plan.styles.forEach { range ->
                addStyle(spanFor(range.style), range.start, range.end)
            }
        }

        val originalToTransformed = plan.originalToTransformed
        val transformedToOriginal = plan.transformedToOriginal
        return TransformedText(
            text = annotated,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    originalToTransformed[offset.coerceIn(0, originalToTransformed.lastIndex)]

                override fun transformedToOriginal(offset: Int): Int =
                    transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
            },
        )
    }

    private fun spanFor(style: MarkdownVisualStyle): SpanStyle = when (style) {
        MarkdownVisualStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        MarkdownVisualStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        MarkdownVisualStyle.HEADING_1 -> SpanStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 1.5.em,
        )

        MarkdownVisualStyle.HEADING_2 -> SpanStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 1.3.em,
        )

        MarkdownVisualStyle.HEADING_3 -> SpanStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 1.15.em,
        )

        MarkdownVisualStyle.LINK -> SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        )

        MarkdownVisualStyle.INLINE_CODE -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = inlineCodeBackground,
        )

        MarkdownVisualStyle.CODE_BLOCK -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = codeBlockBackground,
        )

        MarkdownVisualStyle.LIST_MARKER -> SpanStyle(color = markerColor)
        MarkdownVisualStyle.CHECKLIST_DONE -> SpanStyle(color = markerColor)
        MarkdownVisualStyle.CHECKLIST_PENDING -> SpanStyle(color = markerColor)
    }
}
