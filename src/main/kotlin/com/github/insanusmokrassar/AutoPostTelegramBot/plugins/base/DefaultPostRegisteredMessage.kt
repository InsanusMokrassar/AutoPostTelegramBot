package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionCompletedChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import java.lang.ref.WeakReference

private suspend fun registerPostMessage(
    executor: RequestsExecutor,
    sourceChatId: ChatIdentifier,
    registeredPostId: Int,
    retries: Int = 3
) {
    try {
        val response = executor.execute(
            SendMessage(
                sourceChatId,
                "Post registered",
                parseMode = MarkdownParseMode,
                replyToMessageId = PostsMessagesTable.getMessagesOfPost(
                    registeredPostId
                ).firstOrNull() ?.messageId ?: return
            )
        )
        if (PostsTable.postRegisteredMessage(registeredPostId) == null) {
            PostsTable.postRegistered(registeredPostId, response.messageId)
        } else {
            executor.execute(
                DeleteMessage(
                    response.asMessage.chat.id,
                    response.messageId
                )
            )
        }
    } catch (e: Exception) {
        executor.sendToLogger(
            e,
            "Register message; Left retries: $retries"
        )
        if (retries > 0) {
            registerPostMessage(executor, sourceChatId, registeredPostId, retries - 1)
        }
    }
}

class DefaultPostRegisteredMessage(
    executor: RequestsExecutor,
    sourceChatId: ChatIdentifier
) {
    init {
        val botWR = WeakReference(executor)

        transactionCompletedChannel.subscribeChecking {
            registerPostMessage(
                botWR.get() ?: return@subscribeChecking false,
                sourceChatId,
                it
            )
            true
        }
    }
}
