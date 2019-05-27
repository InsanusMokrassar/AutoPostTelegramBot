package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.schedule
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow

private typealias PostTimeToJob = Pair<PostIdPostTime, Job>

private val cancelledException = CancellationException()

private typealias EventLambda = suspend () -> Unit

class Scheduler(
    private val schedulesTable: PostsSchedulesTable,
    private val publisher: Publisher
) {
    private val scope = NewDefaultCoroutineScope(8)
    private var currentPlannedPostTimeAndJob: PostTimeToJob? = null

    private val updateJobChannel = Channel<EventLambda>(Channel.CONFLATED)

    private val updateLambda: EventLambda = {
        val replaceBy: PostTimeToJob? = schedulesTable.nearPost() ?.let { nearEvent ->
            val current = currentPlannedPostTimeAndJob
            if (current == null || current.first.second.millis != nearEvent.second.millis) {
                nearEvent to createScheduledJob(nearEvent)
            } else {
                current
            }
        }
        if (replaceBy != currentPlannedPostTimeAndJob) {
            currentPlannedPostTimeAndJob ?.second ?.cancel(cancelledException)
            currentPlannedPostTimeAndJob = replaceBy
        }
    }

    private val updateJob: Job = scope.launch {
        for (event in updateJobChannel) {
            try {
                event()
            } catch (e: Exception) {
                commonLogger.throwing(
                    Scheduler::class.java.simpleName,
                    "Update job event handling",
                    e
                )
            }
        }
    }

    init {
        val scope = NewDefaultCoroutineScope(1)

        scope.apply {
            launch {
                updateJobChannel.send(updateLambda)
            }
            launch {
                schedulesTable.postTimeRegisteredChannel.asFlow().collectWithErrors {
                    updateJobChannel.send(updateLambda)
                }
            }
            launch {
                schedulesTable.postTimeChangedChannel.asFlow().collectWithErrors {
                    updateJobChannel.send(updateLambda)
                }
            }
            launch {
                schedulesTable.postTimeRemovedChannel.asFlow().collectWithErrors {
                    updateJobChannel.send(updateLambda)
                }
            }
        }
    }

    private fun createScheduledJob(by: PostIdPostTime): Job {
        return scope.schedule(
            by.second.millis
        ) {
            updateJobChannel.send {
                publisher.publishPost(by.first)
                schedulesTable.unregisterPost(by.first)
            }
        }
    }
}
