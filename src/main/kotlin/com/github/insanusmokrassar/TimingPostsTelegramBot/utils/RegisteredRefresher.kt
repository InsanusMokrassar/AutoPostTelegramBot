package com.github.insanusmokrassar.TimingPostsTelegramBot.utils

import com.github.insanusmokrassar.TimingPostsTelegramBot.InlineReceivers.makeDislikeInline
import com.github.insanusmokrassar.TimingPostsTelegramBot.InlineReceivers.makeLikeInline
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.toTable
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.*

const val like = "\uD83D\uDC4D"
const val dislike = "\uD83D\uDC4E"

private fun makeLikeText(likes: Int) = "$like $likes"
private fun makeDisikeText(dislikes: Int) = "$dislike $dislikes"

fun refreshRegisteredMessage(
    chat: Chat,
    postId: Int,
    bot: TelegramBot
) {
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
                makeDisikeText(
                    PostsLikesTable.postDislikes(postId)
                )
            ).callbackData(
                makeDislikeInline(postId)
            ),
            InlineKeyboardButton(
                makeLikeText(
                    PostsLikesTable.postLikes(postId)
                )
            ).callbackData(
                makeLikeInline(postId)
            )
        )
    )

    val messagesIds = PostsMessagesTable.getMessagesOfPost(postId)

    chat.username() ?.let {
        chatUsername ->
        messagesIds.map {
            makeLinkToMessage(
                chatUsername,
                it
            )
        }.mapIndexed {
            index, s ->
            InlineKeyboardButton(
                (index + 1).toString()
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

    val message = "Post registered. Rating: ${PostsLikesTable.getPostRating(postId)}"

    val registeredMessageId = PostsTable.postRegisteredMessage(postId)

    if (registeredMessageId == null) {
        SendMessage(
            chat.id(),
            message
        ).parseMode(
            ParseMode.Markdown
        ).replyMarkup(
            markup
        ).replyToMessageId(
            messagesIds.first()
        ).let {
            bot.executeAsync(
                it,
                onResponse = {
                    _, sendResponse ->
                    PostsTable.postRegistered(postId, sendResponse.message().messageId())
                }
            )
        }
    } else {
        EditMessageText(
            chat.id(),
            registeredMessageId,
            message
        ).replyMarkup(
            markup
        ).let {
            bot.executeAsync(it)
        }
    }
}
