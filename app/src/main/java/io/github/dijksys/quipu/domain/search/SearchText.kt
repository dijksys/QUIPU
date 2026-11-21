package io.github.dijksys.quipu.domain.search

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizaci��n compartida por la consulta FTS, las etiquetas y el resaltado.
 * Elimina diacr��ticos para que "organizacion" encuentre "organizaci��n".
 */
object SearchText {

    private val diacritics = Regex("""\p{M}+""")
    private val nonTermCharacters = Regex("""[^\p{L}\p{N}]+""")
    private val whitespace = Regex("""\s+""")

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase(Locale.ROOT)

    fun terms(query: String): List<String> =
        normalize(query)
            .split(nonTermCharacters)
            .filter { it.isNotBlank() }

    /**
     * Expresi��n MATCH segura: solo letras y n��meros, todos los t��rminos con
     * semǭntica AND y coincidencia por prefijo.
     */
    fun matchExpression(query: String): String? =
        terms(query)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { "$it*" }

    fun normalizeTag(name: String): String =
        normalize(name).replace(whitespace, " ").trim()
}
