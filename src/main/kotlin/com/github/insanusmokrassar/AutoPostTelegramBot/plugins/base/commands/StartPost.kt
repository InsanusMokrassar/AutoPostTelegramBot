package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import kotlinx.coroutines.*

val startPostRegex: Regex = Regex("^startPost$")

internal fun CoroutineScope.enableStartPostCommand(): Job = launch {
    buildCommandFlow(startPostRegex).collectWithErrors { message ->
        val userId: ChatIdentifier? = (message as? FromUserMessage) ?.user ?.id ?: message.chat.id
        userId ?.let {
            if (usersTransactions[it] == null) {
                usersTransactions[it] = PostTransaction()
            }
        }
    }
}
