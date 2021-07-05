package dev.inmo.AutoPostTelegramBot.plugins.scheduler.commands

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import dev.inmo.AutoPostTelegramBot.utils.*
import dev.inmo.AutoPostTelegramBot.utils.commands.Command
import dev.inmo.AutoPostTelegramBot.utils.extensions.asPairs
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeAsync
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.UpdateIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private val GetSchedulesCommandScope = NewDefaultCoroutineScope(1)

class GetSchedulesCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val postsMessagesTable: PostsMessagesInfoTable,
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
                    SendTextMessage(
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

            posts.sortedBy {
                it.second
            }.let {
                if (it.isEmpty()) {
                    executor.executeAsync(
                        SendTextMessage(
                            chatId,
                            "Nothing to show, schedule queue is empty"
                        )
                    )
                } else {
                    it.forEach { (postId, time) ->
                        executor.execute(
                            ForwardMessage(
                                sourceChatId,
                                chatId,
                                postsMessagesTable.getMessagesOfPost(postId).firstOrNull() ?.messageId ?: return@forEach
                            )
                        )
                        executor.execute(
                            SendTextMessage(
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