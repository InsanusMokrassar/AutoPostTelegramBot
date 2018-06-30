package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.model.Message

private val commandRegex = Regex("^/[^\\s]*")

class OnMessage(
    private val config: FinalConfig,
    private val startPost: StartPost,
    private val fixPost: FixPost,
    private val mostRated: MostRated,
    private val deletePost: DeletePost
) : UpdateCallback<Message> {

    private val commands = mapOf(
        "/startPost" to startPost,
        "/fixPost" to fixPost,
        "/deletePost" to deletePost,
        "/mostRated" to mostRated
    )

    override fun invoke(id: Int, update: IObject<Any>, message: Message) {
        if (message.chat().id().toString() == config.sourceChatId
            || message.chat().username() == config.sourceChatId) {
            message.text() ?. let {
                if (it.startsWith("/")) {
                    val command = commandRegex.find(it) ?. value
                    commands[command] ?. invoke(id, update, message)
                } else {
                    null
                }
            } ?:let {
                if (PostTransactionTable.inTransaction) {
                    PostTransactionTable.addMessageId(
                        PostMessage(message)
                    )
                } else {
                    startPost(id, update, message)
                    PostTransactionTable.addMessageId(
                        PostMessage(message)
                    )
                    fixPost(id, update, message)
                }
            }
        }
    }
}