package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionCompletedChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.bot.exceptions.ReplyMessageNotFoundException
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
        transactionCompletedChannel.asFlow().onEach {
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
            } catch (e: Exception) {
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
