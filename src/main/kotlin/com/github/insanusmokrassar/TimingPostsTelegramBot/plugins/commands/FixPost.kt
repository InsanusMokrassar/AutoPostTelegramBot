package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage

class FixPost : CommandPlugin() {
    override val version: PluginVersion = 0L
    override val commandRegex: Regex = Regex("/fixPost")

    override fun onCommand(updateId: Int, message: Message) {
        try {
            PostTransactionTable.saveNewPost()
        } catch (e: NothingToSaveException) {
            botWR ?.get() ?.executeAsync(
                SendMessage(
                    message.chat().id(),
                    "Nothing to save, transaction is empty"
                )
            )
        }
    }
}
