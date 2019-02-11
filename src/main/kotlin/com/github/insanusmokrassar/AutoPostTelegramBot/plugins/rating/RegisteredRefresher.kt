package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionCompletedChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.makeLinkToMessage
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.bot.exceptions.ReplyMessageNotFound
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.edit.text.EditChatMessageText
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.buttons.InlineKeyboardButtons.*
import com.github.insanusmokrassar.TelegramBotAPI.types.buttons.InlineKeyboardMarkup
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import com.github.insanusmokrassar.TelegramBotAPI.utils.matrix
import com.github.insanusmokrassar.TelegramBotAPI.utils.row
import java.lang.ref.WeakReference

suspend fun disableLikesForPost(
    postId: Int,
    executor: RequestsExecutor,
    sourceChatId: ChatIdentifier,
    postsLikesMessagesTable: PostsLikesMessagesTable
) {
    postsLikesMessagesTable.messageIdByPostId(postId) ?.let {
        messageId ->

        executor.executeUnsafe(
            DeleteMessage(
                sourceChatId,
                messageId
            )
        )

        postsLikesMessagesTable.disableLikes(postId)
    }
}

class RegisteredRefresher(
    sourceChatId: ChatId,
    executor: RequestsExecutor,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable
) {
    private val botWR = WeakReference(executor)

    init {
        postsLikesTable.ratingsChannel.subscribe(
            {
                commonLogger.throwing(
                    "RegisteredRefresher",
                    "updateMessageId",
                    it
                )
                true
            }
        ) {
            refreshRegisteredMessage(
                sourceChatId,
                botWR.get() ?: return@subscribe,
                it.first,
                postsLikesTable,
                postsLikesMessagesTable,
                it.second
            )
        }

        transactionCompletedChannel.subscribe {
            refreshRegisteredMessage(
                sourceChatId,
                botWR.get() ?: return@subscribe,
                it,
                postsLikesTable,
                postsLikesMessagesTable
            )
        }

        PostsTable.postRemovedChannel.subscribe(
            {
                sendToLogger(it, "Try to handle post removing")
                true
            }
        ) {
            disableLikesForPost(
                it,
                botWR.get() ?: return@subscribe,
                sourceChatId,
                postsLikesMessagesTable
            )
        }
    }
}

internal suspend fun refreshRegisteredMessage(
    chatId: ChatId,
    executor: RequestsExecutor,
    postId: Int,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable,
    postRating: Int = postsLikesTable.getPostRating(postId),
    username: String? = null
) {
    val dislikeButton = CallbackDataInlineKeyboardButton(
        makeDislikeText(
            postsLikesTable.postDislikes(postId)
        ),
        makeDislikeInline(postId)
    )
    val likeButton = CallbackDataInlineKeyboardButton(
        makeLikeText(
            postsLikesTable.postLikes(postId)
        ),
        makeLikeInline(postId)
    )

    val buttons = matrix<InlineKeyboardButton> {
        row {
            add(dislikeButton)
            add(likeButton)
        }
    }.let { base ->
        username ?.let { chatUsername ->
            PostsMessagesTable.getMessagesOfPost(
                postId
            ).map {
                makeLinkToMessage(
                    chatUsername,
                    it.messageId
                )
            }.mapIndexed { index, s ->
                URLInlineKeyboardButton(
                    (index + 1).toString(),
                    s
                )
            }.toTable(4)
        } ?.let {
            base + it
        } ?: base
    }

    val markup = InlineKeyboardMarkup(buttons)

    val message = "Rating: $postRating"

    var registeredMessageId = postsLikesMessagesTable.messageIdByPostId(postId)
    var currentMessageIdForReplying = PostsMessagesTable.getMessagesOfPost(postId).firstOrNull()?.messageId

    if (registeredMessageId != null) {
        val request = EditChatMessageText(
            chatId,
            registeredMessageId,
            message,
            replyMarkup = markup
        )
        executor.executeUnsafe(request)
    } else {
        while (registeredMessageId == null && currentMessageIdForReplying != null) {
            registeredMessageId = try {
                val response = SendMessage(
                    chatId,
                    message,
                    replyMarkup = markup,
                    replyToMessageId = currentMessageIdForReplying,
                    parseMode = MarkdownParseMode
                ).let {
                    executor.execute(it)
                }
                if (!postsLikesMessagesTable.enableLikes(postId, response.messageId)) {
                    executor.executeAsync(
                        DeleteMessage(
                            chatId,
                            response.messageId
                        )
                    )
                }
                response.messageId
            } catch (e: ReplyMessageNotFound) {
                PostsMessagesTable.removePostMessage(postId, currentMessageIdForReplying)
                currentMessageIdForReplying = PostsMessagesTable.getMessagesOfPost(postId).firstOrNull() ?.messageId
                null
            }
        }
        if (registeredMessageId == null && currentMessageIdForReplying == null) {
            PostsTable.removePost(postId)
            commonLogger.warning("Message with id $postId was removed from database")
        }
    }
}
