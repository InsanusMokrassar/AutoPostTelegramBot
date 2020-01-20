package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.nearDateTime
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

private val TimerTriggerStrategyScope = NewDefaultCoroutineScope(1)

private val timerScheduleCommandRegex = Regex("((getA)|(a))utoPublication(s [\\d]+)?$")
private val numberRegex: Regex = Regex("[\\d]+$")
private class TimerScheduleCommand(
    private val sourceChatId: ChatId,
    private val timesOfTriggering: List<CalculatedDateTime>,
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
            nearDateTime = timesOfTriggering.nearDateTime(nearDateTime + 1000L) // plus one second
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
    private val time: String = "00:00-00:00 01:00"
) : Plugin {
    @Transient
    private var lastTime = DateTime.now()

    private val timesOfTriggering: List<CalculatedDateTime> by lazy {
        time.parseDateTimes()
    }
    val nextTriggerTime: DateTime?
        get() = delay ?.let {
            lastTime = lastTime.plus(it)
            lastTime
        } ?: timesOfTriggering.nearDateTime()

    @Transient
    private lateinit var timerScheduleCommand: TimerScheduleCommand

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val publisher: Publisher = pluginManager.findFirstPlugin() ?: return
        val chooser: Chooser = pluginManager.findFirstPlugin() ?: return

        timerScheduleCommand = TimerScheduleCommand(
            baseConfig.sourceChatId,
            timesOfTriggering,
            chooser,
            executor,
            PostsMessagesTable,
            PostsTable
        )

        TimerTriggerStrategyScope.launch {
            while (isActive) {
                delay(nextTriggerTime ?.millis ?.minus(System.currentTimeMillis()) ?: break)
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
        }
    }

    fun getTriggersInRange(from: DateTime = DateTime.now(), to: DateTime = DateTime(getPlusTwentyFourHours())): List<DateTime> {
        val times = mutableListOf<DateTime>()
        var current = timesOfTriggering.nearDateTime(from) ?: return times
        val toMillis = to.millis
        while (current.millis <= toMillis) {
            times.add(current)
            current = timesOfTriggering.nearDateTime(current + 1000L) ?: return times
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
