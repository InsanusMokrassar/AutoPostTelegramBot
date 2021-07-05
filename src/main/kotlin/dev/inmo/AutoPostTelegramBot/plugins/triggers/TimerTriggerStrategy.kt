package dev.inmo.AutoPostTelegramBot.plugins.triggers

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.*
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import dev.inmo.AutoPostTelegramBot.mediumBroadcastCapacity
import dev.inmo.AutoPostTelegramBot.plugins.publishers.Publisher
import dev.inmo.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import dev.inmo.AutoPostTelegramBot.utils.CalculatedDateTime
import dev.inmo.AutoPostTelegramBot.utils.commands.Command
import dev.inmo.AutoPostTelegramBot.utils.extensions.nearDateTime
import dev.inmo.AutoPostTelegramBot.utils.extensions.sendToLogger
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.AutoPostTelegramBot.utils.parseDateTimes
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.ParseMode.MarkdownParseMode
import dev.inmo.tgbotapi.types.UpdateIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.DateTime
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
    private val postsMessagesTable: PostsMessagesInfoTable,
    private val postsTable: PostsBaseInfoTable
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
                SendTextMessage(
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
            SendTextMessage(
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
    private val times: List<String>? = null,
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
        (times ?: listOf(time)).flatMap { it.parseDateTimes() }
    }
    fun getNextTime(after: DateTime = DateTime.now()): DateTime? {
        val near = timesOfTriggering.nearDateTime(after) ?: return null
        schedulerPluginToCheckCollision ?.let {
            val afterNear = near + 1000L
            val thereIsScheduled = runBlocking {
                it.getSchedulesTable().registeredPostsTimes(
                    near - 1000L to afterNear
                ).any { (_, dateTime) ->
                    dateTime == near
                }
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
                    it.getSchedulesTable().postTimeRegisteredFlow.collectWithErrors { (_, dateTime) ->
                        publicationTimesPossiblyChangedBroadcastChannel.send(dateTime)
                    }
                }
                TimerTriggerStrategyScope.launch {
                    it.getSchedulesTable().postTimeChangedFlow.collectWithErrors { (_, dateTime) ->
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
            baseConfig.postsMessagesTable,
            baseConfig.postsTable
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
