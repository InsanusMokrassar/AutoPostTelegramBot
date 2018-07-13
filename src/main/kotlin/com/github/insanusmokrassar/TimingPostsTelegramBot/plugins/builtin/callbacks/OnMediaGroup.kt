package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.callbacks

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.mediaGroupsListener
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMediaGroup : Plugin {
    override fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
    ) {
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
                    PostMessage(
                        it.messageId(),
                        it.mediaGroupId()
                    )
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
