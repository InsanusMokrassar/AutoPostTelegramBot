package dev.inmo.AutoPostTelegramBot.plugins.base.commands

import dev.inmo.AutoPostTelegramBot.utils.commands.buildCommandFlow
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.types.ChatIdentifier
import kotlinx.coroutines.*

val startPostRegex: Regex = Regex("^startPost$")

internal fun CoroutineScope.enableStartPostCommand(): Job = launch {
    buildCommandFlow(startPostRegex).collectWithErrors { message ->
        val chatId: ChatIdentifier = message.chat.id
        CommonKnownPostsTransactions.startTransaction(chatId)
    }
}
