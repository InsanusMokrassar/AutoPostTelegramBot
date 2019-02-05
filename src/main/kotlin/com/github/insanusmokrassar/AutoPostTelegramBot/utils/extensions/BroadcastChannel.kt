package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.TimeUnit

fun <T> BroadcastChannel<T>.subscribeChecking(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = GlobalScope,
    by: suspend (T) -> Boolean
): ReceiveChannel<T> {
    return openSubscription().apply {
        subscribeChecking(
            throwableHandler,
            scope,
            by
        )
    }
}

fun <T> BroadcastChannel<T>.subscribe(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = GlobalScope,
    by: suspend (T) -> Unit
): ReceiveChannel<T> {
    return openSubscription().apply {
        subscribeChecking(throwableHandler, scope) {
            by(it)
            true
        }
    }
}

fun <T> ReceiveChannel<T>.subscribeChecking(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = GlobalScope,
    by: suspend (T) -> Boolean
) {
    val channel = this
    scope.launch {
        for (data in channel) {
            try {
                launch {
                    try {
                        if (!by(data)) {
                            channel.cancel()
                        }
                    } catch (e: Throwable) {
                        if (!throwableHandler(e)) {
                            channel.cancel()
                        }
                    }
                }
            } catch (e: CancellationException) {
                break
            }
        }
    }
}

fun <T> ReceiveChannel<T>.subscribe(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = GlobalScope,
    by: suspend (T) -> Unit
) {
    return subscribeChecking(throwableHandler, scope) {
        by(it)
        true
    }
}


fun <T> BroadcastChannel<T>.debounce(
    delay: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    scope: CoroutineScope = GlobalScope
): BroadcastChannel<T> {
    return openSubscription().debounce(
        delay,
        timeUnit,
        scope
    )
}


fun <T> ReceiveChannel<T>.debounce(
    delay: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    scope: CoroutineScope = GlobalScope
): BroadcastChannel<T> {
    return BroadcastChannel<T>(1).also {
        outBroadcast ->
        var lastReceived: Job? = null
        subscribe(scope = scope) {
            lastReceived ?.cancel()
            lastReceived = scope.launch {
                delay(timeUnit.toMillis(delay))

                outBroadcast.send(it)
            }
        }
    }
}
