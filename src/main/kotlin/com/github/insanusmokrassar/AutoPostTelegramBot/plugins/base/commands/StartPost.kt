package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactions.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage

class StartPost : Command() {
    override val commandRegex: Regex = Regex("^/startPost$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val userId: ChatIdentifier? = (message as? FromUserMessage) ?.user ?.id ?: message.chat.id
        userId ?.let {
            if (usersTransactions[it] == null) {
                usersTransactions[it] =
                    PostTransaction()
            }
        }
    }
}