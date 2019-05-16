package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.checkedMediaGroupsFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.mediaGroupsListener
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.usersTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import kotlinx.coroutines.*
import java.util.logging.Logger

internal fun CoroutineScope.enableOnMediaGroupsCallback(
    sourceChatId: ChatIdentifier
): Job = launch {
    checkedMediaGroupsFlow.collectWithErrors(
        { update, e ->
            commonLogger.throwing(
                "Media groups AutoPost callback",
                "Perform update: $update",
                e
            )
        }
    ) {
        val messages = it.data
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
