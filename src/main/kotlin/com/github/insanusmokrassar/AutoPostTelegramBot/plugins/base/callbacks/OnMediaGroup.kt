package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.mediaGroupsListener
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.usersTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import java.util.logging.Logger

private val logger = Logger.getLogger(Plugin::class.java.simpleName)

class OnMediaGroup(
    sourceChatId: ChatIdentifier
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
            val mediaGroup = it.mapNotNull { it.data as? MediaGroupMessage }
            invoke(mediaGroup, sourceChatId)
        }
    }

    private fun invoke(
        messages: List<MediaGroupMessage>,
        sourceChatId: ChatIdentifier
    ) {
        val first = messages.first()
        if (first.chat.id == sourceChatId) {
            val id = when(first) {
                is FromUserMessage -> first.user.id
                else -> first.chat.id
            }
            usersTransactions[id] ?.also {
                messages.forEach {
                        message ->
                    it.addMessageId(PostMessage(message))
                }
            } ?:also {
                PostTransaction().use {
                        transaction ->
                    messages.forEach {
                            message ->
                        transaction.addMessageId(PostMessage(message))
                    }
                }
            }
        }
    }
}
