package io.github.dijksys.quipu.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDateFilterTest {

    private val zone = ZoneId.of("UTC")
    private val now = LocalDate.of(2026, 11, 21)
        .atTime(12, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `all dates has no range`() {
        assertNull(NoteDateFilter.ALL.resolveRange(now, null, zone))
    }

    @Test
    fun `today covers the local day`() {
        val range = NoteDateFilter.TODAY.resolveRange(now, null, zone)!!

        assertTrue(range.contains(LocalDate.of(2026, 11, 21).atStartOfDay(zone).toInstant().toEpochMilli()))
        assertTrue(
            range.contains(
                LocalDate.of(2026, 11, 21).atTime(23, 59).atZone(zone).toInstant().toEpochMilli(),
            ),
        )
    }

    @Test
    fun `last seven and thirty days include today`() {
        val week = NoteDateFilter.LAST_7_DAYS.resolveRange(now, null, zone)!!
        val month = NoteDateFilter.LAST_30_DAYS.resolveRange(now, null, zone)!!

        assertEquals(
            LocalDate.of(2026, 11, 15).atStartOfDay(zone).toInstant().toEpochMilli(),
            week.startMillis,
        )
        assertEquals(
            LocalDate.of(2026, 10, 23).atStartOfDay(zone).toInstant().toEpochMilli(),
            month.startMillis,
        )
    }

    @Test
    fun `custom filter keeps the provided range`() {
        val custom = NoteDateRange(10, 20)
        assertEquals(custom, NoteDateFilter.CUSTOM.resolveRange(now, custom, zone))
    }

    @Test
    fun `date range uses an exclusive end`() {
        val range = NoteDateRange(100, 200)
        assertTrue(range.contains(100))
        assertTrue(range.contains(199))
        assertTrue(!range.contains(200))
    }
}
