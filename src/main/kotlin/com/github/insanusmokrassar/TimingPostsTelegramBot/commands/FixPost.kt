package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class FixPost(
    bot: TelegramBot
) : UpdateCallback<Message> {
    private val bot = WeakReference(bot)
    override fun invoke(updateId: Int, message: Message) {
        try {
            PostTransactionTable.saveNewPost()
        } catch (e: NothingToSaveException) {
            bot.get() ?. executeAsync(
                SendMessage(
                    message.chat().id(),
                    "Nothing to save, transaction is empty"
                )
            )
        }
    }
}
