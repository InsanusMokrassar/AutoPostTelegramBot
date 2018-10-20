package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CalculatedDateTime
import kotlinx.coroutines.experimental.*
import org.joda.time.DateTime

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
