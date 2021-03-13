package dev.inmo.AutoPostTelegramBot.plugins.base.commands

import dev.inmo.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.utils.commands.buildCommandFlow
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import kotlinx.coroutines.*

val fixPostRegex: Regex = Regex("^fixPost$")

internal fun CoroutineScope.enableFixPostCommand(): Job = launch {
    buildCommandFlow(fixPostRegex).collectWithErrors({ message, it ->
        commonLogger.throwing("Post fixer: $message", "fixPost", it)
    }) { message ->
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
