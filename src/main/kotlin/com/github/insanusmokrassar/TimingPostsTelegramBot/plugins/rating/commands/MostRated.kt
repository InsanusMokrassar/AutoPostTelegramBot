package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands.Command
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands.RateCommand
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.toTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.makeLinkToMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.ForwardMessage
import com.pengrad.telegrambot.request.SendMessage
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