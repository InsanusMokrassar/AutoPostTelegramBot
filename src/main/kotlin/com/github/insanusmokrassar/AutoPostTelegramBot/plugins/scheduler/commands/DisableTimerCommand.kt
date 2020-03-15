package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import java.lang.ref.WeakReference

private const val disableSchedulePublishCommand = "disableSchedulePublish"

private fun sendHelpForUsage(
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