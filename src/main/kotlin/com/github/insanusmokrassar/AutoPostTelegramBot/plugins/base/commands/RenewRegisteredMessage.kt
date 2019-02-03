package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.PostMessagesRegistrant
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.CommandPlugin
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe

class RenewRegisteredMessage(
    private val postMessagesRegistrant: PostMessagesRegistrant
) : CommandPlugin() {
    override val commandRegex: Regex = Regex("^renewRegistered(Message)?$")

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val executor = botWR ?.get() ?: return
        val replyTo = message.replyTo ?: let {
            executor.executeUnsafe(
                SendMessage(
                    message.chat.id,
                    "If you want to renew registered message, you must reply to some of messages of post",
                    replyToMessageId = message.messageId
                )
            )
            return
        }
        val postId = PostsMessagesTable.findPostByMessageId(replyTo.messageId) ?: let {
            executor.executeUnsafe(
                SendMessage(
                    message.chat.id,
                    "Replied message does not match to any post",
                    replyToMessageId = message.messageId
                )
            )
            return
        }
        postMessagesRegistrant.registerPostMessage(
            postId
        ) ?.also {
            executor.executeAsync(
                DeleteMessage(
                    message.chat.id,
                    it
                ),
                {
                    commonLogger.warning(
                        "Can't remove old registered message when renew: $it"
                    )
                }
            )
        }
        executor.executeUnsafe(
            DeleteMessage(
                message.chat.id,
                message.messageId
            )
        )
    }
}