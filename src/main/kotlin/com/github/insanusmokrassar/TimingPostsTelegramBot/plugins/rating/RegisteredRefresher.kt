package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
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

fun clearRatingDataForPostId(
    postId: Int,
    bot: TelegramBot,
    sourceChatId: Long,
    postsLikesMessagesTable: PostsLikesMessagesTable
) {
    postsLikesMessagesTable.messageIdByPostId(postId) ?.let {
        messageId ->

        bot.executeAsync(
            DeleteMessage(
                sourceChatId,
                messageId
            )
        )

        postsLikesMessagesTable.clearPostIdMessageId(postId)
    }
}

class RegisteredRefresher(
    sourceChatId: Long,
    bot: TelegramBot,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable
) {
    private val botWR = WeakReference(bot)

    init {
        postsLikesTable.ratingsChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val update = it.receive()

                    try {
                        refreshRegisteredMessage(
                            sourceChatId,
                            botWR.get() ?: break,
                            update.first,
                            postsLikesTable,
                            postsLikesMessagesTable,
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

//        PostsTable.postMessageRegisteredChannel.openSubscription().also {
//            launch {
//                while (isActive) {
//                    val postMessageRegistered = it.receive()
//
//                    try {
//                        refreshRegisteredMessage(
//                            sourceChatId,
//                            botWR.get() ?: break,
//                            postMessageRegistered.first,
//                            postsLikesTable,
//                            postsLikesMessagesTable
//                        )
//                    } catch (e: Exception) {
//                        pluginLogger.throwing(
//                            "RegisteredRefresher",
//                            "updateMessageId",
//                            e
//                        )
//                    }
//                }
//                it.cancel()
//            }
//        }

        PostTransactionTable.transactionCompletedChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val bot = botWR.get() ?: break
                    val postId = it.receive()
                    refreshRegisteredMessage(
                        sourceChatId,
                        bot,
                        postId,
                        postsLikesTable,
                        postsLikesMessagesTable
                    )
                }
                it.cancel()
            }
        }

        PostsTable.postRemovedChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val removedPostId = it.receive()

                    try {
                        clearRatingDataForPostId(
                            removedPostId,
                            botWR.get() ?: break,
                            sourceChatId,
                            postsLikesMessagesTable
                        )
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
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable,
    postRating: Int = postsLikesTable.getPostRating(postId),
    username: String? = null
) {
    val likeButton = InlineKeyboardButton(
        makeDislikeText(
            postsLikesTable.postDislikes(postId)
        )
    ).callbackData(
        makeDislikeInline(postId)
    )
    val dislikeButton = InlineKeyboardButton(
        makeLikeText(
            postsLikesTable.postLikes(postId)
        )
    ).callbackData(
        makeLikeInline(postId)
    )

    val buttons = mutableListOf<MutableList<InlineKeyboardButton>>(
        mutableListOf(
            likeButton,
            dislikeButton
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

    val registeredMessageId = postsLikesMessagesTable.messageIdByPostId(postId)

    if (registeredMessageId == null) {
        SendMessage(
            chatId,
            message
        ).parseMode(
            ParseMode.Markdown
        ).replyMarkup(
            markup
        ).replyToMessageId(
            PostsMessagesTable.getMessagesOfPost(postId).firstOrNull() ?.messageId ?: return
        ).let {
            bot.executeAsync(
                it,
                onResponse = {
                    _, sendResponse ->
                    if (!postsLikesMessagesTable.registerLikeMessageId(postId, sendResponse.message().messageId())) {
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
