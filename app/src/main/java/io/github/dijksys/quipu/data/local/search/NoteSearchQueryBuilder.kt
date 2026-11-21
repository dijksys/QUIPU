package io.github.dijksys.quipu.data.local.search

import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import io.github.dijksys.quipu.domain.search.SearchText

data class SearchSql(
    val sql: String,
    val args: List<Any?>,
)

/**
 * Construye la consulta FTS con argumentos enlazados. El texto del usuario nunca
 * se concatena en el SQL: solo se usa para los parǭmetros de MATCH y LIKE.
 */
object NoteSearchQueryBuilder {

    fun build(
        query: String,
        filter: NoteFilter,
        dateRange: NoteDateRange?,
        sort: NoteSortOrder,
    ): SearchSql? {
        val match = SearchText.matchExpression(query) ?: return null
        val terms = SearchText.terms(query)
        val rawQuery = query.trim()
        val args = mutableListOf<Any?>()

        val rank = if (sort == NoteSortOrder.RELEVANCE) {
            args += rawQuery
            args += rawQuery
            args += "${terms.firstOrNull().orEmpty()}%"
            """
            ,
            CASE
                WHEN lower(trim(n.`title`)) = lower(?) THEN 0
                WHEN lower(trim(n.`title`)) LIKE lower(?) || '%' THEN 1
                WHEN EXISTS (
                    SELECT 1
                    FROM `note_tag_cross_ref` c
                    INNER JOIN `note_tags` t ON t.`id` = c.`tagId`
                    WHERE c.`noteId` = n.`id` AND t.`normalizedName` LIKE ?
                ) THEN 2
                ELSE 3
            END AS quipu_rank
            """.trimIndent()
        } else {
            ""
        }

        val status = when (filter) {
            NoteFilter.ACTIVE -> "n.`deletedAt` IS NULL AND n.`archivedAt` IS NULL"
            NoteFilter.ARCHIVED -> "n.`deletedAt` IS NULL AND n.`archivedAt` IS NOT NULL"
            NoteFilter.TRASH -> "n.`deletedAt` IS NOT NULL"
        }

        val dateClause = if (dateRange == null) {
            ""
        } else {
            "AND n.`updatedAt` >= ? AND n.`updatedAt` < ?"
        }

        args += match
        if (dateRange != null) {
            args += dateRange.startMillis
            args += dateRange.endMillisExclusive
        }

        val order = when (sort) {
            NoteSortOrder.RELEVANCE -> "quipu_rank ASC, n.`updatedAt` DESC"
            NoteSortOrder.RECENT -> "n.`updatedAt` DESC"
            NoteSortOrder.TITLE -> "n.`title` COLLATE NOCASE ASC, n.`updatedAt` DESC"
        }

        val sql = """
            SELECT n.*$rank
            FROM `notes` AS n
            INNER JOIN `notes_fts` ON `notes_fts`.`noteId` = n.`id`
            WHERE `notes_fts` MATCH ?
              AND $status
              $dateClause
            ORDER BY $order
        """.trimIndent()

        return SearchSql(sql = sql, args = args)
    }
}
