package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.extraSmallBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.TimeUnit

fun <T> ReceiveChannel<T>.subscribeChecking(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = NewDefaultCoroutineScope(1),
    by: suspend (T) -> Boolean
) {
    val channel = this
    scope.launch {
        for (data in channel) {
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
        }
    }
}

fun <T> ReceiveChannel<T>.subscribe(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = NewDefaultCoroutineScope(1),
    by: suspend (T) -> Unit
) {
    return subscribeChecking(throwableHandler, scope) {
        by(it)
        true
    }
}

fun <T> ReceiveChannel<T>.debounce(
    delay: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    scope: CoroutineScope = NewDefaultCoroutineScope(1),
    resultBroadcastChannelCapacity: Int = extraSmallBroadcastCapacity
): BroadcastChannel<T> {
    return BroadcastChannel<T>(resultBroadcastChannelCapacity).also { outBroadcast ->
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

fun <T> BroadcastChannel<T>.subscribeChecking(
    throwableHandler: (Throwable) -> Boolean = {
        it.printStackTrace()
        true
    },
    scope: CoroutineScope = NewDefaultCoroutineScope(1),
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
    scope: CoroutineScope = NewDefaultCoroutineScope(1),
    by: suspend (T) -> Unit
): ReceiveChannel<T> {
    return openSubscription().apply {
        subscribe(throwableHandler, scope) {
            by(it)
        }
    }
}


fun <T> BroadcastChannel<T>.debounce(
    delay: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    scope: CoroutineScope = NewDefaultCoroutineScope(1),
    resultBroadcastChannelCapacity: Int = extraSmallBroadcastCapacity
): BroadcastChannel<T> {
    return openSubscription().debounce(
        delay,
        timeUnit,
        scope,
        resultBroadcastChannelCapacity
    )
}
