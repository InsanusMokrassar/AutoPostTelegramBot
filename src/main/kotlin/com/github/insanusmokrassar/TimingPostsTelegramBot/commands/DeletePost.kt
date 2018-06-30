package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.refreshRegisteredMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class DeletePost(
    bot: TelegramBot
) : UpdateCallback<Message> {
    private val bot = WeakReference(bot)
    override fun invoke(updateId: Int, update: IObject<Any>, message: Message) {
        val bot = bot.get() ?: return
        message.replyToMessage() ?.let {
            val messageId = it.messageId() ?: return@let null
            try {
                val postId = PostsTable.findPost(messageId)
                val chatId = message.chat().id()
                val messagesToDelete = mutableListOf(
                    *PostsMessagesTable.getMessagesOfPost(postId).map { it.messageId }.toTypedArray(),
                    PostsTable.postRegisteredMessage(postId),
                    message.messageId()
                )

                PostsTable.removePost(postId)

                messagesToDelete.filterNotNull().forEach {
                    bot.executeAsync(
                        DeleteMessage(
                            chatId,
                            it
                        )
                    )
                }
            } catch (e: Exception) {
                bot.executeAsync(
                    SendMessage(
                        message.chat().id(),
                        "Message in reply is not related to any post"
                    ).parseMode(
                        ParseMode.Markdown
                    )
                )
            }
        }
    }
}
