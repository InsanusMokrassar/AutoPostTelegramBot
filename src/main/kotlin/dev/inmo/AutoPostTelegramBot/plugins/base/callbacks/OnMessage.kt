package dev.inmo.AutoPostTelegramBot.plugins.base.callbacks

import dev.inmo.AutoPostTelegramBot.base.models.PostMessage
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.checkedMessagesFlow
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.PostsTransactions
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageEntity.textsources.BotCommandTextSource
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.MediaGroupMessage
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMessageCallback(
    postsTransactions: PostsTransactions
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
        when (message) {
            is MediaGroupMessage<*> -> return@collectWithErrors
            is ContentMessage<*> -> message.content.asTextContent() ?.let { content ->
                if (content.textEntities.firstOrNull { it.source is BotCommandTextSource } != null) {
                    return@collectWithErrors
                }
            }
        }

        val chatId: ChatIdentifier = message.chat.id
        postsTransactions.doInTransaction(chatId) {
            it.addMessageId(PostMessage(message))
        }
    }
}
