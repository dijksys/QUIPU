package io.github.dijksys.quipu.data.local.search

import io.github.dijksys.quipu.domain.model.NoteDateRange
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.NoteSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchQueryBuilderTest {

    @Test
    fun `query joins fts and status without user text in sql`() {
        val built = NoteSearchQueryBuilder.build(
            query = "organización record",
            filter = NoteFilter.ACTIVE,
            dateRange = null,
            sort = NoteSortOrder.RELEVANCE,
        )!!

        assertTrue(built.sql.contains("`notes_fts` MATCH ?"))
        assertTrue(built.sql.contains("n.`deletedAt` IS NULL AND n.`archivedAt` IS NULL"))
        assertTrue(built.sql.contains("ORDER BY quipu_rank ASC"))
        assertFalse(built.sql.contains("organización"))
        assertEquals(4, built.args.size)
        assertEquals("organizacion* record*", built.args.last())
    }

    @Test
    fun `date range adds bound arguments`() {
        val range = NoteDateRange(100, 200)
        val built = NoteSearchQueryBuilder.build(
            query = "nota",
            filter = NoteFilter.TRASH,
            dateRange = range,
            sort = NoteSortOrder.RECENT,
        )!!

        assertTrue(built.sql.contains("n.`updatedAt` >= ? AND n.`updatedAt` < ?"))
        assertTrue(built.args.contains(100L))
        assertTrue(built.args.contains(200L))
        assertEquals(3, built.args.size)
        assertTrue(built.sql.contains("ORDER BY n.`updatedAt` DESC"))
    }

    @Test
    fun `title sort is stable`() {
        val built = NoteSearchQueryBuilder.build(
            query = "nota",
            filter = NoteFilter.ARCHIVED,
            dateRange = null,
            sort = NoteSortOrder.TITLE,
        )!!

        assertTrue(built.sql.contains("n.`title` COLLATE NOCASE ASC, n.`updatedAt` DESC"))
        assertTrue(built.sql.contains("n.`deletedAt` IS NULL AND n.`archivedAt` IS NOT NULL"))
    }

    @Test
    fun `empty terms do not build a query`() {
        assertNull(
            NoteSearchQueryBuilder.build(
                query = "???",
                filter = NoteFilter.ACTIVE,
                dateRange = null,
                sort = NoteSortOrder.RELEVANCE,
            ),
        )
    }
}
