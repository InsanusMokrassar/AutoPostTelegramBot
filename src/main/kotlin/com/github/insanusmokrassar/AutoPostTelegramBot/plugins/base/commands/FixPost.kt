package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class FixPost(
    private val botWR: WeakReference<TelegramBot>
) : Command() {
    override val commandRegex: Regex = Regex("^/fixPost$")

    override fun onCommand(updateId: Int, message: Message) {
        try {
            val userId: Long? = message.from() ?.id() ?.toLong() ?: message.chat() ?.id()
            userId ?.let {
                usersTransactions[it] ?.saveNewPost() ?: throw NothingToSaveException("Transaction was not started")
                usersTransactions.remove(it)
            }
        } catch (e: NothingToSaveException) {
            commonLogger.warning("Nothing to save: ${e.message}")
        }
    }
}
