package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.MediaGroupCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostTransactionTable
import com.pengrad.telegrambot.model.Message

class OnMediaGroup(
    private val config: FinalConfig
) : MediaGroupCallback {
    override fun invoke(mediaGroupId: String, updates: List<IObject<Any>>, messages: List<Message>) {
        val first = messages.first()
        if (
            first.chat().id().toString() == config.sourceChatId
            || first.chat().username() == config.sourceChatId
        ) {
            if (PostTransactionTable.inTransaction) {
                messages.forEach {
                    PostTransactionTable.addMessageId(it.messageId(), it.mediaGroupId())
                }
            } else {
                PostTransactionTable.startTransaction(mediaGroupId)
                messages.forEach {
                    PostTransactionTable.addMessageId(it.messageId(), it.mediaGroupId())
                }
                PostTransactionTable.saveWithPostId()
            }
        }
    }
}
