package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.experimental.*
import org.joda.time.DateTime

private typealias PostTimeToJob = Pair<PostIdPostTime, Job>

class Scheduler(
    private val schedulesTable: PostsSchedulesTable,
    private val publisher: Publisher
) {
    private var currentPlannedPostTimeAndJob: PostTimeToJob? = null

    init {
        schedulesTable.postTimeRegisteredChannel.subscribe {
            updateJob(it)
        }
        schedulesTable.postTimeChangedChannel.subscribe {
            updateJob(it)
        }
        schedulesTable.postTimeRemovedChannel.subscribe {
            if (currentPlannedPostTimeAndJob ?.first ?.first == it) {
                schedulesTable.nearPost() ?.also {
                    updateJob(it)
                } ?:also {
                    currentPlannedPostTimeAndJob = null
                }
            }
        }
        schedulesTable.nearPost() ?.also {
            updateJob(it)
        }
    }

    @Synchronized
    private fun updateJob(by: PostIdPostTime)  {
        try {
            val update = currentPlannedPostTimeAndJob ?.first ?.second ?.isAfter(by.second) ?: true
            if (update) {
                currentPlannedPostTimeAndJob ?.second ?.cancel()
                currentPlannedPostTimeAndJob = by to createScheduledJob(by)
            }
        } catch (e: Exception) {
            commonLogger.throwing(
                Scheduler::class.java.simpleName,
                "update job",
                e
            )
        }
    }

    private fun createScheduledJob(by: PostIdPostTime): Job {
        return launch {
            delay(by.second.minus(DateTime.now().millis).millis)
            if (isActive) {
                currentPlannedPostTimeAndJob = null

                publisher.publishPost(by.first)

                schedulesTable.unregisterPost(by.first)

                schedulesTable.nearPost() ?.also {
                    updateJob(it)
                }
            }
        }
    }
}
