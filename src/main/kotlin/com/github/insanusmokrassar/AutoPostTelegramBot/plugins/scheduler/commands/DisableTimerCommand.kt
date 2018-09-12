package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeSync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

private const val disableSchedulePublishCommand = "/disableSchedulePublish"

private fun sendHelpForUsage(
    bot: TelegramBot,
    chatId: Long
) {
    bot.executeAsync(
        SendMessage(
            chatId,
            "Usage: `$disableSchedulePublishCommand`.\n" +
                "Reply post registered message and write command"
        ).parseMode(
            ParseMode.Markdown
        )
    )
}

class DisableTimerCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val botWR: WeakReference<TelegramBot>
    ) : Command() {
    override val commandRegex: Regex = Regex("^$disableSchedulePublishCommand$")

    override fun onCommand(updateId: Int, message: Message) {
        val bot = botWR.get() ?: return
        val replyToMessage = message.replyToMessage() ?:let {
            sendHelpForUsage(
                bot,
                message.chat().id()
            )
            return
        }


        try {
            val postId = PostsTable.findPost(replyToMessage.messageId())
            val chatId = message.chat().id()

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