package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.messagesListener
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMessage(
    sourceChatId: Long
) {
    init {
        messagesListener.openSubscription().also {
            launch {
                while (isActive) {
                    val received = it.receive()
                    try {
                        invoke(
                            received.second,
                            sourceChatId
                        )
                    } catch (e: Exception) {
                        logger.throwing(
                            OnMessage::class.java.canonicalName,
                            "Perform message",
                            e
                        )
                    }
                }
                it.cancel()
            }
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