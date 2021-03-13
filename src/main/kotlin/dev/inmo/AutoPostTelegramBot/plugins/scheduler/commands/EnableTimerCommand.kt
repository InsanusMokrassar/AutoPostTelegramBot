package dev.inmo.AutoPostTelegramBot.plugins.scheduler.commands

import dev.inmo.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.commands.Command
import dev.inmo.AutoPostTelegramBot.utils.parseDateTimes
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeAsync
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.ParseMode.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private const val setPostTimeCommandName = "setPublishTime"

private suspend fun sendHelpForUsage(
    executor: RequestsExecutor,
    chatId: ChatId
) {
    executor.executeAsync(
        SendTextMessage(
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
    private val postsTable: PostsBaseInfoTable,
    private val postsMessagesTable: PostsMessagesInfoTable,
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
            val postId = postsTable.findPost(replyToMessage.messageId)
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
                                chatId,
                                logsChatId,
                                postsMessagesTable.getMessagesOfPost(
                                    postId
                                ).firstOrNull() ?.messageId ?: replyToMessage.messageId
                            )
                        ).messageId
                        executor.executeAsync(
                            SendTextMessage(
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