package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.FromUserMessage
import java.lang.ref.WeakReference

class FixPost(
    private val botWR: WeakReference<RequestsExecutor>
) : Command() {
    override val commandRegex: Regex = Regex("^/fixPost$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        try {
            val userId: ChatIdentifier? = (message as? FromUserMessage) ?.user ?.id ?: message.chat.id
            userId ?.let {
                usersTransactions[it] ?.saveNewPost() ?: throw NothingToSaveException("Transaction was not started")
                usersTransactions.remove(it)
            }
        } catch (e: NothingToSaveException) {
            commonLogger.warning("Nothing to save: ${e.message}")
        }
    }
}
