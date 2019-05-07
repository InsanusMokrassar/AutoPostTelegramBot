package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import kotlinx.coroutines.*

suspend fun withDelay(
    delayMilliseconds: Long,
    block: suspend () -> Unit
) {
    delay(delayMilliseconds)
    block()
}

fun CoroutineScope.schedule(
    scheduleTimeMillis: Long,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    withDelay(scheduleTimeMillis - System.currentTimeMillis()) {
        if (isActive) {
            block()
        }
    }
}
