package dev.inmo.AutoPostTelegramBot.plugins.base

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.transactionCompletedFlow
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.extensions.sendToLogger
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.bot.exceptions.*
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.ParseMode.MarkdownParseMode
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class PostMessagesRegistrant(
    executor: RequestsExecutor,
    private val sourceChatId: ChatIdentifier,
    private val postsTable: PostsBaseInfoTable,
    private val postsMessagesTable: PostsMessagesInfoTable
) {
    private val botWR = WeakReference(executor)
    init {
        val scope = NewDefaultCoroutineScope()
        transactionCompletedFlow.onEach {
            registerPostMessage(it)
        }.launchIn(scope)

        val registerJobs = postsTable.getAll().mapNotNull {
            if (postsTable.postRegisteredMessage(it) == null) {
                scope.launch {
                    registerPostMessage(
                        it
                    )
                }
            } else {
                null
            }
        }
        scope.launch {
            registerJobs.joinAll()
        }
    }

    suspend fun registerPostMessage(
        registeredPostId: Int,
        retries: Int = 3
    ): MessageIdentifier? {
        val executor = botWR.get() ?: return null
        val messages = postsMessagesTable.getMessagesOfPost(
            registeredPostId
        ).toMutableList()
        var registeredMessageId: MessageIdentifier? = null
        var actualRetries = retries
        while (registeredMessageId == null && messages.isNotEmpty()) {
            val messageId = messages.first().messageId
            try {
                val response = executor.execute(
                    SendTextMessage(
                        sourceChatId,
                        "Post registered",
                        parseMode = MarkdownParseMode,
                        replyToMessageId = messageId
                    )
                )
                registeredMessageId = response.messageId
                actualRetries = retries
            } catch (e: ReplyMessageNotFoundException) {
                sendToLogger(
                    e,
                    "Register message"
                )
                postsMessagesTable.removePostMessage(registeredPostId, messageId)
                messages.removeAt(0)
            } catch (e: CommonRequestException) {
                if (e.plainAnswer.contains("Bad Request: replied message not found")) {
                    sendToLogger(
                        e,
                        "Register message"
                    )
                    postsMessagesTable.removePostMessage(registeredPostId, messageId)
                    messages.removeAt(0)
                } else {
                    sendToLogger(
                        e,
                        "Register message; Left retries: $actualRetries"
                    )
                    actualRetries--
                }
            } catch (e: Exception) {
                println(e::class)
                sendToLogger(
                    e,
                    "Register message; Left retries: $actualRetries"
                )
                actualRetries--
            }
        }
        return if (messages.isEmpty()) {
            postsTable.removePost(registeredPostId)
            commonLogger.warning("Post $registeredPostId was removed")
            null
        } else {
            registeredMessageId ?.also {
                postsTable.postRegistered(registeredPostId, it)
            }
        }
    }
}
