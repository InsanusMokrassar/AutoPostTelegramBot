package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.pengrad.telegrambot.model.Message

class StartPost : UpdateCallback<Message> {
    override fun invoke(updateId: Int, message: Message) {
        PostTransactionTable.startTransaction()
    }
}