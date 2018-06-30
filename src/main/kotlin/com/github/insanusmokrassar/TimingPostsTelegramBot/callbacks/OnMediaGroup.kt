package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.MediaGroupCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.FixPost
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.StartPost
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.model.Message

class OnMediaGroup(
    private val config: FinalConfig,
    private val startPost: StartPost,
    private val fixPost: FixPost
) : MediaGroupCallback {
    override fun invoke(mediaGroupId: String, updates: List<IObject<Any>>, messages: List<Message>) {
        val first = messages.first()
        if (
            first.chat().id().toString() == config.sourceChatId
            || first.chat().username() == config.sourceChatId
        ) {
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
                startPost(-1, updates.first(), messages.first())
                messages.forEach {
                    PostTransactionTable.addMessageId(
                        PostMessage(
                            it.messageId(),
                            it.mediaGroupId()
                        )
                    )
                }
                fixPost(-1, updates.last(), messages.last())
            }
        }
    }
}
