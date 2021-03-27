package dev.inmo.AutoPostTelegramBot.plugins.base.callbacks

import dev.inmo.AutoPostTelegramBot.base.models.PostMessage
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.checkedMediaGroupsFlow
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.PostsTransactions
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.extensions.utils.shortcuts.chat
import kotlinx.coroutines.*

internal fun CoroutineScope.enableOnMediaGroupsCallback(
    postsTransactions: PostsTransactions
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
        val messages = it.data
        val id = it.chat.id
        println(postsTransactions[id])
        postsTransactions.doInTransaction(id) {
            messages.forEach { message ->
                it.addMessageId(PostMessage(message))
            }
        }
    }
}
