package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import java.util.concurrent.TimeUnit

fun <T> BroadcastChannel<T>.subscribeChecking(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    by: suspend (T) -> Boolean
): ReceiveChannel<T> {
    return openSubscription().apply {
        subscribeChecking(
            throwableHandler,
            by
        )
    }
}

fun <T> BroadcastChannel<T>.subscribe(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    by: suspend (T) -> Unit
): ReceiveChannel<T> {
    return openSubscription().apply {
        subscribeChecking(throwableHandler) {
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
    by: suspend (T) -> Boolean
) {
    val channel = this
    launch {
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
    by: suspend (T) -> Unit
) {
    return subscribeChecking(throwableHandler) {
        by(it)
        true
    }
}


fun <T> BroadcastChannel<T>.debounce(
    delay: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
): BroadcastChannel<T> {
    return openSubscription().debounce(
        delay,
        timeUnit
    )
}


fun <T> ReceiveChannel<T>.debounce(
    delay: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
): BroadcastChannel<T> {
    return BroadcastChannel<T>(Channel.CONFLATED).also {
        outBroadcast ->
        var lastReceived: Job? = null
        subscribe {
            lastReceived ?.cancel()
            lastReceived = launch {
                delay(delay, timeUnit)

                outBroadcast.send(it)
            }
        }
    }
}
