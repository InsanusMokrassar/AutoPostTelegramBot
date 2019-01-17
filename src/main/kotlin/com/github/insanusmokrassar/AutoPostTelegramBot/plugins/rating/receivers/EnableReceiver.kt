package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.AutoPostTelegramBot.allMessagesListener
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.refreshRegisteredMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.answers.createAnswer
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.CallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.DataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JSON

@Serializable
private data class EnableData(
    val enableRatings: Int
)

fun makeEnableInline(postId: Int): String = JSON.stringify(
    EnableData.serializer(),
    EnableData(postId)
)
fun extractEnableInline(from: String): Int? = try {
    JSON.parse(EnableData.serializer(), from).enableRatings
} catch (e: SerializationException) {
    null
}

private fun makeTextToApproveEnable(postId: Int) =
    "Please, write to me `${makeEnableInline(postId)}` if you want to enable ratings for this post"

class EnableReceiver(
    executor: RequestsExecutor,
    sourceChatId: ChatId,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable
) : CallbackQueryReceiver(executor) {
    private val awaitApprove = HashMap<ChatId, Int>()

    init {
        allMessagesListener.subscribeChecking { update ->
            val message = update.data as? CommonMessage<*> ?: return@subscribeChecking true
            val userId = message.chat.id
            val messageContent = message.content as? TextContent ?:return@subscribeChecking true

            val bot = executorWR.get() ?: return@subscribeChecking false
            awaitApprove[userId] ?.let {
                if (extractEnableInline(messageContent.text) == it) {
                    awaitApprove.remove(userId)
                    refreshRegisteredMessage(
                        sourceChatId,
                        bot,
                        it,
                        postsLikesTable,
                        postsLikesMessagesTable
                    )

                    bot.executeAsync(
                        SendMessage(
                            userId,
                            "Rating was enabled",
                            parseMode = MarkdownParseMode
                        )
                    )
                }
            } ?:let {
                val forwarded = message.forwarded
                if (forwarded != null && forwarded.from ?.id == sourceChatId) {
                    try {
                        val postId = PostsTable.findPost(
                            forwarded.messageId
                        )
                        bot.executeAsync(
                            SendMessage(
                                userId,
                                makeTextToApproveEnable(
                                    postId
                                ),
                                parseMode = MarkdownParseMode
                            ),
                            onSuccess = { _ ->
                                awaitApprove[userId] = postId
                            }
                        )
                    } catch (e: NoRowFoundException) { }
                }
            }
            true
        }
    }

    override suspend fun invoke(update: Update<CallbackQuery>) {
        val query = update.data as? DataCallbackQuery ?: return
        extractDisableInline(query.data)?.let {
            awaitApprove[query.user.id] = it
            executorWR.get() ?.execute(
                query.createAnswer(
                    makeTextToApproveEnable(it),
                    true
                )
            )
        }
    }
}