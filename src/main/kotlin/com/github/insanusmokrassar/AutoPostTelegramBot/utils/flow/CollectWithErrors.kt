package com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

suspend fun <T> Flow<T>.collectWithErrors(
    onError: suspend (value: T, throwable: Throwable) -> Unit = { _, _ -> },
    action: suspend (value: T) -> Unit
) = collect {
    try {
        action(it)
    } catch (e: Throwable) {
        onError(it, e)
    }
}
