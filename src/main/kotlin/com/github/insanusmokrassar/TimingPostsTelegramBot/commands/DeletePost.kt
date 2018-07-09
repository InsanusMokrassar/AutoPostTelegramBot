package com.github.insanusmokrassar.TimingPostsTelegramBot.commands

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import java.lang.ref.WeakReference

fun deletePost(
    bot: TelegramBot,
    chatId: Long,
    logsChatId: Long,
    postId: Int,
    vararg additionalMessagesIdsToDelete: Int
) {
    val messagesToDelete = mutableListOf(
        *PostsMessagesTable.getMessagesOfPost(postId).map { it.messageId }.toTypedArray(),
        PostsTable.postRegisteredMessage(postId),
        *additionalMessagesIdsToDelete.toTypedArray()
    ).toSet().filterNotNull()

    PostsTable.removePost(postId)

    messagesToDelete.forEach { currentMessageToDeleteId ->
        bot.executeAsync(
            DeleteMessage(
                chatId,
                currentMessageToDeleteId
            ),
            {
                _, ioException ->
                bot.executeAsync(
                    ForwardMessage(
                        logsChatId,
                        chatId,
                        currentMessageToDeleteId
                    ),
                    onResponse = {
                        _, response ->
                        bot.executeAsync(
                            SendMessage(
                                logsChatId,
                                "Can't delete message. Reason:\n```\n${ioException?.message}\n```\n\nPlease, delete manually"
                            ).parseMode(
                                ParseMode.Markdown
                            ).replyToMessageId(
                                response.message().messageId()
                            )
                        )
                    }
                )
            }
        )
    }
}

class DeletePost(
    bot: TelegramBot,
    private val logsChatId: Long
) : UpdateCallback<Message> {
    private val bot = WeakReference(bot)
    override fun invoke(updateId: Int, message: Message) {
        val bot = bot.get() ?: return
        message.replyToMessage() ?.let {
            val messageId = it.messageId() ?: return@let null
            try {
                val postId = PostsTable.findPost(messageId)
                val chatId = message.chat().id()
                deletePost(
                    bot,
                    chatId,
                    logsChatId,
                    postId,
                    messageId,
                    message.messageId()
                )
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
