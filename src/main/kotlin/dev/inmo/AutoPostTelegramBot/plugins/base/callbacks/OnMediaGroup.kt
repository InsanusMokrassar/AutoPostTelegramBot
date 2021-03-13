package dev.inmo.AutoPostTelegramBot.plugins.base.callbacks

import dev.inmo.AutoPostTelegramBot.base.database.PostTransaction
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.models.PostMessage
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.checkedMediaGroupsFlow
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMediaGroupsCallback(
    postsTable: PostsBaseInfoTable,
    postsMessagesTable: PostsMessagesInfoTable
): Job = launch {
    checkedMediaGroupsFlow.collectWithErrors(
        { update, e ->
            commonLogger.throwing(
                "Media groups AutoPost callback",
                "Perform update: $update",
                e
            )
        }
    ) {
        println(it)
        val messages = it.data
        val id = messages.first().chat.id
        CommonKnownPostsTransactions[id] ?.also {
            messages.forEach { message ->
                it.addMessageId(PostMessage(message))
            }
            return@collectWithErrors
        } ?:also {
            PostTransaction(
                postsTable,
                postsMessagesTable
            ).use { transaction ->
                messages.forEach { message ->
                    transaction.addMessageId(PostMessage(message))
                }
            }
        }
    }
}
