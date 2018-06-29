package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.toTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.makeLinkToMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.EditMessageReplyMarkup
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

const val like = "\uD83D\uDC4D"
const val dislike = "\uD83D\uDC4E"

class FixPost(
    bot: TelegramBot
) : UpdateCallback<Message> {
    private val bot = WeakReference(bot)
    override fun invoke(updateId: Int, update: IObject<Any>, message: Message) {
        val postId = PostsTable.allocatePost()
        PostTransactionTable.saveWithPostId(postId)

        val buttons = mutableListOf<MutableList<InlineKeyboardButton>>(
            mutableListOf(
                InlineKeyboardButton(
                    "Delete"
                ).callbackData(
                    "Delete"
                )
            ),
            mutableListOf(
                InlineKeyboardButton(
                    like
                ).callbackData(
                    like
                ),
                InlineKeyboardButton(
                    dislike
                ).callbackData(
                    dislike
                )
            )
        )

        val messagesIds = PostsMessagesTable.getMessagesOfPost(postId)

        message.chat().username() ?.let {
            chatUsername ->
            messagesIds.map {
                makeLinkToMessage(
                    chatUsername,
                    it
                )
            }.mapIndexed {
                index, s ->
                InlineKeyboardButton(
                    "M ${index + 1}"
                ).url(
                    s
                )
            }.toTable(4).let {
                buttons.addAll(
                    it.map {
                        it.toMutableList()
                    }
                )
            }
        }
        val markup = InlineKeyboardMarkup(
            *buttons.map {
                it.toTypedArray()
            }.toTypedArray()
        )
        bot.get() ?.execute(
            if (messagesIds.size > 1) {
                SendMessage(
                    message.chat().id(),
                    "Post registered"
                ).parseMode(
                    ParseMode.Markdown
                ).replyMarkup(
                    markup
                )
            } else {
                EditMessageReplyMarkup(
                    message.chat().id(),
                    messagesIds.first()
                ).replyMarkup(
                    markup
                )
            }
        )
    }
}