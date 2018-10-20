package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import org.joda.time.*
import org.joda.time.format.DateTimeFormat
import java.lang.IllegalStateException


val dateRegex = Regex("[\\d]{2}(/[\\d]{2}(/[\\d]{2}([\\d]{2})?)?)?")
val timeRegex = Regex("[\\d]{2}(:[\\d]{2}(:[\\d]{2}(\\.[\\d]([\\d]{2})?)?)?)?")
val dateTimeRegex = Regex("(${dateRegex.pattern} )?${timeRegex.pattern}")
val stepRegex = Regex(timeRegex.pattern)
val periodRegex = Regex("(${dateTimeRegex.pattern})-(${dateTimeRegex.pattern})")
val commonFormatRegex = Regex("(${periodRegex.pattern}(( step)? ${stepRegex.pattern})?)|(${dateTimeRegex.pattern})")

private val timeConverters = listOf(
    DefaultConverter(
        "mm",
        DateTime(0L, DateTimeZone.UTC).plusHours(1).millis,
        setOf(DateTimeFieldType.minuteOfHour())
    ),
    DefaultConverter(
        "HH:mm",
        DateTime(0L, DateTimeZone.UTC).plusDays(1).millis,
        setOf(DateTimeFieldType.minuteOfHour(), DateTimeFieldType.hourOfDay())
    ),
    DefaultConverter(
        "HH:mm:ss",
        DateTime(0L, DateTimeZone.UTC).plusDays(1).millis,
        setOf(DateTimeFieldType.minuteOfHour(), DateTimeFieldType.hourOfDay(), DateTimeFieldType.secondOfMinute())
    ),
    DefaultConverter(
        "HH:mm:ss.SSS",
        DateTime(0L, DateTimeZone.UTC).plusDays(1).millis,
        setOf(
            DateTimeFieldType.minuteOfHour(),
            DateTimeFieldType.hourOfDay(),
            DateTimeFieldType.secondOfMinute(),
            DateTimeFieldType.millisOfSecond()
        )
    )
)

private val dateConverters = listOf(
    DefaultConverter(
        "dd",
        DateTime(0L, DateTimeZone.UTC).plusMonths(1).millis,
        setOf(DateTimeFieldType.dayOfMonth())
    ),
    DefaultConverter(
        "dd/MM",
        DateTime(0L, DateTimeZone.UTC).plusYears(1).millis,
        setOf(DateTimeFieldType.dayOfMonth(), DateTimeFieldType.monthOfYear())
    ),
    DefaultConverter(
        "dd/MM/yy",
        DateTime(0L, DateTimeZone.UTC).plusYears(100).millis,
        setOf(DateTimeFieldType.dayOfMonth(), DateTimeFieldType.monthOfYear(), DateTimeFieldType.year())
    ),
    DefaultConverter(
        "dd/MM/yyyy",
        DateTime(0L, DateTimeZone.UTC).plusYears(10000).millis,
        setOf(DateTimeFieldType.dayOfMonth(), DateTimeFieldType.monthOfYear(), DateTimeFieldType.year())
    )
)

private interface Converter {
    fun convert(from: String): CalculatedDateTime?
}

private object CommonConverter : Converter {
    private val splitRegex = Regex(" ")

    override fun convert(from: String): CalculatedDateTime? {
        return if (from.contains(splitRegex)) {
            from.split(splitRegex).let {
                val calculated = listOfNotNull(
                    it.first().calculateDate(),
                    it[1].calculateTime()
                )

                var dateTime = DateTime(0L)
                val importantFields = calculated.flatMap {
                    fieldsOwner ->
                    fieldsOwner.importantFields.forEach {
                        field ->
                        dateTime = dateTime.withField(
                            field,
                            fieldsOwner.dateTime[field]
                        )
                    }

                    fieldsOwner.importantFields
                }.toSet()

                CalculatedDateTime(
                    from,
                    dateTime,
                    calculated.maxBy {
                        it.futureDifference
                    } ?.futureDifference ?: throw IllegalStateException(),
                    importantFields
                )
            }
        } else {
            from.calculateTime()
        }
    }
}

private class DefaultConverter(
    format: String,
    private val futureDifference: Long,
    private val importantFields: Set<DateTimeFieldType>
) : Converter {
    private val formatter = DateTimeFormat.forPattern(format)

    override fun convert(from: String): CalculatedDateTime? {
        return try {
            CalculatedDateTime(
                from,
                formatter.parseDateTime(from),
                futureDifference,
                importantFields
            )
        } catch (e: Throwable) {
            null
        }
    }
}

data class CalculatedDateTime internal constructor(
    internal val source: String,
    internal val dateTime: DateTime,
    internal val futureDifference: Long,
    internal val importantFields: Set<DateTimeFieldType>
) {
    val withoutTimeZoneOffset: DateTime
        get() {
            return dateTime.run {
                withZone(
                    DateTimeZone.UTC
                ).plus(
                    zone.toTimeZone().rawOffset.toLong()
                )
            }
        }
    val asNow: DateTime
        get() {
            var now: DateTime = DateTime.now()

            importantFields.forEach {
                now = now.withField(
                    it,
                    dateTime[it]
                )
            }
            return now
        }

    val asFuture: DateTime
        get() {
            val now = asNow

            if (now.isAfterNow) {
                return now
            }

            return now.plus(futureDifference)
        }
}

private fun String.calculateTime(): CalculatedDateTime? {
    timeConverters.forEach {
        converter ->
        converter.convert(this) ?.also {
            return it
        }
    }
    return null
}

private fun String.calculateDate(): CalculatedDateTime? {
    dateConverters.forEach {
        converter ->
        converter.convert(this) ?.also {
            return it
        }
    }
    return null
}

private fun String.calculateDateTime(): CalculatedDateTime? {
    return CommonConverter.convert(this)
}

fun String.parseDateTimes(): List<CalculatedDateTime> {
    return if (commonFormatRegex.matches(this)) {
        periodRegex.find(this) ?.value ?.let {
            period ->
            val withoutPeriod = replaceFirst(period, "")

            val pair: Pair<CalculatedDateTime, CalculatedDateTime> = dateTimeRegex.findAll(period).mapNotNull {
                it.value.calculateDateTime()
            }.toList().let {
                Pair(it.first(), it[1])
            }
            stepRegex.find(withoutPeriod) ?.value ?.let {
                stepString ->
                stepString.calculateTime() ?.let {
                    step ->

                    val importantFields = pair.first.importantFields.plus(pair.second.importantFields)
                    val futureDifference = pair.first.futureDifference.let {
                        first ->
                        pair.second.futureDifference.let {
                            second ->
                            kotlin.math.max(first, second)
                        }
                    }

                    val firstMillis = pair.first.dateTime.millis
                    val secondMillis = pair.second.dateTime.millis.let {
                        if (it <= firstMillis) {
                            it + futureDifference
                        } else {
                            it
                        }
                    }

                    (firstMillis until secondMillis step step.withoutTimeZoneOffset.millis).map {
                        CalculatedDateTime(
                            this,
                            DateTime(it),
                            futureDifference,
                            importantFields
                        )
                    }
                }
            } ?: listOf(
                pair.first,
                pair.second
            )
        } ?:listOfNotNull(
            calculateDateTime()
        )
    } else {
        emptyList()
    }
}
