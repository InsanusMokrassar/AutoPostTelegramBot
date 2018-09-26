package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.makeLinkToMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.ForwardMessage
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

class MostRated(
    private val botWR: WeakReference<TelegramBot>,
    private val postsLikesTable: PostsLikesTable
): Command() {
    override val commandRegex: Regex = Regex("^/mostRated$")

    override fun onCommand(updateId: Int, message: Message) {
        val bot = botWR.get() ?: return
        val mostRated = postsLikesTable.getMostRated()
        val chatId = message.chat().id()

        message.chat().username() ?.let {
            username ->
            bot.executeAsync(
                SendMessage(
                    chatId,
                    "Most rated posts"
                ).replyMarkup(
                    InlineKeyboardMarkup(
                        *mostRated.mapNotNull {
                                PostsTable.postRegisteredMessage(it)
                        }.mapIndexed {
                            index, id ->
                            InlineKeyboardButton(
                                (index + 1).toString()
                            ).url(
                                makeLinkToMessage(
                                    username,
                                    id
                                )
                            )
                        }.toTable(4)
                    )
                )
            )
        } ?:let {
            launch {
                mostRated.mapNotNull {
                    PostsTable.postRegisteredMessage(it)
                }.forEach {
                    bot.executeBlocking(
                        ForwardMessage(
                            chatId,
                            chatId,
                            it
                        )
                    )
                }
            }
        }
    }
}