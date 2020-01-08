package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.checkedMessagesFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageEntity.textsources.BotCommandTextSource
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.ContentMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMessageCallback(): Job = launch {
    checkedMessagesFlow.collectWithErrors(
        { message, e ->
            commonLogger.throwing(
                "On message AutoPost callback",
                "Perform message: $message",
                e
            )
        }
    ) {
        val message = it.data
        if (message is ContentMessage<*>) {
            (message.content as? TextContent) ?.also { content ->
                if (content.entities.firstOrNull { it.source is BotCommandTextSource } != null) {
                    return@collectWithErrors
                }
            }
        }

        val chatId: ChatIdentifier = message.chat.id
        CommonKnownPostsTransactions[chatId] ?.also { transaction ->
            transaction.addMessageId(PostMessage(message))
            return@collectWithErrors
        } ?: also {
            PostTransaction().use { transaction ->
                transaction.addMessageId(PostMessage(message))
            }
        }
    }
}
