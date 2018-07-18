package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.pengrad.telegrambot.model.Message

class StartPost : Command() {
    override val commandRegex: Regex = Regex("^/startPost$")

    override fun onCommand(updateId: Int, message: Message) {
        PostTransactionTable.startTransaction()
    }
}