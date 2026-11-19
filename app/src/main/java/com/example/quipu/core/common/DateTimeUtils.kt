package com.example.quipu.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilidades de fecha y hora. La aplicación guarda siempre epoch millis y
 * presenta los valores en la zona horaria actual del dispositivo.
 */
object DateTimeUtils {

    /** La interfaz está en español; las fechas se presentan con el mismo idioma. */
    private val locale: Locale = Locale.forLanguageTag("es")

    private fun zone(): ZoneId = ZoneId.systemDefault()

    fun now(): Long = System.currentTimeMillis()

    fun toLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDateTime()

    fun toEpochMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone()).toInstant().toEpochMilli()

    fun combine(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        toEpochMillis(LocalDateTime.of(year, month, day, hour, minute))

    fun datePart(epochMillis: Long): LocalDate = toLocalDateTime(epochMillis).toLocalDate()

    fun timePart(epochMillis: Long): LocalTime = toLocalDateTime(epochMillis).toLocalTime()

    fun hour(epochMillis: Long): Int = toLocalDateTime(epochMillis).hour

    fun minute(epochMillis: Long): Int = toLocalDateTime(epochMillis).minute

    /** Convierte el valor UTC devuelto por Material DatePicker a una fecha local. */
    fun datePickerValueToLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    /** Convierte una fecha local al valor UTC que espera Material DatePicker. */
    fun localDateToDatePickerValue(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun withDate(epochMillis: Long, date: LocalDate): Long {
        val time = timePart(epochMillis)
        return toEpochMillis(LocalDateTime.of(date, time))
    }

    fun withTime(epochMillis: Long, hour: Int, minute: Int): Long {
        val date = datePart(epochMillis)
        return toEpochMillis(LocalDateTime.of(date, LocalTime.of(hour, minute)))
    }

    fun formatDate(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)
            .format(toLocalDateTime(epochMillis))

    fun formatShortDate(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("EEE d MMM", locale)
            .format(toLocalDateTime(epochMillis))

    fun formatLongDate(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)
            .format(toLocalDateTime(epochMillis))

    fun formatTime(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("HH:mm", locale)
            .format(toLocalDateTime(epochMillis))

    fun formatDateTime(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale)
            .format(toLocalDateTime(epochMillis))

    fun formatMonthYear(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
            .format(toLocalDateTime(epochMillis))
}
