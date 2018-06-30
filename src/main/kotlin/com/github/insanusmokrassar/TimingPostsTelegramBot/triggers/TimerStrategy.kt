package com.github.insanusmokrassar.TimingPostsTelegramBot.triggers

import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.MostRatedChooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
import kotlinx.coroutines.experimental.*

class TimerStrategy (
    private val delayMs: Long = 60 * 60 *1000,
    private val chooser: Chooser = MostRatedChooser(),
    private val publisher: Publisher
) : Trigger {
    val job = launch(start = CoroutineStart.LAZY) {
        while (isActive) {
            synchronized(PostsTable) {
                synchronized(PostsMessagesTable) {
                    synchronized(PostsLikesTable) {
                        try {
                            chooser.triggerChoose().forEach {
                                publisher.publishPost(it)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            delay(delayMs)
        }
    }

    override fun start() {
        if (job.isActive) {
            return
        }
        job.start()
    }

    override fun stop() {
        job.cancel()
    }
}
