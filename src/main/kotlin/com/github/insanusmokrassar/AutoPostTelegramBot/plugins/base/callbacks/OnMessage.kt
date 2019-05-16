package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.checkedMessagesFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageEntity.BotCommandMessageEntity
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.ContentMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMessageCallback(
    sourceChatId: ChatIdentifier
): Job = launch {
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
        if (message.chat.id == sourceChatId) {
            if (message is ContentMessage<*>) {
                (message.content as? TextContent) ?.also { content ->
                    if (content.entities.firstOrNull { it is BotCommandMessageEntity } != null) {
                        return@collectWithErrors
                    }
                }
            }

            val userId: ChatIdentifier? = message.chat.id
            userId ?.let {
                CommonKnownPostsTransactions[userId] ?.also { transaction ->
                    transaction.addMessageId(PostMessage(message))
                } ?: also {
                    PostTransaction().use { transaction ->
                        transaction.addMessageId(PostMessage(message))
                    }
                }
            }
        }
    }
}
