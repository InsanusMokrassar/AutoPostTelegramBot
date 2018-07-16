package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.callbacks

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.messagesListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMessage : Plugin {
    override val version: PluginVersion = 0L
    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
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