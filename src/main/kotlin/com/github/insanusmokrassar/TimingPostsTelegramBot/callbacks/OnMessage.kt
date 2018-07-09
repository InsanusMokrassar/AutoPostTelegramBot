package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands.StartPost
import com.pengrad.telegrambot.model.Message

class OnMessage(
    private val config: FinalConfig
) : UpdateCallback<Message> {

    override fun invoke(id: Int, message: Message) {
        if (message.chat().id() == config.sourceChatId) {
            message.text() ?. let {
                if (it.startsWith("/")) {
                    return
                }
            }
            if (PostTransactionTable.inTransaction) {
                PostTransactionTable.addMessageId(
                    PostMessage(message)
                )
            } else {
                PostTransactionTable.startTransaction()
                PostTransactionTable.addMessageId(
                    PostMessage(message)
                )
                PostTransactionTable.saveNewPost()
            }
        }
    }
}