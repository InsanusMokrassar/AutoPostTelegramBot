package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.*

class SafeLazy<T: Any> (
    parentScope: CoroutineScope
) : Closeable {
    private val requestsToGetValue = Channel<Continuation<T>>(Channel.UNLIMITED)
    private lateinit var value: T

    private var scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())

    suspend fun set(value: T) {
        try {
            this.value
            error("Variable already was initialized")
        } catch (e: UninitializedPropertyAccessException) {
            this.value = value
            scope.launch {
                delay(1000)
                requestsToGetValue.close()
            }
            scope.launch {
                for (requestToGet in requestsToGetValue) {
                    requestToGet.resume(value)
                }
            }
        }
    }

    suspend fun get(): T {
        return try {
            value
        } catch (e: UninitializedPropertyAccessException) {
            suspendCoroutine { requestsToGetValue.offer(it) }
        }
    }

    override fun close() {
        requestsToGetValue.close()
        scope.cancel()
    }
}
