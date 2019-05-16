package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import kotlinx.coroutines.*

val fixPostRegex: Regex = Regex("^fixPost$")

internal fun CoroutineScope.enableFixPostCommand(): Job = launch {
    buildCommandFlow(fixPostRegex).collectWithErrors { message ->
        try {
            val userId: ChatIdentifier? = (message as? FromUserMessage) ?.user ?.id ?: message.chat.id
            userId ?.let {
                CommonKnownPostsTransactions[it] ?.saveNewPost() ?: throw NothingToSaveException("Transaction was not started")
            }
        } catch (e: NothingToSaveException) {
            commonLogger.warning("Nothing to save: ${e.message}")
        }
    }
}
