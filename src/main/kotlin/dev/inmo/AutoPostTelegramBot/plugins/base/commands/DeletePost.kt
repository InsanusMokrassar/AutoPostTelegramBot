package dev.inmo.AutoPostTelegramBot.plugins.base.commands

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.utils.commands.buildCommandFlow
import dev.inmo.AutoPostTelegramBot.utils.extensions.sendToLogger
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.ParseMode.MarkdownParseMode
import kotlinx.coroutines.*
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
        executor.executeUnsafe(
            DeleteMessage(
                chatId,
                currentMessageToDeleteId
            )
        ) {
            it.forEach { e ->
                executor.sendToLogger(
                    e,
                    "Deleting of post"
                )
            }
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
