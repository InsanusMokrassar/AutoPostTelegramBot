package dev.inmo.AutoPostTelegramBot.plugins.base.commands

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.plugins.base.PostMessagesRegistrant
import dev.inmo.AutoPostTelegramBot.utils.commands.CommandPlugin
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.UpdateIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage

val renewRegisteredMessageRegex: Regex = Regex("^renewRegistered(Message)?$")

class RenewRegisteredMessage(
    private val postMessagesRegistrant: PostMessagesRegistrant,
    private val postsMessagesTable: PostsMessagesInfoTable
) : CommandPlugin() {
    override val commandRegex: Regex = renewRegisteredMessageRegex

    override suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>) {
        val executor = botWR ?.get() ?: return
        val replyTo = message.replyTo ?: let {
            executor.executeUnsafe(
                SendTextMessage(
                    message.chat.id,
                    "If you want to renew registered message, you must reply to some of messages of post",
                    replyToMessageId = message.messageId
                )
            )
            return
        }
        val postId = postsMessagesTable.findPostByMessageId(replyTo.messageId) ?: let {
            executor.executeUnsafe(
                SendTextMessage(
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
            executor.executeUnsafe(
                DeleteMessage(
                    message.chat.id,
                    it
                )
            ) {
                commonLogger.warning(
                    "Can't remove old registered message when renew: $it"
                )
            }
        }
        executor.executeUnsafe(
            DeleteMessage(
                message.chat.id,
                message.messageId
            )
        )
    }
}