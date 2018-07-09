package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.callbacks

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.messagesListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMessage : Plugin {
    override fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
    ) {
        messagesListener.openSubscription().also {
            launch {
                while (isActive) {
                    val received = it.receive()
                    try {
                        invoke(
                            received.second,
                            baseConfig.sourceChatId
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