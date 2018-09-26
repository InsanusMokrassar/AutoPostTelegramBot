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
    return openSubscription().also {
        launch {
            while (isActive && !it.isClosedForReceive) {
                try {
                    val received = it.receive()

                    launch {
                        try {
                            if (!by(received)) {
                                it.cancel()
                            }
                        } catch (e: Throwable) {
                            if (!throwableHandler(e)) {
                                it.cancel()
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    break
                }
            }
            it.cancel()
        }
    }
}

fun <T> BroadcastChannel<T>.subscribe(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    by: suspend (T) -> Unit
): ReceiveChannel<T> {
    return subscribeChecking(throwableHandler) {
        by(it)
        true
    }
}

fun <T> ReceiveChannel<T>.debounce(delayMs: Long): BroadcastChannel<T> {
    val channel = BroadcastChannel<T>(Channel.CONFLATED)
    var lastReceived: Pair<Long, T>? = null
    var job: Job? = null
    launch {
        while (isActive && !isClosedForReceive) {
            val received = receive()

            lastReceived = Pair(System.currentTimeMillis() + delayMs, received)

            job ?:let {
                job = launch {
                    try {
                        var now = System.currentTimeMillis()
                        while (isActive && lastReceived?.first ?: now >= now) {
                            delay((lastReceived ?.first ?: now) - now, TimeUnit.MILLISECONDS)
                            now = System.currentTimeMillis()
                        }

                        lastReceived?.second?.also {
                            channel.send(it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        job = null
                    }
                }
            }
        }
        cancel()
    }
    return channel
}
