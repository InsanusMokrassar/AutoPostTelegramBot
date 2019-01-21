package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.asPairs
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private val GetSchedulesCommandScope = NewDefaultCoroutineScope(1)

class GetSchedulesCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val executorWR: WeakReference<RequestsExecutor>,
    private val sourceChatId: ChatIdentifier
) : Command() {
    override val commandRegex: Regex = Regex("^/getPublishSchedule(( ${periodRegex.pattern})|( \\d+))?$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val executor = executorWR.get() ?: return

        val chatId = (message as? FromUserMessage) ?.user?.id ?: message.chat.id
        val content = message.content as? TextContent ?: return

        GetSchedulesCommandScope.launch {
            try {
                executor.execute(
                    SendMessage(
                        chatId,
                        "Let me prepare data"
                    )
                )
            } catch (e: Exception) {
                commonLogger.throwing(
                    this::class.java.simpleName,
                    "sending message \"Prepare data\" to user",
                    e
                )
                return@launch
            }

            val arg: String? = content.text.indexOf(" ").let {
                if (it > -1) {
                    content.text.substring(it + 1)
                } else {
                    null
                }
            }

            val posts = arg ?.let { _ ->
                arg.toIntOrNull() ?.let {
                        count ->
                    postsSchedulesTable.registeredPostsTimes().sortedBy {
                        it.second
                    }.let {
                        if (it.size <= count) {
                            it
                        } else {
                            it.subList(0, count)
                        }
                    }
                } ?: arg.parseDateTimes().asPairs().flatMap {
                        (from, to) ->
                    val asFutureFrom = from.asFuture
                    val asFutureTo = to.asFutureFor(asFutureFrom)
                    postsSchedulesTable.registeredPostsTimes(asFutureFrom to asFutureTo)
                }
            } ?: postsSchedulesTable.registeredPostsTimes()

            posts.let {
                if (it.isEmpty()) {
                    executor.executeAsync(
                        SendMessage(
                            chatId,
                            "Nothing to show, schedule queue is empty"
                        )
                    )
                } else {
                    it.forEach {
                            (postId, time) ->
                        executor.execute(
                            ForwardMessage(
                                sourceChatId,
                                chatId,
                                PostsMessagesTable.getMessagesOfPost(postId).firstOrNull() ?.messageId ?: return@forEach
                            )
                        )
                        executor.execute(
                            SendMessage(
                                chatId,
                                "Post time: $time"
                            )
                        )
                        delay(1000)
                    }
                }
            }
        }
    }
}