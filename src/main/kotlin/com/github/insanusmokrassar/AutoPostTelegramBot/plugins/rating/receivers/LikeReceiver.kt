package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers.SafeCallbackQueryReceiver
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.answers.createAnswer
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.CallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JSON

const val like = "\uD83D\uDC4D"

internal fun makeLikeText(likes: Int) = "$like $likes"

@Serializable
private data class LikeData(
    val like: Int
)

fun makeLikeInline(postId: Int): String = JSON.stringify(
    LikeData.serializer(),
    LikeData(postId)
)
fun extractLikeInline(from: String): Int? = try {
    JSON.parse(LikeData.serializer(), from).like
} catch (e: SerializationException) {
    null
}

class LikeReceiver(
    executor: RequestsExecutor,
    sourceChatId: ChatId,
    private val postsLikesTable: PostsLikesTable,
    private val postsLikesMessagesTable: PostsLikesMessagesTable
) : SafeCallbackQueryReceiver(executor, sourceChatId) {
    override suspend fun invoke(query: MessageDataCallbackQuery) {
        extractLikeInline(
            query.data
        ) ?.let {
                postId ->

            postsLikesMessagesTable.messageIdByPostId(postId) ?: query.message.messageId.also {
                    messageId ->
                postsLikesMessagesTable.enableLikes(postId, messageId)
            }

            postsLikesTable.userLikePost(
                query.user.id,
                postId
            )

            executorWR.get() ?.execute (
                query.createAnswer(
                    "Voted +"
                )
            )
        }
    }
}