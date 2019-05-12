package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.schedule
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

private typealias PostTimeToJob = Pair<PostIdPostTime, Job>

private val cancelledException = CancellationException()

class Scheduler(
    private val schedulesTable: PostsSchedulesTable,
    private val publisher: Publisher
) {
    private val scope = NewDefaultCoroutineScope(8)
    private var currentPlannedPostTimeAndJob: PostTimeToJob? = null

    private val updateJobChannel = Channel<Unit>(Channel.CONFLATED)

    private val updateJob: Job = scope.launch {
        for (event in updateJobChannel) {
            try {
                val replaceBy: PostTimeToJob? = schedulesTable.nearPost() ?.let { nearEvent ->
                    currentPlannedPostTimeAndJob ?.let { (currentTime, _) ->
                        if (currentTime.second.millis != nearEvent.second.millis) {
                            nearEvent to createScheduledJob(nearEvent)
                        } else {
                            currentPlannedPostTimeAndJob
                        }
                    }
                }
                if (replaceBy != currentPlannedPostTimeAndJob) {
                    currentPlannedPostTimeAndJob ?.second ?.cancel(cancelledException)
                    currentPlannedPostTimeAndJob = replaceBy
                }
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
        updateJobChannel.offer(Unit)
    }

    private fun createScheduledJob(by: PostIdPostTime): Job {
        return scope.schedule(
            by.second.millis
        ) {
            publisher.publishPost(by.first)
            schedulesTable.unregisterPost(by.first)
        }.also {
            it.invokeOnCompletion { cause ->
                if (cause != cancelledException) {
                    updateJobChannel.offer(Unit)
                }
            }
        }
    }
}
