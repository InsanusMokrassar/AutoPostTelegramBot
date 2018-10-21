package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CalculatedDateTime
import kotlinx.coroutines.experimental.*
import org.joda.time.DateTime

typealias CalculatedPeriod = Pair<CalculatedDateTime, CalculatedDateTime>

fun <R> Iterable<CalculatedDateTime>.launchNearFuture(block: suspend () -> R): Deferred<R>? {
    var dateTimeToTrigger: DateTime? = null
    forEach {
        val currentAsFuture = it.asFuture
        if (dateTimeToTrigger == null || currentAsFuture.isBefore(dateTimeToTrigger)) {
            dateTimeToTrigger = currentAsFuture
        }
    }

    return dateTimeToTrigger ?.let {
        async {
            delay(it.millis - System.currentTimeMillis())

            block()
        }
    }
}

fun Iterable<CalculatedDateTime>.asPairs(): List<CalculatedPeriod> {
    var first: CalculatedDateTime? = null
    val result = mutableListOf<CalculatedPeriod>()

    forEach {
        first = first ?.let {
            currentFirst ->
            result.add(currentFirst to it)
            null
        } ?: it.let {
            _ ->
            it
        }
    }

    return result
}

fun CalculatedPeriod.isBetween(dateTime: DateTime): Boolean {
    val secondDateTime = second.asFutureFor(dateTime)
    var firstDateTime = first.asPastFor(secondDateTime)
    return dateTime.isAfter(firstDateTime) && dateTime.isBefore(secondDateTime)
}

val CalculatedPeriod.nowIsBetween: Boolean
    get() = isBetween(DateTime.now())
