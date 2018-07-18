package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

private fun registerPostMessage(
    bot: TelegramBot,
    sourceChatId: Long,
    registeredPostId: Int
) {
    try {
        bot.executeAsync(
            SendMessage(
                sourceChatId,
                "Post registered"
            ).parseMode(
                ParseMode.Markdown
            ).replyToMessageId(
                PostsMessagesTable.getMessagesOfPost(
                    registeredPostId
                ).firstOrNull() ?.messageId ?: return
            ),
            onResponse = {
                _, sendResponse ->
                if (PostsTable.postRegisteredMessage(registeredPostId) == null) {
                    PostsTable.postRegistered(registeredPostId, sendResponse.message().messageId())
                } else {
                    bot.executeAsync(
                        DeleteMessage(
                            sendResponse.message().chat().id(),
                            sendResponse.message().messageId()
                        )
                    )
                }
            }
        )
    } catch (e: Exception) {
        pluginLogger.throwing(
            DefaultPostRegisteredMessage::class.java.simpleName,
            "Register message",
            e
        )
    }
}

class DefaultPostRegisteredMessage(
    bot: TelegramBot,
    sourceChatId: Long
) {
    init {
        val botWR = WeakReference(bot)

        PostTransactionTable.transactionCompletedChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val registeredPostId = it.receive()
                    registerPostMessage(
                        botWR.get() ?: break,
                        sourceChatId,
                        registeredPostId
                    )
                }
                it.cancel()
            }
        }
    }
}
