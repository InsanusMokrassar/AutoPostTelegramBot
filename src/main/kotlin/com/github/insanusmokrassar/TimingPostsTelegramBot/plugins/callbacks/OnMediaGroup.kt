package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.callbacks

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.mediaGroupsListener
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMediaGroup : Plugin {
    override val version: PluginVersion = 0L
    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        mediaGroupsListener.openSubscription().also {
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
                            OnMediaGroup::class.java.canonicalName,
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
        messages: List<Message>,
        sourceChatId: Long
    ) {
        val first = messages.first()
        if (first.chat().id() == sourceChatId) {
            if (PostTransactionTable.inTransaction) {
                messages.forEach {
                    PostTransactionTable.addMessageId(
                        PostMessage(
                            it.messageId(),
                            it.mediaGroupId()
                        )
                    )
                }
            } else {
                PostTransactionTable.startTransaction()
                messages.map {
                    PostMessage(it)
                }.also {
                    PostTransactionTable.addMessageId(
                        *it.toTypedArray()
                    )
                }
                PostTransactionTable.saveNewPost()
            }
        }
    }
}
