package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.schedule
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

private typealias PostTimeToJob = Pair<PostIdPostTime, Job>

class Scheduler(
    private val schedulesTable: PostsSchedulesTable,
    private val publisher: Publisher
) {
    private val scope = NewDefaultCoroutineScope(8)
    private var currentPlannedPostTimeAndJob: PostTimeToJob? = null

    private val updateJobChannel = Channel<Unit>(Channel.UNLIMITED)

    private val updateJob: Job = scope.launch {
        for (event in updateJobChannel) {
            try {
                schedulesTable.nearPost() ?.let { nearEvent ->
                    val scheduleNew = currentPlannedPostTimeAndJob ?.let { (currentTime, currentJob) ->
                        if (currentTime.second.millis != nearEvent.second.millis) {
                            currentJob.cancel()
                            true
                        } else {
                            false
                        }
                    } ?: true
                    if (scheduleNew) {
                        currentPlannedPostTimeAndJob = nearEvent to createScheduledJob(nearEvent)
                    }
                } ?: (currentPlannedPostTimeAndJob ?.second ?.cancel() ?.also {
                    currentPlannedPostTimeAndJob = null
                })
            } catch (e: Exception) {
                commonLogger.throwing(
                    Scheduler::class.java.simpleName,
                    "update job",
                    e
                )
            }
        }
    }

    init {
        schedulesTable.postTimeRegisteredChannel.subscribe {
            updateJobChannel.send(Unit)
        }
        schedulesTable.postTimeChangedChannel.subscribe {
            updateJobChannel.send(Unit)
        }
        schedulesTable.postTimeRemovedChannel.subscribe {
            updateJobChannel.send(Unit)
        }
        schedulesTable.nearPost() ?.also {
            updateJobChannel.offer(Unit)
        }
    }

    private fun createScheduledJob(by: PostIdPostTime): Job {
        return scope.schedule(
            by.second.millis
        ) {
            publisher.publishPost(by.first)
            schedulesTable.unregisterPost(by.first)
            updateJobChannel.send(Unit)
        }
    }
}
