package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionCompletedChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

class PostMessagesRegistrant(
    executor: RequestsExecutor,
    private val sourceChatId: ChatIdentifier
) {
    private val botWR = WeakReference(executor)
    init {

        transactionCompletedChannel.subscribeChecking {
            registerPostMessage(
                it
            )
            true
        }

        val scope = NewDefaultCoroutineScope()

        val registerJobs = PostsTable.getAll().mapNotNull {
            if (PostsTable.postRegisteredMessage(it) == null) {
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
            scope.coroutineContext.cancel()
        }
    }

    suspend fun registerPostMessage(
        registeredPostId: Int,
        retries: Int = 3
    ): MessageIdentifier? {
        val executor = botWR.get() ?: return null
        return try {
            val response = executor.execute(
                SendMessage(
                    sourceChatId,
                    "Post registered",
                    parseMode = MarkdownParseMode,
                    replyToMessageId = PostsMessagesTable.getMessagesOfPost(
                        registeredPostId
                    ).firstOrNull() ?.messageId ?: return null
                )
            )
            PostsTable.postRegistered(registeredPostId, response.messageId)
        } catch (e: Exception) {
            sendToLogger(
                e,
                "Register message; Left retries: $retries"
            )
            if (retries > 0) {
                registerPostMessage(registeredPostId, retries - 1)
            } else {
                null
            }
        }
    }
}
