package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.SelectClause2
import kotlin.coroutines.CoroutineContext

class UnlimitedBroadcastChannel<T>(
    coroutineContext: CoroutineContext = Dispatchers.Default
) : BroadcastChannel<T> {
    private val scope = CoroutineScope(coroutineContext)
    private val subscriptions = mutableListOf<SendChannel<T>>()

    private val sendChannel = Channel<T>(Channel.UNLIMITED)
    init {
        scope.launch {
            for (msg in sendChannel) {
                ArrayList(subscriptions).forEach {
                    try {
                        it.send(msg)
                    } catch (e: Exception) {
                        if (e !is ClosedSendChannelException) {
                            //TODO:: PANIC
                        }
                        subscriptions.remove(it)
                    }
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    override val isClosedForSend: Boolean
        get() = sendChannel.isClosedForSend
    @ExperimentalCoroutinesApi
    override val isFull: Boolean
        get() = sendChannel.isFull
    override val onSend: SelectClause2<T, SendChannel<T>>
        get() = sendChannel.onSend

    override fun close(cause: Throwable?): Boolean = if (isClosedForSend) {
        false
    } else {
        scope.cancel()
        sendChannel.close(cause)
        subscriptions.forEach {
            it.close(cause)
        }
        subscriptions.clear()
        true
    }

    @ExperimentalCoroutinesApi
    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) = sendChannel.invokeOnClose(handler)

    override fun offer(element: T): Boolean = sendChannel.offer(element)

    override suspend fun send(element: T) = sendChannel.send(element)

    override fun cancel(cause: Throwable?): Boolean = close(cause)

    override fun openSubscription(): ReceiveChannel<T> = Channel<T>(Channel.UNLIMITED).also {
        if (isClosedForSend) {
            it.close()
        } else {
            subscriptions.add(it)
        }
    }
}
