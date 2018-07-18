package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

internal val converters = listOf(
    TimeConverter(),
    SimpleDayTimeConverter(),
    SimpleDayMonthTimeConverter(),
    SimpleDayMonthYearTimeConverter()
)

private class TimeConverter : DateTimeConverter {
    override val formatPattern: String = "HH:mm"
    private val dateTimeFormat = DateTimeFormat.forPattern(formatPattern).withZone(DateTimeZone.getDefault())
    override val timeZoneId: String = dateTimeFormat.zone.id

    override fun tryConvert(from: String): DateTime? {
        return try {
            val parsed = dateTimeFormat.parseDateTime(from)
            val now = DateTime.now()
            parsed
                .withDate(
                    now.year,
                    now.monthOfYear,
                    now.dayOfMonth
                ).let {
                    if (it.isBefore(now)) {
                        it.plusDays(1)
                    } else {
                        it
                    }
                }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

private class SimpleDayTimeConverter : DateTimeConverter {
    override val formatPattern: String = "dd HH:mm"
    private val dateTimeFormat = DateTimeFormat.forPattern(formatPattern).withZone(DateTimeZone.getDefault())
    override val timeZoneId: String = dateTimeFormat.zone.id

    override fun tryConvert(from: String): DateTime? {
        return try {
            val parsed = dateTimeFormat.parseDateTime(from)
            val now = DateTime.now()
            parsed
                .withYear(
                    now.year
                )
                .withMonthOfYear(
                    now.monthOfYear
                ).let {
                    if (it.isBefore(now)) {
                        it.plusMonths(1)
                    } else {
                        it
                    }
                }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

private class SimpleDayMonthTimeConverter : DateTimeConverter {
    override val formatPattern: String = "dd.MM HH:mm"
    private val dateTimeFormat = DateTimeFormat.forPattern(formatPattern).withZone(DateTimeZone.getDefault())
    override val timeZoneId: String = dateTimeFormat.zone.id

    override fun tryConvert(from: String): DateTime? {
        return try {
            val parsed = dateTimeFormat.parseDateTime(from)
            val now = DateTime.now()
            parsed
                .withYear(
                    now.year
                )
                .let {
                    if (it.isBefore(now)) {
                        it.plusYears(1)
                    } else {
                        it
                    }
                }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

private class SimpleDayMonthYearTimeConverter : DateTimeConverter {
    override val formatPattern: String = "dd.MM.yy HH:mm"
    private val dateTimeFormat = DateTimeFormat.forPattern(formatPattern).withZone(DateTimeZone.getDefault())
    override val timeZoneId: String = dateTimeFormat.zone.id

    override fun tryConvert(from: String): DateTime? {
        return try {
            val parsed = dateTimeFormat.parseDateTime(from)
            val now = DateTime.now()
            parsed
                .let {
                    if (it.isBefore(now)) {
                        it.plusYears(100)
                    } else {
                        it
                    }
                }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

