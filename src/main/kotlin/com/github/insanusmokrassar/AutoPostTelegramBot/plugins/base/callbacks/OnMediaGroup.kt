package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.checkedMediaGroupsFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMediaGroupsCallback(): Job = launch {
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
        val id = messages.first().chat.id
        CommonKnownPostsTransactions[id] ?.also {
            messages.forEach { message ->
                it.addMessageId(PostMessage(message))
            }
            return@collectWithErrors
        } ?:also {
            PostTransaction().use { transaction ->
                messages.forEach { message ->
                    transaction.addMessageId(PostMessage(message))
                }
            }
        }
    }
}
