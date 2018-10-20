package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.PostsSchedulesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.ForwardMessage
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

class GetSchedulesCommand(
    private val postsSchedulesTable: PostsSchedulesTable,
    private val botWR: WeakReference<TelegramBot>,
    private val sourceChatId: Long
) : Command() {
    override val commandRegex: Regex = Regex("^/getPublishSchedule( (\\d+)|(${periodRegex.pattern}))?$")

    override fun onCommand(updateId: Int, message: Message) {
        val bot = botWR.get() ?: return

        val chatId = message.from() ?.id() ?: message.chat().id()

        launch {
            try {
                bot.executeBlocking(
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

            val posts = message.text().split(" ").let {
                if (it.size == 1) {
                    postsSchedulesTable.registeredPostsTimes()
                } else {
                    it[1].let {
                        filter ->
                        filter.toIntOrNull() ?.let {
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
                        } ?: filter.parseDateTimes().asPairs().flatMap {
                            (from, to) ->
                            postsSchedulesTable.registeredPostsTimes(from.asFuture to to.asFuture)
                        }
                    }
                }
            }

            posts.let {
                if (it.isEmpty()) {
                    bot.executeAsync(
                        SendMessage(
                            chatId,
                            "Nothing to show, schedule queue is empty"
                        )
                    )
                } else {
                    it.forEach {
                        (postId, time) ->
                        bot.executeBlocking(
                            ForwardMessage(
                                chatId,
                                sourceChatId,
                                PostsMessagesTable.getMessagesOfPost(postId).firstOrNull() ?.messageId ?: return@forEach
                            )
                        )
                        bot.executeBlocking(
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