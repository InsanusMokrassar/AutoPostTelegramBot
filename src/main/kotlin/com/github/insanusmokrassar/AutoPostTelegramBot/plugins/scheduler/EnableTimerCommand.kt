package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeSync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import org.joda.time.DateTime
import java.lang.ref.WeakReference

private val availableConvertersText = converters.joinToString("\n") {
    "${it.formatPattern} (${it.timeZoneId})"
}

private const val setPostTimeCommandName = "setPublishTime"

private fun sendHelpForUsage(
    bot: TelegramBot,
    chatId: Long
) {
    bot.executeAsync(
        SendMessage(
            chatId,
            "Usage: `/$setPostTimeCommandName [time format]`.\n" +
                "Reply post registered message and write command + time in correct format" +
                "\nAvailable time formats:" +
                "\n$availableConvertersText"
        ).parseMode(
            ParseMode.Markdown
        )
    )
}

class EnableTimerCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val botWR: WeakReference<TelegramBot>,
    private val logsChatId: Long
) : Command() {
    override val commandRegex: Regex = Regex("^/$setPostTimeCommandName.*")
    private val removeCommand: Regex = Regex("^/$setPostTimeCommandName ?")

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

            val preparsedText = message.text().let {
                it.replaceFirst(removeCommand, "").also {
                    if (it.isEmpty()) {
                        sendHelpForUsage(
                            bot,
                            message.chat().id()
                        )
                        return
                    }
                }
            }

            converters.map {
                it to it.tryConvert(preparsedText)
            }.firstOrNull {
                it.second != null
            } ?.also {
                (converter, parsed) ->
                parsed ?: return@also
                parsed.also {
                    postsSchedulesTable.registerPostTime(postId, parsed)

                    bot.executeSync(
                        ForwardMessage(
                            logsChatId,
                            chatId,
                            PostsMessagesTable.getMessagesOfPost(
                                postId
                            ).firstOrNull() ?.messageId ?: replyToMessage.messageId()
                        )
                    )
                    bot.executeAsync(
                        SendMessage(
                            logsChatId,
                            "Chosen format: ${converter.formatPattern} (${converter.timeZoneId})\n" +
                                "Parsed time: $parsed\n" +
                                "Post saved with timer"
                        ).parseMode(
                            ParseMode.Markdown
                        )
                    )
                }
            }
        } catch (e: NoRowFoundException) {
            sendHelpForUsage(
                bot,
                message.chat().id()
            )
        } finally {
            bot.executeAsync(
                DeleteMessage(
                    message.chat().id(),
                    message.messageId()
                )
            )
        }
    }
}