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
        setOf(DateTimeFieldType.minuteOfHour()),
        setOf(DateTimeFieldType.secondOfMinute(), DateTimeFieldType.millisOfSecond())
    ),
    DefaultConverter(
        "HH:mm",
        DateTime(0L, DateTimeZone.UTC).plusDays(1).millis,
        setOf(DateTimeFieldType.minuteOfHour(), DateTimeFieldType.hourOfDay()),
        setOf(DateTimeFieldType.secondOfMinute(), DateTimeFieldType.millisOfSecond())
    ),
    DefaultConverter(
        "HH:mm:ss",
        DateTime(0L, DateTimeZone.UTC).plusDays(1).millis,
        setOf(DateTimeFieldType.minuteOfHour(), DateTimeFieldType.hourOfDay(), DateTimeFieldType.secondOfMinute()),
        setOf(DateTimeFieldType.millisOfSecond())
    ),
    DefaultConverter(
        "HH:mm:ss.SSS",
        DateTime(0L, DateTimeZone.UTC).plusDays(1).millis,
        setOf(
            DateTimeFieldType.minuteOfHour(),
            DateTimeFieldType.hourOfDay(),
            DateTimeFieldType.secondOfMinute(),
            DateTimeFieldType.millisOfSecond()
        ),
        emptySet()
    )
)

private val dateConverters = listOf(
    DefaultConverter(
        "dd",
        DateTime(0L, DateTimeZone.UTC).plusMonths(1).millis,
        setOf(DateTimeFieldType.dayOfMonth()),
        emptySet()
    ),
    DefaultConverter(
        "dd/MM",
        DateTime(0L, DateTimeZone.UTC).plusYears(1).millis,
        setOf(DateTimeFieldType.dayOfMonth(), DateTimeFieldType.monthOfYear()),
        emptySet()
    ),
    DefaultConverter(
        "dd/MM/yy",
        DateTime(0L, DateTimeZone.UTC).plusYears(100).millis,
        setOf(DateTimeFieldType.dayOfMonth(), DateTimeFieldType.monthOfYear(), DateTimeFieldType.year()),
        emptySet()
    ),
    DefaultConverter(
        "dd/MM/yyyy",
        DateTime(0L, DateTimeZone.UTC).plusYears(10000).millis,
        setOf(DateTimeFieldType.dayOfMonth(), DateTimeFieldType.monthOfYear(), DateTimeFieldType.year()),
        emptySet()
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

                var dateTime = zeroDateTime
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

                val zeroFields = calculated.commonZeroFields(importantFields)

                CalculatedDateTime(
                    from,
                    dateTime,
                    calculated.maxBy {
                        it.changeDifference
                    } ?.changeDifference ?: throw IllegalStateException(),
                    importantFields,
                    zeroFields
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
    private val importantFields: Set<DateTimeFieldType>,
    private val zeroFields: Set<DateTimeFieldType>? = null
) : Converter {
    private val formatter = DateTimeFormat.forPattern(format)

    override fun convert(from: String): CalculatedDateTime? {
        return try {
            CalculatedDateTime(
                from,
                formatter.parseDateTime(from),
                futureDifference,
                importantFields,
                zeroFields
            )
        } catch (e: Throwable) {
            null
        }
    }
}

private val zeroDateTime = DateTime(0, DateTimeZone.UTC)

data class CalculatedDateTime internal constructor(
    internal val source: String,
    internal val dateTime: DateTime,
    internal val changeDifference: Long,
    internal val importantFields: Set<DateTimeFieldType>,
    internal val zeroFields: Set<DateTimeFieldType>? = null
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

    fun asFor(source: DateTime): DateTime {
        var result: DateTime = source

        zeroFields ?.forEach {
            result = result.withField(
                it,
                zeroDateTime[it]
            )
        }

        importantFields.forEach {
            result = result.withField(
                it,
                dateTime[it]
            )
        }
        return result
    }
    val asNow: DateTime
        get() = asFor(DateTime.now())

    fun asFutureFor(source: DateTime): DateTime {
        return asFor(source).run {
            if (isBefore(source)) {
                source.plus(changeDifference)
            } else {
                this
            }
        }
    }

    val asFuture: DateTime
        get() = asFutureFor(DateTime.now())

    fun asPastFor(source: DateTime): DateTime {
        return asFor(source).run {
            if (isAfter(source)) {
                source.plus(changeDifference)
            } else {
                this
            }
        }
    }

    val asPast: DateTime
        get() = asPastFor(DateTime.now())
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
                    val zeroFields = pair.bothZeroFields(importantFields)

                    val futureDifference = pair.first.changeDifference.let {
                        first ->
                        pair.second.changeDifference.let {
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
                            importantFields,
                            zeroFields
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

fun Pair<CalculatedDateTime, CalculatedDateTime>.bothZeroFields(
    importantFields: Set<DateTimeFieldType> = first.importantFields.plus(second.importantFields)
): Set<DateTimeFieldType>? {
    return first.zeroFields ?.let {
        firstZeroFields ->
        second.zeroFields ?.let {
            secondZeroFields ->
            firstZeroFields
                .asSequence()
                .plus(secondZeroFields)
                .minus(importantFields)
                .toSet()
        } ?: firstZeroFields
    } ?: second.zeroFields
}

fun Iterable<CalculatedDateTime>.commonZeroFields(
    importantFields: Set<DateTimeFieldType> = flatMap {
        it.importantFields
    }.toSet()
): Set<DateTimeFieldType>? {
    return mapNotNull {
        it.zeroFields
    }.flatMap {
        it
    }.asSequence().minus(importantFields).toSet()
}
