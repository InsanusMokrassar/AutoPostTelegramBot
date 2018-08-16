package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.messagesListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.pengrad.telegrambot.model.Message
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMessage(
    sourceChatId: Long
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
                it.second,
                sourceChatId
            )
        }
    }

    private fun invoke(
        message: Message,
        sourceChatId: Long
    ) {
        if (message.chat().id() == sourceChatId) {
            message.text() ?. let {
                if (it.startsWith("/")) {
                    return
                }
            }
            if (PostTransactionTable.inTransaction) {
                PostTransactionTable.addMessageId(
                    PostMessage(message)
                )
            } else {
                PostTransactionTable.startTransaction()
                PostTransactionTable.addMessageId(
                    PostMessage(message)
                )
                PostTransactionTable.saveNewPost()
            }
        }
    }
}