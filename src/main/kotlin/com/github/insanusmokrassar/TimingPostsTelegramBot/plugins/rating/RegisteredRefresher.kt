package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.pluginLogger
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.toTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.makeLinkToMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.*
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

const val like = "\uD83D\uDC4D"
const val dislike = "\uD83D\uDC4E"

private fun makeLikeText(likes: Int) = "$like $likes"
private fun makeDislikeText(dislikes: Int) = "$dislike $dislikes"

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

class RegisteredRefresher(
    sourceChatId: Long,
    bot: TelegramBot
) {
    private val botWR = WeakReference(bot)

    init {
        PostsLikesTable.ratingsChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val update = it.receive()

                    try {
                        refreshRegisteredMessage(
                            sourceChatId,
                            botWR.get() ?: break,
                            update.first,
                            update.second
                        )
                    } catch (e: Exception) {
                        pluginLogger.throwing(
                            "RegisteredRefresher",
                            "updateMessageId",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }

        PostsTable.postMessageRegisteredChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val postMessageRegistered = it.receive()

                    try {
                        refreshRegisteredMessage(
                            sourceChatId,
                            botWR.get() ?: break,
                            postMessageRegistered.first
                        )
                    } catch (e: Exception) {
                        pluginLogger.throwing(
                            "RegisteredRefresher",
                            "updateMessageId",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }

        PostsTable.postRemovedChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val removedPostId = it.receive()

                    try {
                        PostsLikesMessagesTable.messageIdByPostId(removedPostId) ?.let {
                            messageId ->

                            botWR.get() ?.executeAsync(
                                DeleteMessage(
                                    sourceChatId,
                                    messageId
                                )
                            )

                            PostsLikesMessagesTable.clearPostIdMessageId(removedPostId)
                        }
                    } catch (e: Exception) {
                        pluginLogger.throwing(
                            "RegisteredRefresher",
                            "remove registered post-message link",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }
    }
}

fun refreshRegisteredMessage(
    chatId: Long,
    bot: TelegramBot,
    postId: Int,
    postRating: Int = PostsLikesTable.getPostRating(postId),
    username: String? = null
) {
    val postMessageId = PostsTable.postRegisteredMessage(postId) ?: return

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

    username ?.let {
        chatUsername ->
        PostsMessagesTable.getMessagesOfPost(
            postId
        ).map {
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

    val message = "Rating: $postRating"

    val registeredMessageId = PostsLikesMessagesTable.messageIdByPostId(postId)

    if (registeredMessageId == null) {
        SendMessage(
            chatId,
            message
        ).parseMode(
            ParseMode.Markdown
        ).replyMarkup(
            markup
        ).replyToMessageId(
            postMessageId
        ).let {
            bot.executeAsync(
                it,
                onResponse = {
                    _, sendResponse ->
                    if (!PostsLikesMessagesTable.registerLikeMessageId(postId, sendResponse.message().messageId())) {
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
