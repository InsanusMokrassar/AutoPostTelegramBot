package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.ContentMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.Message
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.ref.WeakReference

typealias PostIdListPostMessagesTelegramMessages = Pair<Int, Map<PostMessage, Message>>
private typealias ChatIdMessageIdPair = Pair<ChatId, MessageIdentifier>

@Serializable
class PostPublisher : Publisher {
    @Transient
    val postPublishedChannel = BroadcastChannel<PostIdListPostMessagesTelegramMessages>(
        Channel.CONFLATED
    )

    @Transient
    private var botWR: WeakReference<RequestsExecutor>? = null

    @Transient
    private var sourceChatId: ChatId? = null
    @Transient
    private var targetChatId: ChatId? = null
    @Transient
    private var logsChatId: ChatId? = null

    @Transient
    private var publishPostCommand: PublishPost? = null

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(executor).also {
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
    }

    override suspend fun publishPost(postId: Int) {
        val executor = botWR ?.get() ?: return
        val sourceChatId: ChatId = sourceChatId ?: return
        val targetChatId: ChatId = targetChatId ?: return
        val logsChatId: ChatId = logsChatId ?: return

        val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

        try {
            executor.execute(
                SendMessage(
                    logsChatId,
                    "Start post"
                )
            ).asMessage.let {
                messagesToDelete.add(it.chat.id to it.messageId)
            }

            val messageToPost = PostsMessagesTable.getMessagesOfPost(postId).also {
                if (it.isEmpty()) {
                    PostsTable.removePost(postId)
                    return
                }
                it.forEach {
                    message ->
                    try {
                        executor.execute(
                            ForwardMessage(
                                sourceChatId,
                                logsChatId,
                                message.messageId,
                                disableNotification = true
                            )
                        ).asMessage.also {
                            messagesToDelete.add(it.chat.id to it.messageId)
                            message.message = it
                        }
                    } catch (e: Exception) {
                        commonLogger.warning(
                            "Can't forward message with id: ${message.messageId}"
                        )
                    }
                }
            }

            messageToPost.mapNotNull {
                (it.message as? ContentMessage<*>) ?.content ?.createResend(
                    targetChatId
                ) ?.let { request ->
                    it to request
                }
            }.map {
                it.first to executor.execute(it.second).asMessage
            }.also {
                it.forEach { (postMessage, message) ->
                    try {
                        executor.execute(
                            ForwardMessage(
                                message.chat.id,
                                logsChatId,
                                message.messageId
                            )
                        )
                    } catch (e: Exception) {
                        commonLogger.warning(
                            "Can't forward message with id: ${message.messageId}"
                        )
                    }
                }
                postPublishedChannel.send(
                    PostIdListPostMessagesTelegramMessages(
                        postId,
                        it.toMap()
                    )
                )
            }

            deletePost(
                executor,
                sourceChatId,
                logsChatId,
                postId
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            commonLogger.throwing(
                name,
                "Trying to publish",
                e
            )
        } finally {
            messagesToDelete.forEach {
                executor.executeAsync(
                    DeleteMessage(
                        it.first,
                        it.second
                    )
                )
            }
        }
    }
}