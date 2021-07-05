package dev.inmo.AutoPostTelegramBot.plugins.scheduler

import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.plugins.publishers.Publisher
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.extensions.schedule
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

private typealias PostTimeToJob = Pair<PostIdPostTime, Job>

private val cancelledException = CancellationException()

private typealias EventLambda = suspend () -> Unit

class Scheduler(
    private val schedulesTable: PostsSchedulesTable,
    private val publisher: Publisher
) {
    private val scope = NewDefaultCoroutineScope(8)
    private var currentPlannedPostTimeAndJob: PostTimeToJob? = null

    private val updateJobChannel = Channel<EventLambda>(1)

    private val updateLambda: EventLambda = {
        val replaceBy: PostTimeToJob? = schedulesTable.nearPost() ?.let { nearEvent ->
            val current = currentPlannedPostTimeAndJob
            if (current == null || current.first.first != nearEvent.first) {
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
                schedulesTable.postTimeRegisteredFlow.collectWithErrors {
                    updateJobChannel.send(updateLambda)
                }
            }
            launch {
                schedulesTable.postTimeChangedFlow.collectWithErrors {
                    updateJobChannel.send(updateLambda)
                }
            }
            launch {
                schedulesTable.postTimeRemovedFlow.collectWithErrors {
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
