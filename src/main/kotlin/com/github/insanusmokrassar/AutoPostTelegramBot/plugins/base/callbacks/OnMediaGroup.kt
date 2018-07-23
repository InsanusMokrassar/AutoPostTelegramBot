package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.mediaGroupsListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.pengrad.telegrambot.model.Message
import kotlinx.coroutines.experimental.launch
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMediaGroup(
    sourceChatId: Long
) {
    init {
        mediaGroupsListener.subscribe(
            {
                logger.throwing(
                    OnMediaGroup::class.java.canonicalName,
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
