package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceiver
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers.SafeCallbackQueryReceiver
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.queryAnswer
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

const val dislike = "\uD83D\uDC4E"

internal fun makeDislikeText(dislikes: Int) = "$dislike $dislikes"

private const val dislikeIdentifier = "dislike"

fun makeDislikeInline(postId: Int): String = "$dislikeIdentifier: $postId"
fun extractDislikeInline(from: String): Int? = try {
    from.toIObject().get<String>(dislikeIdentifier).toInt()
} catch (e: ReadException) {
    null
}

class DislikeReceiver(
    bot: TelegramBot,
    sourceChatId: Long,
    private val postsLikesTable: PostsLikesTable,
    private val postsLikesMessagesTable: PostsLikesMessagesTable
) : SafeCallbackQueryReceiver(bot, sourceChatId) {
    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot
    ) {
        extractDislikeInline(
            query.data()
        )?.let {
            postId ->

            postsLikesMessagesTable.messageIdByPostId(postId) ?: query.message().messageId().also {
                messageId ->
                postsLikesMessagesTable.enableLikes(postId, messageId)
            }

            postsLikesTable.userDislikePost(
                query.from().id().toLong(),
                postId
            )

            bot.queryAnswer(
                query.id(),
                "Voted -"
            )
        }
    }
}