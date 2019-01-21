package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.*

class SemaphoreK(
    private val maxAvailable: Int = 1,
    initial: Int = maxAvailable
) {
    private sealed class SemaphoreAction {
        class IncAction(val count: Int = 1) : SemaphoreAction()
        class BlockAction(val count: Int = 1, val continuation: Continuation<Unit>) : SemaphoreAction()
    }

    private val scope = NewDefaultCoroutineScope()
    private val semaphoreChannel = Channel<SemaphoreAction>(Channel.UNLIMITED)
    private var free = if (initial > maxAvailable) {
        maxAvailable
    } else {
        if (initial < 0) {
            0
        } else {
            initial
        }
    }

    private val continuations = Array<Queue<Continuation<Unit>>>(maxAvailable) { ArrayDeque() }

    private suspend fun checkContinuations() {
        for (i in (free - 1) downTo 0) {
            continuations[i].poll() ?.let {
                if (it.context.isActive) {
                    try {
                        it.resume(Unit)
                        free -= (i + 1)
                        checkContinuations()
                    } catch (e: Throwable) {
                        commonLogger.warning("Can't continue some of coroutines: $it")
                        null
                    }
                } else {
                    null
                }
            } ?: continue
            break
        }
    }

    init {
        scope.launch {
            for (msg in semaphoreChannel) {
                when (msg) {
                    is SemaphoreAction.IncAction -> {
                        free += msg.count
                        if (free > maxAvailable) {
                            free = maxAvailable
                        }
                        checkContinuations()
                    }
                    is SemaphoreAction.BlockAction -> {
                        continuations[msg.count - 1].offer(msg.continuation)
                        checkContinuations()
                    }
                }
            }
        }
    }

    suspend fun lock(count: Int = 1) {
        if (count < 1) {
            throw IllegalArgumentException("Lock count can't be less than 1, but was $count")
        }

        suspendCoroutine<Unit> {
            scope.launch {
                semaphoreChannel.send(
                    SemaphoreAction.BlockAction(count, it)
                )
            }
        }
    }

    fun free(count: Int = 1) {
        scope.launch {
            semaphoreChannel.send(
                SemaphoreAction.IncAction(count)
            )
        }
    }
}
