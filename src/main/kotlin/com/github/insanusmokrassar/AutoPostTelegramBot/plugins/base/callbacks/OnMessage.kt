package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactions.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.messagesListener
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.usersTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageEntity.BotCommandMessageEntity
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.ContentMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.Message
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMessage(
    sourceChatId: ChatIdentifier
) {
    init {
        messagesListener.subscribe(
            {
                logger.throwing(
                    OnMessage::class.java.canonicalName,
                    "Perform message",
                    it
                )
                true
            }
        ) {
            invoke(
                it.data,
                sourceChatId
            )
        }
    }

    private fun invoke(
        message: Message,
        sourceChatId: ChatIdentifier
    ) {
        if (message.chat.id == sourceChatId) {
            if (message is ContentMessage<*>) {
                (message.content as? TextContent) ?.also { content ->
                    if (content.entities.firstOrNull { it is BotCommandMessageEntity } != null) {
                        return
                    }
                }
            }

            val userId: ChatIdentifier? = message.chat.id
            userId ?.let {
                usersTransactions[userId] ?.also {
                    it.addMessageId(PostMessage(message))
                } ?: also {
                    PostTransaction().use {
                        transaction ->
                        transaction.addMessageId(PostMessage(message))
                    }
                }
            }
        }
    }
}