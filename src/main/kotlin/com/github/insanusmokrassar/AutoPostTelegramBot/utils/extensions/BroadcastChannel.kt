package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch

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
