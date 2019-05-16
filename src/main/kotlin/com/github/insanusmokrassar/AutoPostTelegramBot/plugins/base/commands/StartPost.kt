package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import kotlinx.coroutines.*

val startPostRegex: Regex = Regex("^startPost$")

internal fun CoroutineScope.enableStartPostCommand(): Job = launch {
    buildCommandFlow(startPostRegex).collectWithErrors { message ->
        val chatId: ChatIdentifier = message.chat.id
        CommonKnownPostsTransactions.startTransaction(chatId)
    }
}
