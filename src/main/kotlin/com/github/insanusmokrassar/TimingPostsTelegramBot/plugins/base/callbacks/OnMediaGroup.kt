package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.base.callbacks

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

class OnMediaGroup(
    sourceChatId: Long
) {
    init {
        mediaGroupsListener.openSubscription().also {
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
