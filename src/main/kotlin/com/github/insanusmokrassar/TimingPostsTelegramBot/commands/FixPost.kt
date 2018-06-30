package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.refreshRegisteredMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import java.lang.ref.WeakReference

class FixPost(
    bot: TelegramBot
) : UpdateCallback<Message> {
    private val bot = WeakReference(bot)
    override fun invoke(updateId: Int, update: IObject<Any>, message: Message) {
        val postId = PostsTable.allocatePost()
        PostTransactionTable.saveWithPostId(postId)

        refreshRegisteredMessage(
            message.chat(),
            postId,
            bot.get() ?: return
        )
    }
}
