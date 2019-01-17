package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
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
        while (isActive && !channel.isClosedForReceive) {
            try {
                val received = channel.receive()

                launch {
                    try {
                        if (!by(received)) {
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
        channel.cancel()
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
    return BroadcastChannel<T>(Channel.CONFLATED).also {
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
