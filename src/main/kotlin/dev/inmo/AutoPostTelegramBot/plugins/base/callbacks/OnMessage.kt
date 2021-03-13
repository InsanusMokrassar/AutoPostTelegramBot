package dev.inmo.AutoPostTelegramBot.plugins.base.callbacks

import dev.inmo.AutoPostTelegramBot.base.database.PostTransaction
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.models.PostMessage
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.checkedMessagesFlow
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageEntity.textsources.BotCommandTextSource
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.MediaGroupMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMessageCallback(
    postsTable: PostsBaseInfoTable,
    postsMessagesTable: PostsMessagesInfoTable
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
        if (message is ContentMessage<*>) {
            (message.content as? TextContent) ?.also { content ->
                if (content.textEntities.firstOrNull { it.source is BotCommandTextSource } != null) {
                    return@collectWithErrors
                }
            }
        }
        if (message is MediaGroupMessage<*>) {
            return@collectWithErrors // do nothing
        }

        val chatId: ChatIdentifier = message.chat.id
        CommonKnownPostsTransactions[chatId] ?.also { transaction ->
            transaction.addMessageId(PostMessage(message))
            return@collectWithErrors
        } ?: also {
            PostTransaction(
                postsTable,
                postsMessagesTable
            ).use { transaction ->
                transaction.addMessageId(PostMessage(message))
            }
        }
    }
}
