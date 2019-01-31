package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.AutoPostTelegramBot.allMessagesListener
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.disableLikesForPost
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.answers.createAnswer
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.DataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.UserId
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.*
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JSON

@Serializable
private data class DisableData(
    val disableRatings: Int
)

fun makeDisableInline(postId: Int): String = JSON.stringify(
    DisableData.serializer(),
    DisableData(postId)
)
fun extractDisableInline(from: String): Int? = try {
    JSON.parse(DisableData.serializer(), from).disableRatings
} catch (e: SerializationException) {
    null
}

private fun makeTextToApproveRemove(postId: Int) =
    "Please, write to me `${makeDisableInline(postId)}` if you want to disable ratings for this post"

private typealias UserIdPostId = Pair<UserId, Int>

class DisableReceiver(
    executor: RequestsExecutor,
    sourceChatId: ChatIdentifier,
    postsLikesMessagesTable: PostsLikesMessagesTable
) : CallbackQueryReceiver(executor) {
    private val awaitApprove = HashSet<UserIdPostId>()

    init {
        allMessagesListener.subscribe { update ->
            val message = update.data
            val userId = (message as? FromUserMessage) ?.user ?.id ?: message.chat.id

            val bot = executorWR.get() ?: return@subscribe
            awaitApprove.firstOrNull { it.first == userId } ?.let { userIdPostId ->
                val postId = userIdPostId.second
                if (message is ContentMessage<*>) {
                    val content = message.content
                    when (content) {
                        is TextContent -> if (extractDisableInline(content.text) == postId) {
                            awaitApprove.remove(userIdPostId)
                            disableLikesForPost(
                                postId,
                                bot,
                                sourceChatId,
                                postsLikesMessagesTable
                            )

                            bot.execute(
                                SendMessage(
                                    userId,
                                    "Rating was disabled",
                                    parseMode = MarkdownParseMode
                                )
                            )
                        } else {
                            null
                        }
                        else -> null
                    }
                } else {
                    null
                }
            } ?: if (message is AbleToBeForwardedMessage) {
                val forwarded = message.forwarded
                val from = forwarded ?.from
                if (forwarded != null && from ?.id == sourceChatId) {
                    postsLikesMessagesTable.postIdByMessageId(
                        forwarded.messageId
                    ) ?.let { postId ->
                        bot.execute(
                            SendMessage(
                                userId,
                                makeTextToApproveRemove(
                                    postId
                                ),
                                parseMode = MarkdownParseMode
                            )
                        )
                        awaitApprove.add(userId to postId)
                    }
                }
            }
        }
    }

    override suspend fun invoke(update: CallbackQueryUpdate) {
        val query = update.data as? DataCallbackQuery ?: return
        extractDisableInline(query.data)?.let {
            val userId = query.user.id
            awaitApprove.add(userId to it)
            executorWR.get() ?.execute(
                query.createAnswer(
                    makeTextToApproveRemove(it),
                    true
                )
            )
        }
    }
}