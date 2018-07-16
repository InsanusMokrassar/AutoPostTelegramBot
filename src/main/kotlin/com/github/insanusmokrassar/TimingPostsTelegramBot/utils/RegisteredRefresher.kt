package com.github.insanusmokrassar.TimingPostsTelegramBot.utils

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.toTable
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.*
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

const val like = "\uD83D\uDC4D"
const val dislike = "\uD83D\uDC4E"

private fun makeLikeText(likes: Int) = "$like $likes"
private fun makeDislikeText(dislikes: Int) = "$dislike $dislikes"

fun initSubscription(
    chatId: Long,
    bot: TelegramBot
) {
    val botWR = WeakReference(bot)
    PostsLikesTable.ratingsChannel.openSubscription().also {
        launch {
            while (isActive) {
                val bot = botWR.get() ?: break
                val update = it.receive()
                refreshRegisteredMessage(
                    chatId,
                    bot,
                    update.first,
                    update.second
                )
            }
            it.cancel()
        }
    }
    PostTransactionTable.transactionCompletedChannel.openSubscription().also {
        launch {
            while (isActive) {
                val bot = botWR.get() ?: break
                val postId = it.receive()
                refreshRegisteredMessage(
                    chatId,
                    bot,
                    postId
                )
            }
            it.cancel()
        }
    }
}

private const val likeIdentifier = "like"

fun makeLikeInline(postId: Int): String = "$likeIdentifier: $postId"
fun extractLikeInline(from: String): Int? = try {
    from.toIObject().get<String>(likeIdentifier).toInt()
} catch (e: ReadException) {
    null
}

private const val dislikeIdentifier = "dislike"

fun makeDislikeInline(postId: Int): String = "$dislikeIdentifier: $postId"
fun extractDislikeInline(from: String): Int? = try {
    from.toIObject().get<String>(dislikeIdentifier).toInt()
} catch (e: ReadException) {
    null
}

fun refreshRegisteredMessage(
    chatId: Long,
    bot: TelegramBot,
    postId: Int,
    postRating: Int = PostsLikesTable.getPostRating(postId),
    username: String? = null
) {
    val buttons = mutableListOf<MutableList<InlineKeyboardButton>>(
        mutableListOf(
            InlineKeyboardButton(
                makeDislikeText(
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

    val messages = PostsMessagesTable.getMessagesOfPost(postId)

    username ?.let {
        chatUsername ->
        messages.map {
            makeLinkToMessage(
                chatUsername,
                it.messageId
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

    val message = "Post registered. Rating: $postRating"

    val registeredMessageId = PostsTable.postRegisteredMessage(postId)

    if (registeredMessageId == null) {
        SendMessage(
            chatId,
            message
        ).parseMode(
            ParseMode.Markdown
        ).replyMarkup(
            markup
        ).replyToMessageId(
            messages.first().messageId
        ).let {
            bot.executeAsync(
                it,
                onResponse = {
                    _, sendResponse ->
                    if (!PostsTable.postRegistered(postId, sendResponse.message().messageId())) {
                        bot.executeAsync(
                            DeleteMessage(
                                chatId,
                                sendResponse.message().messageId()
                            )
                        )
                    }
                }
            )
        }
    } else {
        EditMessageText(
            chatId,
            registeredMessageId,
            message
        ).replyMarkup(
            markup
        ).let {
            bot.executeAsync(it)
        }
    }
}
