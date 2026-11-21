package io.github.dijksys.quipu.domain.model

import io.github.dijksys.quipu.domain.formatting.MarkdownTextExtractor

/**
 * Representación de una nota independiente de Room y de Android.
 * Las fechas se almacenan en epoch millis (UTC).
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val tags: List<String> = emptyList(),
) {
    val isArchived: Boolean get() = archivedAt != null
    val isDeleted: Boolean get() = deletedAt != null
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()

    val displayTitle: String
        get() = title.trim().ifBlank { "Sin título" }

    val preview: String = MarkdownTextExtractor.preview(content)
}
