package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.pengrad.telegrambot.model.Message

class StartPost : Command() {
    override val commandRegex: Regex = Regex("^/startPost$")

    override fun onCommand(updateId: Int, message: Message) {
        val userId: Long? = message.from() ?.id() ?.toLong() ?: message.chat() ?.id()
        userId ?.let {
            if (usersTransactions[it] == null) {
                usersTransactions[it] = PostTransaction()
            }
        }
    }
}