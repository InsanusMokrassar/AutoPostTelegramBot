package dev.inmo.AutoPostTelegramBot.utils.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.supervisorScope

suspend fun <T> Flow<T>.collectWithErrors(
    onError: (suspend (value: T, throwable: Throwable) -> Unit)?,
    action: suspend (value: T) -> Unit
) = collect {
    try {
        supervisorScope {
            action(it)
        }
    } catch (e: Throwable) {
        onError ?.invoke(it, e)
    }
}

suspend inline fun <T> Flow<T>.collectWithErrors(noinline action: suspend (value: T) -> Unit) = collectWithErrors(
    null,
    action
)