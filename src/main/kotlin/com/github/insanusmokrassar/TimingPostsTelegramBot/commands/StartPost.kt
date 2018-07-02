package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostTransactionTable
import com.pengrad.telegrambot.model.Message

class StartPost : UpdateCallback<Message> {
    override fun invoke(updateId: Int, update: IObject<Any>, message: Message) {
        PostTransactionTable.startTransaction()
    }
}