package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands.FixPost
import com.pengrad.telegrambot.model.Message

private val commandRegex = Regex("^/[^\\s]*")

class OnMessage(
    private val config: FinalConfig,
    private val startPost: StartPost
) : UpdateCallback<Message> {

    private val commands = mapOf(
        "/startPost" to startPost
    )

    override fun invoke(id: Int, message: Message) {
        if (message.chat().id() == config.sourceChatId) {
            message.text() ?. let {
                if (it.startsWith("/")) {
                    val command = commandRegex.find(it) ?. value
                    commands[command] ?. invoke(id, message) ?: return
                } else {
                    null
                }
            } ?:let {
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
}