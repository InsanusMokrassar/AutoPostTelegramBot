package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.publishers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands.deletePost
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.forwarders.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.*
import java.lang.ref.WeakReference

private typealias ChatIdMessageIdPair = Pair<Long, Int>

fun makeMapOfExecution(
    messageToPost: List<PostMessage>,
    forwardersList: List<Forwarder>
): List<Pair<Forwarder, List<PostMessage>>> {
    val mapOfExecution = mutableListOf<Pair<Forwarder, MutableList<PostMessage>>>()

    var forwarder: Forwarder? = null

    messageToPost.forEach { message ->
        if (forwarder?.canForward(message) != true) {
            val iterator = forwardersList.iterator()
            while (forwarder?.canForward(message) != true) {
                if (!iterator.hasNext()) {
                    return@forEach
                }
                forwarder = iterator.next()
            }
        }
        if (mapOfExecution.lastOrNull()?.first != forwarder) {
            forwarder?.let {
                mapOfExecution.add(
                    it to mutableListOf()
                )
            }
        }
        mapOfExecution.last().second.add(message)
    }

    return mapOfExecution
}

class PostPublisher : Publisher {
    override val version: PluginVersion = 0L

    private var botWR: WeakReference<TelegramBot>? = null

    private var sourceChatId: Long? = null
    private var targetChatId: Long? = null
    private var logsChatId: Long? = null
    private var forwardersList: List<Forwarder> = emptyList()

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(bot)

        sourceChatId = baseConfig.sourceChatId
        targetChatId = baseConfig.targetChatId
        logsChatId = baseConfig.logsChatId
        forwardersList = (pluginManager.plugins.firstOrNull {
            it is ForwardersPlugin
        } as? ForwardersPlugin) ?.forwarders ?: emptyList()
    }

    override fun publishPost(postId: Int) {
        val bot = botWR ?.get() ?: return
        val sourceChatId: Long = sourceChatId ?: return
        val targetChatId: Long = targetChatId ?: return
        val logsChatId: Long = logsChatId ?: return

        val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

        try {
            bot.execute(
                SendMessage(
                    logsChatId,
                    "Start post"
                )
            )?.message() ?.let {
                messagesToDelete.add(it.chat().id() to it.messageId())
            }

            val messageToPost = PostsMessagesTable.getMessagesOfPost(postId).also {
                if (it.isEmpty()) {
                    PostsTable.removePost(postId)
                    return
                }
                it.forEach {
                    message ->
                    messagesToDelete.add(sourceChatId to message.messageId)

                    bot.execute(
                        ForwardMessage(
                            logsChatId,
                            sourceChatId,
                            message.messageId
                        ).disableNotification(
                            true
                        )
                    ) ?.message() ?.also {
                        messagesToDelete.add(it.chat().id() to it.messageId())
                        message.message = it
                    }
                }
            }

            val mapOfExecution = makeMapOfExecution(
                messageToPost,
                forwardersList
            )

            mapOfExecution.flatMap {
                it.first.forward(
                    bot,
                    targetChatId,
                    *it.second.toTypedArray()
                )
            }.let {
                it.forEach {
                    bot.execute(
                        ForwardMessage(
                            logsChatId,
                            targetChatId,
                            it
                        )
                    )
                }
            }

            deletePost(
                bot,
                sourceChatId,
                logsChatId,
                postId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bot.executeAsync(
                SendMessage(
                    sourceChatId,
                    "Can't publish post:\n```$e```"
                ).parseMode(
                    ParseMode.Markdown
                )
            )
        } finally {
            messagesToDelete.forEach {
                bot.executeAsync(
                    DeleteMessage(
                        it.first,
                        it.second
                    )
                )
            }
        }
    }
}