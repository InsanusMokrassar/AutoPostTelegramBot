package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders.Forwarder
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders.ForwardersPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeSync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.*
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

typealias PostIdListPostMessagesTelegramMessages = Pair<Int, Map<PostMessage, Message>>
private typealias ChatIdMessageIdPair = Pair<Long, Int>
private const val subscribeMaxCount = 256

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
    val postPublishedChannel = BroadcastChannel<PostIdListPostMessagesTelegramMessages>(
        subscribeMaxCount
    )

    private var botWR: WeakReference<TelegramBot>? = null

    private var sourceChatId: Long? = null
    private var targetChatId: Long? = null
    private var logsChatId: Long? = null
    private var forwardersList: List<Forwarder> = emptyList()

    private var publishPostCommand: PublishPost? = null

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(bot).also {
            publishPostCommand = PublishPost(
                pluginManager.plugins.firstOrNull { it is Chooser } as? Chooser,
                pluginManager.plugins.firstOrNull { it is Publisher } as Publisher,
                it,
                baseConfig.logsChatId
            )
        }

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
            bot.executeSync(
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
                    bot.executeSync(
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

            mapOfExecution.map {
                it.first.forward(
                    bot,
                    targetChatId,
                    *it.second.toTypedArray()
                )
            }.let {
                it.flatMap {
                    it.map {
                        it.value
                    }
                }.forEach {
                    bot.executeSync(
                        ForwardMessage(
                            logsChatId,
                            it.chat().id(),
                            it.messageId()
                        )
                    )
                }
                val resultMap = mutableMapOf<PostMessage, Message>()
                it.forEach {
                    resultMap.putAll(it)
                }
                launch {
                    postPublishedChannel.send(
                        postId to resultMap
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
            commonLogger.throwing(
                name,
                "Trying to publish",
                e
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