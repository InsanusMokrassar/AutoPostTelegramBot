package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import kotlinx.coroutines.*
import kotlinx.io.IOException
import java.lang.ref.WeakReference

suspend fun deletePost(
    executor: RequestsExecutor,
    chatId: ChatIdentifier,
    postId: Int,
    postsTable: PostsBaseInfoTable,
    postsMessagesTable: PostsMessagesInfoTable,
    vararg additionalMessagesIdsToDelete: MessageIdentifier
) {
    val messagesToDelete = mutableListOf(
        *postsMessagesTable.getMessagesOfPost(postId).map { it.messageId }.toTypedArray(),
        postsTable.postRegisteredMessage(postId),
        *additionalMessagesIdsToDelete.toTypedArray()
    ).toSet().filterNotNull()

    postsTable.removePost(postId)

    messagesToDelete.forEach { currentMessageToDeleteId ->
        try {
            executor.execute(
                DeleteMessage(
                    chatId,
                    currentMessageToDeleteId
                )
            )
        } catch (e: IOException) {
            executor.sendToLogger(
                e,
                "Deleting of post"
            )
        }
    }
}

val deletePostRegex: Regex = Regex("^deletePost$")

internal fun CoroutineScope.enableDeletingOfPostsCommand(
    botWR: WeakReference<RequestsExecutor>,
    postsTable: PostsBaseInfoTable,
    postsMessagesTable: PostsMessagesInfoTable
): Job = launch {
    buildCommandFlow(
        deletePostRegex
    ).collectWithErrors { message ->
        val bot = botWR.get() ?: return@collectWithErrors
        message.replyTo ?.also {
            val messageId = it.messageId
            try {
                val postId = postsTable.findPost(messageId)
                val chatId = message.chat.id
                deletePost(
                    bot,
                    chatId,
                    postId,
                    postsTable,
                    postsMessagesTable,
                    messageId,
                    message.messageId
                )
            } catch (e: Exception) {
                bot.execute(
                    SendTextMessage(
                        message.chat.id,
                        "Message in reply is not related to any post",
                        parseMode = MarkdownParseMode
                    )
                )
            }
        }
    }
}
