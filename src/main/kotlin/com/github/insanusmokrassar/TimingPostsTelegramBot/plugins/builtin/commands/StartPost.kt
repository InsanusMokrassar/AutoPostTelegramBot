package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.pengrad.telegrambot.model.Message

class StartPost : Command() {
    override val commandRegex: Regex = Regex("^/startPost$")

    override fun onCommand(updateId: Int, message: Message) {
        PostTransactionTable.startTransaction()
    }
}