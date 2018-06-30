package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.toTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.makeLinkToMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.ForwardMessage
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class MostRated (
    bot: TelegramBot
) : UpdateCallback<Message> {
    private val bot = WeakReference(bot)
    override fun invoke(updateId: Int, update: IObject<Any>, message: Message) {
        val bot = bot.get() ?: return
        val mostRated = PostsLikesTable.getMostRated()
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
            mostRated.mapNotNull {
                PostsTable.postRegisteredMessage(it)
            }.forEach {
                bot.execute(
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