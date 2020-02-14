package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.mediumBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.nearDateTime
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

private val TimerTriggerStrategyScope = CoroutineScope(Dispatchers.Default)

private val timerScheduleCommandRegex = Regex("((getA)|(a))utoPublication(s [\\d]+)?$")
private val numberRegex: Regex = Regex("[\\d]+$")
private class TimerScheduleCommand(
    private val sourceChatId: ChatId,
    private val timeChooserCallback: (after: DateTime) -> DateTime?,
    private val chooser: Chooser,
    private val executor: RequestsExecutor,
    private val postsMessagesTable: PostsMessagesTable,
    private val postsTable: PostsTable
) : Command() {
    override val commandRegex: Regex = timerScheduleCommandRegex

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val content = message.content as? TextContent ?: return
        val count = numberRegex.find(content.text) ?.value ?.toIntOrNull() ?: 1
        var nearDateTime = DateTime.now()
        val chosen = mutableMapOf<DateTime, List<PostId>>()
        for (i in 0 until count) {
            nearDateTime = timeChooserCallback(nearDateTime + 1000L) // plus one second
            chosen[nearDateTime] = chooser.triggerChoose(nearDateTime, chosen.values.flatten()).toList()
        }
        commonLogger.info("Was requested auto publishing count: $count; Was chosen to show: ${chosen.values.sumBy { it.size }}")
        chosen.asSequence().sortedBy {
            it.key
        }.forEach { (dateTime, posts) ->
            executor.execute(
                SendMessage(
                    message.chat.id,
                    "Must be posted at `$dateTime`:",
                    MarkdownParseMode
                )
            )
            posts.sorted().forEach {
                postsMessagesTable.getMessagesOfPost(it).firstOrNull() ?.also { firstMessage ->
                    // TODO:: Add handling of case when message was removed
                    executor.executeUnsafe(
                        ForwardMessage(
                            sourceChatId,
                            message.chat.id,
                            firstMessage.messageId
                        )
                    )
                } ?: it.let {
                    postsTable.removePost(it)
                }
                delay(500L)
            }
            delay(1000L)
        }
        executor.execute(
            SendMessage(
                message.chat.id,
                "End of requested auto publishing messages"
            )
        )
    }
}

private const val twentyFourHours = 1000L * 60 * 60 * 24L
private fun getPlusTwentyFourHours() = System.currentTimeMillis() + twentyFourHours

@Serializable
class TimerTriggerStrategy (
    private val delay: Long? = null,
    private val time: String = "00:00-00:00 01:00",
    private val substitutedByScheduler: Boolean = false
) : Plugin {
    @Transient
    private val publicationTimesPossiblyChangedBroadcastChannel = BroadcastChannel<DateTime>(
        mediumBroadcastCapacity
    )
    @Transient
    val triggerTimesAffectedFlow = publicationTimesPossiblyChangedBroadcastChannel.asFlow()

    @Transient
    private var lastTime = DateTime.now()

    private val timesOfTriggering: List<CalculatedDateTime> by lazy {
        time.parseDateTimes()
    }
    fun getNextTime(after: DateTime = DateTime.now()): DateTime? {
        val near = timesOfTriggering.nearDateTime(after) ?: return null
        schedulerPluginToCheckCollision ?.let {
            val afterNear = near + 1000L
            val thereIsScheduled = it.timerSchedulesTable.registeredPostsTimes(
                near - 1000L to afterNear
            ).any { (_, dateTime) ->
                dateTime == near
            }
            if (thereIsScheduled) {
                return getNextTime(afterNear)
            }
        }
        return near
    }
    val nextTriggerTime: DateTime?
        get() = delay ?.let {
            lastTime = lastTime.plus(it)
            lastTime
        } ?: getNextTime()

    @Transient
    private lateinit var timerScheduleCommand: TimerScheduleCommand
    private var schedulerPluginToCheckCollision: SchedulerPlugin? = null

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val publisher: Publisher = pluginManager.findFirstPlugin() ?: return
        val chooser: Chooser = pluginManager.findFirstPlugin() ?: return

        if (substitutedByScheduler) {
            pluginManager.findFirstPlugin<SchedulerPlugin>() ?.let {
                schedulerPluginToCheckCollision = it
                TimerTriggerStrategyScope.launch {
                    it.timerSchedulesTable.postTimeRegisteredFlow.collectWithErrors { (_, dateTime) ->
                        publicationTimesPossiblyChangedBroadcastChannel.send(dateTime)
                    }
                }
                TimerTriggerStrategyScope.launch {
                    it.timerSchedulesTable.postTimeChangedFlow.collectWithErrors { (_, dateTime) ->
                        publicationTimesPossiblyChangedBroadcastChannel.send(dateTime)
                    }
                }
            }
        }

        timerScheduleCommand = TimerScheduleCommand(
            baseConfig.sourceChatId,
            ::getNextTime,
            chooser,
            executor,
            PostsMessagesTable,
            PostsTable
        )

        TimerTriggerStrategyScope.launch {
            suspend fun trigger() {
                launch {
                    try {
                        chooser.triggerChoose().forEach {
                            publisher.publishPost(it)
                        }
                    } catch (e: Exception) {
                        this@TimerTriggerStrategy.sendToLogger(e, "Try to publish with triggering")
                    }
                }
            }
            triggerLoop@while (isActive) {
                val currentNow = DateTime.now()
                val nextTime = getNextTime(currentNow) ?: break
                delay(nextTime.millis - currentNow.millis)
                val nextAfterDelay = getNextTime(currentNow)
                when {
                    nextAfterDelay == nextTime -> trigger()
                    nextAfterDelay == null || nextTime < getNextTime(currentNow) -> continue@triggerLoop
                    else -> {
                        var currentNext = nextAfterDelay
                        do {
                            currentNext = getNextTime(currentNext ?: break)
                            if (currentNext == nextTime) {
                                trigger()
                                continue@triggerLoop
                            }
                        } while (currentNext != null && currentNext < nextTime)
                    }
                }
            }
        }
    }

    fun getTriggersInRange(from: DateTime = DateTime.now(), to: DateTime = DateTime(getPlusTwentyFourHours())): List<DateTime> {
        val times = mutableListOf<DateTime>()
        var current = getNextTime(from) ?: return times
        val toMillis = to.millis
        while (current.millis <= toMillis) {
            times.add(current)
            current = getNextTime(current + 1000L) ?: return times
        }
        return times
    }
}

suspend fun TimerTriggerStrategy.getPostsInRange(
    chooser: Chooser,
    from: DateTime = DateTime.now(),
    to: DateTime = DateTime(getPlusTwentyFourHours())
): Map<DateTime, List<PostId>> {
    val triggerTimes = getTriggersInRange(from, to)
    val chosenPosts = mutableListOf<PostId>()
    val timeToPostsMap = mutableMapOf<DateTime, List<PostId>>()
    triggerTimes.forEach { currentDateTime ->
        val toPostAtTheTime = chooser.triggerChoose(currentDateTime, chosenPosts)
        chosenPosts.addAll(toPostAtTheTime)
        timeToPostsMap[currentDateTime] = toPostAtTheTime.toList()
    }
    return timeToPostsMap
}
