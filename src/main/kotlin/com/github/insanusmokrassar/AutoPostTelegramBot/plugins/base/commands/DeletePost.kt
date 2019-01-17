package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.*
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.*
import kotlinx.io.IOException
import java.lang.ref.WeakReference

suspend fun deletePost(
    executor: RequestsExecutor,
    chatId: ChatIdentifier,
    logsChatId: ChatIdentifier,
    postId: Int,
    vararg additionalMessagesIdsToDelete: MessageIdentifier
) {
    val messagesToDelete = mutableListOf(
        *PostsMessagesTable.getMessagesOfPost(postId).map { it.messageId }.toTypedArray(),
        PostsTable.postRegisteredMessage(postId),
        *additionalMessagesIdsToDelete.toTypedArray()
    ).toSet().filterNotNull()

    PostsTable.removePost(postId)

    messagesToDelete.forEach { currentMessageToDeleteId ->
        try {
            executor.execute(
                DeleteMessage(
                    chatId,
                    currentMessageToDeleteId
                )
            )
        } catch (e: IOException) {
            executor.execute(
                ForwardMessage(
                    chatId,
                    logsChatId,
                    currentMessageToDeleteId
                )
            )
            commonLogger.warning(
                "Can't delete message. Reason:\n```\n${e.message}\n```\n\nPlease, delete manually"
            )
        }
    }
}

class DeletePost(
    private val logsChatId: ChatIdentifier,
    private val botWR: WeakReference<RequestsExecutor>
) : Command() {
    override val commandRegex: Regex = Regex("^/deletePost$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val bot = botWR.get() ?: return
        (message as? AbleToReplyMessage) ?.replyTo ?.also {
            val messageId = it.messageId
            try {
                val postId = PostsTable.findPost(messageId)
                val chatId = message.chat.id
                deletePost(
                    bot,
                    chatId,
                    logsChatId,
                    postId,
                    messageId,
                    message.messageId
                )
            } catch (e: Exception) {
                bot.execute(
                    SendMessage(
                        message.chat.id,
                        "Message in reply is not related to any post",
                        parseMode = MarkdownParseMode
                    )
                )
            }
        }
    }
}
