package io.github.dijksys.quipu.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class NoteDateFilter {
    ALL,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM,
}

enum class NoteSortOrder {
    RELEVANCE,
    RECENT,
    TITLE,
}

data class NoteDateRange(
    val startMillis: Long,
    val endMillisExclusive: Long,
) {
    fun contains(value: Long): Boolean = value >= startMillis && value < endMillisExclusive
}

fun NoteDateFilter.resolveRange(
    nowMillis: Long,
    customRange: NoteDateRange?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): NoteDateRange? {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    return when (this) {
        NoteDateFilter.ALL -> null
        NoteDateFilter.CUSTOM -> customRange
        NoteDateFilter.TODAY -> rangeFor(today, today, zoneId)
        NoteDateFilter.LAST_7_DAYS -> rangeFor(today.minusDays(6), today, zoneId)
        NoteDateFilter.LAST_30_DAYS -> rangeFor(today.minusDays(29), today, zoneId)
    }
}

fun NoteDateRange(start: LocalDate, endInclusive: LocalDate, zoneId: ZoneId): NoteDateRange =
    NoteDateRange(
        startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endMillisExclusive = endInclusive.plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli(),
    )

private fun rangeFor(start: LocalDate, end: LocalDate, zoneId: ZoneId): NoteDateRange =
    NoteDateRange(start, end, zoneId)
