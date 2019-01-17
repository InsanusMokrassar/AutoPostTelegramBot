package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.parseDateTimes
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.*
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private const val setPostTimeCommandName = "setPublishTime"

private fun sendHelpForUsage(
    executor: RequestsExecutor,
    chatId: ChatId
) {
    executor.executeAsync(
        SendMessage(
            chatId,
            "Usage: `/$setPostTimeCommandName [time format]`.\n" +
                "Reply post registered message and write command + time in correct format",
            parseMode = MarkdownParseMode
        )
    )
}

private val EnableTimerCommandScope = NewDefaultCoroutineScope(1)

class EnableTimerCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val executorWR: WeakReference<RequestsExecutor>,
    private val logsChatId: ChatIdentifier
) : Command() {
    override val commandRegex: Regex = Regex("^/$setPostTimeCommandName.*")
    private val removeCommand: Regex = Regex("^/$setPostTimeCommandName ?")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val content = message.content as? TextContent ?: return
        val executor = executorWR.get() ?: return
        val replyToMessage = message.replyTo ?:let {
            sendHelpForUsage(
                executor,
                message.chat.id
            )
            return
        }
        try {
            val postId = PostsTable.findPost(replyToMessage.messageId)
            val chatId = message.chat.id

            val preparsedText = content.text.let {
                it.replaceFirst(removeCommand, "").also {
                    if (it.isEmpty()) {
                        sendHelpForUsage(
                            executor,
                            message.chat.id
                        )
                        return
                    }
                }
            }

            preparsedText.parseDateTimes().asSequence().map {
                it.asFuture
            }.min() ?.also {
                    parsed ->
                parsed.also {
                    postsSchedulesTable.registerPostTime(postId, parsed)

                    EnableTimerCommandScope.launch {
                        val messageId = executor.execute(
                            ForwardMessage(
                                logsChatId,
                                chatId,
                                PostsMessagesTable.getMessagesOfPost(
                                    postId
                                ).firstOrNull() ?.messageId ?: replyToMessage.messageId
                            )
                        ).messageId
                        executor.executeAsync(
                            SendMessage(
                                logsChatId,
                                "Parsed time: $parsed\n" +
                                    "Post saved with timer",
                                parseMode = MarkdownParseMode,
                                replyToMessageId = messageId
                            )
                        )
                    }
                }
            }
        } catch (e: NoRowFoundException) {
            sendHelpForUsage(
                executor,
                message.chat.id
            )
        } finally {
            executor.executeAsync(
                DeleteMessage(
                    message.chat.id,
                    message.messageId
                )
            )
        }
    }
}