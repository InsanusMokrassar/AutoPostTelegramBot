package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CalculatedDateTime
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import kotlinx.coroutines.*
import org.joda.time.DateTime

typealias CalculatedPeriod = Pair<CalculatedDateTime, CalculatedDateTime>

fun Iterable<CalculatedDateTime>.nearDateTime(nearTo: DateTime = DateTime.now()): DateTime? {
    var found: DateTime? = null
    forEach {
        val currentAsFuture = it.asFutureFor(nearTo)
        if (found == null || currentAsFuture.isBefore(found)) {
            found = currentAsFuture
        }
    }

    return found
}

private val futureTasksScope = NewDefaultCoroutineScope()

fun <R> Iterable<CalculatedDateTime>.executeNearFuture(block: suspend () -> R): Deferred<R>? {
    val dateTimeToTrigger: DateTime? = nearDateTime()

    return dateTimeToTrigger ?.let {
        futureTasksScope.async {
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
