package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage

class FixPost : Command() {
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
