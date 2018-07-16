package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*

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

class DeletePost : Command() {
    override val version: PluginVersion = 0L
    override val commandRegex: Regex = Regex("^/deletePost$")

    private var logsChatId: Long? = null

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        super.onInit(bot, baseConfig, pluginManager)

        logsChatId = baseConfig.logsChatId
    }

    override fun onCommand(updateId: Int, message: Message) {
        val bot = botWR ?.get() ?: return
        message.replyToMessage() ?.let {
            val messageId = it.messageId() ?: return@let null
            try {
                val postId = PostsTable.findPost(messageId)
                val chatId = message.chat().id()
                deletePost(
                    bot,
                    chatId,
                    logsChatId ?: return,
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
