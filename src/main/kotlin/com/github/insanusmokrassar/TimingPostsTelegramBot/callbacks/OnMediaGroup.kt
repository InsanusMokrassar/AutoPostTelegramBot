package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.MediaGroupCallback
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands.StartPost
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.model.Message

class OnMediaGroup(
    private val config: FinalConfig
) : MediaGroupCallback {
    override fun invoke(mediaGroupId: String, messages: List<Message>) {
        val first = messages.first()
        if (first.chat().id() == config.sourceChatId) {
            if (PostTransactionTable.inTransaction) {
                messages.forEach {
                    PostTransactionTable.addMessageId(
                        PostMessage(
                            it.messageId(),
                            it.mediaGroupId()
                        )
                    )
                }
            } else {
                PostTransactionTable.startTransaction()
                messages.forEach {
                    PostTransactionTable.addMessageId(
                        PostMessage(
                            it.messageId(),
                            it.mediaGroupId()
                        )
                    )
                }
                PostTransactionTable.saveNewPost()
            }
        }
    }
}
