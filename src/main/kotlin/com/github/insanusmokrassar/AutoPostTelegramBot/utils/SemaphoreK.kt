package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.launch
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class SemaphoreK(
    private val maxAvailable: Int = 1
) {
    private val sync = Object()
    var available: Int = maxAvailable
        private set(value) {
            synchronized(sync) {
                field = value
                if (field > maxAvailable) {
                    field = maxAvailable
                }
            }
        }
        get() {
            return synchronized(sync) {
                field
            }
        }

    private val suspenders = Array<Queue<Continuation<Unit>>>(maxAvailable) { ConcurrentLinkedQueue<Continuation<Unit>>() }
    private var workerContinuation: Continuation<Unit>? = null

    init {
        if (maxAvailable < 1) {
            throw IllegalArgumentException("Max available can't be less than 1, but was $maxAvailable")
        }
        launch {
            while (isActive) {
                synchronized(sync) {
                    var currentLockCount = available
                    while (available > 0 && currentLockCount > 0) {
                        val queue = suspenders[currentLockCount - 1]
                        if (queue.isNotEmpty()) {

                            var current: Continuation<Unit>

                            do {
                                current = queue.poll()
                            } while (!current.context.isActive)

                            current.resume(Unit)
                            available -= currentLockCount

                            currentLockCount = available
                        }
                        currentLockCount--
                    }
                }

                suspendCoroutine<Unit> {
                    synchronized(sync) {
                        workerContinuation = it
                    }
                }
            }
        }
    }

    private fun notifyWorker() {
        synchronized(sync) {
            workerContinuation ?.resume(Unit)
            workerContinuation = null
        }
    }

    suspend fun lock(count: Int = 1) {

        if (count < 1) {
            throw IllegalArgumentException("Lock count can't be less than 1, but was $count")
        }

        suspendCoroutine<Unit> {
            suspenders[count - 1].offer(it)
            notifyWorker()
        }
    }

    fun free(count: Int = 1) {
        synchronized(sync) {
            available += count
            notifyWorker()
        }
    }
}
