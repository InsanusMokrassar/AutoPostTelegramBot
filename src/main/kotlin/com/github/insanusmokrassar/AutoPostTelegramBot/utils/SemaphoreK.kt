package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class SemaphoreK(
    private val maxAvailable: Int = 1
) {
    private sealed class SemaphoreAction {
        class IncAction(val count: Int = 1) : SemaphoreAction()
        class BlockAction(val count: Int = 1, val continuation: Continuation<Unit>) : SemaphoreAction()
    }

    private val semaphoreActor: SendChannel<SemaphoreAction>

    init {
        semaphoreActor = actor {
            var free = maxAvailable

            val continuations = Array<Queue<Continuation<Unit>>>(maxAvailable) { ArrayDeque() }

            fun checkContinuations() {
                for (i in (free - 1) downTo 0) {
                    continuations[i].poll() ?.let {
                        if (it.context.isActive) {
                            it.resume(Unit)
                            free -= (i + 1)
                            checkContinuations()
                        } else {
                            null
                        }
                    } ?: continue
                    break
                }
            }

            for (msg in channel) {
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
            launch {
                semaphoreActor.send(
                    SemaphoreAction.BlockAction(count, it)
                )
            }
        }
    }

    fun free(count: Int = 1) {
        launch {
            semaphoreActor.send(
                SemaphoreAction.IncAction(count)
            )
        }
    }
}
