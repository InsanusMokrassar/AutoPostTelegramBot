package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.commands.Command
import com.pengrad.telegrambot.model.Message

class StartPost : Command() {
    override val commandRegex: Regex = Regex("^/startPost$")

    override fun onCommand(updateId: Int, message: Message) {
        PostTransactionTable.startTransaction()
    }
}