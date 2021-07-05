package dev.inmo.AutoPostTelegramBot.plugins.scheduler.commands

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import dev.inmo.AutoPostTelegramBot.utils.commands.Command
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeAsync
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.ParseMode.MarkdownParseMode
import dev.inmo.tgbotapi.types.UpdateIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import java.lang.ref.WeakReference

private const val disableSchedulePublishCommand = "disableSchedulePublish"

private suspend fun sendHelpForUsage(
    executor: RequestsExecutor,
    chatId: ChatId
) {
    executor.executeAsync(
        SendTextMessage(
            chatId,
            "Usage: `/$disableSchedulePublishCommand`.\n" +
                "Reply post registered message and write command",
            parseMode = MarkdownParseMode
        )
    )
}

class DisableTimerCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val postsTable: PostsBaseInfoTable,
    private val botWR: WeakReference<RequestsExecutor>
) : Command() {
    override val commandRegex: Regex = Regex("^$disableSchedulePublishCommand$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val bot = botWR.get() ?: return
        val replyToMessage = message.replyTo ?:let {
            sendHelpForUsage(
                bot,
                message.chat.id
            )
            return
        }


        try {
            val postId = postsTable.findPost(replyToMessage.messageId)

            postsSchedulesTable.unregisterPost(postId)

            commonLogger.info(
                "Scheduled publish for post $postId disabled"
            )
        } catch (e: Throwable) {
            commonLogger.throwing(
                this::class.java.simpleName,
                "Disable publish by schedule",
                e
            )
        }
    }
}