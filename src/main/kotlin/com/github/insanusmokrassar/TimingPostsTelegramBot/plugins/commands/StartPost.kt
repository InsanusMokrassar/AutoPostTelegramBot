package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.pengrad.telegrambot.model.Message

class StartPost : CommandPlugin() {
    override val version: PluginVersion = 0L
    override val commandRegex: Regex = Regex("^/startPost$")

    override fun onCommand(updateId: Int, message: Message) {
        PostTransactionTable.startTransaction()
    }
}