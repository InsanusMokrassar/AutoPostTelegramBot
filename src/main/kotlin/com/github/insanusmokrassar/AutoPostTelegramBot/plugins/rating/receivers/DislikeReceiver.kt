package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers.SafeCallbackQueryReceiver
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.answers.createAnswer
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.*
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JSON

const val dislike = "\uD83D\uDC4E"

internal fun makeDislikeText(dislikes: Int) = "$dislike $dislikes"

@Serializable
private data class DislikeData(
    val dislike: Int
)

fun makeDislikeInline(postId: Int): String = JSON.stringify(
    DislikeData.serializer(),
    DislikeData(postId)
)
fun extractDislikeInline(from: String): Int? = try {
    JSON.parse(DislikeData.serializer(), from).dislike
} catch (e: SerializationException) {
    null
}

class DislikeReceiver(
    executor: RequestsExecutor,
    sourceChatId: ChatId,
    private val postsLikesTable: PostsLikesTable,
    private val postsLikesMessagesTable: PostsLikesMessagesTable
) : SafeCallbackQueryReceiver(
    executor,
    sourceChatId
) {
    override suspend fun invoke(query: MessageDataCallbackQuery) {
        extractDislikeInline(
            query.data
        )?.let {
                postId ->

            postsLikesMessagesTable.messageIdByPostId(postId) ?: query.message.messageId.also {
                    messageId ->
                postsLikesMessagesTable.enableLikes(postId, messageId)
            }

            postsLikesTable.userDislikePost(
                query.user.id,
                postId
            )

            executorWR.get() ?.execute(
                query.createAnswer(
                    "Voted -"
                )
            )
        }
    }
}