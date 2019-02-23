package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.refreshRegisteredMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.AbleToReplyMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import java.lang.ref.WeakReference

class EnableRating(
    executor: RequestsExecutor,
    private val postsLikesMessagesTable: PostsLikesMessagesTable,
    private val postsLikesTable: PostsLikesTable
    ) : Command() {
    override val commandRegex: Regex = Regex("enableRating")
    private val executorWR = WeakReference(executor)

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val executor = executorWR.get() ?: return
        val replied = (message as? AbleToReplyMessage) ?.replyTo ?: return // TODO:: Add sending tooltip

        val postId = PostsTable.findPost(replied.messageId)
        refreshRegisteredMessage(
            replied.chat.id,
            executor,
            postId,
            postsLikesTable,
            postsLikesMessagesTable
        )

        commonLogger.info("Rating was enabled")
    }
}